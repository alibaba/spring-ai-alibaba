/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.streaming.RunEvent.Channel;
import com.alibaba.cloud.ai.graph.streaming.RunEvent.Data;
import com.alibaba.cloud.ai.graph.streaming.RunEvent.PathSegment;
import com.alibaba.cloud.ai.graph.streaming.RunEvent.Scope;
import com.alibaba.cloud.ai.graph.streaming.RunEvent.ScopeKind;
import com.alibaba.cloud.ai.graph.streaming.RunEvent.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** Runtime event scope propagated through graph, node and tool calls. */
public final class RunEventContext {

	private final Delivery delivery;

	private final Scope scope;

	private final Candidate candidate;

	private final int nesting;

	private final AtomicBoolean closed;

	private RunEventContext(Delivery delivery, Scope scope, Candidate candidate, int nesting) {
		this(delivery, scope, candidate, nesting, new AtomicBoolean());
	}

	private RunEventContext(Delivery delivery, Scope scope, Candidate candidate, int nesting, AtomicBoolean closed) {
		this.delivery = delivery;
		this.scope = scope;
		this.candidate = candidate;
		this.nesting = nesting;
		this.closed = closed;
	}

	public static Flux<RunEvent> capture(StreamOptions options,
			Function<RunEventContext, ? extends Publisher<?>> execution) {
		return Flux.defer(() -> {
			var delivery = new Delivery(options);
			var initial = new RunEventContext(delivery, new Scope(List.of(), null, null, null, null), null, -1);
			Mono<RunEvent> run = Flux.defer(() -> Flux.from(execution.apply(initial))).then()
					.doOnSuccess(ignored -> delivery.finish())
					.doOnError(delivery::executionFailed)
					.onErrorComplete()
					.doFinally(ignored -> delivery.close()).then(Mono.empty());
			return Flux.merge(delivery.events.asFlux().doOnNext(ignored -> delivery.drained())
					.concatWith(delivery.terminal.asMono()), run);
		});
	}

	public RunEventContext child(ScopeKind kind, String name) {
		var path = new ArrayList<>(scope.namespace());
		String id = kind == ScopeKind.INLINE_GRAPH
				? UUID.nameUUIDFromBytes((scope.scopeId() + "/" + name).getBytes(StandardCharsets.UTF_8)).toString()
				: UUID.randomUUID().toString();
		path.add(new PathSegment(kind, Objects.toString(name, ""), id));
		boolean node = kind == ScopeKind.NODE;
		int depth = kind == ScopeKind.GRAPH || kind == ScopeKind.AGENT ? nesting + 1 : nesting;
		return new RunEventContext(delivery, new Scope(path, node ? name : scope.nodeId(),
				node ? id : scope.nodeExecutionId(), scope.taskId(), scope.attempt()), candidate, depth);
	}

	public RunEventContext node(String name, String taskId) {
		var child = child(ScopeKind.NODE, name);
		return new RunEventContext(delivery, new Scope(child.scope.namespace(), name, child.scope.nodeExecutionId(),
				taskId, null), candidate, nesting);
	}

	public RunEventContext attempt(int attempt) {
		return new RunEventContext(delivery, new Scope(scope.namespace(), scope.nodeId(), scope.nodeExecutionId(),
				scope.taskId(), attempt), candidate, nesting, closed);
	}

	public RunEventContext candidate() {
		return new RunEventContext(delivery, scope, new Candidate(this), nesting);
	}

	public Mono<Void> publishCandidate() {
		return candidate == null ? Mono.empty() : candidate.publish();
	}

	public void candidateStatus(RunEvent.CandidateStatus.Phase phase) {
		if (candidate != null && options().includeCandidateLifecycle() && enabled(Channel.LIFECYCLE)) {
			var status = new Pending(Type.CANDIDATE, scope, new RunEvent.CandidateStatus(phase));
			if (candidate.parent.candidate == null) {
				delivery.publish(status);
			}
			else {
				candidate.parent.candidate.accept(status);
			}
		}
	}

	public Scope scope() {
		return scope;
	}

	public StreamOptions options() {
		return delivery.options;
	}

	public boolean enabled(Channel channel) {
		return (nesting <= 0 || options().includeNested()) && options().channels().contains(channel);
	}

