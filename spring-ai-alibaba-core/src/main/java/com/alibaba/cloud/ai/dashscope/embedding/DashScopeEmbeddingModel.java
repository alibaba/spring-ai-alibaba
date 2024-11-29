/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.embedding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * DashScope Embedding Model implementation.
 *
 * @author nuocheng.lxm
 * @author why_ohh
 * @author yuluo
 * @author <a href="mailto:550588941@qq.com">why_ohh</a>
 * @since 2024/7/31 10:57
 */
public class DashScopeEmbeddingModel extends AbstractEmbeddingModel {

	private final DashScopeApi dashScopeApi;

	private final RetryHandler retryHandler;

	private final MetadataMode metadataMode;

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
		this.dashScopeApi = dashScopeApi;
		this.retryHandler = new RetryHandler(3, 1000);
		this.metadataMode = MetadataMode.EMBED;
	}

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode,
			DashScopeEmbeddingOptions options, RetryTemplate retryTemplate) {
		Assert.notNull(dashScopeApi, "DashScopeApi must not be null");
		Assert.notNull(metadataMode, "MetadataMode must not be null");
		Assert.notNull(options, "DashScopeEmbeddingOptions must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");

		this.dashScopeApi = dashScopeApi;
		this.metadataMode = metadataMode;
		this.retryHandler = new RetryHandler(3, 1000);
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		return retryHandler.executeWithRetry(() -> {
			DashScopeApi.EmbeddingRequest apiRequest = new DashScopeApi.EmbeddingRequest(request.getInstructions(),
					DashScopeApi.DEFAULT_EMBEDDING_MODEL, DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE);

			DashScopeApi.EmbeddingList apiEmbeddingResponse = dashScopeApi.embeddings(apiRequest).getBody();

			if (apiEmbeddingResponse == null || apiEmbeddingResponse.message() != null) {
				throw new RuntimeException("Embedding failed: " + apiEmbeddingResponse.message());
			}

			return new EmbeddingResponse(apiEmbeddingResponse.output()
				.embeddings()
				.stream()
				.map(e -> new Embedding(e.embedding(), e.textIndex()))
				.toList(), generateResponseMetadata(apiRequest.model(), apiEmbeddingResponse.usage()));
		}, "Error embedding request");
	}

	private EmbeddingResponseMetadata generateResponseMetadata(String model, DashScopeApi.EmbeddingUsage usage) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("model", model);
		metadata.put("total-tokens", usage.totalTokens());
		return new EmbeddingResponseMetadata(model, usage, metadata);
	}

}