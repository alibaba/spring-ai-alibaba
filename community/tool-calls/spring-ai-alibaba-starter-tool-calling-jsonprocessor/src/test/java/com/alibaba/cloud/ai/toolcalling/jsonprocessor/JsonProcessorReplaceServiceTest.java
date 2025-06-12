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
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.fasterxml.jackson.databind.node.BooleanNode.TRUE;

/**
 * JsonReplaceService Test Class
 */
public class JsonProcessorReplaceServiceTest {

	private JsonProcessorReplaceService jsonProcessorReplaceService;

	private String jsonContent;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		JsonParseTool jsonParseTool = new JsonParseTool(objectMapper);
		jsonProcessorReplaceService = new JsonProcessorReplaceService(jsonParseTool);
		jsonContent = "{\"name\":\"John\",\"age\":30,\"city\":\"Beijing\"}";
	}

	@Test
	void testReplaceStringValue() {
		JsonNode newValue = new TextNode("David");
		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, "name", newValue);

		JsonNode result = (JsonNode) jsonProcessorReplaceService.apply(request);

		Assertions.assertEquals("David", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertEquals("Beijing", result.get("city").asText());
	}

	@Test
	void testReplaceNumberValue() {
		JsonNode newValue = new IntNode(40);
		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, "age", newValue);

		JsonNode result = (JsonNode) jsonProcessorReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(40, result.get("age").asInt());
		Assertions.assertEquals("Beijing", result.get("city").asText());
	}

	@Test
	void testAddNewField() {
		JsonNode newValue = TRUE;
		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, "isActive", newValue);

		JsonNode result = (JsonNode) jsonProcessorReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals(30, result.get("age").asInt());
		Assertions.assertEquals("Beijing", result.get("city").asText());
		Assertions.assertTrue(result.get("isActive").asBoolean());
	}

	@Test
	void testReplaceWithJsonObject() {
		ObjectNode addressObject = objectMapper.createObjectNode();
		addressObject.put("street", "Chang'an Street");
		addressObject.put("zipCode", "100000");

		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, "address", addressObject);

		JsonNode result = (JsonNode) jsonProcessorReplaceService.apply(request);

		Assertions.assertEquals("John", result.get("name").asText());
		Assertions.assertEquals("Chang'an Street", result.get("address").get("street").asText());
	}

	@Test
	void testNullField() {
		JsonNode newValue = new TextNode("David");
		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, null, newValue);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorReplaceService.apply(request);
		});
	}

	@Test
	void testNullValue() {
		JsonProcessorReplaceService.JsonReplaceRequest request = new JsonProcessorReplaceService.JsonReplaceRequest(
				jsonContent, "name", null);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jsonProcessorReplaceService.apply(request);
		});
	}

}
