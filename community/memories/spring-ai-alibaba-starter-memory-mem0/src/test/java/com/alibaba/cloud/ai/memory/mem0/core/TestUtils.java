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

import com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor;
import com.alibaba.cloud.ai.memory.mem0.config.Mem0ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerResp;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test utility class providing common helper methods for testing
 *
 * @author Morain Miao
 * @since 1.0.0
 */
public class TestUtils {

	/**
	 * Creates test Mem0ChatMemoryProperties for testing
	 */
	public static Mem0ChatMemoryProperties createTestProperties() {
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();

		// Configure client
		Mem0ChatMemoryProperties.Client client = new Mem0ChatMemoryProperties.Client();
		client.setBaseUrl("http://localhost:8888");
		client.setTimeoutSeconds(30);
		properties.setClient(client);

		// Configure server
		Mem0ChatMemoryProperties.Server server = new Mem0ChatMemoryProperties.Server();
		server.setVersion("v1.1");
		properties.setServer(server);

		return properties;
	}

	/**
	 * Creates test UserMessage for testing
	 */
	public static UserMessage createTestUserMessage(String content) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(Mem0ChatMemoryAdvisor.USER_ID, "test-user-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.AGENT_ID, "test-agent-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.RUN_ID, "test-run-" + UUID.randomUUID());

		return UserMessage.builder().text(content).metadata(metadata).build();
	}

	/**
	 * Creates test Document for testing
	 */
	public static Document createTestDocument(String content) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("role", "user");
		metadata.put(Mem0ChatMemoryAdvisor.USER_ID, "test-user-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.AGENT_ID, "test-agent-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.RUN_ID, "test-run-" + UUID.randomUUID());

		return new Document(content, metadata);
	}

	/**
	 * Creates test Document list for testing
	 */
	public static java.util.List<Document> createTestDocuments(int count) {
		java.util.List<Document> documents = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			documents.add(createTestDocument("test content " + i));
		}
		return documents;
	}

	/**
	 * Creates Mem0ServerRequest.MemoryCreate for testing
	 */
	public static Mem0ServerRequest.MemoryCreate createTestMemoryCreate() {
		return Mem0ServerRequest.MemoryCreate.builder()
			.messages(List.of(new Mem0ServerRequest.Message("user", "test message")))
			.userId("test-user-" + UUID.randomUUID())
			.agentId("test-agent-" + UUID.randomUUID())
			.runId("test-run-" + UUID.randomUUID())
			.build();
	}

	/**
	 * Creates Mem0ServerRequest.SearchRequest for testing
	 */
	public static Mem0ServerRequest.SearchRequest createTestSearchRequest() {
		Mem0ServerRequest.SearchRequest searchRequest = new Mem0ServerRequest.SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setUserId("test-user-" + UUID.randomUUID());
		searchRequest.setAgentId("test-agent-" + UUID.randomUUID());
		searchRequest.setRunId("test-run-" + UUID.randomUUID());

		return searchRequest;
	}

	/**
	 * Creates Mem0ServerResp for testing
	 */
	public static Mem0ServerResp createTestSearchResponse() {
		Mem0ServerResp response = new Mem0ServerResp();
		response.setResults(List.of());
		return response;
	}

	/**
	 * Generates a random UUID string
	 */
	public static String randomId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generates a random user ID
	 */
	public static String randomUserId() {
		return "user-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Generates a random agent ID
	 */
	public static String randomAgentId() {
		return "agent-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Generates a random run ID
	 */
	public static String randomRunId() {
		return "run-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Creates test message list for testing
	 */
	public static java.util.List<Message> createTestMessages(int count) {
		java.util.List<Message> messages = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			messages.add(createTestUserMessage("test message " + i));
		}
		return messages;
	}

	/**
	 * Creates test metadata for testing
	 */
	public static Map<String, Object> createTestMetadata() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(Mem0ChatMemoryAdvisor.USER_ID, randomUserId());
		metadata.put(Mem0ChatMemoryAdvisor.AGENT_ID, randomAgentId());
		metadata.put(Mem0ChatMemoryAdvisor.RUN_ID, randomRunId());
		metadata.put("role", "user");
		metadata.put("timestamp", System.currentTimeMillis());
		return metadata;
	}

}
