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

import static java.util.Optional.ofNullable;

public interface HasMetadata<B extends HasMetadata.Builder<B>> {

	/**
	 * return metadata value for key
	 * @param key given metadata key
	 * @return metadata value for key if any
	 */
	Optional<Object> metadata(String key);

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

	class Builder<B extends Builder<B>> {

		private Map<String, Object> metadata;

		public Map<String, Object> metadata() {
			return ofNullable(metadata).map(Map::copyOf).orElseGet(Map::of);
		}

		protected Builder() {
		}

		protected Builder(Map<String, Object> metadata) {
			this.metadata = metadata;
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
