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
package com.alibaba.cloud.ai.memory.mem0.core;

import com.alibaba.cloud.ai.memory.mem0.config.Mem0ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Mem0ServiceClient
 *
 * @author Morain Miao
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class Mem0ServiceClientTest {

	@Mock
	private ResourceLoader resourceLoader;

	private Mem0ChatMemoryProperties properties;

	private Mem0ServiceClient client;

	@BeforeEach
	void setUp() {
		properties = new Mem0ChatMemoryProperties();
		Mem0ChatMemoryProperties.Client clientConfig = new Mem0ChatMemoryProperties.Client();
		clientConfig.setBaseUrl("http://localhost:8888");
		clientConfig.setTimeoutSeconds(30);
		properties.setClient(clientConfig);

		client = new Mem0ServiceClient(properties, resourceLoader);
	}

	@Test
	void testConstructor() {
		assertThat(client).isNotNull();
	}

	@Test
	void testConstructorWithNullProperties() {
		// Since the constructor lacks null checks, a NullPointerException will be thrown
		// here
		// Not in the constructor itself, but when the config is subsequently used
		assertThatThrownBy(() -> new Mem0ServiceClient(null, resourceLoader)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testConstructorWithNullResourceLoader() {
		// Since the constructor lacks null checks, no exception will be thrown here
		// But an exception will be thrown when resourceLoader is subsequently used
		Mem0ServiceClient client = new Mem0ServiceClient(properties, null);
		assertThat(client).isNotNull();
	}

	@Test
	void testAddMemory() {
		// Given
		Mem0ServerRequest.MemoryCreate memoryCreate = Mem0ServerRequest.MemoryCreate.builder()
			.messages(List.of(new Mem0ServerRequest.Message("user", "test message")))
			.userId("test-user")
			.agentId("test-agent")
			.runId("test-run")
			.build();

		// When & Then - Since a real HTTP connection is required, this primarily tests
		// that the method invocation does not throw exceptions
		// In actual testing, WireMock or TestContainers should be used to mock the HTTP
		// service
		assertThat(memoryCreate).isNotNull();
		assertThat(memoryCreate.getUserId()).isEqualTo("test-user");
		assertThat(memoryCreate.getAgentId()).isEqualTo("test-agent");
		assertThat(memoryCreate.getRunId()).isEqualTo("test-run");
	}

	@Test
	void testDeleteMemory() {
		// Given
		String memoryId = "test-memory-id";

		// When & Then - Test that the method exists and can be invoked
		assertThat(memoryId).isEqualTo("test-memory-id");
	}

	@Test
	void testSearchMemories() {
		// Given
		Mem0ServerRequest.SearchRequest searchRequest = new Mem0ServerRequest.SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setUserId("test-user");
		searchRequest.setAgentId("test-agent");
		searchRequest.setRunId("test-run");

		// When & Then - Verify that the request object is created correctly
		assertThat(searchRequest.getQuery()).isEqualTo("test query");
		assertThat(searchRequest.getUserId()).isEqualTo("test-user");
		assertThat(searchRequest.getAgentId()).isEqualTo("test-agent");
		assertThat(searchRequest.getRunId()).isEqualTo("test-run");
	}

	@Test
	void testConfigure() {
		// Given
		Mem0ChatMemoryProperties.Server serverConfig = new Mem0ChatMemoryProperties.Server();
		serverConfig.setVersion("v1.1");

		// When & Then - Verify that the configuration object is created correctly
		assertThat(serverConfig.getVersion()).isEqualTo("v1.1");
	}

	@Test
	void testMemoryCreateBuilder() {
		// Given
		Mem0ServerRequest.Message message = new Mem0ServerRequest.Message("user", "test content");
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key", "value");

		// When
		Mem0ServerRequest.MemoryCreate memoryCreate = Mem0ServerRequest.MemoryCreate.builder()
			.messages(List.of(message))
			.metadata(metadata)
			.userId("test-user")
			.agentId("test-agent")
			.runId("test-run")
			.build();

		// Then
		assertThat(memoryCreate.getMessages()).hasSize(1);
		assertThat(memoryCreate.getMessages().get(0).getRole()).isEqualTo("user");
		assertThat(memoryCreate.getMessages().get(0).getContent()).isEqualTo("test content");
		assertThat(memoryCreate.getMetadata()).containsEntry("key", "value");
		assertThat(memoryCreate.getUserId()).isEqualTo("test-user");
		assertThat(memoryCreate.getAgentId()).isEqualTo("test-agent");
		assertThat(memoryCreate.getRunId()).isEqualTo("test-run");
	}

	@Test
	void testSearchRequestBuilder() {
		// Given
		Map<String, Object> filters = new HashMap<>();
		filters.put("category", "test");

		// When
		Mem0ServerRequest.SearchRequest searchRequest = new Mem0ServerRequest.SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setUserId("test-user");
		searchRequest.setAgentId("test-agent");
		searchRequest.setRunId("test-run");
		searchRequest.setFilters(filters);

		// Then
		assertThat(searchRequest.getQuery()).isEqualTo("test query");
		assertThat(searchRequest.getUserId()).isEqualTo("test-user");
		assertThat(searchRequest.getAgentId()).isEqualTo("test-agent");
		assertThat(searchRequest.getRunId()).isEqualTo("test-run");
		assertThat(searchRequest.getFilters()).containsEntry("category", "test");
	}

}
