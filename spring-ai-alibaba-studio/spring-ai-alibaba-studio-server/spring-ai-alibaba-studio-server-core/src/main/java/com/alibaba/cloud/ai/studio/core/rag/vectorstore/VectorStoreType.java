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

package com.alibaba.cloud.ai.studio.core.rag.vectorstore;

import lombok.Getter;

/**
 * Enum representing different types of vector stores supported by the system. Currently
 * supports Elasticsearch as a vector store implementation.
 *
 * @since 1.0.0.3
 */
@Getter
public enum VectorStoreType {

	/** Elasticsearch vector store implementation */
	ELASTICSEARCH("elasticsearch"),;

	/** The string identifier for the vector store type */
	private final String type;

	VectorStoreType(String type) {
		this.type = type;
	}

	/**
	 * Converts a string type to its corresponding VectorStoreType enum value.
	 * @param type The string identifier of the vector store type
	 * @return The corresponding VectorStoreType enum value
	 * @throws IllegalArgumentException if the type is not supported
	 */
	public static VectorStoreType of(String type) {
		for (VectorStoreType vectorStoreType : VectorStoreType.values()) {
			if (vectorStoreType.getType().equals(type)) {
				return vectorStoreType;
			}
		}

		throw new IllegalArgumentException("Unknown vector store type: " + type);
	}

}
