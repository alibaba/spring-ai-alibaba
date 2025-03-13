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
package com.alibaba.cloud.ai.dashscope.protocol;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeWebSocketClient. Tests cover WebSocket connection, message
 * handling, and event processing.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeWebSocketClientTests {

	private static final String TEST_API_KEY = "test-api-key";

	private static final String TEST_WORKSPACE_ID = "test-workspace";

	private static final String TEST_MESSAGE = "Hello, WebSocket!";

	private DashScopeWebSocketClient client;

	private WebSocket mockWebSocket;

	private Response mockResponse;

	@BeforeEach
	void setUp() {
		// Initialize mocks
		mockWebSocket = mock(WebSocket.class);
		mockResponse = mock(Response.class);

		// Set up basic mock behavior
		when(mockWebSocket.send(any(String.class))).thenReturn(true);
		when(mockWebSocket.send(any(ByteString.class))).thenReturn(true);

		// Configure client options
		DashScopeWebSocketClientOptions options = DashScopeWebSocketClientOptions.builder()
			.withApiKey(TEST_API_KEY)
			.withWorkSpaceId(TEST_WORKSPACE_ID)
			.build();

		// Initialize client
		client = new DashScopeWebSocketClient(options);

		// Set webSocketClient using reflection
		try {
			Field webSocketClientField = DashScopeWebSocketClient.class.getDeclaredField("webSocketClient");
			webSocketClientField.setAccessible(true);
			webSocketClientField.set(client, mockWebSocket);

			// Set isOpen to true
			Field isOpenField = DashScopeWebSocketClient.class.getDeclaredField("isOpen");
			isOpenField.setAccessible(true);
			isOpenField.set(client, new AtomicBoolean(true));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to set fields via reflection", e);
		}
	}

	@Test
	void testWebSocketEvents() {
		// Test sending text message
		client.sendText(TEST_MESSAGE);
		verify(mockWebSocket).send(TEST_MESSAGE);

		// Test receiving task started event
		client.onMessage(mockWebSocket, createTaskStartedMessage());

		// Test receiving result generated event
		client.onMessage(mockWebSocket, createResultGeneratedMessage());

		// Test receiving task finished event
		client.onMessage(mockWebSocket, createTaskFinishedMessage());
	}

	@Test
	void testStreamBinaryOut() {
		// Test binary streaming
		String testText = "Test binary streaming";
		Flux<ByteBuffer> result = client.streamBinaryOut(testText);

		StepVerifier.create(result).expectSubscription().then(() -> {
			// Simulate binary message
			ByteString testBinary = ByteString.of(ByteBuffer.wrap("test data".getBytes()));
			client.onMessage(mockWebSocket, testBinary);
		})
			.expectNextMatches(buffer -> buffer.hasRemaining())
			.then(() -> client.onMessage(mockWebSocket, createTaskFinishedMessage()))
			.verifyComplete();
	}

	@Test
	void testStreamTextOut() {
		// Test text streaming
		ByteBuffer testBuffer = ByteBuffer.wrap("Test text streaming".getBytes());
		Flux<String> result = client.streamTextOut(Flux.just(testBuffer));

		StepVerifier.create(result)
			.expectSubscription()
			.then(() -> client.onMessage(mockWebSocket, createResultGeneratedMessage()))
			.expectNextMatches(text -> text.contains("result"))
			.then(() -> client.onMessage(mockWebSocket, createTaskFinishedMessage()))
			.verifyComplete();
	}

	@Test
	void testErrorHandling() {
		// Test error handling
		Exception testException = new Exception("Test error");
		client.onFailure(mockWebSocket, testException, mockResponse);

		// Verify error is propagated to emitters
		StepVerifier.create(client.streamBinaryOut(TEST_MESSAGE)).expectError().verify();
	}

	private String createTaskStartedMessage() {
		return """
				{
				    "header": {
				        "task_id": "test-task-id",
				        "event": "task-started"
				    },
				    "payload": {}
				}""";
	}

	private String createResultGeneratedMessage() {
		return """
				{
				    "header": {
				        "task_id": "test-task-id",
				        "event": "result-generated"
				    },
				    "payload": {
				        "output": {
				            "text": "test result"
				        }
				    }
				}""";
	}

	private String createTaskFinishedMessage() {
		return """
				{
				    "header": {
				        "task_id": "test-task-id",
				        "event": "task-finished"
				    },
				    "payload": {}
				}""";
	}

	private String createTaskFailedMessage() {
		return """
				{
				    "header": {
				        "task_id": "test-task-id",
				        "event": "task-failed",
				        "error_code": "500",
				        "error_message": "Test error"
				    },
				    "payload": {}
				}""";
	}

}
