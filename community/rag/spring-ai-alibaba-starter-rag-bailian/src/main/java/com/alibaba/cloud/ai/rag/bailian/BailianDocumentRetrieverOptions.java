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

import java.util.List;
import java.util.Map;

/**
 * Options for Bailian document retrieval.
 *
 * <p>This class provides configuration options for document retrieval using
 * Bailian knowledge base. Most retrieval parameters are configured via
 * BailianConfig, but this class provides additional options specific to
 * the retrieval operation.
 *
 */
public class BailianDocumentRetrieverOptions {

	private Integer limit;

	private Double scoreThreshold;

	private List<Map<String, String>> searchFilters;

	public static BailianDocumentRetrieverOptions.Builder builder() {
		return new BailianDocumentRetrieverOptions.Builder();
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Double getScoreThreshold() {
		return scoreThreshold;
	}

	public void setScoreThreshold(Double scoreThreshold) {
		this.scoreThreshold = scoreThreshold;
	}

	public List<Map<String, String>> getSearchFilters() {
		return searchFilters;
	}

	public void setSearchFilters(List<Map<String, String>> searchFilters) {
		this.searchFilters = searchFilters;
	}

	public static class Builder {

		protected BailianDocumentRetrieverOptions options;

		public Builder() {
			this.options = new BailianDocumentRetrieverOptions();
		}

		public BailianDocumentRetrieverOptions.Builder withLimit(Integer limit) {
			this.options.setLimit(limit);
			return this;
		}

		public BailianDocumentRetrieverOptions.Builder withScoreThreshold(Double scoreThreshold) {
			this.options.setScoreThreshold(scoreThreshold);
			return this;
		}

		public BailianDocumentRetrieverOptions.Builder withSearchFilters(List<Map<String, String>> searchFilters) {
			this.options.setSearchFilters(searchFilters);
			return this;
		}

		public BailianDocumentRetrieverOptions build() {
			return this.options;
		}
	}
}
