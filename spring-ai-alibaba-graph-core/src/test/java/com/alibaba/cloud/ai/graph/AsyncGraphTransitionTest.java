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
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AsyncGraphTransitionTest {

	@Test
	void asyncRoutesAndEmbeddedOutputsCanUseTheSubscribersSingleThread() throws Exception {
		var scheduler = Schedulers.newSingle("graph-transitions");
		try {
			AsyncCommandAction enter = (state, config) ->
					supplyAsync(() -> new Command("run", Map.of("trace", "entry")), scheduler::schedule);
			AsyncCommandAction leave = (state, config) ->
					supplyAsync(() -> new Command("finish", Map.of("trace", "edge")), scheduler::schedule);
			var graph = new StateGraph(() -> Map.of("trace", new AppendStrategy()))
					.addConditionalEdges(START, enter, Map.of("run", "worker"))
					.addNode("worker", (state, config) -> completedFuture(Map.of("stream", Flux.defer(() ->
							Flux.just(GraphResponse.of(supplyAsync(() -> NodeOutput.of("chunk", "", state, null),
									scheduler::schedule)), GraphResponse.<NodeOutput>done(Map.of("trace", "stream")))))))
					.addConditionalEdges("worker", leave, Map.of("finish", END))
					.compile();
			var output = graph.stream(Map.of(), RunnableConfig.builder().build())
					.subscribeOn(scheduler).collectList().block(Duration.ofSeconds(3));
			assertEquals(List.of(START, "chunk", END), output.stream().map(NodeOutput::node).toList());
			assertEquals(List.of("entry", "stream", "edge"), output.get(2).state().value("trace").orElseThrow());
		}
		finally {
			scheduler.dispose();
		}
	}

	@Test
	void resumedDeferredRouteCanEndTheGraphWithoutRunningAnotherNode() throws Exception {
		var edges = new AtomicInteger();
		AsyncCommandAction finish = (state, config) -> {
			edges.incrementAndGet();
			return completedFuture(new Command("finish", Map.of("fromEdge", true)));
		};
		var graph = new StateGraph()
				.addNode("work", (state, config) -> completedFuture(Map.of("worked", true)))
				.addEdge(START, "work")
				.addConditionalEdges("work", finish, Map.of("finish", END))
				.compile(CompileConfig.builder().interruptAfter("work").interruptBeforeEdge(true).build());
		var config = RunnableConfig.builder().threadId("deferred-end").build();
		assertInstanceOf(InterruptionMetadata.class, graph.stream(Map.of(), config).blockLast(Duration.ofSeconds(3)));
		assertEquals(0, edges.get());

		var output = graph.stream(Map.of(), config.withResume()).blockLast(Duration.ofSeconds(3));
		assertEquals(END, output.node());
		assertEquals(true, output.state().value("fromEdge").orElseThrow());
		assertEquals(1, edges.get());
	}

	@Test
	void aForkKeepsTheSourceCheckpointAndPersistsTheNewConditionalDestination() throws Exception {
		var graph = new StateGraph()
				.addNode("route", (state, config) -> completedFuture(Map.of()))
				.addNode("left", (state, config) -> completedFuture(Map.of("visited", "left")))
				.addNode("right", (state, config) -> completedFuture(Map.of("visited", "right")))
				.addEdge(START, "route")
				.addConditionalEdges("route", (AsyncCommandAction) (state, config) ->
						completedFuture(new Command(state.<String>value("choice").orElseThrow(),
								Map.of("evaluated", true))), Map.of("left", "left", "right", "right"))
				.addEdge("left", END).addEdge("right", END)
				.compile(CompileConfig.builder().interruptBefore("left").build());
		var config = RunnableConfig.builder().threadId("fork").build();
		graph.stream(Map.of("choice", "left"), config).blockLast(Duration.ofSeconds(3));
		var source = graph.getState(config);
		assertEquals("left", source.next());

		var branch = graph.forkState(source.config(), Map.of("choice", "right"), "route");
		assertEquals("left", graph.getState(source.config()).next());
		assertEquals("left", graph.getState(source.config()).state().value("choice").orElseThrow());
		assertEquals("right", graph.getState(branch).next());
		var done = graph.stream(Map.of(), branch.withResume()).blockLast(Duration.ofSeconds(3));
		assertEquals("right", done.state().value("visited").orElseThrow());
		assertEquals(true, done.state().value("evaluated").orElseThrow());
	}

}
