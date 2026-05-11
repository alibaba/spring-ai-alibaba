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
package com.alibaba.cloud.ai.graph.agent.tools;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebFetchToolTest {

	private WebFetchTool webFetchTool;

	@BeforeEach
	void setUp() {
		ChatModel chatModel = mock(ChatModel.class);
		ChatResponse mockResponse = new ChatResponse(
				List.of(new Generation(new AssistantMessage("Mocked summary"))));
		when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

		ChatClient chatClient = ChatClient.builder(chatModel).build();
		webFetchTool = WebFetchTool.builder(chatClient)
			.maxContentLength(100_000)
			.maxCacheSize(100)
			.maxRetries(1)
			.buildWebFetchTool();
	}

	@Test
	void testEmptyUrlReturnsError() {
		String result = webFetchTool.apply(new WebFetchTool.Request("", "Summarize"), new ToolContext(Collections.emptyMap()));
		assertTrue(result.startsWith("Error:"));
		assertTrue(result.contains("URL cannot be empty"));
	}

	@Test
	void testBlankUrlReturnsError() {
		String result = webFetchTool.apply(new WebFetchTool.Request("   ", "Summarize"), new ToolContext(Collections.emptyMap()));
		assertTrue(result.startsWith("Error:"));
		assertTrue(result.contains("URL cannot be empty"));
	}

	@Test
	void testInvalidUrlFormatReturnsError() {
		String result = webFetchTool.apply(new WebFetchTool.Request("not-a-valid-url", "Summarize"),
				new ToolContext(Collections.emptyMap()));
		assertTrue(result.startsWith("Error:"));
		assertTrue(result.contains("Invalid URL"));
	}

	@Test
	void testInvalidUrlMissingSchemeReturnsError() {
		String result = webFetchTool.apply(new WebFetchTool.Request("example.com/path", "Summarize"),
				new ToolContext(Collections.emptyMap()));
		assertTrue(result.startsWith("Error:"));
		assertTrue(result.contains("Invalid URL"));
	}

	@Test
	void testBuilderBuildsToolCallback() {
		ChatModel chatModel = mock(ChatModel.class);
		ChatClient chatClient = ChatClient.builder(chatModel).build();
		var toolCallback = WebFetchTool.builder(chatClient).build();
		assertNotNull(toolCallback);
	}

}
