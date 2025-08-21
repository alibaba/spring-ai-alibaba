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
package com.alibaba.cloud.ai.studio.core.model.llm;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.core.base.manager.ProviderManager;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelCredential;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.embedding.EmbeddingModelDimension;
import com.alibaba.cloud.ai.studio.core.model.reranker.dashscope.DashScopeRerankerOptions;
import com.alibaba.cloud.ai.studio.core.model.reranker.dashscope.DashscopeReranker;
import com.alibaba.cloud.ai.studio.core.utils.ErrorHandlerUtils;
import com.alibaba.cloud.ai.studio.core.utils.api.ApiUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.DEFAULT_DIMENSION;

/**
 * Factory class for creating various AI model instances including chat, embedding, and
 * document ranking models.
 *
 * @since 1.0.0.3
 */

@Slf4j
@Component
public class ModelFactory {

	/** Map of available AI model providers */
	@Resource
	private Map<String, ModelProvider> providerMap;

	/** Manager for handling provider configurations */
	@Resource
	private ProviderManager providerManager;

	/**
	 * Creates and returns a chat model instance for the specified provider
	 * @param provider The provider name
	 * @return ChatModel instance
	 */
	public ChatModel getChatModel(String provider) {
		ModelCredential credential = getModelCredential(provider, null);
		// TODO will adapt other provider in future, now it's only for OpenAI compatible
		// API

		OpenAiApi openAiApi = buildOpenAiApi(credential);
		return OpenAiChatModel.builder().openAiApi(openAiApi).build();
	}

	/**
	 * Creates and returns an embedding model instance with specified configuration
	 * @param metadataMode The metadata mode for the embedding model
	 * @param indexConfig The index configuration containing model details
	 * @return EmbeddingModel instance
	 */
	public EmbeddingModel getEmbeddingModel(MetadataMode metadataMode, IndexConfig indexConfig) {
		ModelCredential credential = getModelCredential(indexConfig.getEmbeddingProvider(),
				indexConfig.getEmbeddingModel());

		int dimension = EmbeddingModelDimension.getDimension(indexConfig.getEmbeddingModel(), DEFAULT_DIMENSION);

		OpenAiApi openAiApi = buildOpenAiApi(credential);
		return new OpenAiEmbeddingModel(openAiApi, metadataMode,
				OpenAiEmbeddingOptions.builder().model(indexConfig.getEmbeddingModel()).dimensions(dimension).build());
	}

	/**
	 * Creates and returns a document ranker instance for search operations
	 * @param searchOptions The search configuration options
	 * @return DocumentRanker instance
	 */
	public DashscopeReranker getDocumentRanker(FileSearchOptions searchOptions) {
		ModelCredential credential = getModelCredential(searchOptions.getRerankProvider(),
				searchOptions.getRerankModel());

		return DashscopeReranker.builder()
			.dashscopeApi(DashScopeApi.builder().apiKey(credential.getApiKey()).build())
			.options(DashScopeRerankerOptions.builder()
				.returnDocuments(false)
				.topN(searchOptions.getTopK())
				.model(searchOptions.getRerankModel())
				.build())
			.build();
	}

	/**
	 * Generates a cache key for model instances
	 * @param modelConfig The model configuration
	 * @return Cache key string
	 */
	private String getModelInstanceKey(ModelConfigInfo modelConfig) {
		return modelConfig.getProvider() + ":" + modelConfig.getModelId();
	}

	/**
	 * Retrieves model credentials for the specified provider and model
	 * @param provider The provider name
	 * @param modelId The model identifier
	 * @return ModelCredential instance
	 */
	private ModelCredential getModelCredential(String provider, String modelId) {
		ProviderConfigInfo providerDetail = providerManager.getProviderDetail(provider, false);
		ModelCredential credential = providerDetail.getCredential();

		if (StringUtils.isBlank(credential.getApiKey())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("apikey", "api key is invalid."));
		}

		return credential;
	}

	/**
	 * Builds an OpenAI API instance with the provided credentials
	 * @param credential The model credentials
	 * @return OpenAiApi instance
	 */
	private OpenAiApi buildOpenAiApi(ModelCredential credential) {
		OpenAiApi.Builder openAiApiBuilder = OpenAiApi.builder()
			.apiKey(credential.getApiKey())
			.responseErrorHandler(ErrorHandlerUtils.OPENAI_RESPONSE_ERROR_HANDLER)
			.headers(ApiUtils.getBaseHeaders());
		if (StringUtils.isNotBlank(credential.getEndpoint())) {
			String endpoint = credential.getEndpoint();

			// to remove the /v1 part as spring ai client will add it
			if (endpoint.endsWith("/v1") || endpoint.endsWith("/v1/")) {
				endpoint = endpoint.replaceAll("/v1/?$", "");
			}

			openAiApiBuilder.baseUrl(endpoint);
		}

		return openAiApiBuilder.build();
	}

}
