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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SerializationHelper {

	static final String METADATA_FIELD = "metadata";

	private static final String THOUGHT_SIGNATURES_METADATA_KEY = "thoughtSignatures";

	static Map<String, Object> deserializeMetadata(ObjectMapper mapper, JsonNode parentNode)
			throws JsonProcessingException {
		if (parentNode == null) {
			return Map.of();
		}

		var node = parentNode.findValue(METADATA_FIELD);

		if (node == null || node.isNull() || node.isEmpty()) {
			return Map.of();
		}
		if (!node.isObject()) {
			throw new IllegalStateException("Metadata must be an object");
		}
		return mapper.treeToValue(node, new TypeReference<>() {
		});
	}

	static void serializeMetadata(JsonGenerator gen, Map<String, Object> metadata) throws IOException {
		gen.writeObjectField(METADATA_FIELD, metadata);
	}

	static Map<String, Object> deserializeAssistantMetadata(ObjectMapper mapper, JsonNode parentNode)
			throws JsonProcessingException {
		return restoreThoughtSignatures(deserializeMetadata(mapper, parentNode));
	}

	static void serializeAssistantMetadata(JsonGenerator gen, Map<String, Object> metadata) throws IOException {
		gen.writeObjectField(METADATA_FIELD, normalizeThoughtSignatures(metadata));
	}

	private static Map<String, Object> normalizeThoughtSignatures(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return metadata;
		}
		Object thoughtSignatures = metadata.get(THOUGHT_SIGNATURES_METADATA_KEY);
		Object normalizedThoughtSignatures = normalizeThoughtSignatureList(thoughtSignatures);
		if (normalizedThoughtSignatures == thoughtSignatures) {
			return metadata;
		}
		Map<String, Object> normalized = new LinkedHashMap<>(metadata);
		normalized.put(THOUGHT_SIGNATURES_METADATA_KEY, normalizedThoughtSignatures);
		return normalized;
	}

	private static Object normalizeThoughtSignatureList(Object value) {
		if (!(value instanceof List<?> signatures)) {
			return value;
		}

		List<Object> normalized = new ArrayList<>(signatures.size());
		boolean changed = false;
		for (Object signature : signatures) {
			Object normalizedSignature = normalizeThoughtSignature(signature);
			normalized.add(normalizedSignature);
			changed = changed || normalizedSignature != signature;
		}
		return changed ? normalized : value;
	}

	private static Object normalizeThoughtSignature(Object value) {
		return (value instanceof byte[] bytes) ? new BinaryMetadata(bytes) : value;
	}

	private static Map<String, Object> restoreThoughtSignatures(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return metadata;
		}
		Object thoughtSignatures = metadata.get(THOUGHT_SIGNATURES_METADATA_KEY);
		Object restoredThoughtSignatures = restoreThoughtSignatureList(thoughtSignatures);
		if (restoredThoughtSignatures == thoughtSignatures) {
			return metadata;
		}
		Map<String, Object> restored = new LinkedHashMap<>(metadata);
		restored.put(THOUGHT_SIGNATURES_METADATA_KEY, restoredThoughtSignatures);
		return restored;
	}

	private static Object restoreThoughtSignatureList(Object value) {
		if (!(value instanceof List<?> signatures)) {
			return value;
		}

		List<Object> restored = new ArrayList<>(signatures.size());
		boolean changed = false;
		for (Object signature : signatures) {
			Object restoredSignature = restoreThoughtSignature(signature);
			restored.add(restoredSignature);
			changed = changed || restoredSignature != signature;
		}
		return changed ? restored : value;
	}

	private static Object restoreThoughtSignature(Object value) {
		if (value instanceof BinaryMetadata binaryMetadata) {
			return binaryMetadata.toByteArray();
		}
		if (value instanceof Map<?, ?> map) {
			byte[] bytes = byteArrayFromNumbers(map.get(BinaryMetadata.DATA_FIELD));
			return bytes != null ? bytes : value;
		}
		byte[] bytes = byteArrayFromNumbers(value);
		return bytes != null ? bytes : value;
	}

	private static byte[] byteArrayFromNumbers(Object value) {
		if (!(value instanceof Collection<?> numbers)) {
			return null;
		}
		byte[] bytes = new byte[numbers.size()];
		int index = 0;
		for (Object item : numbers) {
			if (!(item instanceof Number number)) {
				return null;
			}
			bytes[index++] = number.byteValue();
		}
		return bytes;
	}

	public static class BinaryMetadata {

		private static final String DATA_FIELD = "data";

		private List<Integer> data;

		@SuppressWarnings("unused")
		public BinaryMetadata() {
		}

		BinaryMetadata(byte[] bytes) {
			this.data = new ArrayList<>(bytes.length);
			for (byte item : bytes) {
				this.data.add((int) item);
			}
		}

		public List<Integer> getData() {
			return data;
		}

		@SuppressWarnings("unused")
		public void setData(List<Integer> data) {
			this.data = data;
		}

		private byte[] toByteArray() {
			byte[] bytes = new byte[data.size()];
			for (int i = 0; i < data.size(); i++) {
				bytes[i] = data.get(i).byteValue();
			}
			return bytes;
		}

	}

}
