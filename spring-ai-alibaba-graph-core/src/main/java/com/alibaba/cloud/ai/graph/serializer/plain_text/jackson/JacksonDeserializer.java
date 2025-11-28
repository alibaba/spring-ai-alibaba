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
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	Logger logger = LoggerFactory.getLogger(JacksonDeserializer.class);

	/**
	 * Cache for deserialization strategies to avoid repeated trial-and-error.
	 * Key: target class, Value: the most efficient deserialization strategy for that class
	 * 
	 * Note: This cache is bounded to prevent unbounded memory growth.
	 * In typical applications, the number of unique classes being deserialized is limited.
	 * If the cache fills up, older entries will be evicted (not implemented yet, but recommended for production).
	 */
	Map<Class<?>, DeserializationStrategy> STRATEGY_CACHE = new ConcurrentHashMap<>();
	
	/**
	 * Maximum cache size to prevent unbounded growth.
	 * This limit is reasonable for most applications.
	 */
	int MAX_CACHE_SIZE = 1000;

	/**
	 * Deserialization strategies in order of preference and capability
	 */
	enum DeserializationStrategy {
		/**
		 * Use readValue - full deserialization with custom deserializers support
		 */
		READ_VALUE,
		/**
		 * Use convertValue - simple POJO conversion, faster but limited
		 */
		CONVERT_VALUE,
		/**
		 * Cannot instantiate, degrade to Map
		 */
		DEGRADE_TO_MAP
	}

	/**
	 * Check if a class cannot be instantiated by Jackson.
	 * Non-static inner classes, local classes, and anonymous classes require
	 * an outer class instance and cannot be deserialized.
	 */
	static boolean isNonInstantiableClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !java.lang.reflect.Modifier.isStatic(clazz.getModifiers()))
				|| clazz.isLocalClass()
				|| clazz.isAnonymousClass();
	}

	/**
	 * Degrade an ObjectNode to a Map when the target class cannot be instantiated.
	 */
	static Map<String, Object> degradeToMap(ObjectNode node, ObjectMapper objectMapper, TypeMapper typeMapper)
			throws IOException {
		Map<String, Object> result = new LinkedHashMap<>();
		var fields = node.fields();
		while (fields.hasNext()) {
			var entry = fields.next();
			result.put(entry.getKey(), valueFromNode(entry.getValue(), objectMapper, typeMapper));
		}
		return result;
	}

	/**
	 * Unified deserialization strategy with progressive fallback and caching.
	 * This is the framework-level solution that handles all types automatically:
	 * 
	 * 1. Try readValue first (supports custom deserializers, @JsonCreator, etc.)
	 * 2. Fall back to convertValue if readValue fails (faster for simple POJOs)
	 * 3. Degrade to Map if both fail (for non-instantiable classes)
	 * 4. Cache the successful strategy to avoid repeated trial-and-error
	 * 
	 * @param node the JSON node to deserialize
	 * @param targetClass the target class to deserialize to
	 * @param objectMapper the ObjectMapper instance
	 * @param typeMapper the TypeMapper for type resolution
	 * @return the deserialized object
	 * @throws IOException if deserialization fails completely
	 */
	static Object deserializeWithStrategy(
			ObjectNode node,
			Class<?> targetClass,
			ObjectMapper objectMapper,
			TypeMapper typeMapper) throws IOException {

		// Quick check for known non-instantiable classes
		if (isNonInstantiableClass(targetClass) || Map.class.isAssignableFrom(targetClass)) {
			return degradeToMap(node, objectMapper, typeMapper);
		}

		// Prepare ObjectMapper without default typing
		ObjectMapper mapperNoTyping = objectMapper.copy();
		mapperNoTyping.setDefaultTyping(null);
		mapperNoTyping.deactivateDefaultTyping();

		// Check cached strategy
		DeserializationStrategy cachedStrategy = STRATEGY_CACHE.get(targetClass);

		if (cachedStrategy == DeserializationStrategy.READ_VALUE) {
			try {
				return mapperNoTyping.readValue(mapperNoTyping.treeAsTokens(node), targetClass);
			}
			catch (IOException e) {
				// Strategy might be outdated, clear cache and retry
				STRATEGY_CACHE.remove(targetClass);
			}
		}
		else if (cachedStrategy == DeserializationStrategy.CONVERT_VALUE) {
			try {
				return mapperNoTyping.convertValue(node, targetClass);
			}
			catch (RuntimeException e) {
				// Strategy might be outdated, clear cache and retry
				STRATEGY_CACHE.remove(targetClass);
			}
		}
		else if (cachedStrategy == DeserializationStrategy.DEGRADE_TO_MAP) {
			return degradeToMap(node, objectMapper, typeMapper);
		}

		// First time encountering this type, perform strategy detection
		try {
			// Try readValue first - most comprehensive deserialization
			Object result = mapperNoTyping.readValue(mapperNoTyping.treeAsTokens(node), targetClass);
			cacheStrategy(targetClass, DeserializationStrategy.READ_VALUE);
			return result;
		}
		catch (IOException readValueEx) {
			// Log at TRACE level for debugging
			if (logger.isTraceEnabled()) {
				logger.trace("readValue failed for {}, trying convertValue", targetClass.getName(), readValueEx);
			}
			// readValue failed, try convertValue
			try {
				Object result = mapperNoTyping.convertValue(node, targetClass);
				cacheStrategy(targetClass, DeserializationStrategy.CONVERT_VALUE);
				logStrategyFallback(targetClass, "readValue", "convertValue");
				return result;
			}
			catch (RuntimeException convertEx) {
				// Both methods failed, degrade to Map
				if (logger.isDebugEnabled()) {
					logger.debug("Both readValue and convertValue failed for {}, degrading to Map", 
						targetClass.getName(), convertEx);
				}
				cacheStrategy(targetClass, DeserializationStrategy.DEGRADE_TO_MAP);
				logStrategyFallback(targetClass, "convertValue", "Map");
				return degradeToMap(node, objectMapper, typeMapper);
			}
		}
	}

	/**
	 * Cache a deserialization strategy with size limit protection.
	 * If cache size exceeds MAX_CACHE_SIZE, clear 10% of entries to prevent unbounded growth.
	 */
	static void cacheStrategy(Class<?> targetClass, DeserializationStrategy strategy) {
		// Simple cache size management: if full, clear some space
		if (STRATEGY_CACHE.size() >= MAX_CACHE_SIZE) {
			// Clear approximately 10% of cache entries
			int entriesToRemove = MAX_CACHE_SIZE / 10;
			STRATEGY_CACHE.keySet().stream()
				.limit(entriesToRemove)
				.forEach(STRATEGY_CACHE::remove);
			
			if (logger.isDebugEnabled()) {
				logger.debug("Strategy cache reached limit ({}), cleared {} entries", 
					MAX_CACHE_SIZE, entriesToRemove);
			}
		}
		STRATEGY_CACHE.put(targetClass, strategy);
	}

	/**
	 * Log deserialization strategy fallback for debugging and monitoring.
	 * Uses DEBUG level to avoid excessive logging in production.
	 */
	static void logStrategyFallback(Class<?> targetClass, String from, String to) {
		if (logger.isDebugEnabled()) {
			logger.debug(
					"Deserialization fallback for {}: {} failed, using {} instead. "
							+ "Consider registering a custom deserializer if this occurs frequently.",
					targetClass.getName(), from, to);
		}
	}

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
					
					// Special handling for GraphResponse, ChatResponse and CompletableFuture
					if ("GraphResponse".equals(type)) {
						yield reconstructGraphResponse(valueNode, objectMapper, typeMapper);
					}
					if ("ChatResponse".equals(type)) {
						// ChatResponse cannot be reconstructed (no default constructor),
						// return null as it should not be persisted in state
						yield null;
					}
					if ("CompletableFuture".equals(type)) {
						yield reconstructCompletableFuture(valueNode, objectMapper, typeMapper);
					}
					
					// Use unified deserialization strategy for all registered types
					var ref = typeMapper.getReference(type)
						.orElseThrow(() -> new IllegalStateException("Type not found: " + type));
					ObjectNode copy = valueNode.deepCopy();
					copy.remove(TYPE_PROPERTY);
					copy.remove("@typeHint");
					
					// Get Class from TypeReference using ObjectMapper's TypeFactory
					Class<?> targetClass = objectMapper.getTypeFactory().constructType(ref).getRawClass();
					yield deserializeWithStrategy(copy, targetClass, objectMapper, typeMapper);
				}
				if (valueNode.has("@class")) {
					String className = valueNode.get("@class").asText();
					if (!(typeHint != null && className.startsWith("java.util."))) {
					ObjectNode copy = valueNode.deepCopy();
					copy.remove("@class");
					copy.remove("@typeHint");
					try {
						Class<?> clazz = Class.forName(className);
						// Use unified deserialization strategy
						yield deserializeWithStrategy(copy, clazz, objectMapper, typeMapper);
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
						// Use unified deserialization strategy
						yield deserializeWithStrategy(copy, clazz, objectMapper, typeMapper);
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
			for (int i = 0; i < length; i++) {
				JsonNode elementNode = payload.get(i);
				Object element;
				
				// For object nodes, use unified deserialization strategy
				if (elementNode.isObject()) {
					element = deserializeWithStrategy((ObjectNode) elementNode, componentType, objectMapper, typeMapper);
				}
				else {
					// For non-object nodes, use valueFromNode
					element = valueFromNode(elementNode, objectMapper, typeMapper);
				}
				
				if (element == null && !componentType.isPrimitive()) {
					Array.set(typedArray, i, null);
					continue;
				}
				if (element != null && !componentType.isInstance(element)) {
					// Type mismatch, fall back to generic Object array
					ObjectMapper mapperNoTyping = objectMapper.copy();
					mapperNoTyping.setDefaultTyping(null);
					mapperNoTyping.deactivateDefaultTyping();
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
			Throwable throwable = createThrowable(exceptionClass, message);
			return com.alibaba.cloud.ai.graph.GraphResponse.error(throwable, metadata);
		}
		else if ("done".equals(status) || "completed".equals(status)) {
			// For both "done" and "completed", reconstruct as a done GraphResponse
			// This provides equivalent state: isDone()=true, resultValue available
			return com.alibaba.cloud.ai.graph.GraphResponse.done(result, metadata);
		}
		else {
			// For pending status, create a pending GraphResponse with incomplete CompletableFuture
			java.util.concurrent.CompletableFuture<Object> pendingFuture = new java.util.concurrent.CompletableFuture<>();
			return com.alibaba.cloud.ai.graph.GraphResponse.of(pendingFuture, metadata);
		}
	}
	
	/**
	 * Reconstruct CompletableFuture from snapshot map.
	 */
	private static Object reconstructCompletableFuture(JsonNode valueNode, ObjectMapper objectMapper,
			TypeMapper typeMapper) throws IOException {
		String status = valueNode.has("status") ? valueNode.get("status").asText() : "pending";
		// Check if error field exists and is an object (error map) - this indicates a failed future
		boolean hasErrorMap = valueNode.has("error") && valueNode.get("error").isObject();

		java.util.concurrent.CompletableFuture<Object> future = new java.util.concurrent.CompletableFuture<>();

		if (hasErrorMap || "failed".equals(status)) {
			// Extract error details from error map
			String exceptionClass = "java.lang.RuntimeException";
			String message = "Unknown error";

			if (hasErrorMap) {
				JsonNode errorMap = valueNode.get("error");
				if (errorMap.has("class")) {
					exceptionClass = errorMap.get("class").asText();
				}
				if (errorMap.has("message")) {
					message = errorMap.get("message").asText();
				}
			}

			Throwable throwable = createThrowable(exceptionClass, message);
			future.completeExceptionally(throwable);
		}
		else if ("completed".equals(status)) {
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
	 * Create a Throwable instance from class name and message.
	 * Falls back to RuntimeException if the class cannot be instantiated.
	 */
	private static Throwable createThrowable(String exceptionClass, String message) {
		try {
			Class<?> exClass = Class.forName(exceptionClass);
			if (Throwable.class.isAssignableFrom(exClass)) {
				// Try to create exception with String constructor
				return (Throwable) exClass.getConstructor(String.class).newInstance(message);
			}
		}
		catch (Exception e) {
			// Ignore and fall through to default
		}
		return new RuntimeException(message + " (original: " + exceptionClass + ")");
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
