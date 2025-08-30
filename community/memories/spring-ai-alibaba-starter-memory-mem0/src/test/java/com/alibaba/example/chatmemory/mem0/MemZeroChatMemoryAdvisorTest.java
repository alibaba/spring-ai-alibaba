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
package com.alibaba.example.chatmemory.mem0;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试 for MemZeroChatMemoryAdvisor
 *
 * @author Morain Miao
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MemZeroChatMemoryAdvisorTest {

	@Mock
	private VectorStore vectorStore;

	@Mock
	private AdvisorChain advisorChain;

	private MemZeroChatMemoryAdvisor advisor;

	private Scheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = Schedulers.immediate();
		PromptTemplate systemPromptTemplate = new PromptTemplate("Test template: {query}");
		advisor = new MemZeroChatMemoryAdvisor(systemPromptTemplate, 1, scheduler, vectorStore);
	}

	@Test
	void testConstructor() {
		assertThat(advisor).isNotNull();
	}

	@Test
	void testBeforeWithValidRequest() {
		// Given
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		metadata.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		metadata.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");

		UserMessage userMessage = UserMessage.builder().text("test query").metadata(metadata).build();
		Prompt prompt = new Prompt(userMessage);
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		context.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		context.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");
		ChatClientRequest request = new ChatClientRequest(prompt, context);

		Document mockDocument = new Document("test memory content");
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDocument));

		// When
		ChatClientRequest result = advisor.before(request, advisorChain);

		// Then
		assertThat(result).isNotNull();
		// 验证返回的请求包含增强的用户消息
		assertThat(result.prompt().getUserMessage()).isNotNull();
		assertThat(result.prompt().getUserMessage().getText()).contains("test query");
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void testBeforeWithNullUserMessage() {
		// Given
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		// 使用仅包含 AssistantMessage 的 Prompt，确保 getUserMessage() 返回 null
		Prompt prompt = new Prompt(List.of(new AssistantMessage("assistant-only")));
		ChatClientRequest request = new ChatClientRequest(prompt, context);

		// When
		ChatClientRequest result = advisor.before(request, advisorChain);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.prompt().getUserMessage()).isNotNull();
		assertThat(result.prompt().getUserMessage().getText()).contains("Test template:");
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void testBeforeWithEmptyMetadata() {
		// Given
		// 明确提供一个空的 metadata，触发断言
		UserMessage userMessage = UserMessage.builder().text("test query").metadata(new HashMap<>()).build();
		Prompt prompt = new Prompt(userMessage);
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		ChatClientRequest request = new ChatClientRequest(prompt, context);

		// When
		ChatClientRequest result = advisor.before(request, advisorChain);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.prompt().getUserMessage()).isNotNull();
		assertThat(result.prompt().getUserMessage().getText()).contains("test query");
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void testAfter() {
		// Given
		UserMessage userMessage = UserMessage.builder().text("test query").build();
		AssistantMessage assistantMessage = new AssistantMessage("test response");
		Prompt prompt = new Prompt(List.of(userMessage, assistantMessage));
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		ChatClientRequest request = new ChatClientRequest(prompt, context);
		ChatClientResponse response = mock(ChatClientResponse.class);

		// When
		ChatClientResponse result = advisor.after(response, advisorChain);

		// Then
		assertThat(result).isEqualTo(response);
	}

	@Test
	void testGetOrder() {
		// Given
		PromptTemplate systemPromptTemplate = new PromptTemplate("Test template: {query}");
		MemZeroChatMemoryAdvisor customOrderAdvisor = new MemZeroChatMemoryAdvisor(systemPromptTemplate, 5, scheduler,
				vectorStore);

		// When
		int order = customOrderAdvisor.getOrder();

		// Then
		assertThat(order).isEqualTo(5);
	}

	@Test
	void testConstants() {
		// Then
		assertThat(MemZeroChatMemoryAdvisor.USER_ID).isEqualTo("user_id");
		assertThat(MemZeroChatMemoryAdvisor.AGENT_ID).isEqualTo("agent_id");
		assertThat(MemZeroChatMemoryAdvisor.RUN_ID).isEqualTo("run_id");
		assertThat(MemZeroChatMemoryAdvisor.FILTERS).isEqualTo("filters");
	}

	@Test
	void testBeforeWithFilters() {
		// Given
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		metadata.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		metadata.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");

		UserMessage userMessage = UserMessage.builder().text("test query").metadata(metadata).build();
		Prompt prompt = new Prompt(userMessage);
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		context.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		context.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");
		Map<String, Object> filters = new HashMap<>();
		filters.put("category", "test");
		context.put(MemZeroChatMemoryAdvisor.FILTERS, filters);
		ChatClientRequest request = new ChatClientRequest(prompt, context);

		Document mockDocument = new Document("test memory content");
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDocument));

		// When
		ChatClientRequest result = advisor.before(request, advisorChain);

		// Then
		assertThat(result).isNotNull();
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

	@Test
	void testBeforeWithNoSearchResults() {
		// Given
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		metadata.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		metadata.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");

		UserMessage userMessage = UserMessage.builder().text("test query").metadata(metadata).build();
		Prompt prompt = new Prompt(userMessage);
		Map<String, Object> context = new HashMap<>();
		context.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		context.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		context.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");
		ChatClientRequest request = new ChatClientRequest(prompt, context);

		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		// When
		ChatClientRequest result = advisor.before(request, advisorChain);

		// Then
		assertThat(result).isNotNull();
		// 验证返回的请求包含增强的用户消息，但没有记忆内容
		assertThat(result.prompt().getUserMessage()).isNotNull();
		assertThat(result.prompt().getUserMessage().getText()).contains("test query");
		verify(vectorStore).similaritySearch(any(SearchRequest.class));
	}

}
