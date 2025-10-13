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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatModel;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingModel;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingRequestInput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingRequestInputParameters;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingTextType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

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
		dashScopeApi = DashScopeApi.builder().apiKey("test-api-key").build();
	}

	@Test
	void testChatModelEnum() {
		System.out.println("output: " + dashScopeApi.getApiKey());
		// Test ChatModel enum values
		assertEquals("qwen-max", ChatModel.QWEN_MAX.getValue(), "ChatModel.QWEN_MAX should have value 'qwen-max'");
		assertEquals("qwen-max-longcontext", ChatModel.QWEN_MAX_LONGCONTEXT.getValue(),
				"ChatModel.QWEN_MAX_LONGCONTEXT should have value 'qwen-max-longcontext'");
		assertEquals("qwen-plus", ChatModel.QWEN_PLUS.getValue(), "ChatModel.QWEN_PLUS should have value 'qwen-plus'");
		assertEquals("qwen-turbo", ChatModel.QWEN_TURBO.getValue(),
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

	@Test
	void testEmbeddingRequestWithNullTextType() {
		// Test null textType handling in EmbeddingRequestInputParameters.Builder
		EmbeddingRequestInputParameters params = EmbeddingRequestInputParameters.builder()
				.textType(null)
				.build();
		
		// Should default to "document" when textType is null
		assertEquals("document", params.textType(), "Null textType should default to 'document'");
	}

	@Test
	void testEmbeddingRequestWithEmptyTextType() {
		// Test empty string textType handling
		EmbeddingRequestInputParameters params = EmbeddingRequestInputParameters.builder()
				.textType("")
				.build();
		
		// Empty string should be preserved (not converted to null)
		assertEquals("", params.textType(), "Empty textType should be preserved");
	}

	@Test
	void testEmbeddingRequestWithValidTextType() {
		// Test valid textType values
		EmbeddingRequestInputParameters queryParams = EmbeddingRequestInputParameters.builder()
				.textType("query")
				.build();
		assertEquals("query", queryParams.textType(), "Valid textType 'query' should be preserved");

		EmbeddingRequestInputParameters docParams = EmbeddingRequestInputParameters.builder()
				.textType("document")
				.build();
		assertEquals("document", docParams.textType(), "Valid textType 'document' should be preserved");
	}

	@Test
	void testEmbeddingRequestBuilderWithNullTextType() {
		// Test EmbeddingRequest creation with null textType through constructor
		EmbeddingRequestInput input = new EmbeddingRequestInput(List.of("text1", "text2"));
		EmbeddingRequestInputParameters params = EmbeddingRequestInputParameters.builder()
				.textType(null)
				.build();
		EmbeddingRequest request = new EmbeddingRequest("test-model", input, params);
		
		// Verify the request was created successfully with default textType
		assertNotNull(request, "EmbeddingRequest should be created successfully");
		assertEquals("document", request.parameters().textType(), 
				"Request should have default textType 'document' when null is provided");
	}

}
