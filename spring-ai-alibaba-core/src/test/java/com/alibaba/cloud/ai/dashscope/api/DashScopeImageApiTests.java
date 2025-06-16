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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Tests for DashScopeImageApi class functionality
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
class DashScopeImageApiTests {

	private DashScopeImageApi imageApi;

	private RestClient mockRestClient;

	@BeforeEach
	void setUp() {
		// Setup mock RestClient
		mockRestClient = mock(RestClient.class);

		// Initialize DashScopeImageApi with test API key
		imageApi = new DashScopeImageApi("test-api-key");
	}

	@Test
	void testConstructorWithApiKey() {
		// Test constructor with only API key
		DashScopeImageApi api = new DashScopeImageApi("test-api-key");
		assertNotNull(api, "DashScopeImageApi should be created with API key");
	}

	@Test
	void testConstructorWithApiKeyAndWorkspaceId() {
		// Test constructor with API key and workspace ID
		DashScopeImageApi api = new DashScopeImageApi("test-api-key", "test-workspace-id");
		assertNotNull(api, "DashScopeImageApi should be created with API key and workspace ID");
	}

	@Test
	void testConstructorWithApiKeyWorkspaceIdAndBaseUrl() {
		// Test constructor with API key, workspace ID, and base URL
		DashScopeImageApi api = new DashScopeImageApi("test-api-key", "test-workspace-id", "https://test-base-url.com");
		assertNotNull(api, "DashScopeImageApi should be created with API key, workspace ID, and base URL");
	}

	@Test
	void testDefaultImageModel() {
		// Test the default image model constant
		assertEquals("wanx-v1", DashScopeImageApi.DEFAULT_IMAGE_MODEL, "Default image model should be 'wanx-v1'");
	}

	@Test
	void testImageRequestClasses() {
		// Test creating image request objects
		DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput input = new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput(
				"Test prompt", null, null, "stylization_all",
				"https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/8649386271/p848790.png",
				"https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/8649386271/p848791.png",
				"https://huarong123.oss-cn-hangzhou.aliyuncs.com/image/%E6%B6%82%E9%B8%A6%E8%8D%89%E5%9B%BE.png");
		Integer[][] colorArray = { { 0, 0, 0 }, { 134, 134, 134 } };

		DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter parameter = new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter(
				"anime", "1024*1024", 1, 123456, 0.5f, "canny", true, true, 5, true, colorArray, colorArray);

		DashScopeImageApi.DashScopeImageRequest request = new DashScopeImageApi.DashScopeImageRequest(
				"stable-diffusion-xl", input, parameter);

		// Verify request properties
		assertNotNull(request, "Image request object should be created");
		assertEquals("stable-diffusion-xl", request.model(), "Model should match");
		assertNotNull(request.input(), "Request input should not be null");
		assertNotNull(request.parameters(), "Request parameters should not be null");
		assertEquals("Test prompt", request.input().prompt(), "Prompt should match");
		assertEquals("anime", request.parameters().style(), "Style should match");
		assertEquals("1024*1024", request.parameters().size(), "Size should match");
		assertEquals(1, request.parameters().n(), "Number of images should match");
		assertEquals(123456, request.parameters().seed(), "Seed should match");
		assertEquals(0.5f, request.parameters().refStrength(), "Reference strength should match");
		assertEquals("canny", request.parameters().refMode(), "Reference mode should match");
	}

	@Test
	void testImageResponseClasses() {
		// Test creating image response objects
		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult result = new DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult(
				"https://example.com/image.png");

		List<DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult> results = Collections
			.singletonList(result);

		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseTaskMetrics metrics = new DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseTaskMetrics(
				1, 1, 0);

		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput output = new DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput(
				"task-id", "completed", results, metrics, "200", "success");

		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseUsage usage = new DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseUsage(
				1);

		DashScopeImageApi.DashScopeImageAsyncReponse response = new DashScopeImageApi.DashScopeImageAsyncReponse(
				"request-id", output, usage);

		// Verify response properties
		assertNotNull(response, "Image response object should be created");
		assertEquals("request-id", response.requestId(), "Request ID should match");
		assertNotNull(response.output(), "Response output should not be null");
		assertEquals("completed", response.output().taskStatus(), "Task status should match");
		assertEquals("task-id", response.output().taskId(), "Task ID should match");
		assertNotNull(response.output().results(), "Results should not be null");
		assertEquals(1, response.output().results().size(), "Should have 1 result");
		assertEquals("https://example.com/image.png", response.output().results().get(0).url(),
				"Result URL should match");
		assertNotNull(response.usage(), "Usage should not be null");
		assertEquals(1, response.usage().imageCount(), "Image count should match");
	}

}
