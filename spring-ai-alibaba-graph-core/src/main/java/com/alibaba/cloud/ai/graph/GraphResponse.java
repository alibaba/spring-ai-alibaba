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
package com.alibaba.cloud.ai.graph;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Represents a graph element in the Flux.
 *
 * @param <E> the type of the data element
 */
public class GraphResponse<E> {

	final CompletableFuture<E> output;

	final Object resultValue;

	final Map<String, Object> metadata;

	public GraphResponse(CompletableFuture<E> data, Object resultValue) {
		this(data, resultValue, new HashMap<>());
	}

	public GraphResponse(CompletableFuture<E> data, Object resultValue, Map<String, Object> metadata) {
		this.output = data;
		this.resultValue = resultValue;
		this.metadata = new HashMap<>(metadata);
	}

	public static <E> GraphResponse<E> of(CompletableFuture<E> data) {
		return new GraphResponse<>(data, null);
	}

	public static <E> GraphResponse<E> of(CompletableFuture<E> data, Map<String, Object> metadata) {
		return new GraphResponse<>(data, null, metadata);
	}

	public static <E> GraphResponse<E> of(E data) {
		return new GraphResponse<>(completedFuture(data), null);
	}

	public static <E> GraphResponse<E> of(E data, Map<String, Object> metadata) {
		return new GraphResponse<>(completedFuture(data), null, metadata);
	}

	public static <E> GraphResponse<E> done() {
		return new GraphResponse<>(null, null);
	}

	public static <E> GraphResponse<E> done(Object resultValue) {
		return new GraphResponse<>(null, resultValue);
	}

	public static <E> GraphResponse<E> done(Object resultValue, Map<String, Object> metadata) {
		return new GraphResponse<>(null, resultValue, metadata);
	}

	public static <E> GraphResponse<E> error(Throwable exception) {
		CompletableFuture<E> future = new CompletableFuture<>();
		future.completeExceptionally(exception);
		return GraphResponse.of(future);
	}

	public static <E> GraphResponse<E> error(Throwable exception, Map<String, Object> metadata) {
		CompletableFuture<E> future = new CompletableFuture<>();
		future.completeExceptionally(exception);
		return GraphResponse.of(future, metadata);
	}

	public CompletableFuture<E> getOutput() {
		return output;
	}

	public Optional<Object> resultValue() {
		return resultValue == null ? Optional.empty() : Optional.of(resultValue);
	}

	public boolean isDone() {
		return output == null;
	}

	public boolean isError() {
		return output != null && output.isCompletedExceptionally();
	}

	/**
	 * Add metadata with key and value.
	 * @param key the metadata key
	 * @param value the metadata value
	 */
	public void addMetadata(String key, Object value) {
		this.metadata.put(key, value);
	}

	/**
	 * Get metadata value by key.
	 * @param key the metadata key
	 * @return the metadata value, or null if not found
	 */
	public Object getMetadata(String key) {
		return this.metadata.get(key);
	}

	/**
	 * Get metadata value by key with type casting.
	 * @param key the metadata key
	 * @param type the expected type class
	 * @param <T> the expected type
	 * @return the metadata value cast to the expected type, or null if not found or
	 * cannot be cast
	 */
	@SuppressWarnings("unchecked")
	public <T> T getMetadata(String key, Class<T> type) {
		Object value = this.metadata.get(key);
		if (value != null && type.isAssignableFrom(value.getClass())) {
			return (T) value;
		}
		return null;
	}

	/**
	 * Get all metadata as an immutable map.
	 * @return a copy of all metadata
	 */
	public Map<String, Object> getAllMetadata() {
		return new HashMap<>(this.metadata);
	}

	/**
	 * Check if metadata contains the specified key.
	 * @param key the metadata key
	 * @return true if the key exists, false otherwise
	 */
	public boolean hasMetadata(String key) {
		return this.metadata.containsKey(key);
	}

	/**
	 * Remove metadata by key.
	 * @param key the metadata key
	 * @return the previous value associated with the key, or null if no mapping existed
	 */
	public Object removeMetadata(String key) {
		return this.metadata.remove(key);
	}

	/**
	 * Create a serializable snapshot of this response (without {@link CompletableFuture}).
	 * @return a map representation that can be safely serialized
	 */
	public Map<String, Object> toSnapshot() {
		Map<String, Object> snapshot = new LinkedHashMap<>();

		if (isError()) {
			snapshot.put("status", "error");
			snapshot.put("error", Boolean.TRUE);

			Throwable throwable = extractThrowable();
			if (throwable != null) {
				snapshot.put("exception", throwable.getClass().getName());
				snapshot.put("message", throwable.getMessage());
			}
		}
		else if (isDone()) {
			snapshot.put("status", "done");
			snapshot.put("error", Boolean.FALSE);
			resultValue().ifPresent(result -> snapshot.put("result", sanitizeValue(result)));
		}
		else if (output != null && output.isDone() && !output.isCompletedExceptionally()) {
			snapshot.put("status", "completed");
			snapshot.put("error", Boolean.FALSE);
			Object result = output.join();
			if (result != null) {
				snapshot.put("result", sanitizeValue(result));
			}
		}
		else {
			snapshot.put("status", "pending");
			snapshot.put("error", Boolean.FALSE);
		}

		if (!metadata.isEmpty()) {
			snapshot.put("metadata", sanitizeState(metadata));
		}

		return snapshot;
	}

	/**
	 * Sanitize a state update by converting {@link GraphResponse} instances into
	 * serializable snapshots.
	 * @param source the original state map
	 * @return a sanitized copy of the map
	 */
	public static Map<String, Object> sanitizeState(Map<String, Object> source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		return source.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> sanitizeValue(entry.getValue()),
					(existing, replacement) -> replacement, LinkedHashMap::new));
	}

	private static Object sanitizeValue(Object value) {
		if (value instanceof GraphResponse<?> graphResponse) {
			return graphResponse.toSnapshot();
		}
		if (value instanceof Map<?, ?> mapValue) {
			Map<Object, Object> sanitized = new LinkedHashMap<>();
			mapValue.forEach((key, val) -> sanitized.put(key, sanitizeValue(val)));
			return sanitized;
		}
		if (value instanceof List<?> listValue) {
			return listValue.stream().map(GraphResponse::sanitizeValue)
				.collect(Collectors.toCollection(ArrayList::new));
		}
		if (value != null && value.getClass().isArray()) {
			return sanitizeArray(value);
		}
		return value;
	}

	private Throwable extractThrowable() {
		if (output == null || !output.isDone()) {
			return null;
		}
		Throwable throwable = output.handle((value, ex) -> ex).join();
		if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
			return completionException.getCause();
		}
		return throwable;
	}

	private static Object sanitizeArray(Object array) {
		Class<?> componentType = array.getClass().getComponentType();
		if (componentType.isPrimitive()) {
			return array;
		}
		int length = Array.getLength(array);
		Object[] sanitizedElements = new Object[length];
		boolean preserveComponentType = true;
		for (int i = 0; i < length; i++) {
			Object element = Array.get(array, i);
			Object sanitized = sanitizeValue(element);
			sanitizedElements[i] = sanitized;
			if (sanitized != null && !componentType.isInstance(sanitized)) {
				preserveComponentType = false;
			}
		}
		if (preserveComponentType) {
			Object typedArray = Array.newInstance(componentType, length);
			for (int i = 0; i < length; i++) {
				Array.set(typedArray, i, sanitizedElements[i]);
			}
			return typedArray;
		}
		return sanitizedElements;
	}

}
