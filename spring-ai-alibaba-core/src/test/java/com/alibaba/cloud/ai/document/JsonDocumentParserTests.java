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
package com.alibaba.cloud.ai.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test cases for JsonDocumentParser
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class JsonDocumentParserTests {

	private JsonDocumentParser parser;

	@BeforeEach
	void setUp() {
		// Initialize parser with text and description fields
		parser = new JsonDocumentParser("text", "description");
	}

	@Test
	void testParseSingleJsonObject() {
		// Test parsing a single JSON object with text and description fields
		String json = """
				{
				    "text": "Sample text",
				    "description": "Sample description",
				    "other": "Other field"
				}
				""";

		List<Document> documents = parser.parse(toInputStream(json));

		assertThat(documents).hasSize(1);
		Document doc = documents.get(0);
		assertThat(doc.getText()).contains("Sample text").contains("Sample description");
	}

	@Test
	void testParseJsonArray() {
		// Test parsing an array of JSON objects
		String json = """
				[
				    {
				        "text": "First text",
				        "description": "First description"
				    },
				    {
				        "text": "Second text",
				        "description": "Second description"
				    }
				]
				""";

		List<Document> documents = parser.parse(toInputStream(json));

		assertThat(documents).hasSize(2);
		assertThat(documents.get(0).getText()).contains("First text");
		assertThat(documents.get(1).getText()).contains("Second text");
	}

	@Test
	void testJsonPointerParsing() {
		// Test parsing using JSON pointer to specific location in document
		String json = """
				{
				    "data": {
				        "items": [
				            {
				                "text": "Pointer text",
				                "description": "Pointer description"
				            }
				        ]
				    }
				}
				""";

		List<Document> documents = parser.get("/data/items", toInputStream(json));

		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).contains("Pointer text").contains("Pointer description");
	}

	@Test
	void testEmptyJsonInput() {
		// Test handling of empty JSON object
		String json = "{}";
		List<Document> documents = parser.parse(toInputStream(json));

		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).isEqualTo("{}");
	}

	@Test
	void testInvalidJsonPointer() {
		// Test handling of invalid JSON pointer
		String json = """
				{
				    "data": {}
				}
				""";

		assertThrows(IllegalArgumentException.class, () -> parser.get("/invalid/pointer", toInputStream(json)));
	}

	private InputStream toInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

}
