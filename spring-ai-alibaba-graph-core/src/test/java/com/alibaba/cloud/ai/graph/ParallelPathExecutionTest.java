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

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelPathExecutionTest {

	@Test
	void nestedForkAndConditionalLoopJoinOnceAndMergeWritesInBranchOrder() throws Exception {
		var joins = new AtomicInteger();
		AsyncCommandAction check = (state, config) -> completedFuture(
				new Command(state.value("turn", Integer.class).orElseThrow() < 2 ? "again" : "finished"));
		var graph = new StateGraph(() -> Map.of("trace", new AppendStrategy()))
				.addNode("fork", (state, config) -> completedFuture(Map.of("trace", "fork")))
				.addNode("loop", (state, config) -> completedFuture(Map.of("trace", "loop", "turn",
						state.value("turn", Integer.class).orElse(0) + 1)))
				.addNode("nested", (state, config) -> completedFuture(Map.of("trace", "nested")))
				.addNode("short", (state, config) -> completedFuture(Map.of("trace", "short")))
				.addNode("long", (state, config) -> completedFuture(Map.of("trace", "long")))
				.addNode("finishLong", (state, config) -> completedFuture(Map.of("trace", "finishLong")))
				.addNode("innerJoin", (state, config) -> completedFuture(Map.of("trace", "innerJoin")))
				.addNode("join", (state, config) -> {
					joins.incrementAndGet();
					return completedFuture(Map.of("trace", "join"));
				})
				.addEdge(START, "fork")
				.addEdge("fork", List.of("loop", "nested"))
				.addConditionalEdges("loop", check, Map.of("again", "loop", "finished", "join"))
				.addEdge("nested", List.of("short", "long"))
				.addEdge("short", "innerJoin")
				.addEdge("long", "finishLong")
				.addEdge("finishLong", "innerJoin")
				.addEdge("innerJoin", "join")
				.addEdge("join", END)
				.addParallelJoin("fork", "join")
				.compile();
		var output = graph.stream(Map.of(), RunnableConfig.builder().build()).blockLast(Duration.ofSeconds(3));
		assertEquals(List.of("fork", "loop", "loop", "nested", "short", "long", "finishLong", "innerJoin", "join"),
				output.state().value("trace").orElseThrow());
		assertEquals(1, joins.get());
	}

	@Test
	void resumedBranchContinuesFromItsOwnCheckpointAndOnlyPublishesItsWrites(@TempDir Path path) throws Exception {
		var preparatoryCalls = new AtomicInteger();
		var terminalCalls = new AtomicInteger();
		var shortCalls = new AtomicInteger();
		AsyncMultiCommandAction dispatch = (state, config) -> completedFuture(MultiCommand.dispatch(
				List.of(new Send("path", Map.of("input", "left")), new Send("short", Map.of("input", "right"))),
				Map.of("trace", "dispatch")));
		var definition = new StateGraph(() -> Map.of("trace", new AppendStrategy()))
				.addNode("dispatch", dispatch, Map.of("path", "prepare", "short", "short"))
				.addNode("prepare", (state, config) -> {
					preparatoryCalls.incrementAndGet();
					return completedFuture(Map.of("trace", "prepare:" + state.value("input").orElseThrow()));
				})
				.addNode("endPath", (state, config) -> {
					if (terminalCalls.getAndIncrement() == 0) {
						throw new IllegalStateException("first attempt fails");
					}
					return completedFuture(Map.of("trace", "pathFinished:" + state.value("input").orElseThrow()));
				})
				.addNode("short", (state, config) -> {
					shortCalls.incrementAndGet();
					return completedFuture(Map.of("trace", "short:" + state.value("input").orElseThrow()));
				})
				.addNode("join", (state, config) -> completedFuture(Map.of("trace", "joined")))
				.addEdge(START, "dispatch")
				.addEdge("prepare", "endPath")
				.addEdge("endPath", "join")
				.addEdge("short", "join")
				.addEdge("join", END)
				.addParallelJoin("dispatch", "join");
		var run = RunnableConfig.builder().threadId("parallel-paths").build();
		var first = definition.compile(durable(path));
		assertThrows(IllegalStateException.class,
				() -> first.stream(Map.of("trace", List.of("initial")), run).blockLast(Duration.ofSeconds(3)));

		var output = definition.compile(durable(path)).stream(Map.of(), run.withResume())
				.blockLast(Duration.ofSeconds(3));
		assertEquals(List.of("initial", "dispatch", "prepare:left", "pathFinished:left", "short:right", "joined"),
				output.state().value("trace").orElseThrow());
		assertEquals(1, preparatoryCalls.get());
		assertEquals(2, terminalCalls.get());
		assertEquals(1, shortCalls.get());
		assertTrue(output.state().value("input").isEmpty());
	}

	@Test
	void reportsWhichForkCanSkipADeclaredJoin() throws Exception {
		var graph = new StateGraph()
				.addNode("left", (state, config) -> completedFuture(Map.of()))
				.addNode("right", (state, config) -> completedFuture(Map.of()))
				.addNode("join", (state, config) -> completedFuture(Map.of()))
				.addEdge(START, List.of("left", "right"))
				.addEdge("left", "join")
				.addEdge("right", END)
				.addEdge("join", END)
				.addParallelJoin(START, "join");
		assertTrue(assertThrows(GraphStateException.class, graph::compile).getMessage().contains(START));
	}

	private CompileConfig durable(Path path) {
		return CompileConfig.builder().taskCheckpoints(true)
				.saverConfig(SaverConfig.builder().register(FileSystemSaver.builder().targetFolder(path).build()).build())
				.build();
	}

}
