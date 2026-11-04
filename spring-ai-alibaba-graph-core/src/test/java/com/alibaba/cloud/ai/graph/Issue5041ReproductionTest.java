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

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * Issue #5041 Reproduction Test: {@code updateState(config, values, asNode)} duplicates
 * {@code AppendStrategy} state when the node given as {@code asNode} has a static edge.
 *
 * <p>
 * When a HITL-interrupted graph is resumed via
 * {@code CompiledGraph.updateState(config, values, asNode)}, the next-node command computed for
 * the static edge carries the <b>full state map</b> as its incremental update. Feeding that full
 * state back into the checkpoint merge appends the existing values a second time for keys managed
 * by an {@code AppendStrategy}, so the whole list is duplicated.
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/5041">Issue #5041</a>
 */
class Issue5041ReproductionTest {

	// https://github.com/alibaba/spring-ai-alibaba/issues/5041
	@Test
	void updateStateWithAsNode_staticEdge_doesNotDuplicateAppendStrategyKeys() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages", new AppendStrategy())
			.build();

		var workflow = new StateGraph(keyStrategyFactory)
			.addNode("A", node_async(state -> Map.of("messages", List.of("msg-from-A"))))
			.addNode("B", node_async(state -> Map.of("messages", List.of("msg-from-B"))))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptBefore("B")
				.build());

		var runnableConfig = RunnableConfig.builder().build();

		// first run: START -> A, then interrupt before B (the interruption re-emits the
		// current node as a final InterruptionMetadata output, as in InterruptionTest)
		var outputs = workflow.stream(Map.of(), runnableConfig).collectList().block();
		assertIterableEquals(List.of(START, "A", "A"), outputs.stream().map(NodeOutput::node).toList());
		assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));

		// HITL: record the human feedback as if it was produced by node A (static edge A -> B)
		var updatedConfig = workflow.updateState(runnableConfig, Map.of("messages", "human-input"), "A");

		// resume: B appends its message
		var resumeOutputs = workflow.stream(null, updatedConfig.withResume()).collectList().block();

		// the final state is carried by the last resume output (getState(updatedConfig) would
		// return the checkpoint created by updateState, not the latest one)
		var finalMessages = resumeOutputs.get(resumeOutputs.size() - 1).state().value("messages").orElseThrow();
		assertEquals(List.of("msg-from-A", "human-input", "msg-from-B"), finalMessages);
	}

}
