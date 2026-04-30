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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolResult class.
 *
 * <p>
 * Covers factory methods, merge operations, and serialization.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolResult Tests")
class ToolResultTest {

	@Nested
	@DisplayName("Factory Method Tests")
	class FactoryMethodTests {

		@Test
		@DisplayName("text() should create text-only result")
		void text_shouldCreateTextOnlyResult() {
			ToolResult result = ToolResult.text("Hello, World!");

			assertEquals("Hello, World!", result.getTextContent());
			assertTrue(result.getMediaContent().isEmpty());
			assertFalse(result.isFinal());
		}

		@Test
		@DisplayName("chunk() should create streaming chunk")
		void chunk_shouldCreateStreamingChunk() {
			ToolResult chunk = ToolResult.chunk("Processing...");

			assertEquals("Processing...", chunk.getTextContent());
			assertTrue(chunk.getMediaContent().isEmpty());
			assertFalse(chunk.isFinal());
			assertTrue(chunk.isChunk());
		}

		@Test
		@DisplayName("finalChunk() should create final chunk")
		void finalChunk_shouldCreateFinalChunk() {
			ToolResult result = ToolResult.finalChunk("Done!");

			assertEquals("Done!", result.getTextContent());
			assertTrue(result.getMediaContent().isEmpty());
			assertTrue(result.isFinal());
		}

		@Test
		@DisplayName("media() should create media-only result")
		void media_shouldCreateMediaOnlyResult() {
			byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult result = ToolResult.media(List.of(media));

			assertTrue(result.getTextContent() == null || result.getTextContent().isEmpty());
			assertEquals(1, result.getMediaContent().size());
			assertFalse(result.isFinal());
		}

		@Test
		@DisplayName("mixed() should create text and media result")
		void mixed_shouldCreateMixedResult() {
			byte[] imageData = new byte[] { 1, 2, 3, 4, 5 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult result = ToolResult.mixed("Here's an image:", List.of(media));

			assertEquals("Here's an image:", result.getTextContent());
			assertEquals(1, result.getMediaContent().size());
			assertFalse(result.isFinal());
		}

	}

	@Nested
	@DisplayName("Merge Operation Tests")
	class MergeTests {

		@Test
		@DisplayName("merge() should concatenate text")
		void merge_shouldConcatenateText() {
			ToolResult chunk1 = ToolResult.chunk("Hello, ");
			ToolResult chunk2 = ToolResult.chunk("World!");

			ToolResult merged = chunk1.merge(chunk2);

			assertEquals("Hello, World!", merged.getTextContent());
		}

		@Test
		@DisplayName("merge() should combine media")
		void merge_shouldCombineMedia() {
			byte[] image1Data = new byte[] { 1, 2, 3 };
			byte[] image2Data = new byte[] { 4, 5, 6 };
			Media media1 = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(image1Data));
			Media media2 = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(image2Data));

			ToolResult result1 = ToolResult.media(List.of(media1));
			ToolResult result2 = ToolResult.media(List.of(media2));

			ToolResult merged = result1.merge(result2);

			assertEquals(2, merged.getMediaContent().size());
		}

		@Test
		@DisplayName("merge() should preserve final flag from second result")
		void merge_shouldPreserveFinalFlag() {
			ToolResult chunk = ToolResult.chunk("Processing...");
			ToolResult finalChunk = ToolResult.finalChunk("Done!");

			ToolResult merged = chunk.merge(finalChunk);

			assertTrue(merged.isFinal());
		}

