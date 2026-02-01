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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Rich result model for tool execution supporting text and multimodal content.
 * Immutable class with factory methods for different result types.
 *
 * <p>For multimodal results, content is serialized to JSON with a special prefix
 * to enable detection and deserialization at the consumer side.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Text result
 * ToolResult textResult = ToolResult.text("Hello, World!");
 *
 * // Streaming chunk
 * ToolResult chunk = ToolResult.chunk("Processing... 50%\n");
 *
 * // Multimodal result with image
 * Media image = new Media(MimeTypeUtils.IMAGE_PNG, imageBytes);
 * ToolResult imageResult = ToolResult.mixed("Here's the generated image:", List.of(image));
 *
 * // Reduce stream chunks
 * ToolResult merged = chunk1.merge(chunk2).merge(chunk3);
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolResult {

	/**
	 * Prefix marker for multimodal serialized results.
	 */
	public static final String MULTIMODAL_PREFIX = "$$MULTIMODAL:V1:";

	/**
	 * Suffix marker for multimodal serialized results.
	 */
	public static final String MULTIMODAL_SUFFIX = "$$";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Content type enumeration for tool results.
	 */
	public enum ContentType {

		TEXT, IMAGE, AUDIO, VIDEO, FILE, MIXED, STRUCTURED

	}

	private final ContentType type;

	private final String textContent;

	private final List<Media> mediaContent;

	private final boolean isChunk;

	private final boolean isFinal;

	private ToolResult(ContentType type, String textContent, List<Media> mediaContent, boolean isChunk,
			boolean isFinal) {
		this.type = type;
		this.textContent = textContent;
		this.mediaContent = mediaContent != null ? List.copyOf(mediaContent) : Collections.emptyList();
		this.isChunk = isChunk;
		this.isFinal = isFinal;
	}

	// ==================== Factory Methods ====================

	/**
	 * Creates a text-only result.
	 * @param text the text content
	 * @return a new ToolResult with text content
	 */
	public static ToolResult text(String text) {
		return new ToolResult(ContentType.TEXT, text, null, false, false);
	}

	/**
	 * Creates a media-only result.
	 * @param media the list of media content
	 * @return a new ToolResult with media content
	 */
	public static ToolResult media(List<Media> media) {
		ContentType type = media.size() == 1 ? detectMediaType(media.get(0)) : ContentType.MIXED;
		return new ToolResult(type, null, media, false, false);
	}

	/**
	 * Creates a mixed text and media result.
	 * @param text the text content
	 * @param media the list of media content
	 * @return a new ToolResult with both text and media content
	 */
	public static ToolResult mixed(String text, List<Media> media) {
		return new ToolResult(ContentType.MIXED, text, media, false, false);
	}

	/**
	 * Creates a streaming chunk (partial result).
	 * @param partialText the partial text content
	 * @return a new ToolResult marked as a chunk
	 */
	public static ToolResult chunk(String partialText) {
		return new ToolResult(ContentType.TEXT, partialText, null, true, false);
	}

	/**
	 * Creates a final chunk that marks stream completion.
	 * @param text the final text content
	 * @return a new ToolResult marked as a final chunk
	 */
	public static ToolResult finalChunk(String text) {
		return new ToolResult(ContentType.TEXT, text, null, true, true);
	}

	// ==================== Immutable Modifiers ====================

	/**
	 * Returns a new ToolResult with the final flag set.
	 * @param isFinal whether this is a final result
	 * @return a new ToolResult with the updated final flag
	 */
	public ToolResult withFinal(boolean isFinal) {
		return new ToolResult(type, textContent, mediaContent, isChunk, isFinal);
	}

	/**
	 * Returns a new ToolResult with the chunk flag set.
	 * @param isChunk whether this is a streaming chunk
	 * @return a new ToolResult with the updated chunk flag
	 */
	public ToolResult withChunk(boolean isChunk) {
		return new ToolResult(type, textContent, mediaContent, isChunk, isFinal);
	}

	// ==================== Merge (for stream reduction) ====================

	/**
	 * Merges this result with another, concatenating text and combining media. Used for
	 * reducing a stream of chunks into a final result.
	 * @param other the other ToolResult to merge with
	 * @return a new merged ToolResult
	 */
	public ToolResult merge(ToolResult other) {
		if (other == null) {
			return this;
		}

		String mergedText = mergeText(this.textContent, other.textContent);
		List<Media> mergedMedia = mergeMedia(this.mediaContent, other.mediaContent);

		ContentType mergedType;
		if (!mergedMedia.isEmpty() && mergedText != null && !mergedText.isEmpty()) {
			mergedType = ContentType.MIXED;
		}
		else if (!mergedMedia.isEmpty()) {
			mergedType = mergedMedia.size() == 1 ? detectMediaType(mergedMedia.get(0)) : ContentType.MIXED;
		}
		else {
			mergedType = ContentType.TEXT;
		}

		// Take the most recent final flag
		boolean mergedFinal = other.isFinal || this.isFinal;

		return new ToolResult(mergedType, mergedText, mergedMedia, false, mergedFinal);
	}

	private static String mergeText(String a, String b) {
		if (a == null || a.isEmpty()) {
			return b;
		}
		if (b == null || b.isEmpty()) {
			return a;
		}
		return a + b;
	}

	private static List<Media> mergeMedia(List<Media> a, List<Media> b) {
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		List<Media> merged = new ArrayList<>(a);
		merged.addAll(b);
		return merged;
	}

	// ==================== Serialization ====================

	/**
	 * Converts to string for ToolResponseMessage. Text-only results return plain text;
	 * multimodal results use JSON with prefix.
	 * @return the string representation for serialization
	 */
	public String toStringResult() {
		if (type == ContentType.TEXT && mediaContent.isEmpty()) {
			return textContent != null ? textContent : "";
		}
		return MULTIMODAL_PREFIX + toMultimodalJson() + MULTIMODAL_SUFFIX;
	}

	/**
	 * Serializes to JSON for multimodal content.
	 * @return the JSON representation
	 */
	public String toMultimodalJson() {
		try {
			MultimodalResultDto dto = new MultimodalResultDto();
			dto.type = type.name();
			dto.text = textContent;
			dto.media = mediaContent.stream().map(ToolResult::mediaToDto).toList();
			dto.isFinal = isFinal;
			return OBJECT_MAPPER.writeValueAsString(dto);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize ToolResult", e);
		}
	}

	/**
	 * Deserializes from string (handles both plain text and multimodal JSON).
	 * @param serialized the serialized string
	 * @return the deserialized ToolResult
	 */
	public static ToolResult fromString(String serialized) {
		if (serialized == null || serialized.isEmpty()) {
			return text("");
		}
		if (isToolResultFormat(serialized)) {
			String json = serialized.substring(MULTIMODAL_PREFIX.length(),
					serialized.length() - MULTIMODAL_SUFFIX.length());
			return fromMultimodalJson(json);
		}
		return text(serialized);
	}

	/**
	 * Safe deserialization that returns text result on failure.
	 * @param serialized the serialized string
	 * @return the deserialized ToolResult or a text result on failure
	 */
	public static ToolResult fromStringSafe(String serialized) {
		try {
			return fromString(serialized);
		}
		catch (Exception e) {
			return text(serialized != null ? serialized : "");
		}
	}

	/**
	 * Checks if a string is in ToolResult multimodal format.
	 * @param s the string to check
	 * @return true if the string is in multimodal format
	 */
	public static boolean isToolResultFormat(String s) {
		return s != null && s.startsWith(MULTIMODAL_PREFIX) && s.endsWith(MULTIMODAL_SUFFIX);
	}

	private static ToolResult fromMultimodalJson(String json) {
		try {
			MultimodalResultDto dto = OBJECT_MAPPER.readValue(json, MultimodalResultDto.class);
			ContentType type = ContentType.valueOf(dto.type);
			List<Media> media = dto.media != null ? dto.media.stream().map(ToolResult::dtoToMedia).toList()
					: Collections.emptyList();
			return new ToolResult(type, dto.text, media, false, dto.isFinal);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to deserialize ToolResult", e);
		}
	}

	// ==================== Getters ====================

	/**
	 * Gets the content type.
	 * @return the content type
	 */
	public ContentType getType() {
		return type;
	}

	/**
	 * Gets the text content.
	 * @return the text content
	 */
	public String getTextContent() {
		return textContent;
	}

	/**
	 * Gets the media content.
	 * @return the list of media content
	 */
	public List<Media> getMediaContent() {
		return mediaContent;
	}

	/**
	 * Checks if this is a streaming chunk.
	 * @return true if this is a chunk
	 */
	@JsonIgnore
	public boolean isChunk() {
		return isChunk;
	}

	/**
	 * Checks if this is a final result.
	 * @return true if this is a final result
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * Checks if this result has media content.
	 * @return true if there is media content
	 */
	@JsonIgnore
	public boolean hasMedia() {
		return mediaContent != null && !mediaContent.isEmpty();
	}

	/**
	 * Checks if this is a text-only result.
	 * @return true if this is text-only
	 */
	@JsonIgnore
	public boolean isTextOnly() {
		return type == ContentType.TEXT && !hasMedia();
	}

	// ==================== Helper Methods ====================

	private static ContentType detectMediaType(Media media) {
		if (media == null || media.getMimeType() == null) {
			return ContentType.FILE;
		}
		String mimeType = media.getMimeType().toString().toLowerCase();
		if (mimeType.startsWith("image/")) {
			return ContentType.IMAGE;
		}
		if (mimeType.startsWith("audio/")) {
			return ContentType.AUDIO;
		}
		if (mimeType.startsWith("video/")) {
			return ContentType.VIDEO;
		}
		return ContentType.FILE;
	}

	private static MediaDto mediaToDto(Media media) {
		MediaDto dto = new MediaDto();
		dto.mimeType = media.getMimeType() != null ? media.getMimeType().toString() : null;
		// Store data as base64 or URL depending on content
		Object data = media.getData();
		if (data instanceof byte[] bytes) {
			dto.dataBase64 = Base64.getEncoder().encodeToString(bytes);
		}
		else if (data instanceof ByteArrayResource resource) {
			dto.dataBase64 = Base64.getEncoder().encodeToString(resource.getByteArray());
		}
		else if (data instanceof Resource resource) {
			try {
				dto.dataBase64 = Base64.getEncoder().encodeToString(resource.getContentAsByteArray());
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to read resource data", e);
			}
		}
		else if (data instanceof URL url) {
			dto.dataUrl = url.toString();
		}
		else if (data instanceof URI uri) {
			dto.dataUrl = uri.toString();
		}
		else if (data instanceof String str) {
			dto.dataUrl = str;
		}
		else if (data != null) {
			throw new IllegalArgumentException("Unsupported media data type: " + data.getClass().getName());
		}
		return dto;
	}

	private static Media dtoToMedia(MediaDto dto) {
		MimeType mimeType = dto.mimeType != null ? MimeType.valueOf(dto.mimeType) : null;
		if (dto.dataBase64 != null) {
			byte[] data = Base64.getDecoder().decode(dto.dataBase64);
			return Media.builder().mimeType(mimeType).data(new ByteArrayResource(data)).build();
		}
		else if (dto.dataUrl != null) {
			// Data URIs (e.g., data:image/png;base64,...) cannot be converted to URL
			if (dto.dataUrl.startsWith("data:")) {
				return Media.builder().mimeType(mimeType).data(dto.dataUrl).build();
			}
			try {
				URL url = new URI(dto.dataUrl).toURL();
				return Media.builder().mimeType(mimeType).data(url).build();
			}
			catch (Exception e) {
				// Fallback for unknown schemes: store as string
				return Media.builder().mimeType(mimeType).data(dto.dataUrl).build();
			}
		}
		throw new IllegalArgumentException("MediaDto must have either dataBase64 or dataUrl");
	}

	// ==================== DTO Classes ====================

	static class MultimodalResultDto {

		public String type;

		public String text;

		public List<MediaDto> media;

		public boolean isFinal;

	}

	static class MediaDto {

		public String mimeType;

		public String dataBase64;

		public String dataUrl;

	}

	// ==================== Object Methods ====================

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ToolResult that = (ToolResult) o;
		return isChunk == that.isChunk && isFinal == that.isFinal && type == that.type
				&& Objects.equals(textContent, that.textContent) && Objects.equals(mediaContent, that.mediaContent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, textContent, mediaContent, isChunk, isFinal);
	}

	@Override
	public String toString() {
		return "ToolResult{" + "type=" + type + ", textContent='"
				+ (textContent != null ? textContent.substring(0, Math.min(50, textContent.length())) + "..." : null)
				+ '\'' + ", mediaCount=" + mediaContent.size() + ", isChunk=" + isChunk + ", isFinal=" + isFinal + '}';
	}

}
