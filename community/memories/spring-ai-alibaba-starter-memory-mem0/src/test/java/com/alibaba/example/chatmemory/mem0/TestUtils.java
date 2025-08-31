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

import com.alibaba.example.chatmemory.config.Mem0ChatMemoryProperties;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 测试工具类 提供常用的测试辅助方法
 *
 * @author Morain Miao
 * @since 1.0.0
 */
public class TestUtils {

	/**
	 * 创建测试用的Mem0ChatMemoryProperties
	 */
	public static Mem0ChatMemoryProperties createTestProperties() {
		Mem0ChatMemoryProperties properties = new Mem0ChatMemoryProperties();

		// 配置客户端
		Mem0ChatMemoryProperties.Client client = new Mem0ChatMemoryProperties.Client();
		client.setBaseUrl("http://localhost:8888");
		client.setTimeoutSeconds(30);
		properties.setClient(client);

		// 配置服务器
		Mem0ChatMemoryProperties.Server server = new Mem0ChatMemoryProperties.Server();
		server.setVersion("v1.1");
		properties.setServer(server);

		return properties;
	}

	/**
	 * 创建测试用的UserMessage
	 */
	public static UserMessage createTestUserMessage(String content) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(Mem0ChatMemoryAdvisor.USER_ID, "test-user-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.AGENT_ID, "test-agent-" + UUID.randomUUID());
		metadata.put(Mem0ChatMemoryAdvisor.RUN_ID, "test-run-" + UUID.randomUUID());

		return UserMessage.builder().text(content).metadata(metadata).build();
	}

	/**
	 * 创建测试用的Document
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
	 * 创建测试用的Document列表
	 */
	public static java.util.List<Document> createTestDocuments(int count) {
		java.util.List<Document> documents = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			documents.add(createTestDocument("test content " + i));
		}
		return documents;
	}

	/**
	 * 创建测试用的Mem0ServerRequest.MemoryCreate
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
	 * 创建测试用的Mem0ServerRequest.SearchRequest
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
	 * 创建测试用的Mem0ServerResp
	 */
	public static Mem0ServerResp createTestSearchResponse() {
		Mem0ServerResp response = new Mem0ServerResp();
		response.setResults(List.of());
		return response;
	}

	/**
	 * 生成随机UUID字符串
	 */
	public static String randomId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * 生成随机用户ID
	 */
	public static String randomUserId() {
		return "user-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 生成随机代理ID
	 */
	public static String randomAgentId() {
		return "agent-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 生成随机运行ID
	 */
	public static String randomRunId() {
		return "run-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 创建测试用的消息列表
	 */
	public static java.util.List<Message> createTestMessages(int count) {
		java.util.List<Message> messages = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			messages.add(createTestUserMessage("test message " + i));
		}
		return messages;
	}

	/**
	 * 创建测试用的元数据
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
