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

import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelRequest;

import java.util.List;

/**
 * Title rerank request.<br>
 * Description rerank request.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RerankRequest implements ModelRequest<List<Document>> {

	private final String query;

	private final List<Document> documents;

	private final RerankOptions options;

	public RerankRequest(String query, List<Document> documents) {
		this(query, documents, null);
	}

	public RerankRequest(String query, List<Document> documents, RerankOptions options) {
		this.query = query;
		this.documents = documents;
		this.options = options;
	}

	@Override
	public List<Document> getInstructions() {
		return this.documents;
	}

	@Override
	public RerankOptions getOptions() {
		return this.options;
	}

	public String getQuery() {
		return query;
	}

}
