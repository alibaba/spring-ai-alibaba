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

package com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Configuration for knowledge base indexing. Defines the settings required for creating
 * and managing knowledge base indices.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexConfig implements Serializable {

	/**
	 * Name of the index configuration, normally it's vector store index or collection
	 * name
	 */
	private String name;

	/** Provider for the embedding service */
	@JsonProperty("embedding_provider")
	private String embeddingProvider;

	/** Model used for generating embeddings */
	@JsonProperty("embedding_model")
	private String embeddingModel;

}
