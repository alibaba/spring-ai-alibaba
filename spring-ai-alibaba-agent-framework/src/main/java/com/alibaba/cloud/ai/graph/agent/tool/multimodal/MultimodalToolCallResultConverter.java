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
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link ToolCallResultConverter} that handles {@link ToolMultimodalResult} for
 * image/audio and other multimodal tool outputs.
 *
 * <p>Tools return {@link ToolMultimodalResult} with {@link Media} (URI or bytes) without
 * caring about output format. This converter serializes based on {@link OutputFormat}:
 *
 * <ul>
 *   <li><b>url</b> (default): Prefer url when available; Media(bytes) → save to temp file</li>
 *   <li><b>base64</b>: Prefer base64 when available; Media(URI) → fetch and encode if needed</li>
 * </ul>
 *
 * <p>When both url and base64 are available ({@link ToolMultimodalResult#mediaFromUrlAndBase64}),
 * the converter uses the format matching OutputFormat without unnecessary fetch or encode.
 *
 * <p>Usage: configure at instantiation. For {@code @Tool(resultConverter = X.class)},
 * use default (url) or {@link MultimodalBase64ToolCallResultConverter} for base64.
 */
public class MultimodalToolCallResultConverter implements ToolCallResultConverter {

	private static final ToolCallResultConverter DEFAULT_CONVERTER = new DefaultToolCallResultConverter();

	private final OutputFormat outputFormat;

	private final Path tempDir;

	/**
	 * Default constructor for {@code @Tool(resultConverter = MultimodalToolCallResultConverter.class)}.
	 * Uses {@link OutputFormat#url}.
	 */
	public MultimodalToolCallResultConverter() {
		this(OutputFormat.url);
	}

	/**
	 * Constructor with configurable output format.
	 * @param outputFormat url (model-friendly) or base64 (inline for client)
	 */
	public MultimodalToolCallResultConverter(OutputFormat outputFormat) {
		this.outputFormat = outputFormat != null ? outputFormat : OutputFormat.url;
		try {
			this.tempDir = Files.createTempDirectory("multimodal-converter-");
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to create temp directory for converter", e);
		}
	}

	@Override
	public String convert(@Nullable Object result, @Nullable Type returnType) {
		if (result instanceof ToolMultimodalResult multimodal) {
			return toStructuredJson(multimodal);
		}
		return DEFAULT_CONVERTER.convert(result, returnType);
	}

	private String toStructuredJson(ToolMultimodalResult result) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (result.text() != null && !result.text().isBlank()) {
			map.put("text", result.text());
		}
		if (result.media() != null && !result.media().isEmpty()) {
			List<Map<String, Object>> mediaList = new ArrayList<>();
			for (Media item : result.media()) {
				mediaList.add(mediaToMap(item));
			}
			map.put("media", mediaList);
		}
		return JsonParser.toJson(map);
	}

	private Map<String, Object> mediaToMap(Media media) {
		Map<String, Object> itemMap = new LinkedHashMap<>();
		String mimeTypeStr = media.getMimeType() != null ? media.getMimeType().toString() : null;
		if (mimeTypeStr != null && !mimeTypeStr.isBlank()) {
			itemMap.put("mimeType", mimeTypeStr);
		}
		String type = inferMediaType(mimeTypeStr);
		if (type != null) {
			itemMap.put("type", type);
		}

		Object data = media.getData();
		if (data instanceof ToolMultimodalResult.MediaFormats formats) {
			handleMediaFormats(formats, mimeTypeStr, itemMap);
		}
		else if (data instanceof URI uri) {
			putUriOrEncodeToDataUrl(uri, mimeTypeStr, itemMap);
		}
		else if (data instanceof URL url) {
			try {
				putUriOrEncodeToDataUrl(url.toURI(), mimeTypeStr, itemMap);
			}
			catch (java.net.URISyntaxException ex) {
				putUriOrEncodeToDataUrl(URI.create(url.toExternalForm()), mimeTypeStr, itemMap);
			}
		}
		else if (data instanceof String s && isUriLike(s)) {
			putUriOrEncodeToDataUrl(URI.create(s), mimeTypeStr, itemMap);
		}
		else if (data instanceof Resource resource) {
			handleBytes(() -> {
				try {
					return resource.getContentAsByteArray();
				}
				catch (IOException e) {
					throw new RuntimeException("Failed to read resource", e);
				}
			}, mimeTypeStr, itemMap);
		}
		else if (data != null) {
			try {
				byte[] bytes = media.getDataAsByteArray();
				if (bytes != null && bytes.length > 0) {
					handleBytes(() -> bytes, mimeTypeStr, itemMap);
				}
			}
			catch (IllegalStateException e) {
				String s = data.toString();
				if (isUriLike(s)) {
					putUriOrEncodeToDataUrl(URI.create(s), mimeTypeStr, itemMap);
				}
			}
		}
		return itemMap;
	}

	private void putUriOrEncodeToDataUrl(URI uri, String mimeTypeStr, Map<String, Object> itemMap) {
		if (outputFormat == OutputFormat.base64) {
			encodeUriToDataUrl(uri, mimeTypeStr, itemMap);
		}
		else {
			itemMap.put("url", uri.toString());
		}
	}

	private static boolean isUriLike(String s) {
		if (s == null || s.isBlank()) {
			return false;
		}
		String t = s.trim().toLowerCase();
		return t.startsWith("file:") || t.startsWith("http://") || t.startsWith("https://");
	}

	private void handleBytes(java.util.function.Supplier<byte[]> bytesSupplier, String mimeTypeStr,
			Map<String, Object> itemMap) {
		try {
			byte[] bytes = bytesSupplier.get();
			if (bytes == null || bytes.length == 0) {
				return;
			}
			if (outputFormat == OutputFormat.base64) {
				String base64 = Base64.getEncoder().encodeToString(bytes);
				String dataUrl = (mimeTypeStr != null ? "data:" + mimeTypeStr + ";base64," : "data:application/octet-stream;base64,") + base64;
				itemMap.put("data", dataUrl);
			}
			else {
				Path file = tempDir.resolve("media-" + UUID.randomUUID() + ".bin");
				Files.write(file, bytes);
				itemMap.put("url", file.toUri().toString());
			}
		}
		catch (Exception e) {
			itemMap.put("error", "Failed to process media: " + e.getMessage());
		}
	}

	private void handleMediaFormats(ToolMultimodalResult.MediaFormats formats, String mimeTypeStr,
			Map<String, Object> itemMap) {
		String url = formats.url();
		String base64 = formats.base64();
		if (outputFormat == OutputFormat.base64) {
			if (base64 != null && !base64.isBlank()) {
				String dataUrl = (mimeTypeStr != null ? "data:" + mimeTypeStr + ";base64," : "data:application/octet-stream;base64,") + base64;
				itemMap.put("data", dataUrl);
			}
			else if (url != null && !url.isBlank()) {
				encodeUriToDataUrl(URI.create(url), mimeTypeStr, itemMap);
			}
		}
		else {
			if (url != null && !url.isBlank()) {
				itemMap.put("url", url);
			}
			else if (base64 != null && !base64.isBlank()) {
				handleBytes(() -> Base64.getDecoder().decode(base64), mimeTypeStr, itemMap);
			}
		}
	}

	private void encodeUriToDataUrl(URI uri, String mimeTypeStr, Map<String, Object> itemMap) {
		try {
			byte[] bytes = fetchBytes(uri.toString());
			if (bytes != null && bytes.length > 0) {
				String base64 = Base64.getEncoder().encodeToString(bytes);
				String dataUrl = (mimeTypeStr != null ? "data:" + mimeTypeStr + ";base64," : "data:application/octet-stream;base64,") + base64;
				itemMap.put("data", dataUrl);
			}
			else {
				itemMap.put("url", uri.toString());
			}
		}
		catch (Exception e) {
			itemMap.put("url", uri.toString());
		}
	}

	private byte[] fetchBytes(String urlString) throws IOException {
		try (InputStream in = new URL(urlString).openStream()) {
			return in.readAllBytes();
		}
	}

	private String inferMediaType(@Nullable String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return null;
		}
		if (mimeType.startsWith("image/")) {
			return "image";
		}
		if (mimeType.startsWith("audio/")) {
			return "audio";
		}
		if (mimeType.startsWith("video/")) {
			return "video";
		}
		return null;
	}
}
