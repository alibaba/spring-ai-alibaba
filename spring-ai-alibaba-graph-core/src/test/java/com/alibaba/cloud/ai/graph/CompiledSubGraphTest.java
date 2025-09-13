/*
 * Copyright 2024-2025 the original author or authors.
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

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.SubGraphInterruptionException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mergeMap;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompiledSubGraphTest {

	private static KeyStrategyFactory getStrategyFactory() {
		return KeyStrategy.builder()
			.addStrategy("newAttribute", new ReplaceStrategy())
			.addStrategy("messages", new AppendStrategy())
			.build();
	}

	private AsyncNodeAction _makeNode(String withMessage) {
		return node_async(state -> Map.of("messages", format("[%s]", withMessage)));
	}

	private AsyncNodeAction _makeNodeAndCheckState(String withMessage, String attributeKey) {
		return node_async(state -> {
			var attributeValue = state.value(attributeKey).orElse("");

			return Map.of("messages", format("[%s]", withMessage + attributeValue));

		}

		);
	}

	private AsyncNodeAction _makeSubgraphNode(String parentNodeId, CompiledGraph subGraph) {
		final var runnableConfig = RunnableConfig.builder().threadId(format("%s_subgraph", parentNodeId)).build();
		return node_async(state -> {

			var output = subGraph.streamFromInitialNode(state, runnableConfig)
				.stream()
				.reduce((a, b) -> b)
				.orElseThrow();

			if (!output.isEND()) {
				throw new SubGraphInterruptionException(parentNodeId, output.node(),
						mergeMap(output.state().data(), Map.of("resume_subgraph", true)));
			}
			return Map.of();
		});
	}

	private CompiledGraph subGraph(BaseCheckpointSaver saver) throws Exception {

		var compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), saver).build())
			.interruptAfter("NODE3.2")
			.build();

		return new StateGraph(getStrategyFactory()).addEdge(START, "NODE3.1")
			.addNode("NODE3.1", _makeNode("NODE3.1"))
			.addNode("NODE3.2", _makeNode("NODE3.2"))
			.addNode("NODE3.3", _makeNode("NODE3.3"))
			.addNode("NODE3.4", _makeNodeAndCheckState("NODE3.4", "newAttribute"))
			.addEdge("NODE3.1", "NODE3.2")
			.addEdge("NODE3.2", "NODE3.3")
			.addEdge("NODE3.3", "NODE3.4")
			.addEdge("NODE3.4", END)
			.compile(compileConfig);
	}

	@Test
	public void testCompileSubGraphWithInterruptionUsingException() throws Exception {

		var saver = new MemorySaver();

		var compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), saver).build())
			.build();

		var subGraph = subGraph(saver); // create subgraph

		var parentGraph = new StateGraph(getStrategyFactory()).addEdge(START, "NODE1")
			.addNode("NODE1", _makeNode("NODE1"))
			.addNode("NODE2", _makeNode("NODE2"))
			.addNode("NODE3", _makeSubgraphNode("NODE3", subGraph))
			.addNode("NODE4", _makeNode("NODE4"))
			.addNode("NODE5", _makeNode("NODE5"))
			.addEdge("NODE1", "NODE2")
			.addEdge("NODE2", "NODE3")
			.addEdge("NODE3", "NODE4")
			.addEdge("NODE4", "NODE5")
			.addEdge("NODE5", END)
			.compile(compileConfig);
		var runnableConfig = RunnableConfig.builder().build();
		Map<String, Object> input = Map.of();
		do {
			try {
				for (var output : parentGraph.fluxStream(input, runnableConfig).toIterable()) {
					System.out.println("output: " + output);
				}

				break;
			}
			catch (Exception ex) {
				var interruptException = SubGraphInterruptionException.from(ex);
				if (interruptException.isPresent()) {
					System.out.println("SubGraphInterruptionException: " + interruptException.get().getMessage());
					var interruptionState = interruptException.get().state();

					// ==== METHOD 1 =====
					// FIND NODE BEFORE SUBGRAPH AND RESUME
					/*
					 * StateSnapshot<?> lastNodeBeforeSubGraph =
					 * workflow.getStateHistory(runnableConfig).stream() .skip(1)
					 * .findFirst() .orElseThrow( () -> new
					 * IllegalStateException("lastNodeBeforeSubGraph is null")); var
					 * nodeBeforeSubgraph = lastNodeBeforeSubGraph.node(); runnableConfig
					 * = workflow.updateState( lastNodeBeforeSubGraph.config(),
					 * interruptionState );
					 */

					// ===== METHOD 2 =======
					// UPDATE STATE ASSUMING TO BE ON NODE BEFORE SUBGRAPH ('NODE2') AND
					// RESUME
					var nodeBeforeSubgraph = "NODE2";
					runnableConfig = parentGraph.updateState(runnableConfig, interruptionState, nodeBeforeSubgraph);
					input = null;

					System.out.println("RESUME GRAPH FROM END OF NODE: " + nodeBeforeSubgraph);
					continue;
				}

				throw ex;
			}
		}
		while (true);

	}

	@Test
	public void testCompileSubGraphWithInterruptionSharingSaver() throws Exception {

		var saver = new MemorySaver();

		var compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), saver).build())
			.build();

		var subGraph = subGraph(saver); // create subgraph

		var parentGraph = new StateGraph(getStrategyFactory()).addEdge(START, "NODE1")
			.addNode("NODE1", _makeNode("NODE1"))
			.addNode("NODE2", _makeNode("NODE2"))
			.addNode("NODE3", subGraph)
			.addNode("NODE4", _makeNode("NODE4"))
			.addNode("NODE5", _makeNodeAndCheckState("NODE5", "newAttribute"))
			.addEdge("NODE1", "NODE2")
			.addEdge("NODE2", "NODE3")
			.addEdge("NODE3", "NODE4")
			.addEdge("NODE4", "NODE5")
			.addEdge("NODE5", END)
			.compile(compileConfig);

		var runnableConfig = RunnableConfig.builder().threadId("1").build();

		Map<String, Object> input = Map.of();

		var results = parentGraph.fluxStream(input, runnableConfig).collectList().block();
		var output = results.stream().peek(out -> System.out.println("output: " + out)).reduce((a, b) -> b);

		assertTrue(output.isPresent());

		assertFalse(output.get().isEND());
		assertInstanceOf(NodeOutput.class, output.get());

		// var iteratorResult = AsyncGenerator.resultValue(results.iterator());
		//
		// assertTrue(iteratorResult.isPresent());
		// assertInstanceOf(InterruptionMetadata.class, iteratorResult.get());

		runnableConfig = parentGraph.updateState(runnableConfig, Map.of("newAttribute", "<myNewValue>"));

		input = null;

		results = parentGraph.fluxStream(input, runnableConfig).collectList().block();
		output = results.stream().peek(out -> System.out.println("output: " + out)).reduce((a, b) -> b);

		assertTrue(output.isPresent());
		assertTrue(output.get().isEND());

		assertIterableEquals(List.of("[NODE1]", "[NODE2]", "[NODE3.1]", "[NODE3.2]", "[NODE3.3]",
				"[NODE3.4<myNewValue>]", "[NODE4]", "[NODE5<myNewValue>]"),
				output.get().state().value("messages", List.class).get());
	}

	@Test
	public void testCompileSubGraphWithInterruptionWithDifferentSaver() throws Exception {

		var parentSaver = new MemorySaver();

		BaseCheckpointSaver childSaver = new MemorySaver();
		var subGraph = subGraph(childSaver); // create subgraph

		var compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), parentSaver).build())
			.build();

		var parentGraph = new StateGraph(getStrategyFactory()).addEdge(START, "NODE1")
			.addNode("NODE1", _makeNode("NODE1"))
			.addNode("NODE2", _makeNode("NODE2"))
			.addNode("NODE3", subGraph)
			.addNode("NODE4", _makeNodeAndCheckState("NODE4", "newAttribute"))
			.addNode("NODE5", _makeNode("NODE5"))
			.addEdge("NODE1", "NODE2")
			.addEdge("NODE2", "NODE3")
			.addEdge("NODE3", "NODE4")
			.addEdge("NODE4", "NODE5")
			.addEdge("NODE5", END)
			.compile(compileConfig);

		var runnableConfig = RunnableConfig.builder().build();

		Map<String, Object> input = Map.of();

		var results = parentGraph.fluxStream(input, runnableConfig).collectList().block();
		var output = results.stream().peek(out -> System.out.println("output: " + out)).reduce((a, b) -> b);

		assertTrue(output.isPresent());

		assertFalse(output.get().isEND());
		assertInstanceOf(NodeOutput.class, output.get());

		// var iteratorResult = AsyncGenerator.resultValue(results.iterator());
		//
		// assertTrue(iteratorResult.isPresent());
		// assertInstanceOf(InterruptionMetadata.class, iteratorResult.get());

		runnableConfig = parentGraph.updateState(runnableConfig, Map.of("newAttribute", "<myNewValue>"));

		input = null;

		results = parentGraph.fluxStream(input, runnableConfig).collectList().block();
		output = results.stream().peek(out -> System.out.println("output: " + out)).reduce((a, b) -> b);

		assertTrue(output.isPresent());
		assertTrue(output.get().isEND());

		assertIterableEquals(List.of("[NODE1]", "[NODE2]", "[NODE3.1]", "[NODE3.2]", "[NODE3.3]",
				"[NODE3.4<myNewValue>]", "[NODE4<myNewValue>]", "[NODE5]"),
				output.get().state().value("messages", List.class).get());
	}

	@Test
	public void testNestedCompiledSubgraphFormIssue216() throws Exception {

		var subSubGraph = new StateGraph(getStrategyFactory()).addNode("foo1", _makeNode("foo1"))
			.addNode("foo2", _makeNode("foo2"))
			.addNode("foo3", _makeNode("foo3"))
			.addEdge(StateGraph.START, "foo1")
			.addEdge("foo1", "foo2")
			.addEdge("foo2", "foo3")
			.addEdge("foo3", StateGraph.END)
			.compile();

		var subGraph = new StateGraph(getStrategyFactory()).addNode("bar1", _makeNode("bar1"))
			.addNode("subgraph2", subSubGraph)
			.addNode("bar2", _makeNode("bar2"))
			.addEdge(StateGraph.START, "bar1")
			.addEdge("bar1", "subgraph2")
			.addEdge("subgraph2", "bar2")
			.addEdge("bar2", StateGraph.END)
			.compile();

		var stateGraph = new StateGraph(getStrategyFactory()).addNode("main1", _makeNode("main1"))
			.addNode("subgraph1", subGraph)
			.addNode("main2", _makeNode("main2"))
			.addEdge(StateGraph.START, "main1")
			.addEdge("main1", "subgraph1")
			.addEdge("subgraph1", "main2")
			.addEdge("main2", StateGraph.END)
			.compile();

		var runnableConfig = RunnableConfig.builder().build();

		Map<String, Object> input = Map.of();

		var results = stateGraph.fluxStream(input, runnableConfig).collectList().block();
		var output = results.stream().peek(out -> System.out.println("output: " + out)).reduce((a, b) -> b);

	}

}
