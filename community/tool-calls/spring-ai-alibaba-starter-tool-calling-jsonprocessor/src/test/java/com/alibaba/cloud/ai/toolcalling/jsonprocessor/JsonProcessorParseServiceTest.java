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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JsonParseService Test Class
 */
public class JsonProcessorParseServiceTest {

	private JsonProcessorParseService jsonProcessorParseService;

	private String jsonContent;

	private String complexJsonContent;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		JsonParseTool jsonParseTool = new JsonParseTool(objectMapper);
		jsonProcessorParseService = new JsonProcessorParseService(jsonParseTool);
		jsonContent = "{\"name\":\"John\",\"age\":\"30\",\"city\":\"Beijing\",\"isActive\":\"true\"}";
		complexJsonContent = "{\"person\":{\"name\":\"John\",\"contact\":{\"email\":\"john@example.com\",\"phone\":\"12345678\"}},\"items\":[\"item1\",\"item2\"]}";
	}

	@Test
	void testParseStringValue() {
		JsonProcessorParseService.JsonParseRequest request = new JsonProcessorParseService.JsonParseRequest(jsonContent,
				"name");

		String result = (String) jsonProcessorParseService.apply(request);

		Assertions.assertEquals("John", result);
	}

	@Test
	void testParseNumberAsString() {
		JsonProcessorParseService.JsonParseRequest request = new JsonProcessorParseService.JsonParseRequest(jsonContent,
				"age");

		String result = (String) jsonProcessorParseService.apply(request);

		Assertions.assertEquals("30", result);
	}

	@Test
	void testParseBooleanAsString() {
		JsonProcessorParseService.JsonParseRequest request = new JsonProcessorParseService.JsonParseRequest(jsonContent,
				"isActive");

		String result = (String) jsonProcessorParseService.apply(request);

		Assertions.assertEquals("true", result);
	}

	@Test
	void testParseNonExistentField() {
		JsonProcessorParseService.JsonParseRequest request = new JsonProcessorParseService.JsonParseRequest(jsonContent,
				"nonExistentField");

		Assertions.assertThrows(NullPointerException.class, () -> {
			jsonProcessorParseService.apply(request);
		});
	}

	@Test
	void testParseNestedJsonField() {
		// Note: Current implementation does not support nested field parsing, this test
		// is expected to fail
		// This test case demonstrates the limitation of the current implementation
		JsonProcessorParseService.JsonParseRequest request = new JsonProcessorParseService.JsonParseRequest(
				complexJsonContent, "person.name");

		Assertions.assertThrows(NullPointerException.class, () -> {
			jsonProcessorParseService.apply(request);
		});
	}

}