	public StreamWriter writer() {
		return (name, value) -> emit(Type.CUSTOM, new RunEvent.Custom(Objects.requireNonNull(name), value));
	}

	public void completed(Map<String, Object> state) {
		if (scope.root() || enabled(Channel.LIFECYCLE)) {
			emit(Type.RUN_COMPLETED, new RunEvent.State(state));
		}
	}

	public void stateCommitted(Map<String, Object> update, Map<String, Object> state) {
		stateCommitted(List.of(update), state);
	}

	public void stateCommitted(List<Map<String, Object>> writes, Map<String, Object> state) {
		if (enabled(Channel.UPDATES)) {
			var changes = writes.stream().filter(write -> !write.isEmpty()).toList();
			if (!changes.isEmpty()) {
				emit(Type.STATE_UPDATE, new RunEvent.StateUpdate(changes));
			}
		}
		if (enabled(Channel.VALUES)) {
			emit(Type.STATE_VALUE, new RunEvent.State(state));
		}
	}

	public static RunEvent.Failure failure(Throwable error) {
		return new RunEvent.Failure(error.getClass().getSimpleName(), error.getMessage());
	}

	public StreamWriter.Result emit(Type type, Data data) {
		if (closed.get()) {
			return StreamWriter.Result.CLOSED;
		}
		boolean terminal = scope.root() && switch (type) {
			case RUN_COMPLETED, RUN_FAILED, RUN_PAUSED, RUN_CANCELLED -> true;
			default -> false;
		};
		if (!terminal && !enabled(type.channel())) {
			return StreamWriter.Result.DISABLED;
		}
		var event = new Pending(type, scope, data);
		if (terminal) {
			delivery.terminate(event);
			return StreamWriter.Result.ACCEPTED;
		}
		return candidate == null ? delivery.publish(event) : candidate.accept(event);
	}

	/** Waits for capacity without blocking a reactive producer's thread. */
	public Mono<Void> emitReactive(Type type, Data data) {
		if (closed.get() || !enabled(type.channel())) {
			return Mono.empty();
		}
		var event = new Pending(type, scope, data);
		if (candidate != null) {
			candidate.accept(event);
			return Mono.empty();
		}
		return delivery.publishReactive(event);
	}

	public void close() {
		closed.set(true);
	}

	private record Pending(Type type, Scope scope, Data data) {
	}

	private static final class Delivery {

		private final StreamOptions options;
		private final String runId = UUID.randomUUID().toString();
		private final ArrayBlockingQueue<RunEvent> queue;
		private final Sinks.Many<RunEvent> events;
		private final Sinks.One<RunEvent> terminal = Sinks.one();
		private Sinks.Empty<Void> capacity;
		private long sequence;
		private long omitted;
		private boolean closed;
		private Pending outcome;

		private Delivery(StreamOptions options) {
			this.options = options;
			// Reserve space for the control events emitted while a data producer waits.
			this.queue = new ArrayBlockingQueue<>(Math.addExact(options.bufferCapacity(), 16));
			this.events = Sinks.many().unicast().onBackpressureBuffer(queue);
		}

		private synchronized StreamWriter.Result publish(Pending pending) {
			if (closed) {
				return StreamWriter.Result.CLOSED;
			}
			var result = pending.type() == Type.CUSTOM && queue.size() >= options.bufferCapacity()
					? Sinks.EmitResult.FAIL_OVERFLOW : push(pending);
			if (result == Sinks.EmitResult.OK) {
				return StreamWriter.Result.ACCEPTED;
			}
			if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
				if (pending.type() == Type.CUSTOM) {
					omitted++;
					return StreamWriter.Result.OVERFLOW;
				}
				fail(new IllegalStateException("Run event subscriber exceeded its delivery capacity"));
				return StreamWriter.Result.OVERFLOW;
			}
			return StreamWriter.Result.CLOSED;
		}

		private synchronized Mono<Void> publishReactive(Pending pending) {
			if (closed) {
				return Mono.empty();
			}
			if (queue.size() < options.bufferCapacity()) {
				var result = push(pending);
				if (result != Sinks.EmitResult.FAIL_OVERFLOW) {
					return Mono.empty();
				}
			}
			if (capacity == null) {
				capacity = Sinks.empty();
			}
			return capacity.asMono().then(Mono.defer(() -> publishReactive(pending)));
		}

