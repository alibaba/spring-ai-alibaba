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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReactAgentTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.CallResponseSpec responseSpec;

	@Mock
	private ChatResponse chatResponse;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@Mock
	private ToolCallback toolCallback;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		// Configure mock ChatClient with complete call chain
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.messages(anyList())).thenReturn(requestSpec);
		when(requestSpec.advisors(anyList())).thenReturn(requestSpec);
		when(requestSpec.toolCallbacks(anyList())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);

		// Configure mock ToolCallbackResolver
		when(toolCallbackResolver.resolve(anyString())).thenReturn(toolCallback);
		when(toolCallback.call(anyString(), any(ToolContext.class))).thenReturn("test tool response");
		when(toolCallback.getToolDefinition()).thenReturn(DefaultToolDefinition.builder()
			.name("test_function")
			.description("A test function")
			.inputSchema("{\"type\": \"object\", \"properties\": {\"arg1\": {\"type\": \"string\"}}}")
			.build());

		// Configure mock ChatResponse with ToolCalls
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", "stop");
		List<ToolCall> toolCalls = List
			.of(new ToolCall("call_1", "function", "test_function", "{\"arg1\": \"value1\"}"));
		AssistantMessage assistantMessage = new AssistantMessage("test response", metadata, toolCalls,
				Collections.emptyList());
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder().finishReason("stop").build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
			.id("test-id")
			.usage(new DefaultUsage(10, 20, 30))
			.build();
		ChatResponse response = ChatResponse.builder()
			.generations(List.of(generation))
			.metadata(responseMetadata)
			.build();
		when(responseSpec.chatResponse()).thenReturn(response);
	}

	/**
	 * Tests ReactAgent with preLlmHook that modifies system prompt before LLM call.
	 */
	@Test
	public void testReactAgentWithPreLlmHook() throws Exception {
		Map<String, String> prellmStore = new HashMap<>();

		ReactAgent agent = ReactAgent.builder().name("testAgent").chatClient(chatClient).state(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			return keyStrategyHashMap;
		}).resolver(toolCallbackResolver).preLlmHook(state -> {
			prellmStore.put("timestamp", String.valueOf(System.currentTimeMillis()));
			return Map.of();
		}).build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {

		}
		assertNotNull(prellmStore.get("timestamp"));

	}

	/**
	 * Tests ReactAgent with postLlmHook that processes LLM response.
	 */
	@Test
	public void testReactAgentWithPostLlmHook() throws Exception {
		// Create a map to store processed responses
		Map<String, String> responseStore = new HashMap<>();

		ReactAgent agent = ReactAgent.builder().name("testAgent").chatClient(chatClient).state(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			return keyStrategyHashMap;
		})
			.resolver(toolCallbackResolver)

			.postLlmHook(state -> {
				responseStore.put("response", "Processed: " + state.value("messages"));
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {

		}
		assertNotNull(responseStore.get("response"));
	}

	/**
	 * Tests ReactAgent with preToolHook that prepares tool parameters.
	 */
	@Test
	public void testReactAgentWithPreToolHook() throws Exception {
		// Create a map to store tool parameters
		Map<String, Object> toolParams = new HashMap<>();

		ReactAgent agent = ReactAgent.builder().name("testAgent").chatClient(chatClient).state(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			keyStrategyHashMap.put("toolParams", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).resolver(toolCallbackResolver).preToolHook(state -> {
			toolParams.put("timestamp", System.currentTimeMillis());
			return Map.of();
		}).build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {

		}
		assertNotNull(toolParams.get("timestamp"));
	}

	/**
	 * Tests ReactAgent with postToolHook that collects tool results.
	 */
	@Test
	public void testReactAgentWithPostToolHook() throws Exception {
		// Create a map to store tool results
		Map<String, Object> toolResults = new HashMap<>();

		ReactAgent agent = ReactAgent.builder()
			.name("testAgent")
			.chatClient(chatClient)
			.resolver(toolCallbackResolver)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				keyStrategyHashMap.put("toolOutput", new ReplaceStrategy());
				return keyStrategyHashMap;
			})
			.postToolHook(state -> {
				toolResults.put("result", "collected: " + "tool output");
				return Map.of();
			})
			.build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {

		}
		assertNotNull(toolResults.get("result"));
	}

	@Test
	public void testReactAgentWithAllHooks() throws Exception {
		// Create maps to store results from each hook
		Map<String, String> prellmStore = new HashMap<>();
		Map<String, String> responseStore = new HashMap<>();
		Map<String, Object> toolParams = new HashMap<>();
		Map<String, Object> toolResults = new HashMap<>();

		ReactAgent agent = ReactAgent.builder().name("testAgent").chatClient(chatClient).state(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			keyStrategyHashMap.put("toolOutput", new ReplaceStrategy());
			keyStrategyHashMap.put("toolParams", new ReplaceStrategy());
			return keyStrategyHashMap;
		}).resolver(toolCallbackResolver).preLlmHook(state -> {
			prellmStore.put("timestamp", String.valueOf(System.currentTimeMillis()));
			return Map.of();
		}).postLlmHook(state -> {
			responseStore.put("response", "Processed: " + state.value("messages"));
			return Map.of();
		}).preToolHook(state -> {
			toolParams.put("timestamp", System.currentTimeMillis());
			return Map.of();
		}).postToolHook(state -> {
			toolResults.put("result", "collected: " + "tool output");
			return Map.of();
		}).build();

		CompiledGraph graph = agent.getAndCompileGraph();
		try {
			Optional<OverAllState> invoke = graph.invoke(Map.of("messages", List.of(new UserMessage("test"))));
		}
		catch (java.util.concurrent.CompletionException e) {
			// Ignore max iterations exception
		}

		// Verify all hooks were executed
		assertNotNull(prellmStore.get("timestamp"), "PreLLM hook should store timestamp");
		assertNotNull(responseStore.get("response"), "PostLLM hook should store response");
		assertNotNull(toolParams.get("timestamp"), "PreTool hook should store timestamp");
		assertNotNull(toolResults.get("result"), "PostTool hook should store result");
	}

}