		@Test
		@DisplayName("merge() should handle null text gracefully")
		void merge_shouldHandleNullText() {
			ToolResult result1 = ToolResult.text(null);
			ToolResult result2 = ToolResult.text("World");

			ToolResult merged = result1.merge(result2);

			assertEquals("World", merged.getTextContent());
		}

	}

	@Nested
	@DisplayName("Immutability Tests")
	class ImmutabilityTests {

		@Test
		@DisplayName("withFinal() should return new instance")
		void withFinal_shouldReturnNewInstance() {
			ToolResult original = ToolResult.text("Hello");
			ToolResult withFinal = original.withFinal(true);

			assertFalse(original.isFinal());
			assertTrue(withFinal.isFinal());
			assertEquals(original.getTextContent(), withFinal.getTextContent());
		}

		@Test
		@DisplayName("media list should be unmodifiable")
		void mediaList_shouldBeUnmodifiable() {
			byte[] imageData = new byte[] { 1, 2, 3 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult result = ToolResult.media(List.of(media));

			List<Media> mediaList = result.getMediaContent();

			// Attempt to modify should throw exception
			org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> {
				mediaList.add(media);
			});
		}

	}

	@Nested
	@DisplayName("Serialization Tests")
	class SerializationTests {

		@Test
		@DisplayName("toStringResult() should return plain text for text-only result")
		void toStringResult_shouldReturnPlainText_forTextOnly() {
			ToolResult result = ToolResult.text("Simple text");

			String output = result.toStringResult();

			assertEquals("Simple text", output);
		}

		@Test
		@DisplayName("toStringResult() should use multimodal protocol for media result")
		void toStringResult_shouldUseMultimodalProtocol_forMedia() {
			byte[] imageData = new byte[] { 1, 2, 3 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult result = ToolResult.media(List.of(media));

			String output = result.toStringResult();

			assertTrue(output.startsWith(ToolResult.MULTIMODAL_PREFIX));
			assertTrue(output.endsWith(ToolResult.MULTIMODAL_SUFFIX));
		}

		@Test
		@DisplayName("toStringResult() should handle URL media correctly")
		void toStringResult_shouldHandleUrlMedia() throws Exception {
			URL imageUrl = new URL("https://example.com/image.jpg");
			Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_JPEG).data(imageUrl).build();
			ToolResult result = ToolResult.media(List.of(media));

			String output = result.toStringResult();

			assertTrue(output.startsWith(ToolResult.MULTIMODAL_PREFIX));
			assertTrue(output.contains("https://example.com/image.jpg"));
		}

		@Test
		@DisplayName("toStringResult() should handle URI media correctly")
		void toStringResult_shouldHandleUriMedia() throws Exception {
			URI imageUri = new URI("https://example.com/image.png");
			Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(imageUri).build();
			ToolResult result = ToolResult.media(List.of(media));

			String output = result.toStringResult();

			assertTrue(output.startsWith(ToolResult.MULTIMODAL_PREFIX));
			assertTrue(output.contains("https://example.com/image.png"));
		}

		@Test
		@DisplayName("URL media should round-trip through serialization")
		void urlMedia_shouldRoundTrip() throws Exception {
			URL imageUrl = new URL("https://example.com/test-image.jpg");
			Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_JPEG).data(imageUrl).build();
			ToolResult original = ToolResult.mixed("Check this image:", List.of(media));

			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			assertNotNull(parsed);
			assertEquals("Check this image:", parsed.getTextContent());
			assertEquals(1, parsed.getMediaContent().size());
			// The parsed media should contain the URL
			Media parsedMedia = parsed.getMediaContent().get(0);
			assertNotNull(parsedMedia.getData());
		}

		@Test
		@DisplayName("isToolResultFormat() should detect multimodal string")
		void isToolResultFormat_shouldDetectMultimodalString() {
			byte[] imageData = new byte[] { 1, 2, 3 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult original = ToolResult.media(List.of(media));

			String serialized = original.toStringResult();

			assertTrue(ToolResult.isToolResultFormat(serialized));
			assertFalse(ToolResult.isToolResultFormat("Plain text"));
		}

		@Test
		@DisplayName("fromString() should parse multimodal result")
		void fromString_shouldParseMultimodalResult() {
			byte[] imageData = new byte[] { 1, 2, 3 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult original = ToolResult.mixed("Caption", List.of(media));

			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			assertNotNull(parsed);
			assertEquals("Caption", parsed.getTextContent());
			assertEquals(1, parsed.getMediaContent().size());
		}

		@Test
		@DisplayName("fromString() should return text result for plain text")
		void fromString_shouldReturnTextResult_forPlainText() {
			ToolResult parsed = ToolResult.fromString("Plain text");

			assertNotNull(parsed);
			assertEquals("Plain text", parsed.getTextContent());
		}

	}

	@Nested
	@DisplayName("Type Detection Tests")
	class TypeDetectionTests {

		@Test
		@DisplayName("isTextOnly() should return true for text results")
		void isTextOnly_shouldReturnTrue_forTextResults() {
			ToolResult result = ToolResult.text("Hello");

			assertTrue(result.isTextOnly());
		}

		@Test
		@DisplayName("hasMedia() should return true for media results")
		void hasMedia_shouldReturnTrue_forMediaResults() {
			byte[] imageData = new byte[] { 1, 2, 3 };
			Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageData));
			ToolResult result = ToolResult.media(List.of(media));

			assertTrue(result.hasMedia());
			assertFalse(result.isTextOnly());
		}

	}

	@Nested
	@DisplayName("Equality Tests")
	class EqualityTests {

		@Test
		@DisplayName("equals() should return true for same content")
		void equals_shouldReturnTrue_forSameContent() {
			ToolResult result1 = ToolResult.text("Hello");
			ToolResult result2 = ToolResult.text("Hello");

			assertEquals(result1, result2);
		}

		@Test
		@DisplayName("hashCode() should be consistent with equals")
		void hashCode_shouldBeConsistent() {
			ToolResult result1 = ToolResult.text("Hello");
			ToolResult result2 = ToolResult.text("Hello");

			assertEquals(result1.hashCode(), result2.hashCode());
		}

	}

	@Nested
	@DisplayName("Edge Case Tests")
	class EdgeCaseTests {

		@Test
		@DisplayName("empty text should be handled")
		void emptyText_shouldBeHandled() {
			ToolResult result = ToolResult.text("");

			assertNotNull(result);
			assertEquals("", result.getTextContent());
		}

		@Test
		@DisplayName("null text should be handled")
		void nullText_shouldBeHandled() {
			ToolResult result = ToolResult.text(null);

			assertNotNull(result);
		}

		@Test
		@DisplayName("empty media list should be handled")
		void emptyMediaList_shouldBeHandled() {
			ToolResult result = ToolResult.media(List.of());

			assertNotNull(result);
			assertTrue(result.getMediaContent().isEmpty());
		}

	}

	@Nested
	@DisplayName("Data URI Serialization Tests")
	class DataUriSerializationTests {

		@Test
		@DisplayName("should serialize and deserialize data URI media")
		void shouldSerializeAndDeserialize_dataUriMedia() {
			// Given - create media with data: URI
			String dataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ"
					+ "AAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
			Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(dataUri).build();
			ToolResult original = ToolResult.media(List.of(media));

			// When - serialize and deserialize
			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			// Then - should successfully round-trip
			assertNotNull(parsed);
			assertEquals(1, parsed.getMediaContent().size());
			Media parsedMedia = parsed.getMediaContent().get(0);
			assertNotNull(parsedMedia.getData());
			// Data should be preserved
			assertEquals(dataUri, parsedMedia.getData());
		}

		@Test
		@DisplayName("should handle data URI in mixed content")
		void shouldHandle_dataUriInMixedContent() {
			// Given
			String dataUri = "data:text/plain;base64,SGVsbG8gV29ybGQ=";
			Media media = Media.builder().mimeType(MimeTypeUtils.TEXT_PLAIN).data(dataUri).build();
			ToolResult original = ToolResult.mixed("Here's some data:", List.of(media));

			// When
			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			// Then
			assertNotNull(parsed);
			assertEquals("Here's some data:", parsed.getTextContent());
			assertEquals(1, parsed.getMediaContent().size());
		}

		@Test
		@DisplayName("should handle unknown URL scheme as fallback")
		void shouldHandle_unknownUrlScheme_asFallback() {
			// Given - custom scheme that can't be converted to URL
			String customUri = "custom-scheme://some/resource";
			Media media = Media.builder().mimeType(MimeTypeUtils.APPLICATION_OCTET_STREAM).data(customUri).build();
			ToolResult original = ToolResult.media(List.of(media));

			// When - serialize and deserialize (should not throw)
			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			// Then - should fall back to storing as string
			assertNotNull(parsed);
			assertEquals(1, parsed.getMediaContent().size());
			Media parsedMedia = parsed.getMediaContent().get(0);
			assertEquals(customUri, parsedMedia.getData());
		}

		@Test
		@DisplayName("should still handle regular URLs correctly")
		void shouldStillHandle_regularUrls_correctly() throws Exception {
			// Given - regular HTTP URL
			URL imageUrl = new URL("https://example.com/image.png");
			Media media = Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(imageUrl).build();
			ToolResult original = ToolResult.media(List.of(media));

			// When
			String serialized = original.toStringResult();
			ToolResult parsed = ToolResult.fromString(serialized);

			// Then - URL should be preserved as URL object
			assertNotNull(parsed);
			assertEquals(1, parsed.getMediaContent().size());
			Media parsedMedia = parsed.getMediaContent().get(0);
			assertNotNull(parsedMedia.getData());
			// Should be a URL object
			assertTrue(parsedMedia.getData() instanceof URL);
		}

	}

}
