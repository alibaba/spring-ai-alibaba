/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.rag.bailian;

/**
 * Rerank configuration for Bailian knowledge base retrieval.
 *
 * <p>Reranking can improve the relevance of retrieved documents by re-scoring them
 * using a specialized ranking model.
 */
public class RerankConfig {

	private final String modelName;
	private final Float rerankMinScore;
	private final Integer rerankTopN;

	private RerankConfig(Builder builder) {
		this.modelName = builder.modelName;
		this.rerankMinScore = builder.rerankMinScore;
		this.rerankTopN = builder.rerankTopN;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Gets the rerank model name.
	 *
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Gets the minimum score threshold for reranking.
	 *
	 * @return the minimum score (0.01-1.00)
	 */
	public Float getRerankMinScore() {
		return rerankMinScore;
	}

	/**
	 * Gets the top N results after reranking.
	 *
	 * @return the top N value (1-20)
	 */
	public Integer getRerankTopN() {
		return rerankTopN;
	}

	/**
	 * Builder for RerankConfig.
	 */
	public static class Builder {
		private String modelName = "gte-rerank-hybrid";
		private Float rerankMinScore;
		private Integer rerankTopN;

		private Builder() {
		}

		/**
		 * Sets the rerank model name.
		 *
		 * <p>Supported models:
		 * <ul>
		 *   <li>gte-rerank-hybrid (default)
		 *   <li>gte-rerank
		 * </ul>
		 *
		 * @param modelName the model name
		 * @return this builder
		 */
		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		/**
		 * Sets the minimum score threshold.
		 *
		 * <p>Only documents with scores above this threshold will be returned.
		 * Range: [0.01-1.00]
		 *
		 * @param rerankMinScore the minimum score
		 * @return this builder
		 */
		public Builder rerankMinScore(Float rerankMinScore) {
			if (rerankMinScore != null && (rerankMinScore < 0.01f || rerankMinScore > 1.00f)) {
				throw new IllegalArgumentException("rerankMinScore must be between 0.01 and 1.00");
			}
			this.rerankMinScore = rerankMinScore;
			return this;
		}

		/**
		 * Sets the top N results after reranking.
		 *
		 * <p>Range: [1-20], default: 5
		 *
		 * @param rerankTopN the top N value
		 * @return this builder
		 */
		public Builder rerankTopN(Integer rerankTopN) {
			if (rerankTopN != null && (rerankTopN < 1 || rerankTopN > 20)) {
				throw new IllegalArgumentException("rerankTopN must be between 1 and 20");
			}
			this.rerankTopN = rerankTopN;
			return this;
		}

		/**
		 * Builds a new RerankConfig.
		 *
		 * @return a new RerankConfig instance
		 */
		public RerankConfig build() {
			return new RerankConfig(this);
		}
	}
}
