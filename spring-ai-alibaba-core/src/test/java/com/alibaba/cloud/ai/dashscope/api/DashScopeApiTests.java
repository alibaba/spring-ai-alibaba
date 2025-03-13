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
package com.alibaba.cloud.ai.dashscope.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatModel;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingModel;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingTextType;

/**
 * Tests for DashScopeApi class functionality
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
class DashScopeApiTests {

	private DashScopeApi dashScopeApi;

	private RestClient mockRestClient;

	@BeforeEach
	void setUp() {
		// Setup mock RestClient
		mockRestClient = mock(RestClient.class);

		// Initialize DashScopeApi with test API key
		dashScopeApi = new DashScopeApi("test-api-key");
	}

	@Test
	void testConstructorWithApiKey() {
		// Test constructor with only API key
		DashScopeApi api = new DashScopeApi("test-api-key");
		assertNotNull(api, "DashScopeApi should be created with API key");
	}

	@Test
	void testConstructorWithApiKeyAndWorkspaceId() {
		// Test constructor with API key and workspace ID
		DashScopeApi api = new DashScopeApi("test-api-key", "test-workspace-id");
		assertNotNull(api, "DashScopeApi should be created with API key and workspace ID");
	}

	@Test
	void testConstructorWithApiKeyWorkspaceIdAndBaseUrl() {
		// Test constructor with API key, workspace ID, and base URL
		DashScopeApi api = new DashScopeApi("test-api-key", "test-workspace-id", "https://test-base-url.com");
		assertNotNull(api, "DashScopeApi should be created with API key, workspace ID, and base URL");
	}

	@Test
	void testChatModelEnum() {
		// Test ChatModel enum values
		assertEquals("qwen-max", ChatModel.QWEN_MAX.getModel(), "ChatModel.QWEN_MAX should have value 'qwen-max'");
		assertEquals("qwen-max-longcontext", ChatModel.QWEN_MAX_LONGCONTEXT.getModel(),
				"ChatModel.QWEN_MAX_LONGCONTEXT should have value 'qwen-max-longcontext'");
		assertEquals("qwen-plus", ChatModel.QWEN_PLUS.getModel(), "ChatModel.QWEN_PLUS should have value 'qwen-plus'");
		assertEquals("qwen-turbo", ChatModel.QWEN_TURBO.getModel(),
				"ChatModel.QWEN_TURBO should have value 'qwen-turbo'");
	}

	@Test
	void testEmbeddingModelEnum() {
		// Test EmbeddingModel enum values
		assertEquals("text-embedding-v2", EmbeddingModel.EMBEDDING_V2.getValue(),
				"EmbeddingModel.EMBEDDING_V2 should have value 'text-embedding-v2'");
		assertEquals("text-embedding-v1", EmbeddingModel.EMBEDDING_V1.getValue(),
				"EmbeddingModel.EMBEDDING_V1 should have value 'text-embedding-v1'");
	}

	@Test
	void testEmbeddingTextTypeEnum() {
		// Test EmbeddingTextType enum values
		assertEquals("document", EmbeddingTextType.DOCUMENT.getValue(),
				"EmbeddingTextType.DOCUMENT should have value 'document'");
		assertEquals("query", EmbeddingTextType.QUERY.getValue(), "EmbeddingTextType.QUERY should have value 'query'");
	}

}
