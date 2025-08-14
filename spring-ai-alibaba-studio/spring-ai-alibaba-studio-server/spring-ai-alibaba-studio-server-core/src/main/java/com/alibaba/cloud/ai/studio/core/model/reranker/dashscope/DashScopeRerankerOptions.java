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

package com.alibaba.cloud.ai.studio.core.model.reranker.dashscope;

import com.alibaba.cloud.ai.studio.core.model.reranker.RerankerOptions;
import lombok.Data;

/**
 * Configuration options for DashScope reranker service.
 *
 * @since 1.0.0.3
 */
@Data
public class DashScopeRerankerOptions implements RerankerOptions {

	/**
	 * Model identifier for reranking
	 */
	private String model = "gte-rerank-v2";

	/**
	 * Number of top results to return
	 */
	private Integer topN = 3;

	/**
	 * Whether to return document content with results
	 */
	private Boolean returnDocuments = false;

	@Override
	public String getModel() {
		return model;
	}

	@Override
	public Integer getTopN() {
		return topN;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final DashScopeRerankerOptions options;

		public Builder() {
			this.options = new DashScopeRerankerOptions();
		}

		public Builder model(String model) {
			this.options.setModel(model);
			return this;
		}

		public Builder topN(Integer topN) {
			this.options.setTopN(topN);
			return this;
		}

		public Builder returnDocuments(Boolean returnDocuments) {
			this.options.setReturnDocuments(returnDocuments);
			return this;
		}

		public DashScopeRerankerOptions build() {
			return this.options;
		}

	}

}
