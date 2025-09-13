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
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.TypeMapper.TYPE_PROPERTY;

/**
 * A functional interface for deserializing a {@link JsonNode} into an object of type
 * {@code T}. This is used to provide custom deserialization logic within the Jackson
 * framework.
 *
 * @param <T> the type of the object to deserialize to
 */
@FunctionalInterface
public interface JacksonDeserializer<T> {

	/**
	 * Converts a {@link JsonNode} to a standard Java object based on its node type. This
	 * utility method handles the conversion of primitive JSON types, arrays, and objects.
	 * For objects, it can perform polymorphic deserialization if a special {@code _type}
	 * property is present, using the provided {@link TypeMapper}.
	 * @param valueNode the JSON node to convert
	 * @param objectMapper the Jackson {@link ObjectMapper} to use for data binding
	 * @param typeMapper the {@link TypeMapper} used to resolve custom types for
	 * polymorphic deserialization
	 * @return the converted Java object (e.g., {@link String}, {@link Number},
	 * {@link Boolean}, {@link List}, {@code byte[]}, or a custom POJO)
	 * @throws IOException if the conversion fails due to an I/O error or a data binding
	 * issue
	 */
	static Object valueFromNode(JsonNode valueNode, ObjectMapper objectMapper, TypeMapper typeMapper)
			throws IOException {
		if (valueNode == null) { // GUARD
			return null;
		}
		return switch (valueNode.getNodeType()) {
			case NULL, MISSING -> null;
			case ARRAY -> objectMapper.treeToValue(valueNode, List.class);
			case OBJECT, POJO -> {
				if (valueNode.has(TYPE_PROPERTY)) {
					var type = valueNode.get(TYPE_PROPERTY).asText();
					// Deserialize to a specific class
					var ref = typeMapper.getReference(type)
						.orElseThrow(() -> new IllegalStateException("Type not found: " + type));
					yield objectMapper.treeToValue(valueNode, ref);
				}
				yield objectMapper.treeToValue(valueNode, Object.class);
			}
			case BOOLEAN -> valueNode.asBoolean();
			case NUMBER -> {
				// 保持原始数字类型的逻辑
				if (valueNode.isInt()) {
					yield valueNode.asInt();
				}
				else if (valueNode.isLong()) {
					yield valueNode.asLong();
				}
				else if (valueNode.isFloat()) {
					yield (float) valueNode.asDouble();
				}
				else if (valueNode.isDouble()) {
					yield valueNode.asDouble();
				}
				else if (valueNode.isBigInteger()) {
					yield valueNode.bigIntegerValue();
				}
				else if (valueNode.isBigDecimal()) {
					yield valueNode.decimalValue();
				}
				else {
					// 回退到原始行为
					yield valueNode.numberValue();
				}
			}
			case STRING -> valueNode.asText();
			case BINARY -> valueNode.binaryValue();
		};

	}

	/**
	 * Deserializes the given {@link JsonNode} into an object of type {@code T}.
	 * @param node the JSON node to deserialize
	 * @param ctx the deserialization context
	 * @return the deserialized object
	 * @throws IOException if an I/O error occurs
	 */
	T deserialize(JsonNode node, DeserializationContext ctx) throws IOException;

}
