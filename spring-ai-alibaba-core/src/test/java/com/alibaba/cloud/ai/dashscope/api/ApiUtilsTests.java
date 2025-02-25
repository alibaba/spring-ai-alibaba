/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for ApiUtils
 *
 * @author brianxiadong
 * @since 2025-02-24
 */
class ApiUtilsTests {

	private static final String TEST_API_KEY = "test-api-key";

	private static final String TEST_WORKSPACE_ID = "test-workspace";

	@Test
	void testGetJsonContentHeadersWithApiKeyOnly() {
		// Test getting JSON content headers with API key only
		HttpHeaders headers = new HttpHeaders();
		ApiUtils.getJsonContentHeaders(TEST_API_KEY).accept(headers);

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TEST_API_KEY);
		assertThat(headers.getFirst(HEADER_OPENAPI_SOURCE)).isEqualTo(SOURCE_FLAG);
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(headers.getFirst("user-agent")).contains(SDK_FLAG);
	}

	@Test
	void testGetJsonContentHeadersWithWorkspaceId() {
		// Test getting JSON content headers with workspace ID
		HttpHeaders headers = new HttpHeaders();
		ApiUtils.getJsonContentHeaders(TEST_API_KEY, TEST_WORKSPACE_ID).accept(headers);

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TEST_API_KEY);
		assertThat(headers.getFirst(HEADER_OPENAPI_SOURCE)).isEqualTo(SOURCE_FLAG);
		assertThat(headers.getFirst(HEADER_WORK_SPACE_ID)).isEqualTo(TEST_WORKSPACE_ID);
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(headers.getFirst("user-agent")).contains(SDK_FLAG);
	}

	@Test
	void testGetJsonContentHeadersWithStream() {
		// Test getting JSON content headers with stream enabled
		HttpHeaders headers = new HttpHeaders();
		ApiUtils.getJsonContentHeaders(TEST_API_KEY, TEST_WORKSPACE_ID, true).accept(headers);

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TEST_API_KEY);
		assertThat(headers.getFirst(HEADER_OPENAPI_SOURCE)).isEqualTo(SOURCE_FLAG);
		assertThat(headers.getFirst(HEADER_WORK_SPACE_ID)).isEqualTo(TEST_WORKSPACE_ID);
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(headers.getFirst("X-DashScope-SSE")).isEqualTo("enable");
		assertThat(headers.getFirst("user-agent")).contains(SDK_FLAG);
	}

	@Test
	void testGetMapContentHeaders() {
		// Test getting map content headers
		Map<String, String> customHeaders = new HashMap<>();
		customHeaders.put("Custom-Header", "custom-value");

		Map<String, String> headers = ApiUtils.getMapContentHeaders(TEST_API_KEY, true, TEST_WORKSPACE_ID,
				customHeaders);

		assertThat(headers.get("Authorization")).isEqualTo("bearer " + TEST_API_KEY);
		assertThat(headers.get("X-DashScope-WorkSpace")).isEqualTo(TEST_WORKSPACE_ID);
		assertThat(headers.get("X-DashScope-DataInspection")).isEqualTo("enable");
		assertThat(headers.get("Custom-Header")).isEqualTo("custom-value");
		assertThat(headers.get("user-agent")).contains(SDK_FLAG);
	}

	@Test
	void testGetAudioTranscriptionHeaders() {
		// Test getting audio transcription headers
		HttpHeaders headers = new HttpHeaders();
		ApiUtils.getAudioTranscriptionHeaders(TEST_API_KEY, TEST_WORKSPACE_ID, true, true, true).accept(headers);

		assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TEST_API_KEY);
		assertThat(headers.getFirst("X-DashScope-WorkSpace")).isEqualTo(TEST_WORKSPACE_ID);
		assertThat(headers.getFirst("X-DashScope-DataInspection")).isEqualTo("enable");
		assertThat(headers.getFirst("X-DashScope-Async")).isEqualTo("enable");
		assertThat(headers.getFirst("X-DashScope-SSE")).isEqualTo("enable");
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("no-cache");
		assertThat(headers.getFirst("X-Accel-Buffering")).isEqualTo("no");
		assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("text/event-stream");
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void testGetFileUploadHeaders() {
		// Test getting file upload headers
		Map<String, String> input = new HashMap<>();
		input.put("Content-Type", "multipart/form-data");
		input.put("Custom-Header", "custom-value");

		HttpHeaders headers = new HttpHeaders();
		ApiUtils.getFileUploadHeaders(input).accept(headers);

		assertThat(Objects.requireNonNull(headers.getContentType()).toString()).isEqualTo("multipart/form-data");
		assertThat(headers.getFirst("Custom-Header")).isEqualTo("custom-value");
	}

}