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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for embedded {@link com.alibaba.cloud.ai.graph.streaming.GraphFlux}
 * completion with a single {@link ToolResponseMessage} stored under {@code messages}.
 *
 * <p>
 * Tool execution nodes may publish a done-state map whose {@code messages} value is a
 * single {@link Message}, not a {@link List}. The embedded flux completion path in
 * {@link NodeExecutor} must accept both shapes without throwing {@link ClassCastException}.
 * </p>
 */
class EmbeddedGraphFluxSingleToolMessageTest {

	@Test
	void embeddedGraphFluxShouldAcceptSingleToolResponseMessageInDoneState() throws Exception {
		CompiledGraph graph = buildGraph();

		OverAllState finalState = graph.invoke(Map.of("messages", List.of())).orElseThrow();

		List<?> messages = finalState.value("messages", List.class).orElseThrow();
		assertEquals(1, messages.size());

		ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, messages.get(0));
		assertEquals(1, toolResponseMessage.getResponses().size());
		assertEquals("tool-result", toolResponseMessage.getResponses().get(0).responseData());

		assertEquals("tool-finished", finalState.value("tool_summary", ""));
	}

	private static CompiledGraph buildGraph() throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			strategies.put("tool_summary", new ReplaceStrategy());
			return strategies;
		}).addNode("tool_stream_node", node_async(state -> {
			ToolResponseMessage toolMessage = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "progress_tool", "tool-result")))
				.build();

			return Map.of("tool_stream",
					Flux.just(GraphResponse.done(Map.of("messages", toolMessage))));
		}))
			.addNode("after_tool", node_async(state -> {
				List<?> messages = state.value("messages", List.class).orElseThrow();
				assertTrue(messages.stream().anyMatch(ToolResponseMessage.class::isInstance));
				return Map.of("tool_summary", "tool-finished");
			}))
			.addEdge(START, "tool_stream_node")
			.addEdge("tool_stream_node", "after_tool")
			.addEdge("after_tool", END);
		return stateGraph.compile();
	}

}
