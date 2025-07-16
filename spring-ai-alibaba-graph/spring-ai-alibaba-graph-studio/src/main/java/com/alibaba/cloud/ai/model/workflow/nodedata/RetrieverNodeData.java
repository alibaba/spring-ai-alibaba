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
package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.List;
import java.util.Map;

public class RetrieverNodeData extends NodeData {

	public static final List<Variable> INPUT_SCHEMA = List.of(new Variable("query", VariableType.STRING.value()));

	public static final List<Variable> OUTPUT_SCHEMA = List
		.of(new Variable("documents", VariableType.ARRAY_OBJECT.value()));

	public static final RerankOptions DEFAULT_RERANK_OPTIONS = new RerankOptions();

	private List<RetrievalOptions> options;

	private RerankOptions multipleRetrievalOptions;

	public List<RetrievalOptions> getOptions() {
		return options;
	}

	public RetrieverNodeData setOptions(List<RetrievalOptions> options) {
		this.options = options;
		return this;
	}

	public RerankOptions getMultipleRetrievalOptions() {
		return multipleRetrievalOptions;
	}

	public RetrieverNodeData setMultipleRetrievalOptions(RerankOptions multipleRetrievalOptions) {
		this.multipleRetrievalOptions = multipleRetrievalOptions;
		return this;
	}

	public RetrieverNodeData() {
	}

	public RetrieverNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public enum RetrievalMode {

		DENSE("dense", "semantic_search"),

		SPARSE("sparse", "full_text_search"),

		HYBRID("hybrid", "hybrid_search");
		;

		private String value;

		private String difyValue;

		RetrievalMode(String value, String difyValue) {
			this.value = value;
			this.difyValue = difyValue;
		}

		public String value() {
			return value;
		}

		public RetrievalMode difyValueOf(String difyMode) {
			for (RetrievalMode mode : RetrievalMode.values()) {
				if (mode.difyValue.equals(difyMode)) {
					return mode;
				}
			}
			return null;
		}

	}

	public static class RetrievalOptions {

		public static final String DEFAULT_STORE_NAME = "default";

		public static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v1";

		public static final String DEFAULT_EMBEDDING_MODEL_PROVIDER = "tongyi";

		public static final String DEFAULT_RETRIEVAL_MODE = RetrievalMode.DENSE.value;

		public static final Integer DEFAULT_DENSE_TOP_K = 50;

		public static final Integer DEFAULT_SPARSE_TOP_K = 50;

		private String storeName;

		private String embeddingModelName = DEFAULT_EMBEDDING_MODEL_NAME;

		private String embeddingModelProvider = DEFAULT_EMBEDDING_MODEL_PROVIDER;

		private String retrievalMode = DEFAULT_RETRIEVAL_MODE;

		private Integer denseTopK = DEFAULT_DENSE_TOP_K;

		private Integer sparseTopK = DEFAULT_SPARSE_TOP_K;

		private RerankOptions rerankOptions;

		private Map<String, Object> extraProperties;

		public String getStoreName() {
			return storeName;
		}

		public RetrievalOptions setStoreName(String storeName) {
			this.storeName = storeName;
			return this;
		}

		public String getEmbeddingModelName() {
			return embeddingModelName;
		}

		public RetrievalOptions setEmbeddingModelName(String embeddingModelName) {
			this.embeddingModelName = embeddingModelName;
			return this;
		}

		public String getEmbeddingModelProvider() {
			return embeddingModelProvider;
		}

		public RetrievalOptions setEmbeddingModelProvider(String embeddingModelProvider) {
			this.embeddingModelProvider = embeddingModelProvider;
			return this;
		}

		public String getRetrievalMode() {
			return retrievalMode;
		}

		public RetrievalOptions setRetrievalMode(String retrievalMode) {
			this.retrievalMode = retrievalMode;
			return this;
		}

		public Integer getDenseTopK() {
			return denseTopK;
		}

		public RetrievalOptions setDenseTopK(Integer denseTopK) {
			this.denseTopK = denseTopK;
			return this;
		}

		public Integer getSparseTopK() {
			return sparseTopK;
		}

		public RetrievalOptions setSparseTopK(Integer sparseTopK) {
			this.sparseTopK = sparseTopK;
			return this;
		}

		public RerankOptions getRerankOptions() {
			return rerankOptions;
		}

		public RetrievalOptions setRerankOptions(RerankOptions rerankOptions) {
			this.rerankOptions = rerankOptions;
			return this;
		}

		public Map<String, Object> getExtraProperties() {
			return extraProperties;
		}

		public RetrievalOptions setExtraProperties(Map<String, Object> extraProperties) {
			this.extraProperties = extraProperties;
			return this;
		}

	}

	public static class RerankOptions {

		public static final String DEFAULT_RERANK_MODEL_NAME = "gte-rerank-hybrid";

		public static final String DEFAULT_RERANK_MODEL_PROVIDER = "tongyi";

		public static final Float DEFAULT_RERANK_THRESHOLD = 0.1F;

		public static final Integer DEFAULT_RERANK_TOP_K = 5;

		private Boolean enableRerank = true;

		private String rerankModelName = DEFAULT_RERANK_MODEL_NAME;

		private String rerankModelProvider = DEFAULT_RERANK_MODEL_PROVIDER;

		private Float rerankThreshold = DEFAULT_RERANK_THRESHOLD;

		private Integer rerankTopK = DEFAULT_RERANK_TOP_K;

		public Boolean getEnableRerank() {
			return enableRerank;
		}

		public RerankOptions setEnableRerank(Boolean enableRerank) {
			this.enableRerank = enableRerank;
			return this;
		}

		public String getRerankModelName() {
			return rerankModelName;
		}

		public RerankOptions setRerankModelName(String rerankModelName) {
			this.rerankModelName = rerankModelName;
			return this;
		}

		public String getRerankModelProvider() {
			return rerankModelProvider;
		}

		public RerankOptions setRerankModelProvider(String rerankModelProvider) {
			this.rerankModelProvider = rerankModelProvider;
			return this;
		}

		public Float getRerankThreshold() {
			return rerankThreshold;
		}

		public RerankOptions setRerankThreshold(Float rerankThreshold) {
			this.rerankThreshold = rerankThreshold;
			return this;
		}

		public Integer getRerankTopK() {
			return rerankTopK;
		}

		public RerankOptions setRerankTopK(Integer rerankTopK) {
			this.rerankTopK = rerankTopK;
			return this;
		}

	}

}
