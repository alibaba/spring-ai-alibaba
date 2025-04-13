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
package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.api.*;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.model.function.DefaultFunctionCallbackResolver;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @since 2024/8/16 11:45
 */
// @formatter:off
@ConditionalOnClass(DashScopeApi.class)
@AutoConfiguration(after = {
		RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class})
@ImportAutoConfiguration(classes = {
		SpringAiRetryAutoConfiguration.class,
		RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class
})
@EnableConfigurationProperties({
		DashScopeConnectionProperties.class,
		DashScopeChatProperties.class,
		DashScopeImageProperties.class,
		DashScopeSpeechSynthesisProperties.class,
		DashScopeAudioTranscriptionProperties.class,
		DashScopeEmbeddingProperties.class,
		DashScopeRerankProperties.class
})
public class DashScopeAutoConfiguration {

	@Bean
	public RestClientCustomizer restClientCustomizer(DashScopeConnectionProperties commonProperties) {

		return restClientBuilder -> restClientBuilder
				.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
						.withReadTimeout(Duration.ofSeconds(commonProperties.getReadTimeout()))));
	}

	@Bean
	@ConditionalOnMissingBean
	public FunctionCallbackResolver springAiFunctionManager(ApplicationContext context) {

		DefaultFunctionCallbackResolver manager = new DefaultFunctionCallbackResolver();
		manager.setApplicationContext(context);

		return manager;
	}

	/**
	 * Spring AI Alibaba DashScope Chat Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeChatProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	protected static class DashScopeChatConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DashScopeChatModel dashscopeChatModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeChatProperties chatProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				List<FunctionCallback> toolFunctionCallbacks,
				FunctionCallbackResolver functionCallbackResolver,
				RetryTemplate retryTemplate,
				ResponseErrorHandler responseErrorHandler,
				ObjectProvider<ObservationRegistry> observationRegistry,
				ObjectProvider<ChatModelObservationConvention> observationConvention
		) {

			var dashscopeApi = dashscopeChatApi(
					commonProperties,
					chatProperties,
					restClientBuilder,
					webClientBuilder,
					responseErrorHandler
			);

			var dashscopeModel = new DashScopeChatModel(
					dashscopeApi,
					chatProperties.getOptions(),
					functionCallbackResolver,
					toolFunctionCallbacks,
					retryTemplate,
					observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)
			);

			observationConvention.ifAvailable(dashscopeModel::setObservationConvention);

			return dashscopeModel;
		}

		private DashScopeApi dashscopeChatApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeChatProperties chatProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				ResponseErrorHandler responseErrorHandler
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(
					commonProperties,
					chatProperties,
					"chat"
			);

			return new DashScopeApi(
					resolved.baseUrl(),
					resolved.apiKey(),
					resolved.workspaceId(),
					restClientBuilder,
					webClientBuilder,
					responseErrorHandler
			);
		}

		@Bean
		public DashScopeAgentApi dashscopeAgentApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeChatProperties chatProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				ResponseErrorHandler responseErrorHandler
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, chatProperties,
					"chat");

			return new DashScopeAgentApi(resolved.baseUrl(), resolved.apiKey(), resolved.workspaceId(),
					restClientBuilder, webClientBuilder, responseErrorHandler);
		}

	}

	/**
	 * Spring AI Alibaba DashScope Image Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeImageProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	protected static class DashScopeImageConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DashScopeImageModel dashScopeImageModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeImageProperties imageProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				RetryTemplate retryTemplate,
				ResponseErrorHandler responseErrorHandler
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(
					commonProperties,
					imageProperties,
					"image"
			);

			var dashScopeImageApi = new DashScopeImageApi(
					resolved.baseUrl(),
					resolved.apiKey(),
					resolved.workspaceId(),
					restClientBuilder,
					webClientBuilder,
					responseErrorHandler
			);

			DashScopeImageModel dashScopeImageModel = new DashScopeImageModel(
					dashScopeImageApi,
					imageProperties.getOptions(),
					retryTemplate
			);

			return dashScopeImageModel;
		}
	}

	/**
	 * Spring AI Alibaba DashScope Embedding Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeEmbeddingProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	protected static class DashScopeEmbeddingConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Primary
		public DashScopeEmbeddingModel dashscopeEmbeddingModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeEmbeddingProperties embeddingProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				RetryTemplate retryTemplate,
				ResponseErrorHandler responseErrorHandler,
				ObjectProvider<ObservationRegistry> observationRegistry,
				ObjectProvider<EmbeddingModelObservationConvention> observationConvention
		) {

			var dashScopeApi = dashscopeEmbeddingApi(
					commonProperties,
					embeddingProperties,
					restClientBuilder,
					webClientBuilder,
					responseErrorHandler
			);

			var embeddingModel = new DashScopeEmbeddingModel(
					dashScopeApi,
					embeddingProperties.getMetadataMode(),
					embeddingProperties.getOptions(),
					retryTemplate,
					observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)
			);

			observationConvention.ifAvailable(embeddingModel::setObservationConvention);

			return embeddingModel;
		}

		private DashScopeApi dashscopeEmbeddingApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeEmbeddingProperties embeddingProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				ResponseErrorHandler responseErrorHandler
		) {
			ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, embeddingProperties,
					"embedding");

			return new DashScopeApi(resolved.baseUrl(), resolved.apiKey(), resolved.workspaceId(), restClientBuilder,
					webClientBuilder, responseErrorHandler);
		}

	}

	/**
	 * Spring AI Alibaba DashScope Speech Synthesis Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeSpeechSynthesisProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	protected static class DashScopeSpeechSynthesisConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeSpeechSynthesisProperties speechSynthesisProperties,
				RetryTemplate retryTemplate
		) {

			var dashScopeSpeechSynthesisApi = dashScopeSpeechSynthesisApi(
					commonProperties,
					speechSynthesisProperties
			);

			return new DashScopeSpeechSynthesisModel(
					dashScopeSpeechSynthesisApi,
					speechSynthesisProperties.getOptions(),
					retryTemplate
			);
		}

		private DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeSpeechSynthesisProperties speechSynthesisProperties
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(
					commonProperties,
					speechSynthesisProperties,
					"audio.synthesis"
			);

			return new DashScopeSpeechSynthesisApi(resolved.apiKey());
		}

	}

	/**
	 * Spring AI Alibaba DashScope Audio Transcription Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeAudioTranscriptionProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	protected static class DashScopeAudioTranscriptionConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeAudioTranscriptionProperties audioTranscriptionProperties,
				RetryTemplate retryTemplate
		) {

			var dashScopeAudioTranscriptionApi = dashScopeAudioTranscriptionApi(
					commonProperties,
					audioTranscriptionProperties
			);

			return new DashScopeAudioTranscriptionModel(
					dashScopeAudioTranscriptionApi,
					audioTranscriptionProperties.getOptions(),
					retryTemplate
			);
		}

		private DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeAudioTranscriptionProperties audioTranscriptionProperties
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties,
					audioTranscriptionProperties, "audio.transcription");

			return new DashScopeAudioTranscriptionApi(resolved.apiKey());
		}

	}

	/**
	 * Spring AI Alibaba DashScope Rerank Configuration.
	 */
	@Configuration
	@ConditionalOnProperty(
			prefix = DashScopeRerankProperties.CONFIG_PREFIX,
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	protected static class DashScopeRerankConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DashScopeRerankModel dashscopeRerankModel(
				DashScopeConnectionProperties commonProperties,
				DashScopeRerankProperties rerankProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				RetryTemplate retryTemplate,
				ResponseErrorHandler responseErrorHandler
		) {
			ResolvedConnectionProperties resolved = resolveConnectionProperties(
					commonProperties,
					rerankProperties,
					"rerank"
			);

			var dashscopeApi = new DashScopeApi(
					resolved.baseUrl(),
					resolved.apiKey(),
					resolved.workspaceId(),
					restClientBuilder,
					webClientBuilder,
					responseErrorHandler
			);

			return new DashScopeRerankModel(
					dashscopeApi,
					rerankProperties.getOptions(),
					retryTemplate
			);
		}

	}

}
// @formatter:on
