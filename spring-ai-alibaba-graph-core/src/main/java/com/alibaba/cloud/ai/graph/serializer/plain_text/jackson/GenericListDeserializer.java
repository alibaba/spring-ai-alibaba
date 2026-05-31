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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

class GenericListDeserializer extends StdDeserializer<List<Object>> implements ContextualDeserializer {

	final TypeMapper typeMapper;

	private final JavaType elementType;

	public GenericListDeserializer(TypeMapper typeMapper) {
		this(typeMapper, null);
	}

	private GenericListDeserializer(TypeMapper typeMapper, JavaType elementType) {
		super(List.class);
		this.typeMapper = Objects.requireNonNull(typeMapper, "typeMapper cannot be null");
		this.elementType = elementType;
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property)
			throws JsonMappingException {
		JavaType contextualType = ctx.getContextualType();
		if (contextualType == null && property != null) {
			contextualType = property.getType();
		}

		JavaType contextualElementType = null;
		if (contextualType != null && contextualType.isCollectionLikeType() && contextualType.containedTypeCount() > 0) {
			contextualElementType = contextualType.containedType(0);
		}
		if (contextualElementType == null || contextualElementType.hasRawClass(Object.class)) {
			return this;
		}
		return new GenericListDeserializer(typeMapper, contextualElementType);
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
		final ObjectMapper typedMapper = hasTypedElement() ? mapperWithoutDefaultTyping(mapper) : null;

		for (JsonNode valueNode : node) {
			result.add(deserializeElement(valueNode, mapper, typedMapper));
		}

		return result;
	}

	private Object deserializeElement(JsonNode valueNode, ObjectMapper mapper, ObjectMapper typedMapper)
			throws IOException {
		if (!hasTypedElement() || valueNode == null || valueNode.isNull()) {
			return JacksonDeserializer.valueFromNode(valueNode, mapper, typeMapper);
		}
		if (valueNode.isObject()
				&& (valueNode.has("@class") || valueNode.has("@type") || valueNode.has("@typeHint"))) {
			return JacksonDeserializer.valueFromNode(valueNode, mapper, typeMapper);
		}
		try {
			return typedMapper.readValue(typedMapper.treeAsTokens(valueNode), elementType);
		}
		catch (JsonProcessingException ex) {
			return JacksonDeserializer.valueFromNode(valueNode, mapper, typeMapper);
		}
	}

	private boolean hasTypedElement() {
		return elementType != null && !elementType.hasRawClass(Object.class);
	}

	private ObjectMapper mapperWithoutDefaultTyping(ObjectMapper mapper) {
		ObjectMapper typedMapper = mapper.copy();
		typedMapper.setDefaultTyping(null);
		typedMapper.deactivateDefaultTyping();
		return typedMapper;
	}

}
