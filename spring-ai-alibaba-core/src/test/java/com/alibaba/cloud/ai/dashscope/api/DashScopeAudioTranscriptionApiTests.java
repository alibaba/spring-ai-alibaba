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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Tests for DashScopeAudioTranscriptionApi class functionality
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
class DashScopeAudioTranscriptionApiTests {

	private DashScopeAudioTranscriptionApi audioTranscriptionApi;

	private RestClient mockRestClient;

	@BeforeEach
	void setUp() {
		// Setup mock RestClient
		mockRestClient = mock(RestClient.class);

		// Initialize DashScopeAudioTranscriptionApi with test API key
		audioTranscriptionApi = new DashScopeAudioTranscriptionApi("test-api-key");
	}

	@Test
	void testConstructorWithApiKey() {
		// Test constructor with only API key
		DashScopeAudioTranscriptionApi api = new DashScopeAudioTranscriptionApi("test-api-key");
		assertNotNull(api, "DashScopeAudioTranscriptionApi should be created with API key");
	}

	@Test
	void testConstructorWithApiKeyAndWorkspaceId() {
		// Test constructor with API key and workspace ID
		DashScopeAudioTranscriptionApi api = new DashScopeAudioTranscriptionApi("test-api-key", "test-workspace-id");
		assertNotNull(api, "DashScopeAudioTranscriptionApi should be created with API key and workspace ID");
	}

	@Test
	void testConstructorWithApiKeyWorkspaceIdAndBaseUrl() {
		// Test constructor with API key, workspace ID, and base URL
		DashScopeAudioTranscriptionApi api = new DashScopeAudioTranscriptionApi("test-api-key", "test-workspace-id",
				"https://test-base-url.com");
		assertNotNull(api, "DashScopeAudioTranscriptionApi should be created with API key, workspace ID, and base URL");
	}

	@Test
	void testConstructorWithApiKeyWorkspaceIdBaseUrlAndWebsocketUrl() {
		// Test constructor with API key, workspace ID, base URL, and websocket URL
		DashScopeAudioTranscriptionApi api = new DashScopeAudioTranscriptionApi("test-api-key", "test-workspace-id",
				"https://test-base-url.com", "wss://test-websocket-url.com");
		assertNotNull(api,
				"DashScopeAudioTranscriptionApi should be created with API key, workspace ID, base URL, and websocket URL");
	}

	@Test
	void testRequestClass() {
		// Test creating a Request object
		List<String> fileUrls = Collections.singletonList("https://example.com/audio.mp3");
		DashScopeAudioTranscriptionApi.Request.Input input = new DashScopeAudioTranscriptionApi.Request.Input(fileUrls);

		List<Integer> channelIds = Arrays.asList(1, 2);
		List<String> languageHints = Arrays.asList("en", "zh");
		DashScopeAudioTranscriptionApi.Request.Parameters parameters = new DashScopeAudioTranscriptionApi.Request.Parameters(
				channelIds, "vocab-id", "phrase-id", true, languageHints);

		DashScopeAudioTranscriptionApi.Request request = new DashScopeAudioTranscriptionApi.Request("test-model", input,
				parameters);

		// Verify request properties
		assertNotNull(request, "Request object should be created");
		assertNotNull(request.input(), "Request input should not be null");
		assertNotNull(request.parameters(), "Request parameters should not be null");
		assertEquals("test-model", request.model(), "Model should match");
		assertEquals(fileUrls, request.input().fileUrls(), "File URLs should match");
		assertEquals(channelIds, request.parameters().channelId(), "Channel IDs should match");
		assertEquals("vocab-id", request.parameters().vocabularyId(), "Vocabulary ID should match");
		assertEquals("phrase-id", request.parameters().phraseId(), "Phrase ID should match");
		assertTrue(request.parameters().disfluencyRemovalEnabled(), "Disfluency removal should be enabled");
		assertEquals(languageHints, request.parameters().languageHints(), "Language hints should match");
	}

}
