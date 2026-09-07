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

import java.util.HashMap;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.ObjectNode;

class GenericMapDeserializer extends StdDeserializer<Map<String, Object>> {

	final TypeMapper typeMapper;

	public GenericMapDeserializer(TypeMapper mapper) {
		super(Map.class);
		this.typeMapper = mapper;
	}

	@Override
	public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
		ObjectReadContext readContext = p.objectReadContext();
		final JsonNode jsonNode = ctx.readTree(p);

		// Handle null or non-object nodes
		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isObject()) {
			return new HashMap<>();
		}

		final ObjectNode node = (ObjectNode) jsonNode;
		final Map<String, Object> result = new HashMap<>();

		for (var entry : node.properties()) {
			String key = entry.getKey();

			if ("@class".equals(key) || "@type".equals(key) || "@typeHint".equals(key)) {
				continue;
			}

			JsonNode valueNode = entry.getValue();
			if (valueNode.isObject() && valueNode.has("@type")) {
				var reference = typeMapper.getReference(valueNode.get("@type").asText());
				if (reference.isPresent()) {
					ObjectNode typedNode = (ObjectNode) valueNode.deepCopy();
					typedNode.remove("@type");
					typedNode.remove("@typeHint");
					result.put(key, JacksonDeserializer.deserializeWithContext(typedNode, reference.get(), ctx));
					continue;
				}
			}
			result.put(key, JacksonDeserializer.valueFromNode(valueNode, readContext, typeMapper));
		}

		return result;
	}

}
