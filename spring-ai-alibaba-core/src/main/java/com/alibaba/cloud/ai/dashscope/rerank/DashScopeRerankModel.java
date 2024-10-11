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

package com.alibaba.cloud.ai.dashscope.rerank;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.metadata.DashScopeAiUsage;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.List;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

/**
 * Title Dashscope rerank model.<br>
 * Description Dashscope rerank model.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class DashScopeRerankModel implements RerankModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModel.class);

	/** Low-level access to the DashScope API */
	private final DashScopeApi dashscopeApi;

	/** The retry template used to retry the OpenAI API calls. */
	private final RetryTemplate retryTemplate;

	/** rerank options */
	private final DashScopeRerankOptions options;

	public DashScopeRerankModel(DashScopeApi dashscopeApi) {
		this(dashscopeApi, DashScopeRerankOptions.builder().build());
	}

	public DashScopeRerankModel(DashScopeApi dashscopeApi, DashScopeRerankOptions options) {
		this(dashscopeApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeRerankModel(DashScopeApi dashscopeApi, DashScopeRerankOptions options,
			RetryTemplate retryTemplate) {
		Assert.notNull(dashscopeApi, "DashScopeApi must not be null");
		Assert.notNull(options, "Options must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");

		this.dashscopeApi = dashscopeApi;
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public RerankResponse rerank(String query, List<Document> documents) {
		Assert.notNull(query, "query must not be null");
		Assert.notNull(documents, "Options must not be null");

		List<String> docs = documents.stream().map(Document::getContent).toList();

		DashScopeApi.RerankRequestParameter parameter = new DashScopeApi.RerankRequestParameter(options.getTopN(),
				options.getReturnDocuments());
		DashScopeApi.RerankRequestInput input = new DashScopeApi.RerankRequestInput(query, docs);
		DashScopeApi.RerankRequest request = new DashScopeApi.RerankRequest(options.getModel(), input, parameter);

		ResponseEntity<DashScopeApi.RerankResponse> responseEntity = this.retryTemplate
			.execute(ctx -> this.dashscopeApi.rerankEntity(request));

		var response = responseEntity.getBody();

		if (response == null) {
			logger.warn("No rerank returned for query: {}", query);
			return RerankResponse.builder().build();
		}

		List<DocumentWithScore> documentWithScores = response.output()
			.results()
			.stream()
			.map(data -> DocumentWithScore.builder()
				.withScore(data.relevanceScore())
				.withDocument(documents.get(data.index()))
				.build())
			.toList();

		return RerankResponse.builder()
			.withUsage(DashScopeAiUsage.from(response.usage()))
			.withDocuments(documentWithScores)
			.build();
	}

}
