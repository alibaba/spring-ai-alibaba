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
 * @date 2024/7/31 10:57
 */
public class DashScopeEmbeddingModel extends AbstractEmbeddingModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeEmbeddingModel.class);

	private final DashScopeEmbeddingOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final DashScopeApi dashScopeApi;

	private final MetadataMode metadataMode;

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
		this(dashScopeApi, MetadataMode.EMBED);
	}

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode) {
		this(dashScopeApi, metadataMode,
				DashScopeEmbeddingOptions.builder()
					.withModel(DashScopeApi.DEFAULT_EMBEDDING_MODEL)
					.withTextType(DashScopeApi.DEFAULT_EMBEDDING_TEXT_TYPE)
					.build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode,
			DashScopeEmbeddingOptions dashScopeEmbeddingOptions) {
		this(dashScopeApi, metadataMode, dashScopeEmbeddingOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode,
			DashScopeEmbeddingOptions options, RetryTemplate retryTemplate) {
		Assert.notNull(dashScopeApi, "DashScopeApi must not be null");
		Assert.notNull(metadataMode, "metadataMode must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");

		this.dashScopeApi = dashScopeApi;
		this.metadataMode = metadataMode;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {

		return this.retryTemplate.execute(ctx -> {
			DashScopeApi.EmbeddingRequest apiRequest = (this.defaultOptions != null)
					? new DashScopeApi.EmbeddingRequest(request.getInstructions(), this.defaultOptions.getModel(),
							this.defaultOptions.getTextType())
					: new DashScopeApi.EmbeddingRequest(request.getInstructions());

			if (request.getOptions() != null
					&& !EmbeddingOptionsBuilder.builder().build().equals(request.getOptions())) {
				apiRequest = ModelOptionsUtils.merge(request.getOptions(), apiRequest,
						DashScopeApi.EmbeddingRequest.class);
			}

			DashScopeApi.EmbeddingList apiEmbeddingResponse = this.dashScopeApi.embeddings(apiRequest).getBody();

			if (apiEmbeddingResponse == null) {
				logger.warn("No embeddings returned for request: {}", request);
				return new EmbeddingResponse(List.of());
			}

			if (apiEmbeddingResponse.message() != null) {
				logger.error("Error message returned for request: {}", apiEmbeddingResponse.message());
				throw new RuntimeException("Embedding failed: error code:" + apiEmbeddingResponse.code() + ", message:"
						+ apiEmbeddingResponse.message());
			}

			var metadata = generateResponseMetadata(apiRequest.model(), apiEmbeddingResponse.usage());
			List<Embedding> embeddings = apiEmbeddingResponse.output()
				.embeddings()
				.stream()
				.map(e -> new Embedding(e.embedding(), e.textIndex()))
				.toList();
			return new EmbeddingResponse(embeddings, metadata);
		});
	}

	private EmbeddingResponseMetadata generateResponseMetadata(String model, DashScopeApi.EmbeddingUsage usage) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", model);
		map.put("total-tokens", usage.totalTokens());

		return new EmbeddingResponseMetadata(model, usage, map);
	}

}
