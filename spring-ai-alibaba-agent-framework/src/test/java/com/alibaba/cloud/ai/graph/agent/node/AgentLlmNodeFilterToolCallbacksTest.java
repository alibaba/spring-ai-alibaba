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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AgentLlmNode#filterToolCallbacks(ModelRequest)} method.
 */
class AgentLlmNodeFilterToolCallbacksTest {

	private ChatClient mockChatClient;

	private Method filterToolCallbacksMethod;

	@BeforeEach
	void setUp() throws Exception {
		mockChatClient = mock(ChatClient.class);
		filterToolCallbacksMethod = AgentLlmNode.class.getDeclaredMethod("filterToolCallbacks", ModelRequest.class);
		filterToolCallbacksMethod.setAccessible(true);
	}

	private ToolCallback createMockToolCallback(String name) {
		ToolCallback callback = mock(ToolCallback.class);
		ToolDefinition toolDefinition = mock(ToolDefinition.class);
		when(toolDefinition.name()).thenReturn(name);
		when(callback.getToolDefinition()).thenReturn(toolDefinition);
		return callback;
	}

	private AgentLlmNode createNodeWithToolCallbacks(List<ToolCallback> toolCallbacks) {
		return AgentLlmNode.builder()
			.agentName("test-agent")
			.chatClient(mockChatClient)
			.toolCallbacks(toolCallbacks)
			.build();
	}

	@SuppressWarnings("unchecked")
	private List<ToolCallback> invokeFilterToolCallbacks(AgentLlmNode node, ModelRequest request) throws Exception {
		return (List<ToolCallback>) filterToolCallbacksMethod.invoke(node, request);
	}

	@Test
	void testFilterToolCallbacks_NullModelRequest_ReturnsAllNodeToolCallbacks() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		List<ToolCallback> result = invokeFilterToolCallbacks(node, null);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool2));
	}

	@Test
	void testFilterToolCallbacks_ModelRequestWithNullOptions_ReturnsAllNodeToolCallbacks() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.options(null)
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool2));
	}

	@Test
	void testFilterToolCallbacks_ModelRequestWithOptionsButNullToolCallbacks_ReturnsAllNodeToolCallbacks()
			throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ToolCallingChatOptions options = mock(ToolCallingChatOptions.class);
		when(options.getToolCallbacks()).thenReturn(null);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.options(options)
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool2));
	}

	@Test
	void testFilterToolCallbacks_ModelRequestWithToolCallbacksInOptions_ReturnsOptionsToolCallbacks() throws Exception {
		ToolCallback nodeTool = createMockToolCallback("nodeTool");
		List<ToolCallback> nodeToolCallbacks = List.of(nodeTool);

		ToolCallback optionsTool1 = createMockToolCallback("optionsTool1");
		ToolCallback optionsTool2 = createMockToolCallback("optionsTool2");
		List<ToolCallback> optionsToolCallbacks = List.of(optionsTool1, optionsTool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ToolCallingChatOptions options = mock(ToolCallingChatOptions.class);
		when(options.getToolCallbacks()).thenReturn(optionsToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.options(options)
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(optionsTool1));
		assertTrue(result.contains(optionsTool2));
	}

	@Test
	void testFilterToolCallbacks_WithEmptyRequestedTools_ReturnsAllToolCallbacks() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.tools(new ArrayList<>())
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool2));
	}

	@Test
	void testFilterToolCallbacks_WithNullRequestedTools_ReturnsAllToolCallbacks() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.tools(null)
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool2));
	}

	@Test
	void testFilterToolCallbacks_WithRequestedTools_FiltersToolCallbacks() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		ToolCallback tool3 = createMockToolCallback("tool3");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2, tool3);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.tools(List.of("tool1", "tool3"))
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(tool1));
		assertTrue(result.contains(tool3));
	}

	@Test
	void testFilterToolCallbacks_WithRequestedToolNotInCallbacks_ReturnsEmptyForMissingTool() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.tools(List.of("nonExistentTool"))
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFilterToolCallbacks_WithPartialMatchingRequestedTools_ReturnsOnlyMatchingTools() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		ToolCallback tool2 = createMockToolCallback("tool2");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1, tool2);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.tools(List.of("tool1", "nonExistentTool"))
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(1, result.size());
		assertTrue(result.contains(tool1));
	}

	@Test
	void testFilterToolCallbacks_WithOptionsToolCallbacksAndRequestedTools_FiltersOptionsToolCallbacks()
			throws Exception {
		ToolCallback nodeTool = createMockToolCallback("nodeTool");
		List<ToolCallback> nodeToolCallbacks = List.of(nodeTool);

		ToolCallback optionsTool1 = createMockToolCallback("optionsTool1");
		ToolCallback optionsTool2 = createMockToolCallback("optionsTool2");
		ToolCallback optionsTool3 = createMockToolCallback("optionsTool3");
		List<ToolCallback> optionsToolCallbacks = List.of(optionsTool1, optionsTool2, optionsTool3);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ToolCallingChatOptions options = mock(ToolCallingChatOptions.class);
		when(options.getToolCallbacks()).thenReturn(optionsToolCallbacks);

		ModelRequest request = ModelRequest.builder()
			.messages(new ArrayList<>())
			.options(options)
			.tools(List.of("optionsTool1", "optionsTool3"))
			.build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertEquals(2, result.size());
		assertTrue(result.contains(optionsTool1));
		assertTrue(result.contains(optionsTool3));
	}

	@Test
	void testFilterToolCallbacks_WithEmptyNodeToolCallbacks_ReturnsEmptyList() throws Exception {
		AgentLlmNode node = createNodeWithToolCallbacks(new ArrayList<>());

		ModelRequest request = ModelRequest.builder().messages(new ArrayList<>()).build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFilterToolCallbacks_ResultIsMutable() throws Exception {
		ToolCallback tool1 = createMockToolCallback("tool1");
		List<ToolCallback> nodeToolCallbacks = List.of(tool1);

		AgentLlmNode node = createNodeWithToolCallbacks(nodeToolCallbacks);

		ModelRequest request = ModelRequest.builder().messages(new ArrayList<>()).build();

		List<ToolCallback> result = invokeFilterToolCallbacks(node, request);

		ToolCallback newTool = createMockToolCallback("newTool");
		result.add(newTool);

		assertEquals(2, result.size());
	}

}