		private void drained() {
			Sinks.Empty<Void> ready;
			synchronized (this) {
				ready = capacity;
				capacity = null;
			}
			if (ready != null) {
				ready.tryEmitEmpty();
			}
		}

		private Sinks.EmitResult push(Pending pending) {
			var result = events.tryEmitNext(event(pending, ++sequence));
			if (result != Sinks.EmitResult.OK) {
				sequence--;
			}
			return result;
		}

		private synchronized void terminate(Pending pending) {
			if (outcome == null) {
				outcome = pending;
			}
		}

		private synchronized void finish() {
			if (closed) {
				return;
			}
			closed = true;
			if (outcome != null && omitted > 0) {
				push(new Pending(Type.TRUNCATED, outcome.scope(), new RunEvent.Truncated(omitted)));
			}
			if (outcome == null) {
				terminal.tryEmitEmpty();
			}
			else {
				terminal.tryEmitValue(event(outcome, ++sequence));
			}
			events.tryEmitComplete();
		}

		private synchronized void fail(Throwable error) {
			if (!closed) {
				closed = true;
				events.tryEmitError(error);
			}
		}

		private synchronized void executionFailed(Throwable error) {
			if (outcome != null) {
				finish();
			}
			else {
				fail(error);
			}
		}

		private synchronized void close() {
			closed = true;
			drained();
		}

		private RunEvent event(Pending pending, long number) {
			return new RunEvent(1, runId, number, pending.type(), pending.scope(), pending.data());
		}
	}

	private static final class Candidate {

		private final RunEventContext parent;
		private final ArrayDeque<Pending> events = new ArrayDeque<>();
		private long omitted;
		private long bytes;

		private Candidate(RunEventContext parent) {
			this.parent = parent;
		}

		private synchronized StreamWriter.Result accept(Pending event) {
			long weight = weight(event);
			if (!transientData(event.type()) && weight <= parent.options().candidateByteBudget()) {
				while (full(weight)) {
					var disposable = events.stream().filter(queued -> transientData(queued.type())).findFirst();
					if (disposable.isEmpty()) {
						break;
					}
					var discarded = disposable.get();
					events.remove(discarded);
					bytes -= weight(discarded);
					omitted++;
				}
			}
			if (full(weight)) {
				omitted++;
				return StreamWriter.Result.OVERFLOW;
			}
			events.add(event);
			bytes += weight;
			return StreamWriter.Result.ACCEPTED;
		}

		private boolean full(long weight) {
			return events.size() >= parent.options().bufferCapacity()
					|| bytes + weight > parent.options().candidateByteBudget();
		}

		private synchronized Mono<Void> publish() {
			var selected = new ArrayList<>(events);
			events.clear();
			if (omitted > 0) {
				selected.add(new Pending(Type.TRUNCATED, parent.scope, new RunEvent.Truncated(omitted)));
			}
			return Flux.fromIterable(selected).concatMap(this::forward, 1).then();
		}

		private Mono<Void> forward(Pending event) {
			if (parent.candidate == null) {
				return parent.delivery.publishReactive(event);
			}
			return Mono.fromRunnable(() -> parent.candidate.accept(event));
		}

		private static boolean transientData(Type type) {
			return type == Type.MODEL_DELTA || type == Type.TOOL_DELTA || type == Type.CUSTOM || type == Type.ATTEMPT;
		}

		private static long weight(Pending event) {
			if (event.data() instanceof RunEvent.ModelDelta delta) {
				return 64L + size(delta.text()) + size(delta.reasoning())
						+ (delta.toolArgument() == null ? 0 : size(delta.toolArgument().arguments()));
			}
			if (event.data() instanceof RunEvent.ModelCompleted completed) {
				return 128L + size(completed.message().getText())
						+ completed.message().getToolCalls().stream().mapToLong(call -> size(call.arguments())).sum();
			}
			return 64L + size(event.data());
		}

		private static long size(Object value) {
			return value == null ? 0 : 4L * value.toString().length();
		}
	}
}
