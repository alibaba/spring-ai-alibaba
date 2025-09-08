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
package com.alibaba.cloud.ai.graph.serializer;

import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JacksonStateSerializer.SpringAIDocumentDeserializer}
 */
class SpringAIDocumentDeserializerTest {

	private JacksonStateSerializer.SpringAIDocumentDeserializer deserializer;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		deserializer = new JacksonStateSerializer.SpringAIDocumentDeserializer();
	}

	@Test
	void testDeserializeDocumentWithAllFields() throws Exception {
		// Arrange
		String json = """
				{
				    "id": "doc-123",
				    "content": "This is a test document content",
				    "metadata": {
				        "source": "test-source",
				        "author": "test-author",
				        "score": 0.95
				    }
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);
		assertEquals("org.springframework.ai.document.Document", result.getClass().getName());

		// Verify content through reflection
		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("This is a test document content", content);

		// Verify ID through reflection
		String id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("doc-123", id);

		// Verify metadata through reflection
		Map<?, ?> metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertNotNull(metadata);
		assertEquals("test-source", metadata.get("source"));
		assertEquals("test-author", metadata.get("author"));
		assertEquals(0.95, metadata.get("score"));
	}

	@Test
	void testDeserializeDocumentWithAlternativeFieldNames() throws Exception {
		// Arrange
		String json = """
				{
				    "docId": "doc-456",
				    "text": "Alternative content field",
				    "pageContent": "Page content field",
				    "properties": {
				        "category": "test",
				        "priority": 1
				    }
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);
		assertEquals("org.springframework.ai.document.Document", result.getClass().getName());

		// Should prefer "text" over "pageContent" for content
		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("Alternative content field", content);

		// Should use "docId" as ID
		String id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("doc-456", id);
	}

	@Test
	void testDeserializeDocumentWithMinimalFields() throws Exception {
		// Arrange
		String json = """
				{
				    "content": "Minimal document"
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);
		assertEquals("org.springframework.ai.document.Document", result.getClass().getName());

		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("Minimal document", content);

		String id = (String) result.getClass().getMethod("getId").invoke(result);
		assertNotNull(id);

		// Metadata should be empty
		Map<?, ?> metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertTrue(metadata.isEmpty());
	}

	@Test
	void testDeserializeDocumentWithComplexMetadata() throws Exception {
		// Arrange
		String json = """
				{
				    "id": "complex-doc",
				    "content": "Document with complex metadata",
				    "metadata": {
				        "nested": {
				            "level1": "value1",
				            "level2": {
				                "final": "value2"
				            }
				        },
				        "array": [1, 2, 3],
				        "boolean": true,
				        "number": 42
				    }
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);

		Map<?, ?> metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertNotNull(metadata);

		// Verify complex nested structure
		Map<?, ?> nested = (Map<?, ?>) metadata.get("nested");
		assertEquals("value1", nested.get("level1"));

		Map<?, ?> level2 = (Map<?, ?>) nested.get("level2");
		assertEquals("value2", level2.get("final"));

		// Verify array
		assertTrue(metadata.get("array") instanceof java.util.List);
		assertEquals(true, metadata.get("boolean"));
		assertEquals(42, metadata.get("number"));
	}

	@Test
	void testDeserializeDocumentWithEmptyContent() throws Exception {
		// Arrange
		String json = """
				{
				    "text": "",
				    "metadata": {}
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);

		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("", content);
	}

	@Test
	void testDeserializeDocumentWithNullContent() throws Exception {
		// Arrange
		String json = """
				{
				    "content": null
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);

		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("", content); // Should fallback to empty string
	}

	@Test
	void testDeserializeDocumentWithMissingContent() throws Exception {
		// Arrange
		String json = """
				{
				    "id": "no-content-doc"
				}
				""";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);

		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("", content); // Should fallback to empty string

		String id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("no-content-doc", id);
	}

	@Test
	void testDeserializeDocumentWithTextualNode() throws Exception {
		// Arrange
		String json = "\"Simple text content\"";

		// Act
		Object result = deserializer.deserialize(objectMapper.createParser(json),
				objectMapper.getDeserializationContext());

		// Assert
		assertNotNull(result);
		assertEquals("org.springframework.ai.document.Document", result.getClass().getName());

		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("Simple text content", content);
	}

	@Test
	void testDeserializeDocumentWithInvalidJson() throws Exception {
		// Arrange
		String json = "{invalid json";

		// Act & Assert
		assertThrows(Exception.class, () -> {
			deserializer.deserialize(objectMapper.createParser(json), objectMapper.getDeserializationContext());
		});
	}

	@Test
	void testDeserializeDocumentWithVariousIdFieldNames() throws Exception {
		// Test with "id" field
		String jsonWithId = "{\"id\":\"test-id\",\"content\":\"test\"}";
		Object result = deserializer.deserialize(objectMapper.createParser(jsonWithId),
				objectMapper.getDeserializationContext());
		String id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("test-id", id);

		// Test with "docId" field
		String jsonWithDocId = "{\"docId\":\"test-docid\",\"content\":\"test\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithDocId),
				objectMapper.getDeserializationContext());
		id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("test-docid", id);

		// Test with "documentId" field
		String jsonWithDocumentId = "{\"documentId\":\"test-documentid\",\"content\":\"test\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithDocumentId),
				objectMapper.getDeserializationContext());
		id = (String) result.getClass().getMethod("getId").invoke(result);
		assertEquals("test-documentid", id);

		// Test with no ID field
		String jsonWithoutId = "{\"content\":\"test\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithoutId),
				objectMapper.getDeserializationContext());
		id = (String) result.getClass().getMethod("getId").invoke(result);
		assertNotNull(id);
	}

	@Test
	void testDeserializeDocumentWithVariousContentFieldNames() throws Exception {
		// Test with "content" field
		String jsonWithContent = "{\"content\":\"test-content\"}";
		Object result = deserializer.deserialize(objectMapper.createParser(jsonWithContent),
				objectMapper.getDeserializationContext());
		String content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("test-content", content);

		// Test with "text" field
		String jsonWithText = "{\"text\":\"test-text\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithText),
				objectMapper.getDeserializationContext());
		content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("test-text", content);

		// Test with "pageContent" field
		String jsonWithPageContent = "{\"pageContent\":\"test-pagecontent\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithPageContent),
				objectMapper.getDeserializationContext());
		content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("test-pagecontent", content);

		// Test preference order: content > text > pageContent
		String jsonWithAll = "{\"content\":\"content-value\",\"text\":\"text-value\",\"pageContent\":\"pagecontent-value\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithAll),
				objectMapper.getDeserializationContext());
		content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("content-value", content);

		// Test with textual node
		String textualJson = "\"simple text\"";
		result = deserializer.deserialize(objectMapper.createParser(textualJson),
				objectMapper.getDeserializationContext());
		content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("simple text", content);

		// Test with no content field
		String jsonWithoutContent = "{\"id\":\"test\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithoutContent),
				objectMapper.getDeserializationContext());
		content = (String) result.getClass().getMethod("getText").invoke(result);
		assertEquals("", content);
	}

	@Test
	void testDeserializeDocumentWithVariousMetadataTypes() throws Exception {
		// Test with simple metadata
		String jsonWithSimpleMetadata = "{\"content\":\"test\",\"metadata\":{\"key1\":\"value1\",\"key2\":42}}";
		Object result = deserializer.deserialize(objectMapper.createParser(jsonWithSimpleMetadata),
				objectMapper.getDeserializationContext());
		Map<?, ?> metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertEquals("value1", metadata.get("key1"));
		assertEquals(42, metadata.get("key2"));

		// Test with nested metadata
		String jsonWithNestedMetadata = "{\"content\":\"test\",\"metadata\":{\"nested\":{\"inner\":\"value\"}}}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithNestedMetadata),
				objectMapper.getDeserializationContext());
		metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		Map<?, ?> nested = (Map<?, ?>) metadata.get("nested");
		assertEquals("value", nested.get("inner"));

		// Test with array metadata
		String jsonWithArrayMetadata = "{\"content\":\"test\",\"metadata\":{\"array\":[1,2,3]}}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithArrayMetadata),
				objectMapper.getDeserializationContext());
		metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertTrue(metadata.get("array") instanceof java.util.List);

		// Test with no metadata
		String jsonWithoutMetadata = "{\"content\":\"test\"}";
		result = deserializer.deserialize(objectMapper.createParser(jsonWithoutMetadata),
				objectMapper.getDeserializationContext());
		metadata = (Map<?, ?>) result.getClass().getMethod("getMetadata").invoke(result);
		assertTrue(metadata.isEmpty());
	}

}
