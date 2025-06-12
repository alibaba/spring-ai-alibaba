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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.fasterxml.jackson.databind.node.BooleanNode.TRUE;

/**
 * JsonInsertService Test Class
 */
public class JsonProcessorInsertServiceTest {

	private JsonProcessorInsertService jsonProcessorInsertService;

	private String jsonContent;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		JsonParseTool jsonParseTool = new JsonParseTool(objectMapper);
		jsonProcessorInsertService = new JsonProcessorInsertService(jsonParseTool);
		jsonContent = "{\"name\":\"John\",\"age\":30}";
	}

	@Test
	void testInsertStringValue() {
		JsonNode newValue = new TextNode("Beijing");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "city", newValue);

		JsonNode result = (JsonNode) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertEquals("Beijing", result.get("city").asText());
	}

	@Test
	void testInsertNumberValue() {
		JsonNode newValue = TRUE;
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "isActive", newValue);

		JsonNode result = (JsonNode) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertTrue(result.get("isActive").asBoolean());
	}

	@Test
	void testInsertJsonObjectValue() {
		ObjectNode addressObject = objectMapper.createObjectNode();
		addressObject.put("street", "Chang'an Street");
		addressObject.put("zipCode", "100000");

		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "address", addressObject);

		JsonNode result = (JsonNode) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertEquals("Chang'an Street", result.get("address").get("street").asText());
		Assertions.assertEquals("100000", result.get("address").get("zipCode").asText());
	}

	@Test
	void testInsertJsonArrayValue() {
		ArrayNode hobbiesArray = objectMapper.createArrayNode();
		hobbiesArray.add("Reading");
		hobbiesArray.add("Traveling");
		hobbiesArray.add("Programming");

		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "hobbies", hobbiesArray);

		JsonNode result = (JsonNode) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertEquals(3, result.get("hobbies").size());
		Assertions.assertEquals("Reading", result.get("hobbies").get(0).asText());
		Assertions.assertEquals("Traveling", result.get("hobbies").get(1).asText());
		Assertions.assertEquals("Programming", result.get("hobbies").get(2).asText());
	}

	@Test
	void testNullField() {
		JsonNode newValue = new TextNode("Beijing");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, null, newValue);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorInsertService.apply(request);
		});
	}

	@Test
	void testNullValue() {
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "city", null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorInsertService.apply(request);
		});
	}

	@Test
	void testOverwriteExistingField() {
		JsonNode newValue = new TextNode("David");
		JsonProcessorInsertService.JsonInsertRequest request = new JsonProcessorInsertService.JsonInsertRequest(
				jsonContent, "name", newValue);

		JsonNode result = (JsonNode) jsonProcessorInsertService.apply(request);

		Assertions.assertEquals("David", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
	}

}
