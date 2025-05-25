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

package com.alibaba.cloud.ai.dashscope.rerank;

import com.alibaba.cloud.ai.model.RerankOptions;

/**
 * Title DashScope rerank options.<br>
 * Description DashScope rerank options.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class DashScopeRerankOptions implements RerankOptions {

	/**
	 * ID of the model to use.
	 */
	private String model = "gte-rerank";

	/**
	 * return top n best relevant docs for query
	 */
	private Integer topN = 3;

	/**
	 * if need to return original document
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

	public Boolean getReturnDocuments() {
		return returnDocuments;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setTopN(Integer topN) {
		this.topN = topN;
	}

	public void setReturnDocuments(Boolean returnDocuments) {
		this.returnDocuments = returnDocuments;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final DashScopeRerankOptions options;

		public Builder() {
			this.options = new DashScopeRerankOptions();
		}

		public Builder withModel(String model) {
			this.options.setModel(model);
			return this;
		}

		public Builder withTopN(Integer topN) {
			this.options.setTopN(topN);
			return this;
		}

		public Builder withReturnDocuments(Boolean returnDocuments) {
			this.options.setReturnDocuments(returnDocuments);
			return this;
		}

		public DashScopeRerankOptions build() {
			return this.options;
		}

	}

}
