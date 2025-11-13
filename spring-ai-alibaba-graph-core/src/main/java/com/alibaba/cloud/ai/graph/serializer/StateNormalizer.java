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

package com.alibaba.cloud.ai.graph.serializer;

import com.alibaba.cloud.ai.graph.GraphResponse;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * State normalization utility for serialization.
 * <p>
 * Converts non-serializable objects (GraphResponse, CompletableFuture, nested containers)
 * into serializable structures (Map, List, arrays) before serialization.
 * <p>
 * This utility is used exclusively by serializers to prepare state data for persistence.
 * It does not affect runtime object semantics - objects remain in their original types
 * during execution and are only normalized when crossing the serialization boundary.
 *
 * @author disaster
 * @since 1.0.0
 */
public final class StateNormalizer {

	private StateNormalizer() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Normalize entire state map before serialization.
	 * @param state the state map to normalize
	 * @return normalized state map with all values converted to serializable forms
	 */
	public static Map<String, Object> normalize(Map<String, Object> state) {
		if (state == null || state.isEmpty()) {
			return state;
		}
		Map<String, Object> result = new LinkedHashMap<>(state.size());
		state.forEach((key, value) -> result.put(key, normalizeValue(value)));
		return result;
	}

	/**
	 * Normalize a single value for serialization.
	 * @param value the value to normalize
	 * @return normalized value in serializable form
	 */
	public static Object normalizeValue(Object value) {
		if (value == null) {
			return null;
		}

		// 1. GraphResponse → snapshot map
		if (value instanceof GraphResponse) {
			return normalizeGraphResponse((GraphResponse<?>) value);
		}

		// 2. CompletableFuture → snapshot map
		if (value instanceof CompletableFuture) {
			return normalizeCompletableFuture((CompletableFuture<?>) value);
		}

		// 3. Map → recursive normalization
		if (value instanceof Map) {
			return normalizeMap((Map<?, ?>) value);
		}

		// 4. Collection → recursive normalization
		if (value instanceof Collection) {
			return normalizeCollection((Collection<?>) value);
		}

		// 5. Array → recursive normalization
		if (value.getClass().isArray()) {
			return normalizeArray(value);
		}

		// 6. Optional → unwrap and normalize
		if (value instanceof Optional) {
			return ((Optional<?>) value).map(StateNormalizer::normalizeValue).orElse(null);
		}

		// 7. Other types → return as-is (assumed serializable)
		return value;
	}

	/**
	 * Convert GraphResponse into serializable snapshot map.
	 */
	private static Map<String, Object> normalizeGraphResponse(GraphResponse<?> response) {
		Map<String, Object> snapshot = new LinkedHashMap<>();

		if (response.isError()) {
			snapshot.put("status", "error");
			snapshot.put("error", Boolean.TRUE);
			CompletableFuture<?> output = response.getOutput();
			if (output != null && output.isDone()) {
				Throwable throwable = extractThrowable(output);
				if (throwable != null) {
					snapshot.put("exception", throwable.getClass().getName());
					snapshot.put("message", throwable.getMessage());
				}
			}
		}
		else if (response.isDone()) {
			snapshot.put("status", "done");
			snapshot.put("error", Boolean.FALSE);
			response.resultValue().ifPresent(result -> snapshot.put("result", normalizeValue(result)));
		}
		else {
			CompletableFuture<?> output = response.getOutput();
			if (output != null && output.isDone() && !output.isCompletedExceptionally()) {
				snapshot.put("status", "completed");
				snapshot.put("error", Boolean.FALSE);
				Object result = output.join();
				if (result != null) {
					snapshot.put("result", normalizeValue(result));
				}
			}
			else {
				snapshot.put("status", "pending");
				snapshot.put("error", Boolean.FALSE);
			}
		}

		Map<String, Object> metadata = response.getAllMetadata();
		if (!metadata.isEmpty()) {
			snapshot.put("metadata", normalizeMap(metadata));
		}

		return snapshot;
	}

	/**
	 * Convert CompletableFuture into serializable snapshot map.
	 */
	private static Map<String, Object> normalizeCompletableFuture(CompletableFuture<?> future) {
		Map<String, Object> snapshot = new LinkedHashMap<>();

		if (!future.isDone()) {
			snapshot.put("status", "pending");
			snapshot.put("error", Boolean.FALSE);
		}
		else if (future.isCompletedExceptionally()) {
			snapshot.put("status", "error");
			snapshot.put("error", Boolean.TRUE);
			Throwable throwable = extractThrowable(future);
			if (throwable != null) {
				snapshot.put("exception", throwable.getClass().getName());
				snapshot.put("message", throwable.getMessage());
			}
		}
		else {
			snapshot.put("status", "completed");
			snapshot.put("error", Boolean.FALSE);
			Object result = future.getNow(null);
			if (result != null) {
				snapshot.put("result", normalizeValue(result));
			}
		}

		return snapshot;
	}

	/**
	 * Normalize Map recursively.
	 */
	private static Map<String, Object> normalizeMap(Map<?, ?> map) {
		if (map == null || map.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Object> result = new LinkedHashMap<>(map.size());
		map.forEach((k, v) -> result.put(String.valueOf(k), normalizeValue(v)));
		return result;
	}

	/**
	 * Normalize Collection recursively.
	 */
	private static List<Object> normalizeCollection(Collection<?> collection) {
		if (collection == null || collection.isEmpty()) {
			return Collections.emptyList();
		}
		return collection.stream().map(StateNormalizer::normalizeValue).collect(Collectors.toList());
	}

	/**
	 * Normalize array recursively.
	 */
	private static Object normalizeArray(Object array) {
		Class<?> componentType = array.getClass().getComponentType();

		// Primitive arrays → return as-is (already serializable)
		if (componentType.isPrimitive()) {
			return array;
		}

		int length = Array.getLength(array);
		Object[] result = new Object[length];
		boolean typePreserved = true;

		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			Object normalized = normalizeValue(element);
			result[i] = normalized;

			if (normalized != null && !componentType.isInstance(normalized)) {
				typePreserved = false;
			}
		}

		// If all elements are compatible with component type, preserve array type
		if (typePreserved) {
			try {
				Object typedArray = Array.newInstance(componentType, length);
				System.arraycopy(result, 0, typedArray, 0, length);
				return typedArray;
			}
			catch (Exception e) {
				// Fall back to Object[] if type preservation fails
			}
		}

		return result;
	}

	/**
	 * Extract throwable from CompletableFuture.
	 */
	private static Throwable extractThrowable(CompletableFuture<?> future) {
		if (future == null || !future.isDone()) {
			return null;
		}
		Throwable throwable = future.handle((value, ex) -> ex).join();
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}

}

