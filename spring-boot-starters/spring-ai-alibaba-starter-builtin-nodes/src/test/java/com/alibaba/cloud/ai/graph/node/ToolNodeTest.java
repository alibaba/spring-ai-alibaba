/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.node;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ToolNode}.
 */
public class ToolNodeTest {

	private static OverAllState stateOf(Map<String, Object> entries) {
		OverAllState state = new OverAllState();
		entries.forEach((k, v) -> {
			state.registerKeyAndStrategy(k, new ReplaceStrategy());
			state.updateState(Map.of(k, v));
		});
		return state;
	}

	private static ToolCallback toolCallback(String name, String result) {
		ToolCallback callback = mock(ToolCallback.class);
		ToolDefinition definition = mock(ToolDefinition.class);
		when(definition.name()).thenReturn(name);
		when(callback.getToolDefinition()).thenReturn(definition);
		when(callback.call(anyString(), any(ToolContext.class))).thenReturn(result);
		return callback;
	}

	private static AssistantMessage messageWithToolCalls(AssistantMessage.ToolCall... toolCalls) {
		return AssistantMessage.builder().content("").toolCalls(List.of(toolCalls)).build();
	}

	@Test
	public void testExecutesToolFromCallbackList() {
		ToolCallback calculator = toolCallback("calculator", "3");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{\"a\":1,\"b\":2}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator)).build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals(1, message.getResponses().size());
		ToolResponseMessage.ToolResponse response = message.getResponses().get(0);
		assertEquals("call_1", response.id());
		assertEquals("calculator", response.name());
		assertEquals("3", response.responseData());
	}

	@Test
	public void testFallsBackToResolverWhenToolNotInList() {
		ToolCallback weather = toolCallback("weather", "sunny");
		ToolCallbackResolver resolver = mock(ToolCallbackResolver.class);
		when(resolver.resolve("weather")).thenReturn(weather);

		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "weather", "{\"city\":\"Beijing\"}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		// Empty callback list forces resolution through the resolver.
		ToolNode node = ToolNode.builder().toolCallbackResolver(resolver).build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		verify(resolver).resolve("weather");
		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals("sunny", message.getResponses().get(0).responseData());
	}

	@Test
	public void testPrefersCallbackListOverResolver() {
		ToolCallback listed = toolCallback("calculator", "from-list");
		ToolCallbackResolver resolver = mock(ToolCallbackResolver.class);

		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(listed)).toolCallbackResolver(resolver).build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals("from-list", message.getResponses().get(0).responseData());
		verifyNoInteractions(resolver);
	}

	@Test
	public void testMultipleToolCallsProduceOrderedResponses() {
		ToolCallback calculator = toolCallback("calculator", "42");
		ToolCallback weather = toolCallback("weather", "25C");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{}"),
				new AssistantMessage.ToolCall("call_2", "function", "weather", "{}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator, weather)).build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals(2, message.getResponses().size());
		assertEquals("calculator", message.getResponses().get(0).name());
		assertEquals("42", message.getResponses().get(0).responseData());
		assertEquals("weather", message.getResponses().get(1).name());
		assertEquals("25C", message.getResponses().get(1).responseData());
	}

	@Test
	public void testCustomOutputKeyPopulatesBothKeys() {
		ToolCallback calculator = toolCallback("calculator", "3");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator)).outputKey("tool_result").build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		assertSame(result.get("messages"), result.get("tool_result"));
	}

	@Test
	public void testUsesCustomLlmResponseKey() {
		ToolCallback calculator = toolCallback("calculator", "3");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{}"));
		OverAllState state = stateOf(Map.of("custom_response", assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator)).llmResponseKey("custom_response").build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals("3", message.getResponses().get(0).responseData());
	}

	@Test
	public void testFallsBackToLastMessageWhenLlmResponseKeyAbsent() {
		ToolCallback calculator = toolCallback("calculator", "3");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{}"));
		// No 'llm_response' key registered; ToolNode should read the last entry of 'messages'.
		List<Message> messages = List.of(new AssistantMessage("ignored"), assistantMessage);
		OverAllState state = stateOf(Map.of("messages", messages));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator)).build();
		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));

		ToolResponseMessage message = (ToolResponseMessage) result.get("messages");
		assertEquals("3", message.getResponses().get(0).responseData());
	}

	@Test
	public void testToolContextCarriesState() {
		ToolCallback calculator = toolCallback("calculator", "3");
		AssistantMessage assistantMessage = messageWithToolCalls(
				new AssistantMessage.ToolCall("call_1", "function", "calculator", "{\"a\":1}"));
		OverAllState state = stateOf(Map.of(LlmNode.LLM_RESPONSE_KEY, assistantMessage));

		ToolNode node = ToolNode.builder().toolCallbacks(List.of(calculator)).build();
		assertDoesNotThrow(() -> node.apply(state));

		ArgumentCaptor<ToolContext> contextCaptor = ArgumentCaptor.forClass(ToolContext.class);
		verify(calculator).call(eq("{\"a\":1}"), contextCaptor.capture());
		assertSame(state, contextCaptor.getValue().getContext().get("state"));
	}

}
