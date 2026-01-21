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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for Issue #4094: https://github.com/alibaba/spring-ai-alibaba/issues/4094
 *
 * This test ensures that toolCallbacks are correctly managed even when chatOptions is not
 * explicitly provided during ReactAgent construction.
 */
public class Issue4094BugReproductionTest {

	private ChatModel mockChatModel;

	@BeforeEach
	void setUp() {
		mockChatModel = new MockChatModel();
	}

	@Test
	@DisplayName("Scenario 1: Set chatOptions and tools -> tools should be passed correctly")
	void testWithChatOptionsAndTools() throws Exception {
		System.out.println("\n" + "=".repeat(60));
		System.out.println("Scenario 1: Set chatOptions and tools");
		System.out.println("=".repeat(60));

		ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
				.temperature(0.7)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-with-options")
				.model(mockChatModel)
				.chatOptions(chatOptions)
				.tools(ToolCallbacks.from(new MyTools()))
				.saver(new MemorySaver())
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		ToolCallingChatOptions internalOptions = getChatOptions(llmNode);
		List<ToolCallback> toolCallbacksField = getToolCallbacksField(llmNode);

		System.out.println("AgentLlmNode.chatOptions is null: " + (internalOptions == null));
		System.out.println("AgentLlmNode.toolCallbacks count: " + toolCallbacksField.size());
		if (internalOptions != null && internalOptions.getToolCallbacks() != null) {
			System.out.println("chatOptions.toolCallbacks count: " + internalOptions.getToolCallbacks().size());
		}

		List<String> toolNames = toolCallbacksField.stream()
				.map(tc -> tc.getToolDefinition().name())
				.toList();
		ModelRequest modelRequest = ModelRequest.builder()
				.messages(List.of(new UserMessage("test")))
				.options(internalOptions)
				.tools(toolNames)
				.build();

		List<ToolCallback> filtered = invokeFilterToolCallbacks(llmNode, modelRequest);
		System.out.println("filterToolCallbacks returned count: " + filtered.size());

		assertNotNull(internalOptions, "chatOptions should not be null");
		assertFalse(filtered.isEmpty(), "filtered tools should not be empty");
		assertEquals(1, filtered.size(), "should have 1 tool");

		System.out.println("Scenario 1 passed");
	}

	@Test
	@DisplayName("Scenario 2 (Fix verification): tools should be preserved when chatOptions is null")
	void testToolsArePreservedWhenChatOptionsIsNull() throws Exception {
		System.out.println("\n" + "=".repeat(60));
		System.out.println("Scenario 2 (Fix verification): No chatOptions + tools");
		System.out.println("=".repeat(60));

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent-no-options")
				.model(mockChatModel)
				// .chatOptions(...)  // Intentional: chatOptions is not set
				.tools(ToolCallbacks.from(new MyTools()))
				.saver(new MemorySaver())
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		ToolCallingChatOptions internalOptions = getChatOptions(llmNode);
		List<ToolCallback> toolCallbacksField = getToolCallbacksField(llmNode);

		System.out.println("AgentLlmNode.chatOptions is null: " + (internalOptions == null));
		System.out.println("AgentLlmNode.toolCallbacks count: " + toolCallbacksField.size());

		assertNotNull(internalOptions, "chatOptions should be automatically created");
		assertNotNull(internalOptions.getToolCallbacks(), "chatOptions.toolCallbacks should not be null");
		assertFalse(internalOptions.getToolCallbacks().isEmpty(), "chatOptions.toolCallbacks should not be empty");

		System.out.println("chatOptions.toolCallbacks count: " + internalOptions.getToolCallbacks().size());

		List<String> toolNames = toolCallbacksField.stream()
				.map(tc -> tc.getToolDefinition().name())
				.toList();

		ModelRequest modelRequest = ModelRequest.builder()
				.messages(List.of(new UserMessage("test")))
				.options(internalOptions)
				.tools(toolNames)
				.build();

		List<ToolCallback> filtered = invokeFilterToolCallbacks(llmNode, modelRequest);

		System.out.println("\n----- filterToolCallbacks verification -----");
		System.out.println("modelRequest.getOptions() is null: " + (modelRequest.getOptions() == null));
		System.out.println("filterToolCallbacks returned count: " + filtered.size());

		assertFalse(filtered.isEmpty(), "filterToolCallbacks returned tools should not be empty");
		assertEquals("testTool", filtered.get(0).getToolDefinition().name(), "testTool should be preserved");

		System.out.println("Scenario 2 passed: tools are preserved even if chatOptions is null");
	}

	private AgentLlmNode getLlmNode(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		return (AgentLlmNode) llmNodeField.get(agent);
	}

	private ToolCallingChatOptions getChatOptions(AgentLlmNode llmNode) throws Exception {
		Field chatOptionsField = AgentLlmNode.class.getDeclaredField("chatOptions");
		chatOptionsField.setAccessible(true);
		return (ToolCallingChatOptions) chatOptionsField.get(llmNode);
	}

	@SuppressWarnings("unchecked")
	private List<ToolCallback> getToolCallbacksField(AgentLlmNode llmNode) throws Exception {
		Field toolCallbacksField = AgentLlmNode.class.getDeclaredField("toolCallbacks");
		toolCallbacksField.setAccessible(true);
		return (List<ToolCallback>) toolCallbacksField.get(llmNode);
	}

	@SuppressWarnings("unchecked")
	private List<ToolCallback> invokeFilterToolCallbacks(AgentLlmNode llmNode, ModelRequest modelRequest)
			throws Exception {
		Method method = AgentLlmNode.class.getDeclaredMethod("filterToolCallbacks", ModelRequest.class);
		method.setAccessible(true);
		return (List<ToolCallback>) method.invoke(llmNode, modelRequest);
	}

	static class MockChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Mock response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock stream response")))));
		}
	}

	static class MyTools {
		@Tool(description = "A test tool")
		public String testTool(String input) {
			return "result: " + input;
		}
	}
}
