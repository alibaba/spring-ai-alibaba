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

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class InterruptionTest {

	private AsyncNodeActionWithConfig _nodeAction(String id) {
		return node_async((state, config) -> Map.of("messages", id));
	}

	@Test
	public void interruptAfterEdgeEvaluation() throws Exception {
		var saver = new MemorySaver();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();
		var workflow = new StateGraph(keyStrategyFactory).addNode("A", _nodeAction("A"))
			.addNode("B", _nodeAction("B"))
			.addNode("C", _nodeAction("C"))
			.addNode("D", _nodeAction("D"))
			.addConditionalEdges("B", edge_async(state -> {
				var message = state.value("messages").orElse(END);
				return message.equals("B") ? "D" : message.toString();
			}), EdgeMappings.builder().to("A").to("C").to("D").toEND().build())
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("C", END)
			.addEdge("D", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptAfter("B")
				.build());

		var runnableConfig = RunnableConfig.builder().build();

		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();
		var results = workflow.stream(Map.of(), runnableConfig)
			.doOnNext( output -> {
				System.out.println(output);
				lastOutputRef.set(output);
			})
			.map(NodeOutput::node)
			.collectList()
			.block();

		//The last 'B' node is duplicated because an InterruptionMetadata NodeOutput is emitted after the edge evaluation of node 'B'.
		assertIterableEquals(List.of(START, "A", "B", "B"), results);
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());


		RunnableConfig resumeConfig = RunnableConfig.builder()
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("D", END), results);

		var snapshotForNodeB = workflow.getStateHistory(runnableConfig)
			.stream()
			.filter(s -> s.node().equals("B"))
			.findFirst()
			.orElseThrow();

		// FIXME
		runnableConfig = workflow.updateState(snapshotForNodeB.config(), Map.of("messages", "C"));
		resumeConfig = RunnableConfig.builder(runnableConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("D", END), results);
	}

	@Test
	public void interruptBeforeEdgeEvaluation() throws Exception {

		var saver = new MemorySaver();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();
		var workflow = new StateGraph(keyStrategyFactory).addNode("A", _nodeAction("A"))
			.addNode("B", _nodeAction("B"))
			.addNode("C", _nodeAction("C"))
			.addConditionalEdges("B", edge_async(state -> state.value("messages").orElse(END).toString()),
					EdgeMappings.builder().to("A").to("C").toEND().build())
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptAfter("B")
				.interruptBeforeEdge(true)
				.build());

		var runnableConfig = RunnableConfig.builder().build();

		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();
		var results = workflow.stream(Map.of(), runnableConfig)
			.doOnNext( output -> {
				System.out.println(output);
				lastOutputRef.set(output);
			})
			.map(NodeOutput::node)
			.collectList()
			.block();

		assertIterableEquals(List.of(START, "A", "B", "B"), results);
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());

		runnableConfig = workflow.updateState(runnableConfig, Map.of("messages", "C"));
		// FIXME
		RunnableConfig resumeConfig = RunnableConfig.builder(runnableConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("C", END), results);
	}

}
