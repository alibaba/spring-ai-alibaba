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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

class SerializationHelper {

	static final String METADATA_FIELD = "metadata";

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

	static void serializeMetadata(JsonGenerator gen, SerializerProvider provider, Map<String, Object> metadata)
			throws IOException {
		gen.writeObjectField(METADATA_FIELD, normalizeMetadataValue(provider, metadata));
	}

	private static Object normalizeMetadataValue(SerializerProvider provider, Object value) throws IOException {
		if (value instanceof Map<?, ?> map) {
			Map<Object, Object> normalized = new LinkedHashMap<>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				normalized.put(entry.getKey(), normalizeMetadataValue(provider, entry.getValue()));
			}
			return normalized;
		}
		if (value instanceof Collection<?> collection) {
			List<Object> normalized = new ArrayList<>(collection.size());
			for (Object item : collection) {
				normalized.add(normalizeMetadataValue(provider, item));
			}
			return normalized;
		}
		if (value != null && value.getClass().isArray()) {
			int length = Array.getLength(value);
			List<Object> normalized = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				normalized.add(normalizeMetadataValue(provider, Array.get(value, i)));
			}
			return normalized;
		}
		if (value instanceof Record record) {
			Map<String, Object> normalized = new LinkedHashMap<>();
			List<BeanPropertyDefinition> properties = provider.getConfig()
				.introspect(provider.constructType(record.getClass()))
				.findProperties();
			for (BeanPropertyDefinition property : properties) {
				if (!property.couldSerialize()) {
					continue;
				}
				AnnotatedMember accessor = property.getAccessor();
				if (accessor == null) {
					continue;
				}
				try {
					accessor.fixAccess(provider.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
					normalized.put(property.getName(), normalizeMetadataValue(provider, accessor.getValue(record)));
				}
				catch (IllegalArgumentException ex) {
					throw new IOException("Failed to serialize metadata record " + record.getClass().getName(), ex);
				}
			}
			return normalized;
		}
		return value;
	}

}
