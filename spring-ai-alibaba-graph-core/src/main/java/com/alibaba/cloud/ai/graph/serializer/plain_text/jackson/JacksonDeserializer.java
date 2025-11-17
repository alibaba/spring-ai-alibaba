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
import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
			case ARRAY -> deserializeArrayNode(valueNode, objectMapper, typeMapper);
			case OBJECT, POJO -> {
				String typeHint = null;
				if (valueNode.has("@typeHint")) {
					typeHint = valueNode.get("@typeHint").asText();
				}
				if (valueNode.has(TYPE_PROPERTY)) {
					var type = valueNode.get(TYPE_PROPERTY).asText();
					
					// Special handling for GraphResponse and CompletableFuture
					if ("GraphResponse".equals(type)) {
						yield reconstructGraphResponse(valueNode, objectMapper, typeMapper);
					}
					if ("CompletableFuture".equals(type)) {
						yield reconstructCompletableFuture(valueNode, objectMapper, typeMapper);
					}
					
					var ref = typeMapper.getReference(type)
						.orElseThrow(() -> new IllegalStateException("Type not found: " + type));
					ObjectNode copy = valueNode.deepCopy();
					copy.remove(TYPE_PROPERTY);
					copy.remove("@typeHint");
					ObjectMapper mapperNoTyping = objectMapper.copy();
					mapperNoTyping.setDefaultTyping(null);
					mapperNoTyping.deactivateDefaultTyping();
					yield mapperNoTyping.convertValue(copy, ref);
				}
				if (valueNode.has("@class")) {
					String className = valueNode.get("@class").asText();
					if (!(typeHint != null && className.startsWith("java.util."))) {
						ObjectNode copy = valueNode.deepCopy();
						copy.remove("@class");
						copy.remove("@typeHint");
						try {
							Class<?> clazz = Class.forName(className);
							if (Map.class.isAssignableFrom(clazz)) {
								Map<String, Object> result = new LinkedHashMap<>();
								var fields = copy.fields();
								while (fields.hasNext()) {
									var entry = fields.next();
									result.put(entry.getKey(), valueFromNode(entry.getValue(), objectMapper, typeMapper));
								}
								yield result;
							}
							ObjectMapper mapperNoTyping = objectMapper.copy();
							mapperNoTyping.setDefaultTyping(null);
							mapperNoTyping.deactivateDefaultTyping();
							yield mapperNoTyping.convertValue(copy, clazz);
						}
						catch (ClassNotFoundException ex) {
							throw new IllegalStateException(
									"Cannot instantiate class " + className + " for @class deserialization", ex);
						}
					}
				}
				if (typeHint != null) {
					ObjectNode copy = valueNode.deepCopy();
					copy.remove("@typeHint");
					copy.remove(TYPE_PROPERTY);
					copy.remove("@class");
					try {
						Class<?> clazz = Class.forName(typeHint);
						ObjectMapper mapperNoTyping = objectMapper.copy();
						mapperNoTyping.setDefaultTyping(null);
						mapperNoTyping.deactivateDefaultTyping();
						yield mapperNoTyping.convertValue(copy, clazz);
					}
					catch (ClassNotFoundException ex) {
						throw new IllegalStateException(
								"Cannot instantiate class " + typeHint + " for @typeHint deserialization", ex);
					}
				}
				Map<String, Object> result = new LinkedHashMap<>();
				var fields = valueNode.fields();
				while (fields.hasNext()) {
					var entry = fields.next();
					result.put(entry.getKey(), valueFromNode(entry.getValue(), objectMapper, typeMapper));
				}
				yield result;
			}
			case BOOLEAN -> valueNode.asBoolean();
			case NUMBER -> {
				// Preserve original number type logic
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
					// Fall back to original behavior
					yield valueNode.numberValue();
				}
			}
			case STRING -> valueNode.asText();
			case BINARY -> valueNode.binaryValue();
		};

	}

	private static Object deserializeArrayNode(JsonNode valueNode, ObjectMapper objectMapper, TypeMapper typeMapper)
			throws IOException {
		if (valueNode.size() == 2 && valueNode.get(0).isTextual()) {
			String className = valueNode.get(0).asText();
			JsonNode payload = valueNode.get(1);
			if (payload.isArray()) {
				if (className.startsWith("[") || className.endsWith("[]")) {
					return instantiateArray(className, payload, objectMapper, typeMapper);
				}
				if (className.startsWith("java.")) {
					List<Object> list = new java.util.ArrayList<>(payload.size());
					for (JsonNode element : payload) {
						list.add(valueFromNode(element, objectMapper, typeMapper));
					}
					return list;
				}
			}
		}
		List<Object> list = new java.util.ArrayList<>(valueNode.size());
		for (JsonNode element : valueNode) {
			list.add(valueFromNode(element, objectMapper, typeMapper));
		}
		return list;
	}

	private static Object instantiateArray(String className, JsonNode payload, ObjectMapper objectMapper,
			TypeMapper typeMapper) throws IOException {
		try {
			Class<?> arrayClass = resolveArrayClass(className);
			Class<?> componentType = arrayClass.componentType();
			if (componentType.isPrimitive()) {
				return objectMapper.treeToValue(payload, arrayClass);
			}
			int length = payload.size();
			Object typedArray = Array.newInstance(componentType, length);
			ObjectMapper mapperNoTyping = objectMapper.copy();
			mapperNoTyping.setDefaultTyping(null);
			mapperNoTyping.deactivateDefaultTyping();
			for (int i = 0; i < length; i++) {
				JsonNode elementNode = payload.get(i);
				Object element;
				try {
					element = mapperNoTyping.convertValue(elementNode, componentType);
				}
				catch (IllegalArgumentException ex) {
					element = valueFromNode(elementNode, objectMapper, typeMapper);
				}
				if (element == null && !componentType.isPrimitive()) {
					Array.set(typedArray, i, null);
					continue;
				}
				if (element != null && !componentType.isInstance(element)) {
					return payload.traverse(mapperNoTyping).readValueAs(Object[].class);
				}
				Array.set(typedArray, i, element);
			}
			return typedArray;
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Cannot instantiate array type: " + className, ex);
		}
	}

	private static Class<?> resolveArrayClass(String className) throws ClassNotFoundException {
		if (className.startsWith("[")) {
			return Class.forName(className);
		}
		if (!className.endsWith("[]")) {
			throw new ClassNotFoundException("Unsupported array type representation: " + className);
		}
		String componentName = className.substring(0, className.length() - 2);
		return switch (componentName) {
			case "boolean" -> boolean[].class;
			case "byte" -> byte[].class;
			case "char" -> char[].class;
			case "short" -> short[].class;
			case "int" -> int[].class;
			case "long" -> long[].class;
			case "float" -> float[].class;
			case "double" -> double[].class;
			default -> Class.forName("[L" + componentName + ";");
		};
	}

	/**
	 * Reconstruct GraphResponse from snapshot map.
	 */
	private static Object reconstructGraphResponse(JsonNode valueNode, ObjectMapper objectMapper, TypeMapper typeMapper)
			throws IOException {
		String status = valueNode.has("status") ? valueNode.get("status").asText() : "pending";
		boolean isError = valueNode.has("error") && valueNode.get("error").asBoolean();
		
		Object result = null;
		if (valueNode.has("result")) {
			result = valueFromNode(valueNode.get("result"), objectMapper, typeMapper);
		}
		
		Map<String, Object> metadata = new java.util.LinkedHashMap<>();
		if (valueNode.has("metadata")) {
			JsonNode metadataNode = valueNode.get("metadata");
			if (metadataNode.isObject()) {
				var fields = metadataNode.fields();
				while (fields.hasNext()) {
					var entry = fields.next();
					metadata.put(entry.getKey(), valueFromNode(entry.getValue(), objectMapper, typeMapper));
				}
			}
		}
		
	// Reconstruct GraphResponse based on status
	if (isError) {
		// Extract error details from errorDetails map
		String exceptionClass = "java.lang.RuntimeException";
		String message = "Unknown error";
		
		if (valueNode.has("errorDetails")) {
			JsonNode errorDetails = valueNode.get("errorDetails");
			if (errorDetails.has("class")) {
				exceptionClass = errorDetails.get("class").asText();
			}
			if (errorDetails.has("message")) {
				message = errorDetails.get("message").asText();
			}
		}
		
		// Create exception instance
		Throwable throwable;
		try {
			Class<?> exClass = Class.forName(exceptionClass);
			if (Throwable.class.isAssignableFrom(exClass)) {
				throwable = (Throwable) exClass.getConstructor(String.class).newInstance(message);
			} else {
				throwable = new RuntimeException(message);
			}
		} catch (Exception e) {
			throwable = new RuntimeException(message);
		}
		
		return com.alibaba.cloud.ai.graph.GraphResponse.error(throwable, metadata);
	} else if ("done".equals(status) || "completed".equals(status)) {
		// For both "done" and "completed", reconstruct as a done GraphResponse
		// This provides equivalent state: isDone()=true, resultValue available
		return com.alibaba.cloud.ai.graph.GraphResponse.done(result, metadata);
	} else {
		// For pending status, return null result
		return com.alibaba.cloud.ai.graph.GraphResponse.done(null, metadata);
	}
}
	
	/**
	 * Reconstruct CompletableFuture from snapshot map.
	 */
	private static Object reconstructCompletableFuture(JsonNode valueNode, ObjectMapper objectMapper, TypeMapper typeMapper)
			throws IOException {
		String status = valueNode.has("status") ? valueNode.get("status").asText() : "pending";
		boolean isError = valueNode.has("error") && valueNode.get("error").asBoolean();
		
	java.util.concurrent.CompletableFuture<Object> future = new java.util.concurrent.CompletableFuture<>();
	
	if (isError || "failed".equals(status)) {
		// Extract error details from error map
		String exceptionClass = "java.lang.RuntimeException";
		String message = "Unknown error";
		
		if (valueNode.has("error") && valueNode.get("error").isObject()) {
			JsonNode errorMap = valueNode.get("error");
			if (errorMap.has("class")) {
				exceptionClass = errorMap.get("class").asText();
			}
			if (errorMap.has("message")) {
				message = errorMap.get("message").asText();
			}
		}
		
		Throwable throwable;
		try {
			Class<?> exClass = Class.forName(exceptionClass);
			if (Throwable.class.isAssignableFrom(exClass)) {
				throwable = (Throwable) exClass.getConstructor(String.class).newInstance(message);
			} else {
				throwable = new RuntimeException(message);
			}
		} catch (Exception e) {
			throwable = new RuntimeException(message);
		}
		
		future.completeExceptionally(throwable);
	} else if ("completed".equals(status)) {
		Object result = null;
		if (valueNode.has("result")) {
			result = valueFromNode(valueNode.get("result"), objectMapper, typeMapper);
		}
		future.complete(result);
	}
	// For pending status, leave future incomplete
	
	return future;
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
