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
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.EmbeddingTextType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void testEmbeddingRequestInputParametersNullTextTypeHandling() {
		// Test null textType handling for FastJson compatibility.
		DashScopeApi.EmbeddingRequestInputParameters parameters = DashScopeApi.EmbeddingRequestInputParameters.builder()
			.textType(null)
			.dimension(1024)
			.build();
			
		// Should use default when null.
		assertEquals(DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE, parameters.textType(),
				"Null textType should be replaced with default embedding text type");
	}

	@Test
	void testEmbeddingRequestInputParametersEmptyTextTypeHandling() {
		// Test empty textType preservation.
		DashScopeApi.EmbeddingRequestInputParameters parameters = DashScopeApi.EmbeddingRequestInputParameters.builder()
			.textType("")
			.dimension(1024)
			.build();
			
		// Should preserve user's explicit empty string.
		assertEquals("", parameters.textType(),
				"Empty textType should be preserved as user's explicit choice");
	}

	@Test
	void testEmbeddingRequestInputParametersValidTextTypeHandling() {
		// Test valid textType preservation.
		DashScopeApi.EmbeddingRequestInputParameters documentsParameters = DashScopeApi.EmbeddingRequestInputParameters.builder()
			.textType("document")
			.dimension(1024)
			.build();
			
		assertEquals("document", documentsParameters.textType(),
				"Valid textType should be preserved");
				
		DashScopeApi.EmbeddingRequestInputParameters queryParameters = DashScopeApi.EmbeddingRequestInputParameters.builder()
			.textType("query")
			.dimension(1024)
			.build();
			
		assertEquals("query", queryParameters.textType(),
				"Valid textType should be preserved");
	}

	@Test
	void testDeprecatedEmbeddingRequestConstructorNullTextTypeHandling() {
		// Test deprecated single text constructor with null textType
		@SuppressWarnings("deprecation")
		DashScopeApi.EmbeddingRequest singleTextRequest = new DashScopeApi.EmbeddingRequest(
				"test text", "test-model", null);
		
		assertEquals(DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE, singleTextRequest.parameters().textType(),
				"Deprecated constructor should handle null textType");
		
		// Test deprecated list texts constructor with null textType
		@SuppressWarnings("deprecation")
		DashScopeApi.EmbeddingRequest listTextsRequest = new DashScopeApi.EmbeddingRequest(
				List.of("text1", "text2"), "test-model", null);
		
		assertEquals(DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE, listTextsRequest.parameters().textType(),
				"Deprecated constructor should handle null textType");
	}

	@Test
	void testDeprecatedEmbeddingRequestInputParametersConstructorNullTextTypeHandling() {
		// Test deprecated single parameter constructor with null textType
		@SuppressWarnings("deprecation")
		DashScopeApi.EmbeddingRequestInputParameters parameters = new DashScopeApi.EmbeddingRequestInputParameters(null);
		
		assertEquals(DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE, parameters.textType(),
				"Deprecated single parameter constructor should handle null textType");
		
		// Test deprecated single parameter constructor with valid textType
		@SuppressWarnings("deprecation")
		DashScopeApi.EmbeddingRequestInputParameters validParameters = new DashScopeApi.EmbeddingRequestInputParameters("query");
		
		assertEquals("query", validParameters.textType(),
				"Deprecated single parameter constructor should preserve valid textType");
	}

}