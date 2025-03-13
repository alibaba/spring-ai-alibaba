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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.cloud.ai.dashscope.protocol.DashScopeWebSocketClient;

/**
 * Tests for DashScopeSpeechSynthesisApi class functionality
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
class DashScopeSpeechSynthesisApiTests {

	private DashScopeSpeechSynthesisApi speechSynthesisApi;

	@BeforeEach
	void setUp() {
		// Initialize DashScopeSpeechSynthesisApi with test API key
		speechSynthesisApi = new DashScopeSpeechSynthesisApi("test-api-key");
	}

	@Test
	void testConstructorWithApiKey() {
		// Test constructor with only API key
		DashScopeSpeechSynthesisApi api = new DashScopeSpeechSynthesisApi("test-api-key");
		assertNotNull(api, "DashScopeSpeechSynthesisApi should be created with API key");
	}

	@Test
	void testConstructorWithApiKeyAndWorkspaceId() {
		// Test constructor with API key and workspace ID
		DashScopeSpeechSynthesisApi api = new DashScopeSpeechSynthesisApi("test-api-key", "test-workspace-id");
		assertNotNull(api, "DashScopeSpeechSynthesisApi should be created with API key and workspace ID");
	}

	@Test
	void testConstructorWithApiKeyWorkspaceIdAndWebsocketUrl() {
		// Test constructor with API key, workspace ID, and websocket URL
		DashScopeSpeechSynthesisApi api = new DashScopeSpeechSynthesisApi("test-api-key", "test-workspace-id",
				"wss://test-websocket-url.com");
		assertNotNull(api,
				"DashScopeSpeechSynthesisApi should be created with API key, workspace ID, and websocket URL");
	}

	@Test
	void testRequestClasses() {
		// Test creating request objects
		DashScopeSpeechSynthesisApi.Request.RequestHeader header = new DashScopeSpeechSynthesisApi.Request.RequestHeader(
				"action", "task-id", "true");

		DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadInput input = new DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadInput(
				"Hello, world!");

		DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadParameters parameters = new DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadParameters(
				100, "plain", "female", 16000, 1.0, "wav", 1.0, true, true);

		DashScopeSpeechSynthesisApi.Request.RequestPayload payload = new DashScopeSpeechSynthesisApi.Request.RequestPayload(
				"model", "task-group", "task", "function", input, parameters);

		DashScopeSpeechSynthesisApi.Request request = new DashScopeSpeechSynthesisApi.Request(header, payload);

		// Verify request properties
		assertNotNull(request, "Request object should be created");
		assertNotNull(request.header(), "Request header should not be null");
		assertNotNull(request.payload(), "Request payload should not be null");

		// Verify header properties
		assertEquals("action", request.header().action(), "Action should match");
		assertEquals("task-id", request.header().taskId(), "Task ID should match");
		assertEquals("true", request.header().streaming(), "Streaming should match");

		// Verify payload properties
		assertEquals("model", request.payload().model(), "Model should match");
		assertEquals("task-group", request.payload().taskGroup(), "Task group should match");
		assertEquals("task", request.payload().task(), "Task should match");
		assertEquals("function", request.payload().function(), "Function should match");

		// Verify input properties
		assertNotNull(request.payload().input(), "Payload input should not be null");
		assertEquals("Hello, world!", request.payload().input().text(), "Text should match");

		// Verify parameters properties
		assertNotNull(request.payload().parameters(), "Payload parameters should not be null");
		assertEquals(100, request.payload().parameters().volume(), "Volume should match");
		assertEquals("plain", request.payload().parameters().textType(), "Text type should match");
		assertEquals("female", request.payload().parameters().voice(), "Voice should match");
		assertEquals(16000, request.payload().parameters().sampleRate(), "Sample rate should match");
		assertEquals(1.0, request.payload().parameters().rate(), "Rate should match");
		assertEquals("wav", request.payload().parameters().format(), "Format should match");
		assertEquals(1.0, request.payload().parameters().pitch(), "Pitch should match");
		assertTrue(request.payload().parameters().phonemeTimestampEnabled(), "Phoneme timestamp should be enabled");
		assertTrue(request.payload().parameters().wordTimestampEnabled(), "Word timestamp should be enabled");
	}

	@Test
	void testRequestTextTypeEnum() {
		// Test RequestTextType enum values
		assertEquals("PlainText", DashScopeSpeechSynthesisApi.RequestTextType.PLAIN_TEXT.getValue(),
				"PLAIN_TEXT should have value 'PlainText'");
		assertEquals("SSML", DashScopeSpeechSynthesisApi.RequestTextType.SSML.getValue(),
				"SSML should have value 'SSML'");
	}

	@Test
	void testResponseFormatEnum() {
		// Test ResponseFormat enum values
		assertEquals("pcm", DashScopeSpeechSynthesisApi.ResponseFormat.PCM.getValue(), "PCM should have value 'pcm'");
		assertEquals("wav", DashScopeSpeechSynthesisApi.ResponseFormat.WAV.getValue(), "WAV should have value 'wav'");
		assertEquals("mp3", DashScopeSpeechSynthesisApi.ResponseFormat.MP3.getValue(), "MP3 should have value 'mp3'");
	}

}
