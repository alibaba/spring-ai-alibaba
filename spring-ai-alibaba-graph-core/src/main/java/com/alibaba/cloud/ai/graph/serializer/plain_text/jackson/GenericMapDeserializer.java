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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

class GenericMapDeserializer extends StdDeserializer<Map<String, Object>> {

	final TypeMapper typeMapper;

	public GenericMapDeserializer(TypeMapper mapper) {
		super(Map.class);
		this.typeMapper = mapper;
	}

	@Override
	public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		var mapper = (ObjectMapper) p.getCodec();
		final JsonNode jsonNode = mapper.readTree(p);

		// Handle null or non-object nodes
		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isObject()) {
			return new HashMap<>();
		}

		final ObjectNode node = (ObjectNode) jsonNode;
		final Map<String, Object> result = new HashMap<>();

		final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

		while (fields.hasNext()) {
			final var entry = fields.next();

			result.put(entry.getKey(), JacksonDeserializer.valueFromNode(entry.getValue(), mapper, typeMapper));
		}

		return result;
	}

}
