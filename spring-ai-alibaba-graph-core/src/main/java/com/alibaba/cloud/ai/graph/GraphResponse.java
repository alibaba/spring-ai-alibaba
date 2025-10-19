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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

}
