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

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphRunnerContextResumeMessageOrderTest {

	@Test
	void resumeShouldAppendNewMessagesAfterCheckpointHistory() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = () -> Map.of(
				"messages", new AppendStrategy(),
				OverAllState.DEFAULT_INPUT_KEY, KeyStrategy.REPLACE);

		CompiledGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("A", node_async((state, config) -> Map.of("messages", "assistant:history")))
				.addNode("B", node_async((state, config) -> Map.of("messages", "assistant:resume")))
				.addEdge(START, "A")
				.addEdge("A", "B")
				.addEdge("B", END)
				.compile(CompileConfig.builder()
						.saverConfig(SaverConfig.builder().register(saver).build())
						.interruptAfter("A")
						.build());

		RunnableConfig initialConfig = RunnableConfig.builder().threadId("resume-order-thread").build();
		List<NodeOutput> firstRun = workflow.stream(Map.of("messages", List.of("user:initial")), initialConfig)
				.collectList()
				.block();
		assertTrue(firstRun != null && !firstRun.isEmpty(), "First run should interrupt after node A");

		RunnableConfig resumeConfig = RunnableConfig.builder()
				.threadId("resume-order-thread")
				.resume()
				.build();
		OverAllState resumedState = workflow.invoke(Map.of("messages", List.of("user:resume")), resumeConfig)
				.orElseThrow();

		@SuppressWarnings("unchecked")
		List<String> messages = (List<String>) resumedState.value("messages").orElseThrow();
		assertIterableEquals(
				List.of("user:initial", "assistant:history", "user:resume", "assistant:resume"),
				messages,
				"Resume should keep checkpoint history first and append new resume input after it");
	}
}
