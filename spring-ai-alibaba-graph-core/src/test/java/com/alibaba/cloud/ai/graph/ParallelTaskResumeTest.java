/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.NodePause;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.SystemMessage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelTaskResumeTest {

	@TempDir
	Path directory;

	@Test
	void restoresTheDispatchAndOnlyRerunsUnfinishedTasksWithJson() throws Exception {
		verifyResume(StateGraph.DEFAULT_JACKSON_SERIALIZER);
	}

	@Test
	void restoresTheDispatchAndOnlyRerunsUnfinishedTasksWithBinary() throws Exception {
		verifyResume(new SpringAIStateSerializer());
	}

	@Test
	void sequentialDurableActionsKeepTheirTaskIdOnResumeAndChangeItForANewTurn() throws Exception {
		var taskIds = new ArrayList<String>();
		var definition = new StateGraph()
				.addNode("work", (state, run) -> {
					taskIds.add(run.taskId().orElseThrow());
					if (taskIds.size() == 1) {
						throw new IllegalStateException("temporary failure");
					}
					return CompletableFuture.completedFuture(Map.of("completed", true));
				})
				.addEdge(START, "work").addEdge("work", END);
		var config = RunnableConfig.builder().threadId("sequential-task").build();
		assertThrows(IllegalStateException.class, () -> definition.compile(durableFileConfig())
				.stream(Map.of(), config).blockLast(Duration.ofSeconds(5)));
		var restarted = definition.compile(durableFileConfig());
		restarted.stream(Map.of(), config.withResume()).blockLast(Duration.ofSeconds(5));
		restarted.stream(Map.of(), config).blockLast(Duration.ofSeconds(5));
		assertEquals(taskIds.get(0), taskIds.get(1));
		assertNotEquals(taskIds.get(1), taskIds.get(2));
	}

	@Test
	void pausedNodeWritesRemainLocalUntilParallelJoinAndMergeOnce() throws Exception {
		var definition = new StateGraph(() -> Map.of("log", new AppendStrategy()))
				.addNode("worker", (state, run) -> {
					if (state.value("log").isEmpty()) {
						var pause = InterruptionMetadata.builder("worker", state).build();
						return CompletableFuture.completedFuture(new NodePause(pause).withUpdates(Map.of("log", "paused")));
					}
					assertEquals(List.of("paused"), state.value("log").orElseThrow());
					return CompletableFuture.completedFuture(Map.of("log", "finished"));
				})
				.addNode("sibling", (state, run) -> {
					assertTrue(state.value("log").isEmpty());
					return CompletableFuture.completedFuture(Map.of("log", "sibling"));
				})
				.addEdge(START, "worker").addEdge(START, "sibling")
				.addEdge("worker", END).addEdge("sibling", END);
		var run = RunnableConfig.builder().threadId("paused-branch").build();
		var initial = definition.compile(durableFileConfig());
		var pause = assertInstanceOf(InterruptionMetadata.class,
				initial.stream(Map.of(), run).blockLast(Duration.ofSeconds(5)));
		assertTrue(initial.getState(run).state().value("log").isEmpty());
		var resumed = definition.compile(durableFileConfig()).stream(Map.of(),
				RunnableConfig.builder(run).addHumanFeedback(pause).build()).blockLast(Duration.ofSeconds(5));
		assertEquals(List.of("paused", "finished", "sibling"), resumed.state().value("log").orElseThrow());
	}

	@Test
	void resumesAnAnyOfWinnerAfterTheJoinReducerFails() throws Exception {
		var wins = new AtomicInteger();
		var losses = new AtomicInteger();
		var reductions = new AtomicInteger();
		var definition = new StateGraph(() -> Map.of("result", (oldValue, newValue) -> {
			if (reductions.incrementAndGet() == 1) {
				throw new IllegalStateException("temporary reducer failure");
			}
			return newValue;
		}))
				.addNode("winner", (state, run) -> {
					wins.incrementAndGet();
					return CompletableFuture.completedFuture(Map.of("result", "winner"));
				})
				.addNode("loser", (state, run) -> {
					losses.incrementAndGet();
					return new CompletableFuture<>();
				})
				.addEdge(START, "winner").addEdge(START, "loser")
				.addEdge("winner", END).addEdge("loser", END);
		var config = RunnableConfig.builder().threadId("any-of")
				.defaultParallelAggregationStrategy(NodeAggregationStrategy.ANY_OF).build();
		assertThrows(IllegalStateException.class,
				() -> definition.compile(durableFileConfig()).stream(Map.of(), config).blockLast(Duration.ofSeconds(5)));
		int loserCalls = losses.get();
		var last = definition.compile(durableFileConfig()).stream(Map.of(), config.withResume())
				.blockLast(Duration.ofSeconds(5));
		assertEquals("winner", last.state().value("result").orElseThrow());
		assertEquals(1, wins.get());
		assertEquals(loserCalls, losses.get());
	}

	private CompileConfig durableFileConfig() throws Exception {
		var saver = FileSystemSaver.builder().targetFolder(directory).build();
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.taskCheckpoints(true).build();
	}

	@Test
	void preventsConcurrentDurableRunsForTheSameFileThreadAndReleasesOnCancel() throws Exception {
		var firstSaver = FileSystemSaver.builder().targetFolder(directory).build();
		var secondSaver = FileSystemSaver.builder().targetFolder(directory).build();
		var gate = new CompletableFuture<Map<String, Object>>();
		var definition = new StateGraph()
				.addNode("task", (state, config) -> "hold".equals(state.value("mode").orElseThrow())
						? gate : CompletableFuture.completedFuture(Map.of("done", true)))
				.addEdge(START, "task").addEdge("task", END);
		var first = definition.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(firstSaver).build()).taskCheckpoints(true).build());
		var second = definition.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(secondSaver).build()).taskCheckpoints(true).build());
		var config = RunnableConfig.builder().threadId("shared-thread").build();

		var subscription = first.stream(Map.of("mode", "hold"), config).subscribe();
		try {
			assertThrows(IllegalStateException.class,
					() -> second.stream(Map.of("mode", "continue"), config).blockLast(Duration.ofSeconds(5)));
		}
		finally {
			subscription.dispose();
		}
		var completed = second.stream(Map.of("mode", "continue"), config).blockLast(Duration.ofSeconds(5));
		assertEquals(true, completed.state().value("done").orElseThrow());
	}

	@Test
	void fileSaverRefreshesCachedStateAtTheNextExecutionLease() throws Exception {
		var first = FileSystemSaver.builder().targetFolder(directory).build();
		var second = FileSystemSaver.builder().targetFolder(directory).build();
		var run = RunnableConfig.builder().threadId("file-lease").build();
		var initial = Checkpoint.builder().nodeId(START).nextNodeId(END).state(Map.of("revision", "first")).build();
		var updated = Checkpoint.builder().nodeId(START).nextNodeId(END).state(Map.of("revision", "second")).build();
		try (var lease = first.acquireExecutionLease(run)) {
			first.put(run, initial);
			assertThrows(IllegalStateException.class, () -> second.acquireExecutionLease(run));
		}
		try (var lease = second.acquireExecutionLease(run)) {
			second.put(run, updated);
		}
		try (var lease = first.acquireExecutionLease(run)) {
			assertEquals("second", first.get(run).orElseThrow().getState().get("revision"));
		}
	}

	@Test
	void resumesConcurrentSubgraphInterruptionsIndependentlyAfterReopeningTheSaver() throws Exception {
		var prepares = new AtomicInteger();
		var approved = new AtomicInteger();
		var run = RunnableConfig.builder().threadId("parallel-approvals").build();
		var firstGraph = approvalGraph(prepares, approved);
		var first = assertInstanceOf(InterruptionMetadata.class,
				firstGraph.stream(Map.of(), run).blockLast(Duration.ofSeconds(5)));
		var pending = first.pendingInterruptions();
		assertEquals(2, pending.size());
		var left = pending.get(0);
		var right = pending.get(1);
		assertEquals("left", left.state().value("item").orElseThrow());
		assertEquals("right", right.state().value("item").orElseThrow());
		assertNotEquals(left.interruptionId().orElseThrow(), right.interruptionId().orElseThrow());
		assertEquals(2, prepares.get());
		assertEquals(0, approved.get());

		var secondGraph = approvalGraph(prepares, approved);
		var leftFeedback = RunnableConfig.builder(run).addHumanFeedback(left).build();
		var second = assertInstanceOf(InterruptionMetadata.class,
				secondGraph.stream(Map.of(), leftFeedback).blockLast(Duration.ofSeconds(5)));
		assertEquals(right.interruptionId(), second.interruptionId());
		assertEquals(2, prepares.get());
		assertEquals(1, approved.get());

		var thirdGraph = approvalGraph(prepares, approved);
		var rightFeedback = RunnableConfig.builder(run).addHumanFeedback(second).build();
		var result = thirdGraph.stream(Map.of(), rightFeedback).blockLast(Duration.ofSeconds(5)).state();
		assertEquals(List.of("left", "right"), result.value("approved").orElseThrow());
		assertEquals(2, prepares.get());
		assertEquals(2, approved.get());
	}

	private CompiledGraph approvalGraph(AtomicInteger prepares, AtomicInteger approved) throws Exception {
		var saver = FileSystemSaver.builder().targetFolder(directory).build();
		var durable = CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.taskCheckpoints(true).build();
		var worker = new StateGraph()
				.addNode("prepare", (state, config) -> {
					prepares.incrementAndGet();
					return CompletableFuture.completedFuture(Map.of());
				})
				.addNode("approve", (state, config) -> {
					approved.incrementAndGet();
					return CompletableFuture.completedFuture(Map.of("approved", state.value("item").orElseThrow()));
				})
				.addEdge(START, "prepare").addEdge("prepare", "approve").addEdge("approve", END)
				.compile(CompileConfig.builder(durable).interruptBefore("approve").build());
		return new StateGraph(() -> Map.of("approved", new AppendStrategy()))
				.addNode("route", node_async((state, config) -> MultiCommand.dispatch(
						List.of(new Send("worker", Map.of("item", "left")),
								new Send("worker", Map.of("item", "right"))), Map.of())), Map.of("worker", "worker"))
				.addNode("worker", worker)
				.addEdge(START, "route").addEdge("worker", END).compile(durable);
	}

	private void verifyResume(StateSerializer serializer) throws Exception {
		var routes = new AtomicInteger();
		var leftCalls = new AtomicInteger();
		var rightCalls = new AtomicInteger();
		var config = RunnableConfig.builder().threadId("resume").build();
		var left = SystemMessage.builder().text("left").metadata(Map.of("worker", "left")).build();
		var right = SystemMessage.builder().text("right").metadata(Map.of("worker", "right")).build();
		var definition = new StateGraph("parallel-resume", () -> Map.of("results", new AppendStrategy()), serializer)
				.addNode("route", node_async((state, run) -> {
					routes.incrementAndGet();
					return MultiCommand.dispatch(List.of(new Send("worker", Map.of("side", "left")),
							new Send("worker", Map.of("side", "right"))), Map.of("routeValue", "shared"));
				}), Map.of("worker", "worker"))
				.addNode("worker", (state, run) -> {
					if ("left".equals(state.value("side").orElseThrow())) {
						leftCalls.incrementAndGet();
						return CompletableFuture.completedFuture(
								Map.of("results", List.of(left), "removeMe", OverAllState.MARK_FOR_REMOVAL));
					}
					if (rightCalls.incrementAndGet() == 1) {
						throw new IllegalStateException("worker failed");
					}
					return CompletableFuture.completedFuture(Map.of("results", List.of(right)));
				})
				.addEdge(START, "route").addEdge("worker", END);

		var firstSaver = FileSystemSaver.builder().targetFolder(directory).stateSerializer(serializer).build();
		var firstGraph = definition.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(firstSaver).build()).taskCheckpoints(true).build());
		assertThrows(IllegalStateException.class,
				() -> firstGraph.stream(Map.of("removeMe", true), config).blockLast(Duration.ofSeconds(5)));
		assertEquals(1, leftCalls.get());
		assertEquals(1, rightCalls.get());
		assertEquals(1, routes.get());

		var secondSaver = FileSystemSaver.builder().targetFolder(directory).stateSerializer(serializer).build();
		var pending = secondSaver.get(config).orElseThrow();
		assertFalse(pending.runtimeData().isEmpty());
		assertFalse(pending.getState().containsKey("results"));
		var resumedGraph = definition.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(secondSaver).build()).taskCheckpoints(true).build());
		var result = resumedGraph.stream(Map.of(), config.withResume()).blockLast(Duration.ofSeconds(5)).state();
		assertEquals(List.of(left, right), result.value("results").orElseThrow());
		assertEquals("shared", result.value("routeValue").orElseThrow());
		assertFalse(result.value("removeMe").isPresent());
		assertEquals(1, leftCalls.get());
		assertEquals(2, rightCalls.get());
		assertEquals(1, routes.get());
	}

}
