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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckpointRollbackConsistencyTest {

	@Test
	void newUserInputShouldRollbackToLatestSafeCheckpointWhenLatestEndsWithToolResponse() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph graph = buildContinuationGraph(saver, new AtomicBoolean(false),
				List.of("user", "assistant", "user"));
		RunnableConfig config = RunnableConfig.builder().threadId("rollback-safe-checkpoint-thread").build();

		saver.put(config, Checkpoint.builder().nodeId("stable").nextNodeId(END).state(stableState()).build());
		saver.put(config, Checkpoint.builder()
			.nodeId("tool_node")
			.nextNodeId("streaming_model")
			.state(incompleteToolResponseState())
			.build());

		@SuppressWarnings("unchecked")
		List<Message> nextTurnMessages = (List<Message>) graph
			.getInitialState(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.get("messages");

		assertEquals(List.of("user", "assistant", "user"),
				toOpenAiMessages(nextTurnMessages).stream().map(message -> (String) message.get("role")).toList());
		assertFalse(nextTurnMessages.stream().anyMatch(ToolResponseMessage.class::isInstance),
				"next user input must not be appended after an incomplete tool response checkpoint");
		assertEquals("please continue", nextTurnMessages.get(nextTurnMessages.size() - 1).getText());
	}

	@Test
	void nextTurnAfterToolCancellationShouldInvokeModelWithOpenAiCompatibleMessages() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph cancelledGraph = buildToolThenModelGraph(saver, Flux.just(chatResponse("done")));
		RunnableConfig config = RunnableConfig.builder().threadId("rollback-after-tool-cancel-thread").build();

		List<NodeOutput> observed = cancelledGraph.stream(initialMessages(), config)
			.takeUntil(output -> "tool_node".equals(output.node()))
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(observed, "stream should emit the tool node before cancellation");
		assertTrue(observed.stream().anyMatch(output -> "tool_node".equals(output.node())),
				"test should cancel after the tool response checkpoint is written");

		AtomicBoolean openAiPayloadVerified = new AtomicBoolean(false);
		CompiledGraph continuationGraph = buildContinuationGraph(saver, openAiPayloadVerified, List.of("user"));

		List<NodeOutput> outputs = continuationGraph
			.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(outputs);
		assertTrue(openAiPayloadVerified.get(), "next model call should receive OpenAI-compatible messages");
		List<?> checkpointMessages = checkpointMessages(continuationGraph, config);
		Object lastMessage = checkpointMessages.get(checkpointMessages.size() - 1);
		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void nextTurnAfterStreamTimeoutShouldInvokeModelWithOpenAiCompatibleMessages() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph timeoutGraph = buildToolThenModelGraph(saver,
				Flux.concat(Flux.just(chatResponse("partial")), Flux.just(chatResponse(" done"))
					.delayElements(Duration.ofMillis(200))));
		RunnableConfig config = RunnableConfig.builder().threadId("rollback-after-stream-timeout-thread").build();

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> timeoutAfterStreamingModelStarts(timeoutGraph.stream(initialMessages(), config))
					.collectList()
					.block(Duration.ofSeconds(5)));
		assertTrue(hasCause(error, TimeoutException.class), "test should first reproduce timeout cancellation");

		AtomicBoolean openAiPayloadVerified = new AtomicBoolean(false);
		CompiledGraph continuationGraph = buildContinuationGraph(saver, openAiPayloadVerified, List.of("user"));

		List<NodeOutput> outputs = continuationGraph
			.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(outputs);
		assertTrue(openAiPayloadVerified.get(), "next model call after timeout should receive OpenAI-compatible messages");
	}

	private static CompiledGraph buildToolThenModelGraph(MemorySaver saver, Flux<ChatResponse> modelStream)
			throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("tool_node", node_async(state -> Map.of("messages", toolResponse())))
			.addNode("streaming_model",
					node_async(state -> Map.of("messages", modelStream)))
			.addEdge(START, "tool_node")
			.addEdge("tool_node", "streaming_model")
			.addEdge("streaming_model", END);

		return stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.build());
	}

	private static CompiledGraph buildContinuationGraph(MemorySaver saver, AtomicBoolean openAiPayloadVerified,
			List<String> expectedRoles) throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("streaming_model", node_async(state -> {
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			ChatModel chatModel = verifyingOpenAiPayloadChatModel(openAiPayloadVerified, expectedRoles);
			return Map.of("messages", chatModel.stream(new Prompt(messages)));
		})).addEdge(START, "streaming_model").addEdge("streaming_model", END);

		return stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.build());
	}

	private static ChatModel verifyingOpenAiPayloadChatModel(AtomicBoolean verified, List<String> expectedRoles) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage("continued"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				List<Map<String, Object>> openAiMessages = toOpenAiMessages(prompt.getInstructions());
				assertOpenAiToolMessageOrder(openAiMessages);
				assertEquals(expectedRoles, openAiMessages.stream()
					.map(message -> (String) message.get("role"))
					.toList());
				assertEquals("please continue", openAiMessages.get(openAiMessages.size() - 1).get("content"));
				verified.set(true);
				return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("continued")))));
			}
		};
	}

	private static Map<String, Object> initialMessages() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		List<Message> messages = List.of(
				new UserMessage("search first"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build());
		return Map.of(OverAllState.DEFAULT_INPUT_KEY, "search first", "messages", messages);
	}

	private static Map<String, Object> stableState() {
		return Map.of("messages", List.of(new UserMessage("hello"), new AssistantMessage("previous answer")));
	}

	private static Map<String, Object> incompleteToolResponseState() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build(),
				toolResponse()));
	}

	private static ToolResponseMessage toolResponse() {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "ok")))
			.build();
	}

	private static ChatResponse chatResponse(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static List<?> checkpointMessages(CompiledGraph compiledGraph, RunnableConfig config) {
		return compiledGraph.stateOf(config)
			.flatMap(state -> state.state().value("messages"))
			.filter(List.class::isInstance)
			.map(List.class::cast)
			.orElse(List.of());
	}

	private static List<Map<String, Object>> toOpenAiMessages(List<Message> messages) {
		List<Map<String, Object>> openAiMessages = new ArrayList<>();
		for (Message message : messages) {
			if (message instanceof UserMessage userMessage) {
				openAiMessages.add(Map.of("role", "user", "content", userMessage.getText()));
			}
			else if (message instanceof AssistantMessage assistantMessage) {
				Map<String, Object> openAiMessage = new HashMap<>();
				openAiMessage.put("role", "assistant");
				openAiMessage.put("content", assistantMessage.getText());
				if (assistantMessage.hasToolCalls()) {
					openAiMessage.put("tool_calls", assistantMessage.getToolCalls()
						.stream()
						.map(toolCall -> Map.of("id", toolCall.id(), "type", toolCall.type(), "function",
								Map.of("name", toolCall.name(), "arguments", toolCall.arguments())))
						.toList());
				}
				openAiMessages.add(openAiMessage);
			}
			else if (message instanceof ToolResponseMessage toolResponseMessage) {
				for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
					openAiMessages.add(Map.of("role", "tool", "tool_call_id", response.id(), "content",
							response.responseData()));
				}
			}
		}
		return openAiMessages;
	}

	private static Flux<NodeOutput> timeoutAfterStreamingModelStarts(Flux<NodeOutput> stream) {
		return stream.timeout(Mono.delay(Duration.ofSeconds(1)), output -> "streaming_model".equals(output.node())
				? Mono.delay(Duration.ofMillis(25)) : Mono.delay(Duration.ofSeconds(1)));
	}

	private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
		Throwable current = throwable;
		while (current != null) {
			if (causeType.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static void assertOpenAiToolMessageOrder(List<Map<String, Object>> messages) {
		Set<String> pendingToolCallIds = new HashSet<>();
		boolean waitingForAssistantAfterTool = false;
		for (Map<String, Object> message : messages) {
			String role = (String) message.get("role");
			if ("assistant".equals(role)) {
				assertTrue(pendingToolCallIds.isEmpty(), "assistant cannot appear before all tool calls are answered");
				waitingForAssistantAfterTool = false;
				Object toolCalls = message.get("tool_calls");
				if (toolCalls instanceof List<?> calls) {
					for (Object call : calls) {
						pendingToolCallIds.add((String) ((Map<String, Object>) call).get("id"));
					}
				}
			}
			else if ("tool".equals(role)) {
				assertFalse(pendingToolCallIds.isEmpty(), "tool message must follow assistant tool calls");
				assertTrue(pendingToolCallIds.remove(message.get("tool_call_id")),
						"tool message must reference a pending assistant tool call");
				waitingForAssistantAfterTool = pendingToolCallIds.isEmpty();
			}
			else {
				assertTrue(pendingToolCallIds.isEmpty() && !waitingForAssistantAfterTool,
						role + " message cannot follow an unfinished tool response sequence");
			}
		}
		assertTrue(pendingToolCallIds.isEmpty(), "all assistant tool calls must have tool responses");
		assertFalse(waitingForAssistantAfterTool, "tool response must be followed by assistant before new user input");
	}

}
