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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class GenericListDeserializer extends StdDeserializer<List<Object>> {

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
	public ValueDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) {
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
	public List<Object> deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
		final ObjectReadContext readContext = p.objectReadContext();
		final JsonNode jsonNode = ctx.readTree(p);

		// Handle null or non-array nodes
		if (jsonNode == null || jsonNode.isNull() || !jsonNode.isArray()) {
			return new LinkedList<>();
		}

		final ArrayNode node = (ArrayNode) jsonNode;
		final List<Object> result = new LinkedList<>();
		for (JsonNode valueNode : node) {
			result.add(deserializeElement(valueNode, readContext, ctx));
		}

		return result;
	}

	private Object deserializeElement(JsonNode valueNode, ObjectReadContext readContext, DeserializationContext context)
			throws JacksonException {
		if (!hasTypedElement() || valueNode == null || valueNode.isNull()) {
			return JacksonDeserializer.valueFromNode(valueNode, readContext, typeMapper);
		}
		if (valueNode.isObject()
				&& (valueNode.has("@class") || valueNode.has("@type") || valueNode.has("@typeHint"))) {
			if (valueNode.has("@type")) {
				var reference = typeMapper.getReference(valueNode.get("@type").asText());
				if (reference.isPresent()) {
					ObjectNode typedNode = (ObjectNode) valueNode.deepCopy();
					typedNode.remove("@type");
					typedNode.remove("@typeHint");
					return JacksonDeserializer.deserializeWithContext(typedNode, reference.get(), context);
				}
			}
			return JacksonDeserializer.valueFromNode(valueNode, readContext, typeMapper);
		}
		try {
			return readContext.readValue(readContext.treeAsTokens(valueNode), elementType);
		}
		catch (JacksonException ex) {
			return JacksonDeserializer.valueFromNode(valueNode, readContext, typeMapper);
		}
	}

	private boolean hasTypedElement() {
		return elementType != null && !elementType.hasRawClass(Object.class);
	}

}
