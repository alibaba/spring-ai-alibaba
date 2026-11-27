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

import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Channel;
import static com.alibaba.cloud.ai.graph.streaming.RunEvent.ScopeKind;
import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Type;

/**
 * An explicitly started invocation. Subscribe to the live views before calling
 * {@link #start()}. The root outcome remains available through {@link #output()}.
 */
public final class RunStream implements AutoCloseable {

	private final Flux<RunEvent> source;

	private final int capacity;

	private final List<Observer> observers = new ArrayList<>();

	private final ArrayDeque<RunEvent> history = new ArrayDeque<>();

	private final Sinks.One<RunEvent> output = Sinks.one();

	private final AtomicBoolean started = new AtomicBoolean();

	private final Disposable.Swap subscription = Disposables.swap();

	private boolean closed;

	private Throwable failure;

	public RunStream(Flux<RunEvent> source, int capacity) {
		this.source = source;
		this.capacity = capacity;
	}

	public void start() {
		if (started.compareAndSet(false, true) && !subscription.isDisposed()) {
			source.subscribe(new BaseSubscriber<>() {
				@Override
				protected void hookOnSubscribe(Subscription upstream) {
					if (subscription.update(this)) {
						requestUnbounded();
					}
				}

				@Override
				protected void hookOnNext(RunEvent event) {
					publish(event);
				}

				@Override
				protected void hookOnError(Throwable error) {
					finish(error);
				}

				@Override
				protected void hookOnComplete() {
					finish(null);
				}
			});
		}
	}

	public Flux<RunEvent> events() {
		return observe(event -> true, null);
	}

	private Flux<RunEvent> observe(Predicate<RunEvent> filter, Long afterSequence) {
		return Flux.defer(() -> {
			var sink = Sinks.many().unicast()
					.<RunEvent>onBackpressureBuffer(new ArrayBlockingQueue<>(capacity));
			var observer = new Observer(sink, filter);
			synchronized (this) {
				if (afterSequence != null) {
					var first = history.peekFirst();
					if (first != null && first.sequence() > afterSequence + 1) {
						return Flux.error(new IllegalStateException("Run projection is outside the retained event window"));
					}
					history.stream().filter(event -> event.sequence() > afterSequence).filter(filter).forEach(sink::tryEmitNext);
				}
				if (closed) {
					if (failure == null) {
						sink.tryEmitComplete();
					}
					else {
						sink.tryEmitError(failure);
					}
				}
				else {
					observers.add(observer);
				}
			}
			return sink.asFlux().publishOn(Schedulers.parallel(), 1).doFinally(signal -> remove(observer));
		});
	}

	public Flux<RunEvent> events(String scopeId) {
		return observe(event -> inScope(event, scopeId), null);
	}

	public Flux<RunEvent> messages() {
		return observe(event -> event.type().channel() == Channel.MESSAGES, null);
	}

	public Flux<RunEvent> toolCalls() {
		return observe(event -> event.type().channel() == Channel.TOOLS, null);
	}

	public Flux<RunEvent> values() {
		return observe(event -> event.type() == Type.STATE_VALUE, null);
	}

	public Flux<ScopedRun> subgraphs() {
		return scopes(ScopeKind.GRAPH, ScopeKind.AGENT);
	}

	public Flux<ScopedRun> subagents() {
		return scopes(ScopeKind.AGENT);
	}

	private Flux<ScopedRun> scopes(ScopeKind... kinds) {
		var selected = List.of(kinds);
		return observe(event -> !event.scope().root() && isScopeStart(event, selected), null)
				.map(ScopedRun::new);
	}

	private static boolean isScopeStart(RunEvent event, List<ScopeKind> kinds) {
		var path = event.scope().namespace();
		return event.type() == Type.RUN_STARTED && kinds.contains(path.get(path.size() - 1).kind());
	}

	public Mono<RunEvent> output() {
		return output.asMono();
	}

	private synchronized void publish(RunEvent event) {
		if (closed) {
			return;
		}
		if (history.size() == capacity) {
			history.removeFirst();
		}
		history.addLast(event);
		for (var observer : List.copyOf(observers)) {
			if (observer.filter().test(event) && observer.sink().tryEmitNext(event) == Sinks.EmitResult.FAIL_OVERFLOW) {
				remove(observer);
				observer.sink().tryEmitError(new IllegalStateException("Run projection fell behind the live event stream"));
			}
		}
		if (event.scope().root() && terminal(event)) {
			output.tryEmitValue(event);
		}
	}

	private synchronized void remove(Observer observer) {
		observers.remove(observer);
	}

	private synchronized void finish(Throwable error) {
		if (closed) {
			return;
		}
		closed = true;
		failure = error;
		for (var observer : List.copyOf(observers)) {
			if (error == null) {
				observer.sink().tryEmitComplete();
			}
			else {
				observer.sink().tryEmitError(error);
			}
		}
		observers.clear();
		output.tryEmitError(error == null ? new IllegalStateException("The run produced no outcome") : error);
	}

	@Override
	public void close() {
		finish(new CancellationException("Run cancelled"));
		subscription.dispose();
	}

	private static boolean inScope(RunEvent event, String id) {
		return event.scope().namespace().stream().anyMatch(segment -> segment.id().equals(id));
	}

	private static boolean terminal(RunEvent event) {
		return switch (event.type()) {
			case RUN_COMPLETED, RUN_PAUSED, RUN_FAILED, RUN_CANCELLED -> true;
			default -> false;
		};
	}

	private record Observer(Sinks.Many<RunEvent> sink, Predicate<RunEvent> filter) {
	}

	/** A nested invocation discovered in the live run. Its events share the root run ID. */
	public final class ScopedRun {

		private final RunEvent start;

		private ScopedRun(RunEvent start) {
			this.start = start;
		}

		public RunEvent.Scope scope() {
			return start.scope();
		}

		public Flux<RunEvent> events() {
			return observe(event -> inScope(event, scope().scopeId()), start.sequence() - 1)
					.takeUntil(event -> terminal(event) && event.scope().scopeId().equals(scope().scopeId()));
		}

		public Flux<RunEvent> messages() {
			return events().filter(event -> event.type().channel() == Channel.MESSAGES);
		}

		public Flux<RunEvent> toolCalls() {
			return events().filter(event -> event.type().channel() == Channel.TOOLS);
		}

		public Flux<RunEvent> values() {
			return events().filter(event -> event.type() == Type.STATE_VALUE);
		}

		public Flux<ScopedRun> subgraphs() {
			return nested(ScopeKind.GRAPH, ScopeKind.AGENT);
		}

		public Flux<ScopedRun> subagents() {
			return nested(ScopeKind.AGENT);
		}

		private Flux<ScopedRun> nested(ScopeKind... kinds) {
			var selected = List.of(kinds);
			return events().filter(event -> !event.scope().scopeId().equals(scope().scopeId())
					&& isScopeStart(event, selected)).map(ScopedRun::new);
		}

		public Mono<RunEvent> output() {
			return events().filter(event -> terminal(event) && event.scope().scopeId().equals(scope().scopeId())).next();
		}
	}
}
