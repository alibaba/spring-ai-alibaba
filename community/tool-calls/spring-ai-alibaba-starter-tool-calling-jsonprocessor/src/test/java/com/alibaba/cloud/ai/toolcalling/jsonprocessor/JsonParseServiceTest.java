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

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JsonParseService Test Class
 */
public class JsonParseServiceTest {

	private JsonParseService jsonParseService;

	private String jsonContent;

	private String complexJsonContent;

	@BeforeEach
	void setUp() {
		jsonParseService = new JsonParseService();
		jsonContent = "{\"name\":\"John\",\"age\":\"30\",\"city\":\"Beijing\",\"isActive\":\"true\"}";
		complexJsonContent = "{\"person\":{\"name\":\"John\",\"contact\":{\"email\":\"john@example.com\",\"phone\":\"12345678\"}},\"items\":[\"item1\",\"item2\"]}";
	}

	@Test
	void testParseStringValue() {
		JsonParseService.JsonParseRequest request = new JsonParseService.JsonParseRequest(jsonContent, "name");

		String result = (String) jsonParseService.apply(request);

		Assertions.assertEquals("John", result);
	}

	@Test
	void testParseNumberAsString() {
		JsonParseService.JsonParseRequest request = new JsonParseService.JsonParseRequest(jsonContent, "age");

		String result = (String) jsonParseService.apply(request);

		Assertions.assertEquals("30", result);
	}

	@Test
	void testParseBooleanAsString() {
		JsonParseService.JsonParseRequest request = new JsonParseService.JsonParseRequest(jsonContent, "isActive");

		String result = (String) jsonParseService.apply(request);

		Assertions.assertEquals("true", result);
	}

	@Test
	void testParseNonExistentField() {
		JsonParseService.JsonParseRequest request = new JsonParseService.JsonParseRequest(jsonContent,
				"nonExistentField");

		Assertions.assertThrows(NullPointerException.class, () -> {
			jsonParseService.apply(request);
		});
	}

	@Test
	void testParseNestedJsonField() {
		// Note: Current implementation does not support nested field parsing, this test
		// is expected to fail
		// This test case demonstrates the limitation of the current implementation
		JsonParseService.JsonParseRequest request = new JsonParseService.JsonParseRequest(complexJsonContent,
				"person.name");

		Assertions.assertThrows(NullPointerException.class, () -> {
			jsonParseService.apply(request);
		});
	}

}