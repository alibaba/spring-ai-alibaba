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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

		// Mock WebClient.Builder behavior
		when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
		when(webClientBuilder.build()).thenReturn(webClient);

		transport = new StreamableHttpClientTransport(webClientBuilder, objectMapper, "/test-endpoint");
	}

	@Test
	void testHandleIncomingMessageWithIntegerId() {
		// Test JSON response with integer ID (the case that was causing the
		// ClassCastException)
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

		}
		catch (Exception e) {
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

		}
		catch (Exception e) {
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

		}
		catch (Exception e) {
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

		}
		catch (Exception e) {
			fail("Test setup failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithIntegerId() {
		// Test the preprocessJsonForDeserialization method with integer ID
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":0,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";
		String expectedJson = "{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The result should have the id field converted to string
			assertTrue(result.contains("\"id\":\"0\""), "ID should be converted to string format");
			assertFalse(result.contains("\"id\":0"), "ID should not remain as integer");

			// Verify the JSON structure is preserved
			assertTrue(result.contains("\"jsonrpc\":\"2.0\""), "jsonrpc field should be preserved");
			assertTrue(result.contains("\"result\""), "result field should be preserved");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithStringId() {
		// Test the preprocessJsonForDeserialization method with string ID (should remain
		// unchanged)
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":\"123\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The result should remain the same since id is already a string
			assertTrue(result.contains("\"id\":\"123\""), "ID should remain as string");
			assertTrue(result.contains("\"jsonrpc\":\"2.0\""), "jsonrpc field should be preserved");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithNullId() {
		// Test the preprocessJsonForDeserialization method with null ID
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":null,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The result should remain the same since null should be preserved
			assertTrue(result.contains("\"id\":null"), "null ID should be preserved");
			assertTrue(result.contains("\"jsonrpc\":\"2.0\""), "jsonrpc field should be preserved");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithMissingId() {
		// Test the preprocessJsonForDeserialization method without id field
		String originalJson = "{\"jsonrpc\":\"2.0\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The result should remain the same since there's no id field
			assertFalse(result.contains("\"id\""), "Should not contain id field");
			assertTrue(result.contains("\"jsonrpc\":\"2.0\""), "jsonrpc field should be preserved");
			assertTrue(result.contains("\"result\""), "result field should be preserved");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithNonStringMethod() {
		// Test the preprocessJsonForDeserialization method with non-string method field
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":\"123\",\"method\":42,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The method field should be converted to string
			assertTrue(result.contains("\"method\":\"42\""), "method should be converted to string");
			assertFalse(result.contains("\"method\":42"), "method should not remain as integer");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithNonStringJsonrpc() {
		// Test the preprocessJsonForDeserialization method with non-string jsonrpc field
		String originalJson = "{\"jsonrpc\":2.0,\"id\":\"123\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The jsonrpc field should be converted to string
			assertTrue(result.contains("\"jsonrpc\":\"2.0\""), "jsonrpc should be converted to string");
			assertFalse(result.contains("\"jsonrpc\":2.0"), "jsonrpc should not remain as number");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithInvalidJson() {
		// Test the preprocessJsonForDeserialization method with invalid JSON
		String invalidJson = "{\"jsonrpc\":\"2.0\",\"id\":0,invalid}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, invalidJson);

			// Should return the original content when parsing fails
			assertEquals(invalidJson, result, "Should return original content when JSON is invalid");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testPreprocessJsonForDeserializationWithComplexId() {
		// Test the preprocessJsonForDeserialization method with complex integer ID
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":12345,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method preprocessMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("preprocessJsonForDeserialization", String.class);
			preprocessMethod.setAccessible(true);

			String result = (String) preprocessMethod.invoke(transport, originalJson);

			// The id field should be converted to string
			assertTrue(result.contains("\"id\":\"12345\""), "ID should be converted to string format");
			assertFalse(result.contains("\"id\":12345"), "ID should not remain as integer");

		}
		catch (Exception e) {
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	void testHandleIncomingMessageWithAllIntegerFields() {
		// Test JSON response with all fields as integers (edge case)
		String jsonResponse = "{\"jsonrpc\":2.0,\"id\":42,\"method\":100,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		try {
			Method handleIncomingMessageMethod = StreamableHttpClientTransport.class
				.getDeclaredMethod("handleIncomingMessage", String.class);
			handleIncomingMessageMethod.setAccessible(true);

			// This should not throw a ClassCastException anymore
			assertDoesNotThrow(() -> {
				handleIncomingMessageMethod.invoke(transport, jsonResponse);
			}, "handleIncomingMessage should not throw ClassCastException when all fields are integers");

		}
		catch (Exception e) {
			fail("Test setup failed: " + e.getMessage());
		}
	}

}
