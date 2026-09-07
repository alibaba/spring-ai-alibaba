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

import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

class SerializationHelper {

	static final String METADATA_FIELD = "metadata";

	static Map<String, Object> deserializeMetadata(ObjectReadContext context, JsonNode parentNode)
			throws JacksonException {
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
		return context.readValue(context.treeAsTokens(node), new TypeReference<>() {
		});
	}

	static void serializeMetadata(JsonGenerator gen, SerializationContext context, Map<String, Object> metadata)
			throws JacksonException {
		context.defaultSerializeProperty(METADATA_FIELD, metadata, gen);
	}

}
