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

import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class CheckpointMessageConsistencyTest {

	private static final String THREAD_ID = "checkpoint-message-consistency-thread";

	@Test
	void newUserInputShouldLoadLastConsistentCheckpointWhenLatestEndsWithToolResponse() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph compiledGraph = buildGraph(saver);
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		saver.put(config, checkpoint("stable", END, stableState()));
		saver.put(config, checkpoint("tool_node", "streaming_model", inconsistentStateEndingWithToolResponse()));

		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) compiledGraph
			.getInitialState(Map.of("messages", List.of(new UserMessage("continue"))), config)
			.get("messages");

		assertFalse(messages.stream().anyMatch(ToolResponseMessage.class::isInstance),
				"new user input must not be appended after an incomplete tool response checkpoint");
		assertInstanceOf(UserMessage.class, messages.get(messages.size() - 1));
		assertIterableEquals(List.of("hello", "previous answer", "continue"),
				messages.stream().map(Message::getText).toList());
	}

	@Test
	void newUserInputShouldLoadLastConsistentCheckpointWhenLatestHasPartialToolResponses() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph compiledGraph = buildGraph(saver);
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		saver.put(config, checkpoint("stable", END, stableState()));
		saver.put(config, checkpoint("tool_node", "model_node", inconsistentStateWithPartialToolResponses()));

		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) compiledGraph
			.getInitialState(Map.of("messages", List.of(new UserMessage("continue"))), config)
			.get("messages");

		assertFalse(messages.stream().anyMatch(ToolResponseMessage.class::isInstance),
				"new user input must not be appended after a partial tool response checkpoint");
		assertInstanceOf(UserMessage.class, messages.get(messages.size() - 1));
		assertIterableEquals(List.of("hello", "previous answer", "continue"),
				messages.stream().map(Message::getText).toList());
	}

	@Test
	void newUserInputShouldLoadLastConsistentCheckpointWhenLatestEndsWithAssistantToolCall() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph compiledGraph = buildGraph(saver);
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		saver.put(config, checkpoint("stable", END, stableState()));
		saver.put(config, checkpoint("model_node", "tool_node", inconsistentStateEndingWithAssistantToolCall()));

		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) compiledGraph
			.getInitialState(Map.of("messages", List.of(new UserMessage("continue"))), config)
			.get("messages");

		assertFalse(messages.stream()
			.filter(AssistantMessage.class::isInstance)
			.map(AssistantMessage.class::cast)
			.anyMatch(AssistantMessage::hasToolCalls),
				"new user input must not be appended after an incomplete assistant tool call checkpoint");
		assertInstanceOf(UserMessage.class, messages.get(messages.size() - 1));
		assertIterableEquals(List.of("hello", "previous answer", "continue"),
				messages.stream().map(Message::getText).toList());
	}

	@Test
	void newUserInputShouldKeepLatestCheckpointWhenToolCallIsClosedByAssistantMessage() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph compiledGraph = buildGraph(saver);
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		saver.put(config, checkpoint("stable", END, stableState()));
		saver.put(config, checkpoint("final_model", END, consistentToolCallState()));

		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) compiledGraph
			.getInitialState(Map.of("messages", List.of(new UserMessage("continue"))), config)
			.get("messages");

		assertInstanceOf(ToolResponseMessage.class, messages.get(4));
		assertInstanceOf(AssistantMessage.class, messages.get(5));
		assertInstanceOf(UserMessage.class, messages.get(6));
		assertIterableEquals(List.of("hello", "previous answer", "search", "", "", "final answer", "continue"),
				messages.stream().map(Message::getText).toList());
	}

	private static CompiledGraph buildGraph(MemorySaver saver) throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("echo", node_async(state -> Map.of("messages", new AssistantMessage("ok"))))
			.addEdge(START, "echo")
			.addEdge("echo", END);

		return stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.build());
	}

	private static Checkpoint checkpoint(String nodeId, String nextNodeId, Map<String, Object> state) {
		return Checkpoint.builder().nodeId(nodeId).nextNodeId(nextNodeId).state(state).build();
	}

	private static Map<String, Object> stableState() {
		return Map.of("messages", List.of(new UserMessage("hello"), new AssistantMessage("previous answer")));
	}

	private static Map<String, Object> inconsistentStateEndingWithToolResponse() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build(),
				ToolResponseMessage.builder()
					.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "result")))
					.build()));
	}

	private static Map<String, Object> inconsistentStateWithPartialToolResponses() {
		AssistantMessage.ToolCall toolCall1 = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		AssistantMessage.ToolCall toolCall2 = new AssistantMessage.ToolCall("call_2", "function", "lookup", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall1, toolCall2)).build(),
				ToolResponseMessage.builder()
					.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "result")))
					.build()));
	}

	private static Map<String, Object> inconsistentStateEndingWithAssistantToolCall() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build()));
	}

	private static Map<String, Object> consistentToolCallState() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build(),
				ToolResponseMessage.builder()
					.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "result")))
					.build(),
				new AssistantMessage("final answer")));
	}

}
