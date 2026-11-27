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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.NodePause;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.StreamOptions;
import com.alibaba.cloud.ai.graph.streaming.StreamWriter;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction.node_async;
import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Type;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunEventTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	@Test
	void rootEventStreamKeepsCheckpointsAndRequestedStateSnapshots() throws Exception {
		var graph = new StateGraph(() -> Map.of("log", new AppendStrategy()))
				.addNode("first", (state, run) -> completedFuture(Map.of("log", "first")))
				.addNode("second", (state, run) -> completedFuture(Map.of("log", "second")))
				.addEdge(START, "first").addEdge("first", "second").addEdge("second", END)
				.compile(checkpoint(false));
		var run = runConfig();
		var outcome = graph.streamEvents(Map.of(), run).blockLast(TIMEOUT);
		assertThat(outcome.type()).isEqualTo(Type.RUN_COMPLETED);
		assertThat(((RunEvent.State) outcome.data()).values()).containsEntry("log", List.of("first", "second"));
		assertThat(graph.getState(run).state().<List<String>>value("log")).contains(List.of("first", "second"));

		var snapshots = graph.streamEvents(Map.of(), RunnableConfig.builder().threadId("snapshots").build(),
						StreamOptions.all())
				.filter(event -> event.type() == Type.STATE_VALUE && event.scope().nodeId() != null)
				.map(event -> ((RunEvent.State) event.data()).values().get("log"))
				.collectList().block(TIMEOUT);
		assertThat(snapshots).containsExactly(List.of("first"), List.of("first", "second"));
	}

	@Test
	void liveChildEventsArriveBeforeParentCommitsItsMappedResult() throws Exception {
		var done = new CompletableFuture<ChatResponse>();
		var firstChunk = new CountDownLatch(1);
		var calls = new AtomicInteger();
		var child = graph("child", "model", (state, run) -> {
			calls.incrementAndGet();
			return completedFuture(Map.of("messages", Flux.just(reply("first")).concatWith(Mono.fromFuture(done))));
		});
		var parent = graph("parent", "wrapper", (state, run) -> {
			assertThat(run.streamWriter().tryEmit("progress", 1)).isEqualTo(StreamWriter.Result.ACCEPTED);
			return run.runChild(child, Map.of()).map(output -> {
				var message = output.state().<AssistantMessage>value("messages").orElseThrow();
				return Map.<String, Object>of("answer", message.getText());
			}).toFuture();
		});
		var result = parent.streamEvents(Map.of(), runConfig(), StreamOptions.all()).doOnNext(event -> {
			if (event.type() == Type.MODEL_DELTA) {
				firstChunk.countDown();
			}
		}).collectList().toFuture();
		assertThat(firstChunk.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(result).isNotDone();
		done.complete(reply(" last"));
		var events = result.get(5, TimeUnit.SECONDS);
		assertThat(calls).hasValue(1);
		assertThat(events).extracting(RunEvent::runId).containsOnly(events.get(0).runId());
		assertThat(events).extracting(RunEvent::sequence).isSorted().doesNotHaveDuplicates();
		var delta = events.stream().filter(event -> event.type() == Type.MODEL_DELTA).findFirst().orElseThrow();
		assertThat(delta.scope().namespace()).extracting(RunEvent.PathSegment::name)
				.containsExactly("parent", "wrapper", "child", "model");
		var outcome = events.get(events.size() - 1);
		assertThat(outcome.type()).isEqualTo(Type.RUN_COMPLETED);
		assertThat(((RunEvent.State) outcome.data()).values()).containsEntry("answer", "first last");
	}

	@Test
	void twoInvocationsOfSameChildGetDistinctScopesAndSharedRunProjections() throws Exception {
		var calls = new AtomicInteger();
		var child = graph("worker", "model", (state, run) -> {
			calls.incrementAndGet();
			return completedFuture(Map.of("messages", Flux.just(reply("answer"))));
		});
		var parent = graph("parent", "delegate", (state, run) -> run.runChild(child, Map.of())
			.then(run.runChild(child, Map.of())).thenReturn(Map.<String, Object>of("done", true)).toFuture());
		try (var handle = parent.eventRun(Map.of(), runConfig(), StreamOptions.all())) {
			var starts = handle.subgraphs().collectList().toFuture();
			var messages = handle.messages().collectList().toFuture();
			handle.start();
			handle.start();
			var outcome = handle.output().block(TIMEOUT);
			assertThat(handle.output().block(TIMEOUT)).isSameAs(outcome);
			var children = starts.get(5, TimeUnit.SECONDS);
			assertThat(children).extracting(childRun -> childRun.scope().scopeId())
					.hasSize(2).doesNotHaveDuplicates();
			assertThat(children.get(0).output().block(TIMEOUT).type()).isEqualTo(Type.RUN_COMPLETED);
			assertThat(messages.get(5, TimeUnit.SECONDS)).extracting(RunEvent::runId).containsOnly(outcome.runId());
			assertThat(calls).hasValue(2);
		}
	}

	@Test
	void mappedSubgraphResumesAndOnlyWritesSelectedData() throws Exception {
		var compile = checkpoint(false);
		var child = new StateGraph("child", Map::of).addNode("before", (state, run) ->
				completedFuture(Map.of("internal", "secret")))
			.addNode("after", (state, run) -> completedFuture(Map.of("output", state.value("value").orElseThrow())))
			.addEdge(START, "before").addEdge("before", "after").addEdge("after", END)
			.compile(CompileConfig.builder(compile).interruptBefore("after").build());
		var parent = new StateGraph("parent", Map::of)
			.addNode("nested", child, state -> Map.of("value", state.value("input").orElseThrow()),
					write -> write.containsKey("output") ? Map.of("result", write.get("output")) : Map.of())
			.addEdge(START, "nested").addEdge("nested", END).compile(compile);
		var initial = parent.streamEvents(Map.of("input", "hello"), runConfig(), StreamOptions.all()).blockLast(TIMEOUT);
		assertThat(initial.type()).isEqualTo(Type.RUN_PAUSED);
		var events = parent.streamEvents(Map.of(), runConfig().withResume(), StreamOptions.all()).collectList().block(TIMEOUT);
		var result = events.get(events.size() - 1);
		assertThat(result.type()).isEqualTo(Type.RUN_COMPLETED);
		assertThat(((RunEvent.State) result.data()).values()).containsEntry("result", "hello").doesNotContainKey("internal");
		assertThat(result.runId()).isNotEqualTo(initial.runId());
	}

	@Test
	void resolvesPausedParallelBranchesWithoutTaskCheckpoints() throws Exception {
		var siblings = new AtomicInteger();
		var graph = new StateGraph(() -> Map.of("log", new AppendStrategy()))
			.addNode("worker", (state, run) -> {
				if (state.value("log").isEmpty()) {
					var pause = InterruptionMetadata.builder("worker", state).build();
					return completedFuture(new NodePause(pause).withUpdates(Map.of("log", "paused")));
				}
				return completedFuture(Map.of("log", "finished"));
			}).addNode("sibling", (state, run) -> {
				siblings.incrementAndGet();
				return completedFuture(Map.of("log", "sibling"));
			}).addEdge(START, "worker").addEdge(START, "sibling").addEdge("worker", END).addEdge("sibling", END)
			.compile(checkpoint(false));
		var config = runConfig();
		var paused = (InterruptionMetadata) graph.stream(Map.of(), config).blockLast(TIMEOUT);
		var resumed = graph.stream(Map.of(), RunnableConfig.builder(config).addHumanFeedback(paused).build())
				.blockLast(TIMEOUT);
		assertThat(resumed.state().<List<String>>value("log")).contains(List.of("paused", "finished", "sibling"));
		assertThat(siblings).hasValue(1);
	}

	@Test
	void durableDispatchKeepsTheOriginalResolvedTarget() throws Exception {
		var calls = new AtomicInteger();
		var graph = new StateGraph()
			.addNode("route", node_async((state, run) -> MultiCommand.dispatch(List.of(new Send("left", Map.of())), Map.of())),
					Map.of("left", "worker", "worker", "other"))
			.addNode("worker", (state, run) -> {
				if (calls.getAndIncrement() == 0) {
					return CompletableFuture.failedFuture(new IllegalStateException("temporary"));
				}
				return completedFuture(Map.of("result", "worker"));
			}).addNode("other", (state, run) -> completedFuture(Map.of("result", "other")))
			.addEdge(START, "route").addEdge("worker", END).addEdge("other", END).compile(checkpoint(true));
		var run = runConfig();
		assertThatThrownBy(() -> graph.stream(Map.of(), run).blockLast(TIMEOUT)).hasMessageContaining("temporary");
		assertThat(graph.stream(Map.of(), run.withResume()).blockLast(TIMEOUT).state().<String>value("result"))
				.contains("worker");
		assertThat(calls).hasValue(2);
	}

	private static CompileConfig checkpoint(boolean tasks) {
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(new MemorySaver()).build())
				.taskCheckpoints(tasks).build();
	}

	@Test
	void slowConsumerBackpressuresChunksAndKeepsTheRootOutcome() throws Exception {
		int total = 2048;
		var produced = new AtomicInteger();
		var graph = graph("root", "model", (state, run) -> completedFuture(Map.of("messages",
				Flux.range(1, total).map(index -> {
					produced.incrementAndGet();
					return reply("x");
				}))));
		var options = new StreamOptions(Set.of(RunEvent.Channel.MESSAGES), false, false, false, 2, 4096);
		var received = new AtomicInteger();
		var terminal = new CompletableFuture<RunEvent>();
		var subscriber = new BaseSubscriber<RunEvent>() {
			@Override
			protected void hookOnSubscribe(Subscription subscription) {
			}

			@Override
			protected void hookOnNext(RunEvent event) {
				if (event.type() == Type.MODEL_DELTA) {
					received.incrementAndGet();
				}
				if (event.type() == Type.RUN_COMPLETED) {
					terminal.complete(event);
				}
			}

			@Override
			protected void hookOnError(Throwable error) {
				terminal.completeExceptionally(error);
			}
		};
		graph.streamEvents(Map.of(), runConfig(), options).subscribe(subscriber);
		try {
			assertThat(produced.get()).isBetween(1, total - 1);
			subscriber.requestUnbounded();
			assertThat(terminal.get(5, TimeUnit.SECONDS).type()).isEqualTo(Type.RUN_COMPLETED);
			assertThat(received).hasValue(total);
		}
		finally {
			subscriber.dispose();
		}
	}

	@Test
	void cancellingAnEventsSubscriberCancelsTheModelAndClosesItsWriter() throws Exception {
		var writer = new AtomicReference<StreamWriter>();
		var cancelled = new CountDownLatch(1);
		var first = new CountDownLatch(1);
		var graph = graph("root", "model", (state, run) -> {
			writer.set(run.streamWriter());
			return completedFuture(Map.of("messages", Flux.just(reply("first"))
					.concatWith(Flux.<ChatResponse>never().doOnCancel(cancelled::countDown))));
		});
		var subscription = graph.streamEvents(Map.of(), runConfig(), StreamOptions.all()).subscribe(event -> {
			if (event.type() == Type.MODEL_DELTA) {
				first.countDown();
			}
		});
		try {
			assertThat(first.await(5, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			subscription.dispose();
		}
		assertThat(cancelled.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(writer.get().tryEmit("late", 1)).isEqualTo(StreamWriter.Result.CLOSED);
	}

	@Test
	void anyOfPublishesOnlyTheWinnerAndSignalsTruncatedCandidateData() throws Exception {
		var readyToWin = new CompletableFuture<ChatResponse>();
		var loserStarted = new CountDownLatch(1);
		var loserCancelled = new CountDownLatch(1);
		var graph = new StateGraph()
			.addNode("winner", (state, run) -> completedFuture(Map.of("answer",
					Flux.range(0, 20).map(index -> reply("win")).concatWith(Mono.fromFuture(readyToWin)))))
			.addNode("loser", (state, run) -> completedFuture(Map.of("answer", Flux.defer(() -> {
				loserStarted.countDown();
				return Flux.just(reply("lose")).concatWith(Flux.<ChatResponse>never().doOnCancel(loserCancelled::countDown));
			}))))
			.addEdge(START, "winner").addEdge(START, "loser")
			.addEdge("winner", END).addEdge("loser", END).compile();
		var config = RunnableConfig.builder(runConfig()).defaultParallelAggregationStrategy(NodeAggregationStrategy.ANY_OF).build();
		var options = new StreamOptions(Set.of(RunEvent.Channel.MESSAGES, RunEvent.Channel.LIFECYCLE),
				true, false, false, 8, 8192);
		var seen = new AtomicInteger();
		var future = graph.streamEvents(Map.of(), config, options).doOnNext(event -> {
			if (event.type() == Type.MODEL_DELTA) {
				seen.incrementAndGet();
			}
		}).collectList().toFuture();
		assertThat(loserStarted.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(seen).hasValue(0);
		readyToWin.complete(reply("done"));
		var events = future.get(5, TimeUnit.SECONDS);
		assertThat(loserCancelled.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(events).extracting(RunEvent::type).contains(Type.TRUNCATED, Type.MODEL_COMPLETED);
		assertThat(events.stream().filter(event -> event.type() == Type.MODEL_DELTA || event.type() == Type.MODEL_COMPLETED))
				.allSatisfy(event -> assertThat(event.scope().nodeId()).isEqualTo("winner"));
		assertThat(events.get(events.size() - 1).type()).isEqualTo(Type.RUN_COMPLETED);
	}

	private static RunnableConfig runConfig() {
		return RunnableConfig.builder().threadId("events").build();
	}

	private static CompiledGraph graph(String name, String node,
			AsyncNodeActionWithConfig action) throws Exception {
		return new StateGraph(name, Map::of).addNode(node, action).addEdge(START, node).addEdge(node, END).compile();
	}

	private static ChatResponse reply(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}
}
