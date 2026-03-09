/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tool.multimodal;

import org.springframework.ai.content.Media;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic result type for tools that return multimodal content (image, audio, etc.).
 *
 * <p>Reuses {@link Media} from Spring AI to support both URL references and raw byte data.
 * A custom {@link MultimodalToolCallResultConverter} converts this to a structured string
 * for the LLM and for frontend parsing.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // From URL
 * return ToolMultimodalResult.builder()
 *         .text("Image generated.")
 *         .media(ToolMultimodalResult.mediaFromUrl(url, MimeTypeUtils.IMAGE_PNG))
 *         .build();
 *
 * // From URL and base64 (when model returns both - converter picks by OutputFormat)
 * return ToolMultimodalResult.builder()
 *         .text("Image generated.")
 *         .media(ToolMultimodalResult.mediaFromUrlAndBase64(url, b64Json, MimeTypeUtils.IMAGE_PNG))
 *         .build();
 *
 * // From byte data
 * return ToolMultimodalResult.builder()
 *         .text("Audio generated.")
 *         .media(ToolMultimodalResult.mediaFromBytes(audioBytes, MimeTypeUtils.parseMimeType("audio/mpeg")))
 *         .build();
 * }</pre>
 */
public final class ToolMultimodalResult {

	private final String text;

	private final List<Media> media;

	private ToolMultimodalResult(String text, List<Media> media) {
		this.text = text;
		this.media = media != null ? List.copyOf(media) : List.of();
	}

	public String text() {
		return text;
	}

	public List<Media> media() {
		return media;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ToolMultimodalResult of(String text) {
		return builder().text(text).build();
	}

	public static ToolMultimodalResult of(String text, Media... mediaItems) {
		return builder().text(text).media(mediaItems).build();
	}

	/**
	 * Create Media from a URL/URI string.
	 */
	public static Media mediaFromUrl(String url, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(URI.create(url)).build();
	}

	/**
	 * Create Media from a URI.
	 */
	public static Media mediaFromUri(URI uri, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(uri).build();
	}

	/**
	 * Create Media from raw byte data.
	 */
	public static Media mediaFromBytes(byte[] data, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(new ByteArrayResource(data)).build();
	}

	/**
	 * Create Media from a Spring Resource.
	 */
	public static Media mediaFromResource(Resource resource, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(resource).build();
	}

	/**
	 * Create Media when both URL and base64 are available (e.g. from model response).
	 * The converter will choose the appropriate format based on OutputFormat, avoiding
	 * unnecessary fetch when base64 is already present.
	 *
	 * @param url    URL reference (may be null)
	 * @param base64 Base64-encoded data (raw string, not data URL; may be null)
	 */
	public static Media mediaFromUrlAndBase64(String url, String base64, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(new MediaFormats(url, base64)).build();
	}

	/**
	 * Create Media from base64 string only (when model returns b64_json directly).
	 *
	 * @param base64 Base64-encoded data (raw string, not data URL)
	 */
	public static Media mediaFromBase64(String base64, MimeType mimeType) {
		return Media.builder().mimeType(mimeType).data(new MediaFormats(null, base64)).build();
	}

	/**
	 * Holds both URL and base64 when available. Used by {@link MultimodalToolCallResultConverter}
	 * to prefer the format matching OutputFormat without unnecessary conversion.
	 */
	public record MediaFormats(String url, String base64) {
	}

	public static class Builder {

		private String text;

		private final List<Media> media = new ArrayList<>();

		public Builder text(String text) {
			this.text = text;
			return this;
		}

		public Builder media(Media... items) {
			if (items != null) {
				Collections.addAll(this.media, items);
			}
			return this;
		}

		public Builder media(List<Media> items) {
			if (items != null) {
				this.media.addAll(items);
			}
			return this;
		}

		public ToolMultimodalResult build() {
			return new ToolMultimodalResult(text, media);
		}
	}
}
