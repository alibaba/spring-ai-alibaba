/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.ai.util.json;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.springframework.ai.util.JacksonUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utilities to perform parsing operations between JSON and Java.
 */
public final class JsonParser {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.addModules(JacksonUtils.instantiateAvailableModules())
			.build();

	private JsonParser() {
	}

	/**
	 * Returns a Jackson {@link ObjectMapper} instance tailored for JSON-parsing
	 * operations for tool calling and structured output.
	 */
	public static ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, Class<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			// Handle String type specially - if JSON is an object/array, return as-is
			if (type == String.class) {
				// Use Jackson to parse and check the actual token type
				try (com.fasterxml.jackson.core.JsonParser parser = OBJECT_MAPPER.getFactory().createParser(json)) {
					JsonToken token = parser.nextToken();
					// If it's actually a JSON object or array (not a string value), return as-is
					if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
						return type.cast(json);
					}
					// Otherwise, it's a JSON string value, parse it normally to remove quotes
				}
				return OBJECT_MAPPER.readValue(json, type);
			}

			// Handle primitive types and their wrappers specially
			Class<?> resolvedType = ClassUtils.resolvePrimitiveIfNecessary(type);

			// For primitive types and their wrappers, handle simple JSON values
			if (isPrimitiveOrWrapper(resolvedType)) {
				return OBJECT_MAPPER.readValue(json, type);
			}

			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getName()), ex);
		}
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, Type type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			// Handle String type specially - if JSON is an object/array, return as-is
			if (type == String.class) {
				// Use Jackson to parse and check the actual token type
				try (com.fasterxml.jackson.core.JsonParser parser = OBJECT_MAPPER.getFactory().createParser(json)) {
					JsonToken token = parser.nextToken();
					// If it's actually a JSON object or array (not a string value), return as-is
					if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
						@SuppressWarnings("unchecked")
						T result = (T) json;
						return result;
					}
					// Otherwise, it's a JSON string value, parse it normally to remove quotes
				}
				JavaType javaType = OBJECT_MAPPER.constructType(type);
				return OBJECT_MAPPER.readValue(json, javaType);
			}

			JavaType javaType = OBJECT_MAPPER.constructType(type);
			return OBJECT_MAPPER.readValue(json, javaType);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getTypeName()), ex);
		}
	}

	/**
	 * Converts a JSON string to a Java object.
	 */
	public static <T> T fromJson(String json, TypeReference<T> type) {
		Assert.notNull(json, "json cannot be null");
		Assert.notNull(type, "type cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, type);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getType().getTypeName()),
					ex);
		}
	}

	/**
	 * Converts a JSON string to a HashMap.
	 * @param json the JSON string to convert
	 * @return a HashMap containing the parsed JSON data
	 * @throws IllegalStateException if the JSON string cannot be parsed
	 */
	public static Map<String, Object> toMap(String json) {
		Assert.notNull(json, "json cannot be null");

		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<HashMap<String, Object>>() {
			});
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from JSON to Map failed", ex);
		}
	}

	/**
	 * Checks if a type is a primitive type or its wrapper class.
	 */
	private static boolean isPrimitiveOrWrapper(Class<?> type) {
		return type.isPrimitive()
			|| type == String.class
			|| type == Boolean.class
			|| type == Integer.class
			|| type == Long.class
			|| type == Double.class
			|| type == Float.class
			|| type == Short.class
			|| type == Byte.class
			|| type == Character.class
			|| Number.class.isAssignableFrom(type);
	}

	/**
	 * Checks if a string is a valid JSON string.
	 */
	private static boolean isValidJson(String input) {
		try {
			OBJECT_MAPPER.readTree(input);
			return true;
		}
		catch (JsonProcessingException e) {
			return false;
		}
	}

	/**
	 * Converts a Java object to a JSON string if it's not already a valid JSON string.
	 */
	public static String toJson(@Nullable Object object) {
		if (object instanceof String && isValidJson((String) object)) {
			return (String) object;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Conversion from Object to JSON failed", ex);
		}
	}

	/**
	 * Convert a Java Object to a typed Object. Based on the implementation in
	 * MethodToolCallback.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object toTypedObject(Object value, Class<?> type) {
		Assert.notNull(value, "value cannot be null");
		Assert.notNull(type, "type cannot be null");

		var javaType = ClassUtils.resolvePrimitiveIfNecessary(type);

		if (javaType == String.class) {
			return value.toString();
		}
		else if (javaType == Byte.class) {
			return Byte.parseByte(value.toString());
		}
		else if (javaType == Integer.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.intValueExact();
		}
		else if (javaType == Short.class) {
			return Short.parseShort(value.toString());
		}
		else if (javaType == Long.class) {
			BigDecimal bigDecimal = new BigDecimal(value.toString());
			return bigDecimal.longValueExact();
		}
		else if (javaType == Double.class) {
			return Double.parseDouble(value.toString());
		}
		else if (javaType == Float.class) {
			return Float.parseFloat(value.toString());
		}
		else if (javaType == Boolean.class) {
			return Boolean.parseBoolean(value.toString());
		}
		else if (javaType.isEnum()) {
			return Enum.valueOf((Class<Enum>) javaType, value.toString());
		}

		String json = JsonParser.toJson(value);
		return JsonParser.fromJson(json, javaType);
	}

}
