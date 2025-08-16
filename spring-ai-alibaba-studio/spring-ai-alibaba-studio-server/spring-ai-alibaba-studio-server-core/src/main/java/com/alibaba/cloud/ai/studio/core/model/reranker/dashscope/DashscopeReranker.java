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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Implementation of DocumentRanker using DashScope's reranking service. Reranks documents
 * based on their relevance to a given query.
 *
 * @since 1.0.0.3
 */

public class DashscopeReranker implements DocumentPostProcessor {

	/** Low-level access to the DashScope API */
	private final DashScopeApi dashscopeApi;

	/** Retry template for handling API call retries */
	private final RetryTemplate retryTemplate;

	/** Configuration options for reranking */
	private final DashScopeRerankerOptions options;

	/**
	 * Creates a new DashscopeReranker instance.
	 * @param dashscopeApi API client for DashScope
	 * @param retryTemplate Template for handling retries
	 * @param options Reranking configuration options
	 */
	public DashscopeReranker(DashScopeApi dashscopeApi, RetryTemplate retryTemplate, DashScopeRerankerOptions options) {
		Assert.notNull(dashscopeApi, "dashscopeApi must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		Assert.notNull(options, "options must not be null");

		this.dashscopeApi = dashscopeApi;
		this.retryTemplate = retryTemplate;
		this.options = options;
	}

	private DashscopeReranker(Builder builder) {
		dashscopeApi = builder.dashscopeApi;
		retryTemplate = builder.retryTemplate;
		options = builder.options;
	}

	/**
	 * Creates a new builder instance.
	 * @return A new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Reranks the given documents based on their relevance to the query.
	 * @param query The search query
	 * @param documents List of documents to rerank
	 * @return Reranked list of documents with relevance scores
	 */
	@NotNull
	@Override
	public List<Document> process(@NotNull Query query, @NotNull List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			return documents;
		}

		Assert.notNull(query, "query must not be null");
		Assert.notNull(documents, "documents must not be null");

		DashScopeApi.RerankRequest rerankRequest = createRequest(query, documents, options);

		ResponseEntity<DashScopeApi.RerankResponse> responseEntity = this.retryTemplate
			.execute(ctx -> this.dashscopeApi.rerankEntity(rerankRequest));

		var response = responseEntity.getBody();
		if (response == null) {
			throw new RuntimeException("rerank response is null.");
		}

		return response.output().results().stream().map(data -> {
			var doc = documents.get(data.index());
			return Document.builder()
				.score(data.relevanceScore())
				.id(doc.getId())
				.text(doc.getText())
				.metadata(doc.getMetadata())
				.media(doc.getMedia())
				.build();
		}).toList();
	}

	/**
	 * Creates a rerank request for the DashScope API.
	 * @param query The search query
	 * @param documents List of documents to rerank
	 * @param options Reranking configuration options
	 * @return Configured rerank request
	 */
	private DashScopeApi.RerankRequest createRequest(Query query, List<Document> documents,
			DashScopeRerankerOptions options) {
		List<String> docs = documents.stream().map(Document::getText).toList();

		DashScopeApi.RerankRequestParameter parameter = new DashScopeApi.RerankRequestParameter(options.getTopN(),
				options.getReturnDocuments());
		var input = new DashScopeApi.RerankRequestInput(query.text(), docs);
		return new DashScopeApi.RerankRequest(options.getModel(), input, parameter);
	}

	/**
	 * Builder class for creating DashscopeReranker instances.
	 */
	public static final class Builder {

		private DashScopeApi dashscopeApi;

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private DashScopeRerankerOptions options = DashScopeRerankerOptions.builder().build();

		private Builder() {
		}

		public Builder dashscopeApi(DashScopeApi dashscopeApi) {
			this.dashscopeApi = dashscopeApi;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public Builder options(DashScopeRerankerOptions options) {
			this.options = options;
			return this;
		}

		public DashscopeReranker build() {
			return new DashscopeReranker(this);
		}

	}

}
