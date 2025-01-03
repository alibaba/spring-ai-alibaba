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

import java.util.Collections;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.metadata.DashScopeAiUsage;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankOptions;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.alibaba.cloud.ai.model.RerankResponseMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * Title Dashscope rerank model.<br>
 * Description Dashscope rerank model.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class DashScopeRerankModel implements RerankModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeRerankModel.class);

	/** Low-level access to the DashScope API */
	private final DashScopeApi dashscopeApi;

	/** The retry template used to retry the OpenAI API calls. */
	private final RetryTemplate retryTemplate;

	/** rerank options */
	private final DashScopeRerankOptions defaultOptions;

	public DashScopeRerankModel(DashScopeApi dashscopeApi) {
		this(dashscopeApi, DashScopeRerankOptions.builder().build());
	}

	public DashScopeRerankModel(DashScopeApi dashscopeApi, DashScopeRerankOptions defaultOptions) {
		this(dashscopeApi, defaultOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeRerankModel(DashScopeApi dashscopeApi, DashScopeRerankOptions defaultOptions,
			RetryTemplate retryTemplate) {
		Assert.notNull(dashscopeApi, "DashScopeApi must not be null");
		Assert.notNull(defaultOptions, "Options must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");

		this.dashscopeApi = dashscopeApi;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public RerankResponse call(RerankRequest request) {
		Assert.notNull(request.getQuery(), "query must not be null");
		Assert.notNull(request.getInstructions(), "documents must not be null");

		DashScopeRerankOptions requestOptions = mergeOptions(request.getOptions(), this.defaultOptions);
		DashScopeApi.RerankRequest rerankRequest = createRequest(request, requestOptions);

		ResponseEntity<DashScopeApi.RerankResponse> responseEntity = this.retryTemplate
			.execute(ctx -> this.dashscopeApi.rerankEntity(rerankRequest));

		var response = responseEntity.getBody();

		if (response == null) {
			logger.warn("No rerank returned for query: {}", request.getQuery());
			return new RerankResponse(Collections.emptyList());
		}

		List<DocumentWithScore> documentWithScores = response.output()
			.results()
			.stream()
			.map(data -> DocumentWithScore.builder()
				.withScore(data.relevanceScore())
				.withDocument(request.getInstructions().get(data.index()))
				.build())
			.toList();

		var metadata = new RerankResponseMetadata(DashScopeAiUsage.from(response.usage()));
		return new RerankResponse(documentWithScores, metadata);
	}

	private DashScopeApi.RerankRequest createRequest(RerankRequest request, DashScopeRerankOptions requestOptions) {
		List<String> docs = request.getInstructions().stream().map(Document::getText).toList();

		DashScopeApi.RerankRequestParameter parameter = new DashScopeApi.RerankRequestParameter(
				requestOptions.getTopN(), requestOptions.getReturnDocuments());
		var input = new DashScopeApi.RerankRequestInput(request.getQuery(), docs);
		return new DashScopeApi.RerankRequest(requestOptions.getModel(), input, parameter);
	}

	/**
	 * Merge runtime and default {@link RerankOptions} to compute the final options to use
	 * in the request.
	 */
	private DashScopeRerankOptions mergeOptions(@Nullable RerankOptions runtimeOptions,
			DashScopeRerankOptions defaultOptions) {
		var runtimeOptionsForProvider = ModelOptionsUtils.copyToTarget(runtimeOptions, RerankOptions.class,
				DashScopeRerankOptions.class);

		if (runtimeOptionsForProvider == null) {
			return defaultOptions;
		}

		return DashScopeRerankOptions.builder()
			.withModel(ModelOptionsUtils.mergeOption(runtimeOptionsForProvider.getModel(), defaultOptions.getModel()))
			.withTopN(ModelOptionsUtils.mergeOption(runtimeOptionsForProvider.getTopN(), defaultOptions.getTopN()))
			.withReturnDocuments(ModelOptionsUtils.mergeOption(runtimeOptionsForProvider.getReturnDocuments(),
					defaultOptions.getReturnDocuments()))
			.build();
	}

}
