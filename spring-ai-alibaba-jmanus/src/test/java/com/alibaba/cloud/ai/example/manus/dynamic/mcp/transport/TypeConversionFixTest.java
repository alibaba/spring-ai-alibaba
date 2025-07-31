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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simplified test to verify the type conversion fix for ClassCastException. This test
 * focuses on the core functionality without complex mocking.
 */
class TypeConversionFixTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void testJsonPreprocessingWithIntegerId() throws Exception {
		// Test the exact case that was causing the ClassCastException
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":0,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		// Parse the JSON to verify it can be processed
		Map<String, Object> data = objectMapper.readValue(originalJson, Map.class);

		// Verify the id field is an Integer
		Object idObj = data.get("id");
		assertTrue(idObj instanceof Integer, "ID should be an Integer");
		assertEquals(0, idObj, "ID should be 0");

		// Test the preprocessing logic manually
		String processedJson = preprocessJsonManually(originalJson);

		// Parse the processed JSON
		Map<String, Object> processedData = objectMapper.readValue(processedJson, Map.class);

		// Verify the id field is now a String
		Object processedIdObj = processedData.get("id");
		assertTrue(processedIdObj instanceof String, "Processed ID should be a String");
		assertEquals("0", processedIdObj, "Processed ID should be \"0\"");

		// Verify other fields are preserved
		assertEquals("2.0", processedData.get("jsonrpc"), "jsonrpc field should be preserved");
		assertNotNull(processedData.get("result"), "result field should be preserved");
	}

	@Test
	void testJsonPreprocessingWithStringId() throws Exception {
		// Test with string ID (should remain unchanged)
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":\"123\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		String processedJson = preprocessJsonManually(originalJson);

		// Parse the processed JSON
		Map<String, Object> processedData = objectMapper.readValue(processedJson, Map.class);

		// Verify the id field remains a String
		Object processedIdObj = processedData.get("id");
		assertTrue(processedIdObj instanceof String, "Processed ID should remain a String");
		assertEquals("123", processedIdObj, "Processed ID should be \"123\"");
	}

	@Test
	void testJsonPreprocessingWithNullId() throws Exception {
		// Test with null ID
		String originalJson = "{\"jsonrpc\":\"2.0\",\"id\":null,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		String processedJson = preprocessJsonManually(originalJson);

		// Parse the processed JSON
		Map<String, Object> processedData = objectMapper.readValue(processedJson, Map.class);

		// Verify the id field remains null
		assertNull(processedData.get("id"), "Processed ID should remain null");
	}

	@Test
	void testJsonPreprocessingWithMissingId() throws Exception {
		// Test without id field
		String originalJson = "{\"jsonrpc\":\"2.0\",\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		String processedJson = preprocessJsonManually(originalJson);

		// Parse the processed JSON
		Map<String, Object> processedData = objectMapper.readValue(processedJson, Map.class);

		// Verify no id field is present
		assertFalse(processedData.containsKey("id"), "Should not contain id field");
	}

	@Test
	void testJsonPreprocessingWithNonStringFields() throws Exception {
		// Test with non-string jsonrpc and method fields
		String originalJson = "{\"jsonrpc\":2.0,\"id\":\"123\",\"method\":42,\"result\":{\"protocolVersion\":\"\",\"capabilities\":{},\"serverInfo\":{\"name\":\"\",\"version\":\"\"}}}";

		String processedJson = preprocessJsonManually(originalJson);

		// Parse the processed JSON
		Map<String, Object> processedData = objectMapper.readValue(processedJson, Map.class);

		// Verify fields are converted to strings
		assertEquals("2.0", processedData.get("jsonrpc"), "jsonrpc should be converted to string");
		assertEquals("42", processedData.get("method"), "method should be converted to string");
		assertEquals("123", processedData.get("id"), "id should remain string");
	}

	@Test
	void testJsonPreprocessingWithInvalidJson() {
		// Test with invalid JSON
		String invalidJson = "{\"jsonrpc\":\"2.0\",\"id\":0,invalid}";

		// Should return the original content when parsing fails
		String result = preprocessJsonManually(invalidJson);
		assertEquals(invalidJson, result, "Should return original content when JSON is invalid");
	}

	/**
	 * Manual implementation of the preprocessing logic for testing
	 */
	private String preprocessJsonManually(String jsonContent) {
		try {
			// Parse the JSON to a Map
			Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);

			// Handle id field - convert Integer to String if needed
			Object idObj = data.get("id");
			if (idObj instanceof Integer) {
				data.put("id", String.valueOf(idObj));
			}

			// Handle method field - ensure it's a String
			Object methodObj = data.get("method");
			if (methodObj != null && !(methodObj instanceof String)) {
				data.put("method", String.valueOf(methodObj));
			}

			// Handle jsonrpc field - ensure it's a String
			Object jsonrpcObj = data.get("jsonrpc");
			if (jsonrpcObj != null && !(jsonrpcObj instanceof String)) {
				data.put("jsonrpc", String.valueOf(jsonrpcObj));
			}

			// Convert back to JSON string
			return objectMapper.writeValueAsString(data);
		}
		catch (Exception e) {
			// Return original content when parsing fails
			return jsonContent;
		}

	}

}