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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JsonInsertService Test Class
 */
public class JsonProcessorInsertServiceTest {

	private JsonProcessorInsertService jsonProcessorInsertService;

	private String jsonContent;

	@BeforeEach
	void setUp() {
		jsonProcessorInsertService = new JsonProcessorInsertService();
		jsonContent = "{\"name\":\"John\",\"age\":30}";
	}

	@Test
	void testInsertStringValue() {
		JsonElement newValue = new JsonPrimitive("Beijing");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "city",
				newValue);

		JsonObject result = (JsonObject) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertEquals("Beijing", result.get("city").getAsString());
	}

	@Test
	void testInsertNumberValue() {
		JsonElement newValue = new JsonPrimitive(true);
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "isActive",
				newValue);

		JsonObject result = (JsonObject) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertTrue(result.get("isActive").getAsBoolean());
	}

	@Test
	void testInsertJsonObjectValue() {
		JsonObject addressObject = new JsonObject();
		addressObject.addProperty("street", "Chang'an Street");
		addressObject.addProperty("zipCode", "100000");

		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "address",
				addressObject);

		JsonObject result = (JsonObject) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertEquals("Chang'an Street", result.get("address").getAsJsonObject().get("street").getAsString());
		Assertions.assertEquals("100000", result.get("address").getAsJsonObject().get("zipCode").getAsString());
	}

	@Test
	void testInsertJsonArrayValue() {
		JsonArray hobbiesArray = new JsonArray();
		hobbiesArray.add("Reading");
		hobbiesArray.add("Traveling");
		hobbiesArray.add("Programming");

		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "hobbies",
				hobbiesArray);

		JsonObject result = (JsonObject) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
		Assertions.assertEquals(3, result.get("hobbies").getAsJsonArray().size());
		Assertions.assertEquals("Reading", result.get("hobbies").getAsJsonArray().get(0).getAsString());
		Assertions.assertEquals("Traveling", result.get("hobbies").getAsJsonArray().get(1).getAsString());
		Assertions.assertEquals("Programming", result.get("hobbies").getAsJsonArray().get(2).getAsString());
	}

	@Test
	void testNullField() {
		JsonElement newValue = new JsonPrimitive("Beijing");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, null,
				newValue);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorInsertService.apply(request);
		});
	}

	@Test
	void testNullValue() {
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "city",
				null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorInsertService.apply(request);
		});
	}

	@Test
	void testOverwriteExistingField() {
		JsonElement newValue = new JsonPrimitive("David");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(jsonContent, "name",
				newValue);

		JsonObject result = (JsonObject) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("David", result.get("name").getAsString());
		Assertions.assertEquals(30, result.get("age").getAsInt());
	}

}
