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
package com.alibaba.cloud.ai.dashscope.embedding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.*;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.lang.Nullable;
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

	private static final Logger logger = LoggerFactory.getLogger(DashScopeEmbeddingModel.class);

	private static final EmbeddingModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultEmbeddingModelObservationConvention();

	private final DashScopeEmbeddingOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final DashScopeApi dashScopeApi;

	private final MetadataMode metadataMode;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	/**
	 * Conventions to use for generating observations.
	 */
	private EmbeddingModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

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
			DashScopeEmbeddingOptions dashScopeEmbeddingOptions, RetryTemplate retryTemplate) {
		this(dashScopeApi, metadataMode, dashScopeEmbeddingOptions, retryTemplate, ObservationRegistry.NOOP);
	}

	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi, MetadataMode metadataMode,
			DashScopeEmbeddingOptions options, RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
		Assert.notNull(dashScopeApi, "DashScopeApi must not be null");
		Assert.notNull(metadataMode, "metadataMode must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		Assert.notNull(observationRegistry, "observationRegistry must not be null");

		this.dashScopeApi = dashScopeApi;
		this.metadataMode = metadataMode;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		DashScopeEmbeddingOptions requestOptions = mergeOptions(request.getOptions(), this.defaultOptions);
		DashScopeApi.EmbeddingRequest apiRequest = createRequest(request, requestOptions);

		var observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(request)
			.provider(DashScopeApiConstants.PROVIDER_NAME)
			.requestOptions(requestOptions)
			.build();

		return EmbeddingModelObservationDocumentation.EMBEDDING_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				DashScopeApi.EmbeddingList apiEmbeddingResponse = this.retryTemplate.execute(ctx -> {
					DashScopeApi.EmbeddingList embeddingResponse = null;
					try {
						embeddingResponse = this.dashScopeApi.embeddings(apiRequest).getBody();
					}
					catch (Exception e) {
						logger.error("Error embedding request: {}", request.getInstructions(), e);
						throw e;
					}
					return embeddingResponse;
				});

				if (apiEmbeddingResponse == null) {
					logger.warn("No embeddings returned for request: {}", request);
					return new EmbeddingResponse(List.of());
				}

				if (apiEmbeddingResponse.message() != null) {
					logger.error("Error message returned for request: {}", apiEmbeddingResponse.message());
					throw new RuntimeException("Embedding failed: error code:" + apiEmbeddingResponse.code()
							+ ", message:" + apiEmbeddingResponse.message());
				}

				var metadata = generateResponseMetadata(apiRequest.model(), apiEmbeddingResponse.usage());
				List<Embedding> embeddings = apiEmbeddingResponse.output()
					.embeddings()
					.stream()
					.map(e -> new Embedding(e.embedding(), e.textIndex()))
					.toList();

				EmbeddingResponse embeddingResponse = new EmbeddingResponse(embeddings, metadata);

				observationContext.setResponse(embeddingResponse);

				return embeddingResponse;
			});
	}

	private DashScopeApi.EmbeddingRequest createRequest(EmbeddingRequest request,
			DashScopeEmbeddingOptions requestOptions) {
		return new DashScopeApi.EmbeddingRequest(request.getInstructions(), requestOptions.getModel(),
				requestOptions.getTextType());
	}

	/**
	 * Merge runtime and default {@link EmbeddingOptions} to compute the final options to
	 * use in the request.
	 */
	private DashScopeEmbeddingOptions mergeOptions(@Nullable EmbeddingOptions runtimeOptions,
			DashScopeEmbeddingOptions defaultOptions) {
		if (runtimeOptions == null) {
			return defaultOptions;
		}

		return DashScopeEmbeddingOptions.builder()
			// Handle portable embedding options
			.withModel(ModelOptionsUtils.mergeOption(runtimeOptions.getModel(), defaultOptions.getModel()))
			.withDimensions(
					ModelOptionsUtils.mergeOption(runtimeOptions.getDimensions(), defaultOptions.getDimensions()))
			// Handle DashScope specific embedding options
			.withTextType(defaultOptions.getTextType())
			.build();
	}

	private EmbeddingResponseMetadata generateResponseMetadata(String model, DashScopeApi.EmbeddingUsage usage) {
		Map<String, Object> map = new HashMap<>();
		map.put("model", model);
		map.put("total-tokens", usage.totalTokens());

		return new EmbeddingResponseMetadata(model, usage, map);
	}

	/**
	 * Use the provided convention for reporting observation data
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(EmbeddingModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

}
