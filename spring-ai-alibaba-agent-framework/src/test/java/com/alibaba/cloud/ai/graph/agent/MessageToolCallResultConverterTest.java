/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.agent.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for MessageToolCallResultConverter.
 *
 * <p>
 * Covers conversion of various result types including ToolResult, Media,
 * AssistantMessage, and arbitrary objects.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("MessageToolCallResultConverter Tests")
class MessageToolCallResultConverterTest {

	private MessageToolCallResultConverter converter;

	@BeforeEach
	void setUp() {
		converter = new MessageToolCallResultConverter();
	}

	@Nested
	@DisplayName("Void and Null Result Tests")
	class VoidAndNullResultTests {

		@Test
		@DisplayName("should return 'Done' JSON for void return type")
		void convert_shouldReturnDone_forVoidReturnType() {
			String result = converter.convert(null, Void.TYPE);

			assertNotNull(result);
			assertEquals("\"Done\"", result);
		}

		@Test
		@DisplayName("should return empty string for null result")
		void convert_shouldReturnEmptyString_forNullResult() {
			String result = converter.convert(null, String.class);

			assertEquals("", result);
		}

	}

	@Nested
	@DisplayName("String Result Tests")
	class StringResultTests {

		@Test
		@DisplayName("should pass through string result unchanged")
		void convert_shouldPassthrough_forStringResult() {
			String input = "Hello, World!";

			String result = converter.convert(input, String.class);

			assertEquals(input, result);
		}

		@Test
		@DisplayName("should handle empty string")
		void convert_shouldHandleEmptyString() {
			String result = converter.convert("", String.class);

			assertEquals("", result);
		}

	}

	@Nested
	@DisplayName("ToolResult Tests")
	class ToolResultTests {

		@Test
		@DisplayName("should use toStringResult() for ToolResult")
		void convert_shouldUseToStringResult_forToolResult() {
			ToolResult toolResult = ToolResult.text("Test content");

			String result = converter.convert(toolResult, ToolResult.class);

			assertNotNull(result);
			assertTrue(result.contains("Test content"));
		}

		@Test
		@DisplayName("should handle ToolResult with media")
		void convert_shouldHandleToolResult_withMedia() {
			Media media = Media.builder().mimeType(MimeType.valueOf("image/png")).data(URI.create("data:image/png;base64,abc123")).build();
			ToolResult toolResult = ToolResult.media(List.of(media));

			String result = converter.convert(toolResult, ToolResult.class);

			assertNotNull(result);
		}

	}

	@Nested
	@DisplayName("Single Media Tests")
	class SingleMediaTests {

		@Test
		@DisplayName("should serialize single Media correctly")
		void convert_shouldSerialize_forSingleMedia() {
			Media media = Media.builder().mimeType(MimeType.valueOf("image/jpeg")).data(URI.create("data:image/jpeg;base64,test")).build();

			String result = converter.convert(media, Media.class);

			assertNotNull(result);
		}

	}

	@Nested
	@DisplayName("Media Collection Tests")
	class MediaCollectionTests {

		@Test
		@DisplayName("should serialize collection of Media")
		void convert_shouldSerialize_forMediaCollection() {
			Media media1 = Media.builder().mimeType(MimeType.valueOf("image/png")).data(URI.create("data:image/png;base64,abc")).build();
			Media media2 = Media.builder().mimeType(MimeType.valueOf("image/jpeg")).data(URI.create("data:image/jpeg;base64,def")).build();
			List<Media> mediaList = List.of(media1, media2);

			String result = converter.convert(mediaList, List.class);

			assertNotNull(result);
		}

		@Test
		@DisplayName("should handle empty collection")
		void convert_shouldHandleEmptyCollection() {
			List<Media> emptyList = Collections.emptyList();

			String result = converter.convert(emptyList, List.class);

			assertNotNull(result);
			// Empty collection should be serialized as JSON
			assertEquals("[]", result);
		}

	}

	@Nested
	@DisplayName("AssistantMessage Tests")
	class AssistantMessageTests {

		@Test
		@DisplayName("should handle AssistantMessage with text only")
		void convert_shouldHandle_assistantMessageTextOnly() {
			AssistantMessage message = new AssistantMessage("Hello from assistant");

			String result = converter.convert(message, AssistantMessage.class);

			assertEquals("Hello from assistant", result);
		}

		@Test
		@DisplayName("should handle AssistantMessage with media only")
		void convert_shouldHandle_assistantMessageMediaOnly() {
			Media media = Media.builder().mimeType(MimeType.valueOf("image/png")).data(URI.create("data:image/png;base64,test")).build();
			AssistantMessage message = AssistantMessage.builder()
				.content("")
				.media(List.of(media))
				.build();

			String result = converter.convert(message, AssistantMessage.class);

			assertNotNull(result);
		}

		@Test
		@DisplayName("should handle AssistantMessage with both text and media")
		void convert_shouldHandle_assistantMessageMixed() {
			Media media = Media.builder().mimeType(MimeType.valueOf("image/png")).data(URI.create("data:image/png;base64,test")).build();
			AssistantMessage message = AssistantMessage.builder()
				.content("Here is an image:")
				.media(List.of(media))
				.build();

			String result = converter.convert(message, AssistantMessage.class);

			assertNotNull(result);
			assertTrue(result.contains("Here is an image:"));
		}

		@Test
		@DisplayName("should handle empty AssistantMessage")
		void convert_shouldHandle_emptyAssistantMessage() {
			AssistantMessage message = AssistantMessage.builder()
				.content("")
				.media(List.of())
				.build();

			String result = converter.convert(message, AssistantMessage.class);

			assertNotNull(result);
			assertEquals("\"Done\"", result);
		}

	}

	@Nested
	@DisplayName("Arbitrary Object Tests")
	class ArbitraryObjectTests {

		@Test
		@DisplayName("should JSON serialize arbitrary object")
		void convert_shouldJsonSerialize_forArbitraryObject() {
			TestObject obj = new TestObject("test", 42);

			String result = converter.convert(obj, TestObject.class);

			assertNotNull(result);
			assertTrue(result.contains("\"name\":\"test\""));
			assertTrue(result.contains("\"value\":42"));
		}

		@Test
		@DisplayName("should serialize Map correctly")
		void convert_shouldSerializeMap() {
			Map<String, Object> map = Map.of("key1", "value1", "key2", 123);

			String result = converter.convert(map, Map.class);

			assertNotNull(result);
			assertTrue(result.contains("key1"));
			assertTrue(result.contains("value1"));
		}

		@Test
		@DisplayName("should serialize List of non-Media objects")
		void convert_shouldSerializeList_ofNonMediaObjects() {
			List<String> list = List.of("item1", "item2", "item3");

			String result = converter.convert(list, List.class);

			assertNotNull(result);
			assertTrue(result.contains("item1"));
			assertTrue(result.contains("item2"));
		}

	}

	/**
	 * Test helper class for arbitrary object serialization tests.
	 */
	static class TestObject {

		private String name;

		private int value;

		TestObject(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public int getValue() {
			return value;
		}

	}

}
