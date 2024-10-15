/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;

import java.util.Collections;
import java.util.List;

/**
 * Title rerank response.<br>
 * Description rerank response.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RerankResponse {

	private Usage usage = new EmptyUsage();

	private List<DocumentWithScore> documents = Collections.emptyList();

	public Usage getUsage() {
		return usage;
	}

	public List<DocumentWithScore> getDocuments() {
		return documents;
	}

	public static RerankResponse.Builder builder() {
		return new RerankResponse.Builder();
	}

	public static class Builder {

		private final RerankResponse rerankResponse;

		public Builder() {
			this.rerankResponse = new RerankResponse();
		}

		public RerankResponse.Builder withUsage(Usage usage) {
			this.rerankResponse.usage = usage;
			return this;
		}

		public RerankResponse.Builder withDocuments(List<DocumentWithScore> documents) {
			this.rerankResponse.documents = documents;
			return this;
		}

		public RerankResponse build() {
			return this.rerankResponse;
		}

	}

}
