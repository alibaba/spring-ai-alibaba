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
package com.alibaba.cloud.ai.graph.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for object serialization and deep copying operations.
 */
public class SerializationUtils {
	private static final Logger log = LoggerFactory.getLogger(SerializationUtils.class);

	// Jackson ObjectMapper for serialization
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private SerializationUtils() {
		// Utility class - prevent instantiation
	}

	/**
	 * Deep copy a Map, recursively handling all nested collections and Maps
	 *
	 * @param original the original Map object
	 * @return the deep copied Map object, returns null if the original object is null
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> deepCopyMap(Map<String, Object> original) {
		if (original == null) {
			return null;
		}

		// Preserve the original Map type if it's not a standard Map implementation
		// This handles cases like fastjson2's JSONObject (which extends LinkedHashMap)
		Map<String, Object> copy;
		try {
			// Try to create an instance of the same class
			var constructor = original.getClass().getDeclaredConstructor();
			constructor.setAccessible(true);  // Handle non-public constructors
			Object instance = constructor.newInstance();
			if (!(instance instanceof Map)) {
				throw new IllegalStateException("Constructor did not produce a Map instance: " + original.getClass().getName());
			}
			copy = (Map<String, Object>) instance;
			log.debug("Successfully preserved Map type: {}", original.getClass().getName());
		} catch (Exception e) {
			// If instantiation fails, fall back to HashMap
			log.debug("Could not preserve Map type {}, falling back to HashMap: {}", 
				original.getClass().getName(), e.getMessage());
			copy = new HashMap<>();
		}
		
		for (Map.Entry<String, Object> entry : original.entrySet()) {
			copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
		}
		return copy;
	}

	/**
	 * Recursively deep copy values of any type
	 *
	 * @param value the value that needs to be deep copied
	 * @return the deep copied value
	 */
	@SuppressWarnings("unchecked")
	public static Object deepCopyValue(Object value) {
		if (value == null) {
			return null;
		}

		// Handle primitive types and immutable objects
		if (value instanceof String || value instanceof Number ||
			value instanceof Boolean || value instanceof Character) {
			return value;
		}

		// Handle Map
		if (value instanceof Map) {
			return deepCopyMap((Map<String, Object>) value);
		}

		// Handle List
		if (value instanceof java.util.List) {
			java.util.List<Object> originalList = (java.util.List<Object>) value;
			java.util.List<Object> copyList = new java.util.ArrayList<>();
			for (Object item : originalList) {
				copyList.add(deepCopyValue(item));
			}
			return copyList;
		}

		// Handle Set
		if (value instanceof java.util.Set) {
			java.util.Set<Object> originalSet = (java.util.Set<Object>) value;
			java.util.Set<Object> copySet = new java.util.HashSet<>();
			for (Object item : originalSet) {
				copySet.add(deepCopyValue(item));
			}
			return copySet;
		}

		// Handle arrays
		if (value.getClass().isArray()) {
			Object[] originalArray = (Object[]) value;
			Object[] copyArray = new Object[originalArray.length];
			for (int i = 0; i < originalArray.length; i++) {
				copyArray[i] = deepCopyValue(originalArray[i]);
			}
			return copyArray;
		}

		// For other complex objects, try using Jackson serialization
		// If it fails, return the original object (shallow copy)
		try {
			String json = objectMapper.writeValueAsString(value);
			return objectMapper.readValue(json, value.getClass());
		} catch (Exception e) {
			// If serialization fails, log a warning and return the original object (shallow copy)
			log.debug("Could not deep copy object of type " +
				value.getClass().getName() + ", using shallow copy instead: " + e.getMessage());
			return value;
		}
	}
}
