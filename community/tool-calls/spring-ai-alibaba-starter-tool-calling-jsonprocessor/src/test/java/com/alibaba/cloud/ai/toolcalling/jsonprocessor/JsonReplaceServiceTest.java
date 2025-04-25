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
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JsonReplaceService Test Class
 */
public class JsonReplaceServiceTest {

	private JsonReplaceService jsonReplaceService;

	private String jsonContent;

	@BeforeEach
	void setUp() {
		jsonReplaceService = new JsonReplaceService();
		jsonContent = "{\"name\":\"John\",\"age\":30,\"city\":\"Beijing\"}";
	}

	@Test
	void testReplaceStringValue() {
		JsonElement newValue = new JsonPrimitive("David");
		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent, "name",
				newValue);

		JsonObject result = (JsonObject) jsonReplaceService.apply(request);

		Assertions.assertEquals("David", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertEquals("Beijing", result.get("city").getAsString());
	}

	@Test
	void testReplaceNumberValue() {
		JsonElement newValue = new JsonPrimitive(40);
		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent, "age",
				newValue);

		JsonObject result = (JsonObject) jsonReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(40, result.get("age").getAsInt());
		Assertions.assertEquals("Beijing", result.get("city").getAsString());
	}

	@Test
	void testAddNewField() {
		JsonElement newValue = new JsonPrimitive(true);
		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent,
				"isActive", newValue);

		JsonObject result = (JsonObject) jsonReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertEquals("Beijing", result.get("city").getAsString());
		Assertions.assertTrue(result.get("isActive").getAsBoolean());
	}

	@Test
	void testReplaceWithJsonObject() {
		JsonObject addressObject = new JsonObject();
		addressObject.addProperty("street", "Chang'an Street");
		addressObject.addProperty("zipCode", "100000");

		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent,
				"address", addressObject);

		JsonObject result = (JsonObject) jsonReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals("Chang'an Street", result.get("address").getAsJsonObject().get("street").getAsString());
	}

	@Test
	void testNullField() {
		JsonElement newValue = new JsonPrimitive("David");
		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent, null,
				newValue);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonReplaceService.apply(request);
		});
	}

	@Test
	void testNullValue() {
		JsonReplaceService.JsonReplaceRequest request = new JsonReplaceService.JsonReplaceRequest(jsonContent, "name",
				null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonReplaceService.apply(request);
		});
	}

}
