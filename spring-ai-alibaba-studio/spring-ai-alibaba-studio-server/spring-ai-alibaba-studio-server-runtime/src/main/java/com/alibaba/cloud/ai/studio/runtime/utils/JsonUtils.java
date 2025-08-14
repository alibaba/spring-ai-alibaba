/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Utility class for JSON operations. Provides methods for JSON serialization and
 * deserialization.
 *
 * @since 1.0.0.3
 */
public class JsonUtils {

	/**
	 * Jackson ObjectMapper instance configured with common settings -- GETTER -- Returns
	 * the configured ObjectMapper instance
	 *
	 */
	@Getter
	private static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		objectMapper.findAndRegisterModules();
	}

	/**
	 * Converts an object to JSON string
	 */
	public static String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts JSON string to specified class object
	 */
	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts JSON string to JsonNode
	 */
	public static JsonNode fromJson(String json) {
		try {
			return objectMapper.readTree(json);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts JSON input stream to JsonNode
	 */
	public static JsonNode fromJson(InputStream json) {
		try {
			return objectMapper.readTree(json);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts JSON string to List of specified class objects
	 */
	public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
		try {
			JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
			return objectMapper.readValue(json, javaType);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts JSON string to Map
	 */
	public static <K, V> Map<K, V> fromJsonToMap(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads JSON from file and converts to specified class object
	 */
	public static <T> T fromJsonFile(String filePath, Class<T> clazz) {
		try {
			return objectMapper.readValue(filePath, clazz);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts Map to specified class object
	 */
	public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
		return objectMapper.convertValue(map, clazz);
	}

	/**
	 * Converts object to Map
	 */
	public static Map<String, Object> fromObjectToMap(Object obj) {
		return objectMapper.convertValue(obj, new TypeReference<>() {
		});
	}

	/**
	 * Validates if string is valid JSON
	 */
	public static boolean isValidJson(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			return node != null;
		}
		catch (JsonProcessingException e) {
			return false;
		}
	}

	/**
	 * Checks if string is JSON array
	 */
	public static boolean isJsonArray(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			return node.isArray();
		}
		catch (JsonProcessingException e) {
			return false;
		}
	}

}
