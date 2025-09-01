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

import com.alibaba.cloud.ai.graph.utils.TypeRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public interface HasMetadata<B extends HasMetadata.Builder<B>> {

	/**
	 * return metadata value for key
	 * @param key given metadata key
	 * @return metadata value for key if any
	 */
	Optional<Object> metadata(String key);

	/**
	 * Prefix used to indicate an interrupt node.
	 */
	String INTERRUPT_PREFIX = "__NODE_INTERRUPT__";

	/**
	 * Returns a type-safe metadata value for the given key.
	 * <p>
	 * This method retrieves the metadata object and attempts to cast it to the specified
	 * type.
	 * @param <T> the type of the metadata value
	 * @param key the metadata key
	 * @param typeRef a {@link TypeRef} representing the desired type of the value
	 * @return an {@link Optional} containing the metadata value cast to the specified
	 * type, or an empty {@link Optional} if the key is not found or the value cannot be
	 * cast.
	 */
	default <T> Optional<T> metadata(String key, TypeRef<T> typeRef) {
		return metadata(key).flatMap(typeRef::cast);
	}

	/**
	 * return metadata value for key
	 * @param key given metadata key
	 * @return metadata value for key if any
	 * @deprecated use {@link #metadata(String)} instead
	 */
	@Deprecated(forRemoval = true)
	default Optional<Object> getMetadata(String key) {
		return metadata(key);
	};

	/**
	 * Formats a node ID by prefixing it with the interrupt prefix. The formatted node ID
	 * follows the pattern "__INTERRUPT__(nodeId)".
	 * @param nodeId the node ID to format, cannot be null
	 * @return the formatted node ID string
	 * @throws NullPointerException if nodeId is null
	 */
	static String formatNodeId(String nodeId) {
		return format("%s(%s)", INTERRUPT_PREFIX, requireNonNull(nodeId, "nodeId cannot be null!"));
	}

	class Builder<B extends Builder<B>> {

		private Map<String, Object> metadata;

		public Map<String, Object> metadata() {
			return ofNullable(metadata).map(Map::copyOf).orElseGet(Map::of);
		}

		protected Builder() {
		}

		protected Builder(Map<String, Object> metadata) {
			if (metadata != null && !metadata.isEmpty()) {
				this.metadata = new HashMap<>(metadata);
			}
		}

		@SuppressWarnings("unchecked")
		public B addMetadata(String key, Object value) {
			if (metadata == null) {
				// Lazy initialization of metadata map
				metadata = new HashMap<>();
			}

			metadata.put(key, value);

			return (B) this;
		};

	}

}
