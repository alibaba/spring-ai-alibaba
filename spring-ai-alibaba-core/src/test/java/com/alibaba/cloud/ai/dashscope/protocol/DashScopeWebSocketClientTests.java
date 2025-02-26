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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeWebSocketClient. Tests cover WebSocket connection,
 * message handling, and event processing.
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

        // 设置基本的 mock 行为
        when(mockWebSocket.send(any(String.class))).thenReturn(true);
        when(mockWebSocket.send(any(ByteString.class))).thenReturn(true);

        // Configure client options
        DashScopeWebSocketClientOptions options = DashScopeWebSocketClientOptions.builder()
                .withApiKey(TEST_API_KEY)
                .withWorkSpaceId(TEST_WORKSPACE_ID)
                .build();

        // Initialize client
        client = new DashScopeWebSocketClient(options);

        // 使用反射设置 webSocketClient
        try {
            // 设置 webSocketClient
            Field webSocketClientField = DashScopeWebSocketClient.class.getDeclaredField("webSocketClient");
            webSocketClientField.setAccessible(true);
            webSocketClientField.set(client, mockWebSocket);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set webSocketClient field", e);
        }

        // 通过调用 onOpen 来设置连接状态
        client.onOpen(mockWebSocket, mockResponse);
    }

    @Test
    void testWebSocketEvents() {
        // Test message sending
        client.sendText(TEST_MESSAGE);
        verify(mockWebSocket).send(TEST_MESSAGE);

        // Test message receiving
        String taskStartedMessage = createTaskStartedMessage();
        client.onMessage(mockWebSocket, taskStartedMessage);

        String resultGeneratedMessage = createResultGeneratedMessage();
        client.onMessage(mockWebSocket, resultGeneratedMessage);

        String taskFinishedMessage = createTaskFinishedMessage();
        client.onMessage(mockWebSocket, taskFinishedMessage);

        // Test onClosed event
        client.onClosed(mockWebSocket, 1000, "Normal closure");
    }

    @Test
    void testStreamBinaryOut() {
        // 创建测试数据
        ByteString testBytes = ByteString.encodeUtf8(TEST_MESSAGE);

        // 调用被测试方法并验证结果
        Flux<ByteBuffer> result = client.streamBinaryOut(TEST_MESSAGE);

        StepVerifier.create(result)
                .expectSubscription()
                .then(() -> client.onMessage(mockWebSocket, testBytes))
                .expectNextMatches(buffer -> {
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    return new String(bytes, StandardCharsets.UTF_8).equals(TEST_MESSAGE);
                })
                .expectNoEvent(Duration.ofMillis(100))
                .then(() -> client.onClosed(mockWebSocket, 1000, "normal closure"))
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        // 验证消息发送
        verify(mockWebSocket).send(TEST_MESSAGE);
    }

    @Test
    void testStreamTextOut() {
        // 创建测试数据
        ByteBuffer testBuffer = ByteBuffer.wrap(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
        Flux<ByteBuffer> testFlux = Flux.just(testBuffer);

        // 调用被测试方法并验证结果
        Flux<String> result = client.streamTextOut(testFlux);

        String resultGeneratedMessage = createResultGeneratedMessage();
        StepVerifier.create(result)
                .expectSubscription()
                .then(() -> client.onMessage(mockWebSocket, resultGeneratedMessage))
                .expectNext(resultGeneratedMessage)
                .expectNoEvent(Duration.ofMillis(100))
                .then(() -> client.onClosed(mockWebSocket, 1000, "normal closure"))
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        // 验证二进制消息发送
        verify(mockWebSocket).send(any(ByteString.class));
    }

    @Test
    void testErrorHandling() {
        // 调用被测试方法并验证结果
        Flux<ByteBuffer> result = client.streamBinaryOut(TEST_MESSAGE);

        RuntimeException testError = new RuntimeException("Test error");
        StepVerifier.create(result)
                .expectSubscription()
                .then(() -> client.onFailure(mockWebSocket, testError, mockResponse))
                .expectError(Exception.class)
                .verify(Duration.ofSeconds(5));

        // 验证消息发送
        verify(mockWebSocket).send(TEST_MESSAGE);
    }

    @Test
    void testTaskFailedEvent() {
        // 调用被测试方法并验证结果
        Flux<ByteBuffer> result = client.streamBinaryOut(TEST_MESSAGE);

        StepVerifier.create(result)
                .expectSubscription()
                .then(() -> client.onMessage(mockWebSocket, createTaskFailedMessage()))
                .expectError(Exception.class)
                .verify(Duration.ofSeconds(5));

        // 验证消息发送
        verify(mockWebSocket).send(TEST_MESSAGE);
    }

    // 辅助方法：创建各种测试消息
    private String createTaskStartedMessage() {
        return """
                {
                    "header": {
                        "task_id": "test-task",
                        "event": "task-started"
                    },
                    "payload": {
                        "output": null,
                        "usage": null
                    }
                }
                """;
    }

    private String createTaskFinishedMessage() {
        return """
                {
                    "header": {
                        "task_id": "test-task",
                        "event": "task-finished"
                    },
                    "payload": {
                        "output": null,
                        "usage": null
                    }
                }
                """;
    }

    private String createTaskFailedMessage() {
        return """
                {
                    "header": {
                        "task_id": "test-task",
                        "event": "task-failed",
                        "error_code": "ERROR",
                        "error_message": "Test error"
                    },
                    "payload": {
                        "output": null,
                        "usage": null
                    }
                }
                """;
    }

    private String createResultGeneratedMessage() {
        return """
                {
                    "header": {
                        "task_id": "test-task",
                        "event": "result-generated"
                    },
                    "payload": {
                        "output": {
                            "text": "Test result"
                        },
                        "usage": {
                            "total_tokens": 10
                        }
                    }
                }
                """;
    }
}
