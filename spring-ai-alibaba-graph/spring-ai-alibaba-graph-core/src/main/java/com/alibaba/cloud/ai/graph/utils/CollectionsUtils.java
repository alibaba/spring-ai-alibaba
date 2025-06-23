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
package com.alibaba.cloud.ai.graph.utils;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Utility class for creating collections.
 */
public final class CollectionsUtils {

	/**
	 * Returns the last value in the list, if present.
	 * @return an Optional containing the last value if present, otherwise an empty
	 * Optional
	 */
	public static <T> Optional<T> last(List<T> values) {
		return (values == null || values.isEmpty()) ? Optional.empty() : Optional.of(values.get(values.size() - 1));
	}

	/**
	 * Returns the value at the specified position from the end of the list, if present.
	 * @param n the position from the end of the list
	 * @return an Optional containing the value at the specified position if present,
	 * otherwise an empty Optional
	 */
	public static <T> Optional<T> lastMinus(List<T> values, int n) {
		if (n < 0 || values == null || values.isEmpty()) {
			return Optional.empty();
		}
		var index = values.size() - n - 1;
		return (index < 0) ? Optional.empty() : Optional.of(values.get(index));
	}

	/**
	 * Merges two maps into a new map, doesn't accept duplicates.
	 * @param map1 the first map
	 * @param map2 the second map
	 * @param <K> the type of the keys in the maps
	 * @param <V> the type of the values in the maps
	 * @return a new map containing all entries from both maps, with collisions resolved
	 * by the merge function
	 * @throws NullPointerException if map1 or map2 is null
	 */
	public static <K, V> Map<K, V> mergeMap(Map<K, V> map1, Map<K, V> map2) {
		var map1Entries = Objects.requireNonNull(map1, "map1 cannot be null").entrySet().stream();
		var map2Entries = Objects.requireNonNull(map2, "map2 cannot be null").entrySet().stream();

		return Stream.concat(map1Entries, map2Entries)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Merges two maps into a new map, resolving key collisions using the specified merge
	 * function.
	 * @param map1 the first map
	 * @param map2 the second map
	 * @param mergeFunction the function used to resolve collisions between values
	 * associated with the same key
	 * @param <K> the type of the keys in the maps
	 * @param <V> the type of the values in the maps
	 * @return a new map containing all entries from both maps, with collisions resolved
	 * by the merge function
	 * @throws NullPointerException if map1, map2, or mergeFunction is null
	 */
	public static <K, V> Map<K, V> mergeMap(Map<K, V> map1, Map<K, V> map2, BinaryOperator<V> mergeFunction) {
		var map1Entries = Objects.requireNonNull(map1, "map1 cannot be null").entrySet().stream();
		var map2Entries = Objects.requireNonNull(map2, "map2 cannot be null").entrySet().stream();
		Objects.requireNonNull(mergeFunction, "mergeFunction cannot be null");

		return Stream.concat(map1Entries, map2Entries)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction));
	}

	public static <K, V> Map.Entry<K, V> entryOf(K key, V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}

	@Deprecated
	public static <T> List<T> listOf(Class<T> clazz) {
		return Collections.emptyList();
	}

	/**
	 * Creates a list containing the provided elements. It allows null values.
	 * @param objects the elements to be included in the list
	 * @param <T> the type of the elements
	 * @return a list containing the provided elements
	 */
	@SafeVarargs
	public static <T> List<T> listOf(T... objects) {
		if (objects == null) {
			return Collections.emptyList();
		}
		if (objects.length == 0) {
			return Collections.emptyList();
		}
		if (objects.length == 1) {
			return Collections.singletonList(objects[0]);
		}
		return Collections.unmodifiableList(Arrays.asList(objects));
	}

	/**
	 * Creates an empty map. It allows null values.
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 * @return an empty map
	 */
	public static <K, V> Map<K, V> mapOf() {
		return emptyMap();
	}

	/**
	 * Creates a map containing a single key-value pair. It allows null values.
	 * @param k1 the key
	 * @param v1 the value
	 * @param <K> the type of the key
	 * @param <V> the type of the value
	 * @return an unmodifiable map containing the provided key-value pair
	 */
	public static <K, V> Map<K, V> mapOf(K k1, V v1) {
		return Collections.singletonMap(k1, v1);
	}

	/**
	 * Creates a map containing two key-value pairs. It allows null values.
	 * @param k1 the first key
	 * @param v1 the first value
	 * @param k2 the second key
	 * @param v2 the second value
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 * @return an unmodifiable map containing the provided key-value pairs
	 */
	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
		Map<K, V> result = new HashMap<K, V>();
		result.put(k1, v1);
		result.put(k2, v2);
		return unmodifiableMap(result);
	}

	/**
	 * Creates a map containing three key-value pairs. It allows null values.
	 * @param k1 the first key
	 * @param v1 the first value
	 * @param k2 the second key
	 * @param v2 the second value
	 * @param k3 the third key
	 * @param v3 the third value
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 * @return an unmodifiable map containing the provided key-value pairs
	 */
	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
		Map<K, V> result = new HashMap<K, V>();
		result.put(k1, v1);
		result.put(k2, v2);
		result.put(k3, v3);
		return unmodifiableMap(result);
	}

	/**
	 * Creates a map containing three key-value pairs. It allows null values.
	 * @param k1 the first key
	 * @param v1 the first value
	 * @param k2 the second key
	 * @param v2 the second value
	 * @param k3 the third key
	 * @param v3 the third value
	 * @param k4 the fourth key
	 * @param v4 the fourth value@
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 * @return an unmodifiable map containing the provided key-value pairs
	 */
	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		Map<K, V> result = new HashMap<K, V>();
		result.put(k1, v1);
		result.put(k2, v2);
		result.put(k3, v3);
		result.put(k4, v4);
		return unmodifiableMap(result);
	}

	/**
	 * Creates a map containing three key-value pairs. It allows null values.
	 * @param k1 the first key
	 * @param v1 the first value
	 * @param k2 the second key
	 * @param v2 the second value
	 * @param k3 the third key
	 * @param v3 the third value
	 * @param k4 the fourth key
	 * @param v4 the fourth value@
	 * @param k5 the fifth key
	 * @param v5 the fifth value@
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 * @return an unmodifiable map containing the provided key-value pairs
	 */
	public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		Map<K, V> result = new HashMap<K, V>();
		result.put(k1, v1);
		result.put(k2, v2);
		result.put(k3, v3);
		result.put(k4, v4);
		result.put(k5, v5);
		return unmodifiableMap(result);
	}

}
