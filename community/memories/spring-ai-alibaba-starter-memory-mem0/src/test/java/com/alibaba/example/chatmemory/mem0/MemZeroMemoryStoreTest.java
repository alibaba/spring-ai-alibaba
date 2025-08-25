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
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试 for MemZeroMemoryStore
 *
 * @author Morain Miao
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MemZeroMemoryStoreTest {

	@Mock
	private MemZeroServiceClient mem0Client;

	private MemZeroMemoryStore memoryStore;

	@BeforeEach
	void setUp() {
		memoryStore = MemZeroMemoryStore.builder(mem0Client).build();
	}

	@Test
	void testBuilder() {
		// When
		MemZeroMemoryStore store = MemZeroMemoryStore.builder(mem0Client).build();

		// Then
		assertThat(store).isNotNull();
	}

	@Test
	void testAddDocuments() {
		// Given
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("role", "user");
		metadata.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user");
		metadata.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent");
		metadata.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run");

		Document document = new Document("test content", metadata);
		List<Document> documents = List.of(document);

		// When
		memoryStore.add(documents);

		// Then
		verify(mem0Client, times(1)).addMemory(any(MemZeroServerRequest.MemoryCreate.class));
	}

	@Test
	void testAddDocumentsWithMissingMetadata() {
		// Given
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("role", "user");
		// 缺少必要的metadata

		Document document = new Document("test content", metadata);
		List<Document> documents = List.of(document);

		// When
		memoryStore.add(documents);

		// Then
		verify(mem0Client, times(1)).addMemory(any(MemZeroServerRequest.MemoryCreate.class));
	}

	@Test
	void testDeleteByIds() {
		// Given
		List<String> ids = List.of("id1", "id2", "id3");

		// When
		memoryStore.delete(ids);

		// Then
		verify(mem0Client, times(1)).deleteMemory("id1");
		verify(mem0Client, times(1)).deleteMemory("id2");
		verify(mem0Client, times(1)).deleteMemory("id3");
	}

	@Test
	void testDeleteByFilterExpression() {
		// Given
		Filter.Expression filterExpression = mock(Filter.Expression.class);

		// When & Then
		assertThatThrownBy(() -> memoryStore.delete(filterExpression)).isInstanceOf(UnsupportedOperationException.class)
			.hasMessageContaining(
					"The Mem0 Server only supports delete operation that must include userId, agentId, or runId");
	}

	@Test
	void testSimilaritySearchWithString() {
		// When & Then
		assertThatThrownBy(() -> memoryStore.similaritySearch("test query"))
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessageContaining("The Mem0 Server only supports queries that must include userId, agentId, or runId");
	}

	@Test
	void testSimilaritySearchWithSearchRequest() {
		// Given
		MemZeroServerRequest.SearchRequest searchRequest = new MemZeroServerRequest.SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setUserId("test-user");
		searchRequest.setAgentId("test-agent");
		searchRequest.setRunId("test-run");

		MemZeroServerResp mockResponse = new MemZeroServerResp();
		mockResponse.setResults(List.of());
		when(mem0Client.searchMemories(any(MemZeroServerRequest.SearchRequest.class))).thenReturn(mockResponse);

		// When
		List<Document> result = memoryStore.similaritySearch(searchRequest);

		// Then
		assertThat(result).isNotNull();
		verify(mem0Client).searchMemories(searchRequest);
	}

	@Test
	void testSimilaritySearchWithSearchRequestAndFilter() {
		// Given
		MemZeroServerRequest.SearchRequest searchRequest = new MemZeroServerRequest.SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setUserId("test-user");
		searchRequest.setAgentId("test-agent");
		searchRequest.setRunId("test-run");

		MemZeroServerResp mockResponse = new MemZeroServerResp();
		mockResponse.setResults(List.of());
		when(mem0Client.searchMemories(any(MemZeroServerRequest.SearchRequest.class))).thenReturn(mockResponse);

		// When
		List<Document> result = memoryStore.similaritySearch(searchRequest);

		// Then
		assertThat(result).isNotNull();
		verify(mem0Client).searchMemories(searchRequest);
	}

	@Test
	void testAfterPropertiesSet() throws Exception {
		// When
		memoryStore.afterPropertiesSet();

		// Then - 不应该抛出异常
		assertThat(memoryStore).isNotNull();
	}

	@Test
	void testAddMultipleDocuments() {
		// Given
		Map<String, Object> metadata1 = new HashMap<>();
		metadata1.put("role", "user");
		metadata1.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user-1");
		metadata1.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent-1");
		metadata1.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run-1");

		Map<String, Object> metadata2 = new HashMap<>();
		metadata2.put("role", "assistant");
		metadata2.put(MemZeroChatMemoryAdvisor.USER_ID, "test-user-2");
		metadata2.put(MemZeroChatMemoryAdvisor.AGENT_ID, "test-agent-2");
		metadata2.put(MemZeroChatMemoryAdvisor.RUN_ID, "test-run-2");

		Document document1 = new Document("test content 1", metadata1);
		Document document2 = new Document("test content 2", metadata2);
		List<Document> documents = List.of(document1, document2);

		// When
		memoryStore.add(documents);

		// Then
		verify(mem0Client, times(2)).addMemory(any(MemZeroServerRequest.MemoryCreate.class));
	}

	@Test
	void testDeleteEmptyIdList() {
		// Given
		List<String> ids = List.of();

		// When
		memoryStore.delete(ids);

		// Then
		verify(mem0Client, never()).deleteMemory(anyString());
	}

}
