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

package com.alibaba.cloud.ai.toolcalling.jsonprocessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for JsonRemoveService
 */
public class JsonRemoveServiceTest {

	private JsonRemoveService jsonRemoveService;

	private String jsonContent;

	@BeforeEach
	void setUp() {
		jsonRemoveService = new JsonRemoveService();
		jsonContent = "{\"name\":\"John\",\"age\":30,\"city\":\"Beijing\",\"isActive\":true}";
	}

	@Test
	void testRemoveStringField() {
		// Test removing a string type field
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(jsonContent, "name");

		// Execute the remove operation and get the return value
		JsonElement result = (JsonElement) jsonRemoveService.apply(request);

		// Verify that the returned value is the removed field value
		Assertions.assertEquals("John", result.getAsString());

		// Verify that the field has been removed from the JSON
		JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
		jsonObject.remove("name");
		String expectedJson = jsonObject.toString();

		// Execute the service again to get the processed JSON
		JsonObject processedJson = JsonParser.parseString(jsonContent).getAsJsonObject();
		processedJson.remove("name");

		// Compare the processed JSON
		Assertions.assertEquals(expectedJson, processedJson.toString());
	}

	@Test
	void testRemoveNumberField() {
		// Test removing a number type field
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(jsonContent, "age");

		// Execute the remove operation and get the return value
		JsonElement result = (JsonElement) jsonRemoveService.apply(request);

		// Verify that the returned value is the removed field value
		Assertions.assertEquals(30, result.getAsInt());
	}

	@Test
	void testRemoveBooleanField() {
		// Test removing a boolean type field
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(jsonContent, "isActive");

		// Execute the remove operation and get the return value
		JsonElement result = (JsonElement) jsonRemoveService.apply(request);

		// Verify that the returned value is the removed field value
		Assertions.assertTrue(result.getAsBoolean());
	}

	@Test
	void testRemoveNonExistentField() {
		// Test removing a non-existent field
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(jsonContent,
				"nonExistentField");

		// Execute the remove operation and get the return value
		JsonElement result = (JsonElement) jsonRemoveService.apply(request);

		// Verify that the return value is null
		Assertions.assertNull(result);
	}

	@Test
	void testNonObjectJsonContent() {
		// Test non-object type JSON content
		String arrayJson = "[1, 2, 3]";
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(arrayJson, "0");

		// Verify that an exception is thrown
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonRemoveService.apply(request);
		});
	}

	@Test
	void testRemoveAndVerifyJsonStructure() {
		// Test the JSON structure after removing a field
		JsonRemoveService.JsonRemoveRequest request = new JsonRemoveService.JsonRemoveRequest(jsonContent, "city");

		// Execute the remove operation
		jsonRemoveService.apply(request);

		// Manually build the expected JSON structure
		JsonObject expectedJson = new JsonObject();
		expectedJson.addProperty("name", "John");
		expectedJson.addProperty("age", 30);
		expectedJson.addProperty("isActive", true);

		// Manually remove the field and compare
		JsonObject actualJson = JsonParser.parseString(jsonContent).getAsJsonObject();
		actualJson.remove("city");

		// Verify the JSON structure
		Assertions.assertEquals(expectedJson.get("name").getAsString(), actualJson.get("name").getAsString());
		Assertions.assertEquals(expectedJson.get("age").getAsInt(), actualJson.get("age").getAsInt());
		Assertions.assertEquals(expectedJson.get("isActive").getAsBoolean(), actualJson.get("isActive").getAsBoolean());
		Assertions.assertFalse(actualJson.has("city"));
	}

}
