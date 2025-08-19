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

package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Configuration options for file search and retrieval.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileSearchOptions implements Serializable {

	/** List of knowledge base IDs to search in */
	@JsonProperty("kb_ids")
	private List<String> kbIds;

	/** Whether to enable search functionality */
	@JsonProperty("enable_search")
	@Builder.Default
	private Boolean enableSearch = false;

	/** Whether to enable citation in search results */
	@JsonProperty("enable_citation")
	private Boolean enableCitation;

	/** Number of top results to return, default to 3 */
	@JsonProperty("top_k")
	private Integer topK;

	/** Maximum length of retrieved content */
	@JsonProperty("retrieve_max_length")
	private Integer retrieveMaxLength;

	/** Minimum similarity threshold for search results, default to 0.2 */
	@JsonProperty("similarity_threshold")
	private Float similarityThreshold;

	/** Weight for hybrid search algorithm, default to 0.7 */
	@JsonProperty("hybrid_weight")
	private Float hybridWeight;

	/** Type of search to perform (e.g., "hybrid") */
	@JsonProperty("search_type")
	@Builder.Default
	private String searchType = "hybrid";

	/** Whether to enable reranking of results */
	@JsonProperty("enable_rerank")
	@Builder.Default
	private Boolean enableRerank = false;

	/** Provider for reranking service */
	@JsonProperty("rerank_provider")
	private String rerankProvider;

	/** Model to use for reranking */
	@JsonProperty("rerank_model")
	private String rerankModel;

}
