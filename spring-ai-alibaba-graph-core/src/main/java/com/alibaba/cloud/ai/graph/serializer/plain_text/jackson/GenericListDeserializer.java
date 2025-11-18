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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

class GenericListDeserializer extends StdDeserializer<List<Object>> {

	final TypeMapper typeMapper;

	public GenericListDeserializer(TypeMapper typeMapper) {
		super(List.class);
		this.typeMapper = Objects.requireNonNull(typeMapper, "typeMapper cannot be null");
	}

	@Override
	public List<Object> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		final ObjectMapper mapper = (ObjectMapper) p.getCodec();
		final JsonNode jsonNode = mapper.readTree(p);

		// Handle null or non-array nodes
		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isArray()) {
			return new LinkedList<>();
		}

		final ArrayNode node = (ArrayNode) jsonNode;
		final List<Object> result = new LinkedList<>();

		for (JsonNode valueNode : node) {

			result.add(JacksonDeserializer.valueFromNode(valueNode, mapper, typeMapper));
		}

		return result;
	}

}
