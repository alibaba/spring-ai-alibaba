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

package com.alibaba.cloud.ai.model;

import java.util.List;

import com.alibaba.cloud.ai.document.DocumentWithScore;

import org.springframework.ai.model.ModelResponse;
import org.springframework.util.CollectionUtils;

/**
 * Title rerank response.<br>
 * Description rerank response.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RerankResponse implements ModelResponse<DocumentWithScore> {

	private final List<DocumentWithScore> documents;

	private final RerankResponseMetadata metadata;

	public RerankResponse(List<DocumentWithScore> documents) {
		this(documents, new RerankResponseMetadata());
	}

	public RerankResponse(List<DocumentWithScore> documents, RerankResponseMetadata metadata) {
		this.documents = documents;
		this.metadata = metadata;
	}

	@Override
	public DocumentWithScore getResult() {
		if (CollectionUtils.isEmpty(this.documents)) {
			return null;
		}

		return this.documents.get(0);
	}

	@Override
	public List<DocumentWithScore> getResults() {
		return this.documents;
	}

	@Override
	public RerankResponseMetadata getMetadata() {
		return this.metadata;
	}

}
