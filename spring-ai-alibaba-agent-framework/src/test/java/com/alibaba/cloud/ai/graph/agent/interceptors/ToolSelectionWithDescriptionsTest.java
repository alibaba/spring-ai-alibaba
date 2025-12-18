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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ToolSelectionInterceptor with tool descriptions.
 * Verifies that tool descriptions are properly included in the selection prompt.
 */
class ToolSelectionWithDescriptionsTest {

	@Mock
	private ChatModel selectionModel;

	@Mock
	private ModelCallHandler handler;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testToolSelectionPromptIncludesDescriptions() {
		// Setup mock response for tool selection
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"weather_tool\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);

		// Setup mock handler response
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Weather is sunny")));

		// Create interceptor
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(2)
			.build();

		// Create request with tool descriptions
		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("weather_tool", "Get current weather information for any city worldwide");
		toolDescriptions.put("ticket_tool", "Book train or flight tickets for travel");
		toolDescriptions.put("hotel_tool", "Search and reserve hotel accommodations");

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("What's the weather in Shanghai?")))
			.tools(List.of("weather_tool", "ticket_tool", "hotel_tool"))
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		// Execute interceptor
		interceptor.interceptModel(request, handler);

		// Capture the prompt sent to selection model
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(selectionModel, times(1)).call(promptCaptor.capture());

		// Verify prompt contains tool descriptions
		Prompt capturedPrompt = promptCaptor.getValue();
		String promptContent = capturedPrompt.getContents();

		assertTrue(promptContent.contains("weather_tool"),
			"Prompt should contain weather_tool name");
		assertTrue(promptContent.contains("Get current weather information for any city worldwide"),
			"Prompt should contain weather_tool description");
		assertTrue(promptContent.contains("ticket_tool"),
			"Prompt should contain ticket_tool name");
		assertTrue(promptContent.contains("Book train or flight tickets for travel"),
			"Prompt should contain ticket_tool description");
		assertTrue(promptContent.contains("hotel_tool"),
			"Prompt should contain hotel_tool name");
		assertTrue(promptContent.contains("Search and reserve hotel accommodations"),
			"Prompt should contain hotel_tool description");
	}

	@Test
	void testToolSelectionWithPartialDescriptions() {
		// Setup mock response
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"tool_with_desc\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(1)  // Changed from 2 to 1 to trigger selection
			.build();

		// Create request with partial descriptions (some tools have descriptions, some don't)
		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("tool_with_desc", "This tool has a description");
		// tool_without_desc intentionally not added

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("Test query")))
			.tools(List.of("tool_with_desc", "tool_without_desc", "tool_third"))  // Added third tool to exceed maxTools
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		// Execute interceptor
		interceptor.interceptModel(request, handler);

		// Capture the prompt
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(selectionModel).call(promptCaptor.capture());

		String promptContent = promptCaptor.getValue().getContents();

		// Tool with description should have description in prompt
		assertTrue(promptContent.contains("tool_with_desc: This tool has a description"),
			"Tool with description should show description");

		// Tool without description should still be listed (just without description)
		assertTrue(promptContent.contains("tool_without_desc"),
			"Tool without description should still be listed");
	}

	@Test
	void testToolSelectionWithNullDescriptions() {
		// Setup mock response
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"tool1\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(1)
			.build();

		// Create request without tool descriptions (null)
		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("Test query")))
			.tools(List.of("tool1", "tool2", "tool3"))
			.context(new HashMap<>())
			.build();

		// Execute interceptor - should not fail
		ModelResponse response = interceptor.interceptModel(request, handler);

		// Verify it still works
		assertNotNull(response);

		// Capture the prompt
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(selectionModel).call(promptCaptor.capture());

		String promptContent = promptCaptor.getValue().getContents();

		// Tools should be listed without descriptions
		assertTrue(promptContent.contains("tool1"), "tool1 should be in prompt");
		assertTrue(promptContent.contains("tool2"), "tool2 should be in prompt");
		assertTrue(promptContent.contains("tool3"), "tool3 should be in prompt");
	}

	@Test
	void testToolSelectionWithEmptyDescription() {
		// Setup mock response
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"tool1\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(1)
			.build();

		// Create request with empty string description
		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("tool1", "Valid description");
		toolDescriptions.put("tool2", ""); // Empty description
		toolDescriptions.put("tool3", null); // Null description

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("Test query")))
			.tools(List.of("tool1", "tool2", "tool3"))
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		// Execute interceptor
		interceptor.interceptModel(request, handler);

		// Capture the prompt
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(selectionModel).call(promptCaptor.capture());

		String promptContent = promptCaptor.getValue().getContents();

		// tool1 should have description
		assertTrue(promptContent.contains("tool1: Valid description"),
			"tool1 should have its description");
	}

	@Test
	void testToolSelectionSkippedWhenToolsWithinLimit() {
		// Create interceptor with maxTools = 5
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(5)
			.build();

		// Create handler response
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		// Create request with only 2 tools (less than maxTools)
		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("tool1", "Description 1");
		toolDescriptions.put("tool2", "Description 2");

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("Test query")))
			.tools(List.of("tool1", "tool2"))
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		// Execute interceptor
		interceptor.interceptModel(request, handler);

		// Selection model should NOT be called when tools <= maxTools
		verify(selectionModel, times(0)).call(any(Prompt.class));

		// Handler should be called directly with original request
		verify(handler, times(1)).call(request);
	}

	@Test
	void testToolSelectionFiltersToolsCorrectly() {
		// Setup mock to return specific tools
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"weather_tool\", \"hotel_tool\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(2)
			.build();

		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("weather_tool", "Weather info");
		toolDescriptions.put("ticket_tool", "Ticket booking");
		toolDescriptions.put("hotel_tool", "Hotel booking");

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("I need weather and hotel info")))
			.tools(List.of("weather_tool", "ticket_tool", "hotel_tool"))
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		// Execute interceptor
		interceptor.interceptModel(request, handler);

		// Capture the request passed to handler
		ArgumentCaptor<ModelRequest> requestCaptor = ArgumentCaptor.forClass(ModelRequest.class);
		verify(handler).call(requestCaptor.capture());

		ModelRequest filteredRequest = requestCaptor.getValue();

		// Verify only selected tools are passed to handler
		assertEquals(2, filteredRequest.getTools().size());
		assertTrue(filteredRequest.getTools().contains("weather_tool"));
		assertTrue(filteredRequest.getTools().contains("hotel_tool"));
	}

	@Test
	void testToolSelectionPromptFormat() {
		// Setup mock response
		ChatResponse mockResponse = createMockChatResponse("{\"tools\": [\"api_tool\"]}");
		when(selectionModel.call(any(Prompt.class))).thenReturn(mockResponse);
		when(handler.call(any(ModelRequest.class)))
			.thenReturn(ModelResponse.of(new AssistantMessage("Done")));

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(selectionModel)
			.maxTools(1)
			.build();

		Map<String, String> toolDescriptions = new HashMap<>();
		toolDescriptions.put("api_tool", "Call external API endpoints");

		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("Call the API")))
			.tools(List.of("api_tool", "db_tool"))
			.toolDescriptions(toolDescriptions)
			.context(new HashMap<>())
			.build();

		interceptor.interceptModel(request, handler);

		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(selectionModel).call(promptCaptor.capture());

		String promptContent = promptCaptor.getValue().getContents();

		// Verify format: "- toolName: description"
		assertTrue(promptContent.contains("- api_tool: Call external API endpoints"),
			"Prompt should use format '- toolName: description'");
	}

	private ChatResponse createMockChatResponse(String content) {
		AssistantMessage assistantMessage = new AssistantMessage(content);
		Generation generation = new Generation(assistantMessage);
		return new ChatResponse(List.of(generation));
	}

}
