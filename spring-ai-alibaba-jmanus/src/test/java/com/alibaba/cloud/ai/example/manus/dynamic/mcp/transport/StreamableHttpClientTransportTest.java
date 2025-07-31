/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.manus.dynamic.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StreamableHttpClientTransport to verify the fix for ClassCastException.
 */
@ExtendWith(MockitoExtension.class)
class StreamableHttpClientTransportTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private ObjectMapper objectMapper;
    private StreamableHttpClientTransport transport;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transport = new StreamableHttpClientTransport(webClientBuilder, objectMapper, "/test-endpoint");
    }

    @Test
    void testHandleIncomingMessageWithIntegerId() {
        // Test JSON response with integer ID (the case that was causing the ClassCastException)
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"id\":0,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";
        
        // Use reflection to access the private method
        try {
            Method handleIncomingMessageMethod = StreamableHttpClientTransport.class
                .getDeclaredMethod("handleIncomingMessage", String.class);
            handleIncomingMessageMethod.setAccessible(true);
            
            // This should not throw a ClassCastException anymore
            assertDoesNotThrow(() -> {
                handleIncomingMessageMethod.invoke(transport, jsonResponse);
            }, "handleIncomingMessage should not throw ClassCastException when id is an integer");
            
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    void testHandleIncomingMessageWithStringId() {
        // Test JSON response with string ID
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"id\":\"123\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";
        
        try {
            Method handleIncomingMessageMethod = StreamableHttpClientTransport.class
                .getDeclaredMethod("handleIncomingMessage", String.class);
            handleIncomingMessageMethod.setAccessible(true);
            
            // This should not throw any exception
            assertDoesNotThrow(() -> {
                handleIncomingMessageMethod.invoke(transport, jsonResponse);
            }, "handleIncomingMessage should not throw exception when id is a string");
            
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    void testHandleIncomingMessageWithNullId() {
        // Test JSON response with null ID
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"id\":null,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";
        
        try {
            Method handleIncomingMessageMethod = StreamableHttpClientTransport.class
                .getDeclaredMethod("handleIncomingMessage", String.class);
            handleIncomingMessageMethod.setAccessible(true);
            
            // This should not throw any exception
            assertDoesNotThrow(() -> {
                handleIncomingMessageMethod.invoke(transport, jsonResponse);
            }, "handleIncomingMessage should not throw exception when id is null");
            
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    void testHandleIncomingMessageWithMissingId() {
        // Test JSON response without id field
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";
        
        try {
            Method handleIncomingMessageMethod = StreamableHttpClientTransport.class
                .getDeclaredMethod("handleIncomingMessage", String.class);
            handleIncomingMessageMethod.setAccessible(true);
            
            // This should not throw any exception
            assertDoesNotThrow(() -> {
                handleIncomingMessageMethod.invoke(transport, jsonResponse);
            }, "handleIncomingMessage should not throw exception when id field is missing");
            
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }
} 