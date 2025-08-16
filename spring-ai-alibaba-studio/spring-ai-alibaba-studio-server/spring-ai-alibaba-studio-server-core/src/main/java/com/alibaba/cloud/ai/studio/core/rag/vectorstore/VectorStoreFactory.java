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

import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory class for creating vector store services. Provides a centralized way to obtain
 * the appropriate vector store implementation based on configuration.
 *
 * @since 1.0.0.3
 */
@Component
public class VectorStoreFactory {

	/** Configuration properties for the application */
	private final StudioProperties studioProperties;

	/** Map of available vector store service implementations */
	private final Map<String, VectorStoreService> vdbServiceMap;

	public VectorStoreFactory(StudioProperties studioProperties, Map<String, VectorStoreService> vdbServiceMap) {
		this.studioProperties = studioProperties;
		this.vdbServiceMap = vdbServiceMap;
	}

	/**
	 * Retrieves the configured vector store service. Currently supports Elasticsearch
	 * implementation.
	 * @return The configured vector store service
	 * @throws IllegalArgumentException if the configured vector store type is not
	 * supported
	 */
	public VectorStoreService getVectorStoreService() {
		VectorStoreType type = VectorStoreType.of(studioProperties.getVectorStoreType());
		if (type == VectorStoreType.ELASTICSEARCH) {
			return vdbServiceMap.get("elasticSearchVectorStoreService");
		}
		throw new IllegalArgumentException("Unsupported vector store type: " + type);
	}

}
