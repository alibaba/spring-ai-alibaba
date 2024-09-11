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

package com.alibaba.cloud.ai.autoconfigure.dashscope;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;

import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
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
import org.springframework.context.annotation.Scope;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DASHSCOPE_API_KEY;

/**
 * @author nuocheng.lxm
 * @date 2024/8/16 11:45
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(DashScopeApi.class)
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeChatProperties.class,
		DashScopeImageProperties.class, DashScopeAudioTranscriptionProperties.class,
		DashScopeAudioSpeechProperties.class, DashScopeEmbeddingProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class DashScopeAutoConfiguration {

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public ImageSynthesis imageSynthesis() {
		return new ImageSynthesis();
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public SpeechSynthesizer speechSynthesizer() {
		return new SpeechSynthesizer();
	}

	@Bean
	@ConditionalOnMissingBean
	public Transcription transcription() {
		return new Transcription();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = DashScopeChatProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public DashScopeChatModel dashscopeChatModel(DashScopeApi dashscopeApi, DashScopeChatProperties chatProperties,
			List<FunctionCallback> toolFunctionCallbacks, FunctionCallbackContext functionCallbackContext,
			RetryTemplate retryTemplate) {

		if (!CollectionUtils.isEmpty(toolFunctionCallbacks)) {
			chatProperties.getOptions().getFunctionCallbacks().addAll(toolFunctionCallbacks);
		}

		return new DashScopeChatModel(dashscopeApi, chatProperties.getOptions(), functionCallbackContext,
				retryTemplate);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = DashScopeEmbeddingProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public DashScopeEmbeddingModel dashscopeEmbeddingModel(DashScopeApi dashscopeApi,
			DashScopeEmbeddingProperties embeddingProperties, RetryTemplate retryTemplate) {

		return new DashScopeEmbeddingModel(dashscopeApi, embeddingProperties.getMetadataMode(),
				embeddingProperties.getOptions(), retryTemplate);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = DashScopeEmbeddingProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public DashScopeApi dashscopeApi(DashScopeConnectionProperties commonProperties,
			DashScopeChatProperties chatProperties, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

		String baseUrl = chatProperties.getBaseUrl();
		String commonBaseUrl = commonProperties.getBaseUrl();
		String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : commonBaseUrl;
		Assert.hasText(resolvedBaseUrl, "DashScope base URL must be set");

		String apiKey = chatProperties.getApiKey();
		String commonApiKey = commonProperties.getApiKey();
		String resolvedApiKey = StringUtils.hasText(apiKey) ? apiKey : commonApiKey;
		Assert.hasText(resolvedApiKey, "DashScope API key must be set");

		return new DashScopeApi(resolvedBaseUrl, resolvedApiKey, restClientBuilder, webClientBuilder,
				responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = DashScopeEmbeddingProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
			matchIfMissing = true)
	public DashScopeAgentApi dashscopeAgentApi(DashScopeConnectionProperties commonProperties,
			DashScopeChatProperties chatProperties, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

		String baseUrl = chatProperties.getBaseUrl();
		String commonBaseUrl = commonProperties.getBaseUrl();
		String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : commonBaseUrl;
		Assert.hasText(resolvedBaseUrl, "DashScope base URL must be set");

		String apiKey = chatProperties.getApiKey();
		String commonApiKey = commonProperties.getApiKey();
		String resolvedApiKey = StringUtils.hasText(apiKey) ? apiKey : commonApiKey;
		Assert.hasText(resolvedApiKey, "DashScope API key must be set");

		return new DashScopeAgentApi(resolvedBaseUrl, resolvedApiKey, restClientBuilder, webClientBuilder,
				responseErrorHandler);
	}

	@Bean
	public RestClientCustomizer restClientCustomizer(DashScopeConnectionProperties commonProperties) {
		return restClientBuilder -> restClientBuilder
			.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
				.withReadTimeout(Duration.ofSeconds(commonProperties.getReadTimeout()))));
	}
	//
	// @Bean
	// @ConditionalOnMissingBean
	// @ConditionalOnProperty(prefix = OpenAiImageProperties.CONFIG_PREFIX, name =
	// "enabled", havingValue = "true",
	// matchIfMissing = true)
	// public DashScopeImageModel openAiImageModel(OpenAiConnectionProperties
	// commonProperties,
	// OpenAiImageProperties imageProperties, RestClient.Builder restClientBuilder,
	// RetryTemplate retryTemplate,
	// ResponseErrorHandler responseErrorHandler) {
	//
	// String apiKey = StringUtils.hasText(imageProperties.getApiKey()) ?
	// imageProperties.getApiKey()
	// : commonProperties.getApiKey();
	//
	// String baseUrl = StringUtils.hasText(imageProperties.getBaseUrl()) ?
	// imageProperties.getBaseUrl()
	// : commonProperties.getBaseUrl();
	//
	// Assert.hasText(apiKey,
	// "OpenAI API key must be set. Use the property: spring.ai.openai.api-key or
	// spring.ai.openai.image.api-key property.");
	// Assert.hasText(baseUrl,
	// "OpenAI base URL must be set. Use the property: spring.ai.openai.base-url or
	// spring.ai.openai.image.base-url property.");
	//
	// var openAiImageApi = new OpenAiImageApi(baseUrl, apiKey, restClientBuilder,
	// responseErrorHandler);
	//
	// return new DashScopeImageModel(openAiImageApi, imageProperties.getOptions(),
	// retryTemplate);
	// }
	//
	// @Bean
	// @ConditionalOnMissingBean
	// @ConditionalOnProperty(prefix = OpenAiAudioTranscriptionProperties.CONFIG_PREFIX,
	// name = "enabled",
	// havingValue = "true", matchIfMissing = true)
	// public DashScopeAudioTranscriptionModel
	// openAiAudioTranscriptionModel(OpenAiConnectionProperties commonProperties,
	// OpenAiAudioTranscriptionProperties transcriptionProperties, RetryTemplate
	// retryTemplate,
	// RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
	// ResponseErrorHandler responseErrorHandler) {
	//
	// String apiKey = StringUtils.hasText(transcriptionProperties.getApiKey()) ?
	// transcriptionProperties.getApiKey()
	// : commonProperties.getApiKey();
	//
	// String baseUrl = StringUtils.hasText(transcriptionProperties.getBaseUrl())
	// ? transcriptionProperties.getBaseUrl() : commonProperties.getBaseUrl();
	//
	// Assert.hasText(apiKey,
	// "OpenAI API key must be set. Use the property: spring.ai.openai.api-key or
	// spring.ai.openai.audio.transcription.api-key property.");
	// Assert.hasText(baseUrl,
	// "OpenAI base URL must be set. Use the property: spring.ai.openai.base-url or
	// spring.ai.openai.audio.transcription.base-url property.");
	//
	// var openAiAudioApi = new OpenAiAudioApi(baseUrl, apiKey, restClientBuilder,
	// webClientBuilder,
	// responseErrorHandler);
	//
	// return new OpenAiAudioTranscriptionModel(openAiAudioApi,
	// transcriptionProperties.getOptions(), retryTemplate);
	//
	// }
	//
	// @Bean
	// @ConditionalOnMissingBean
	// @ConditionalOnProperty(prefix = OpenAiAudioSpeechProperties.CONFIG_PREFIX, name =
	// "enabled", havingValue = "true",
	// matchIfMissing = true)
	// public OpenAiAudioSpeechModel openAiAudioSpeechClient(OpenAiConnectionProperties
	// commonProperties,
	// OpenAiAudioSpeechProperties speechProperties, RetryTemplate retryTemplate,
	// RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
	// ResponseErrorHandler responseErrorHandler) {
	//
	// String apiKey = StringUtils.hasText(speechProperties.getApiKey()) ?
	// speechProperties.getApiKey()
	// : commonProperties.getApiKey();
	//
	// String baseUrl = StringUtils.hasText(speechProperties.getBaseUrl()) ?
	// speechProperties.getBaseUrl()
	// : commonProperties.getBaseUrl();
	//
	// Assert.hasText(apiKey,
	// "OpenAI API key must be set. Use the property: spring.ai.openai.api-key or
	// spring.ai.openai.audio.speech.api-key property.");
	// Assert.hasText(baseUrl,
	// "OpenAI base URL must be set. Use the property: spring.ai.openai.base-url or
	// spring.ai.openai.audio.speech.base-url property.");
	//
	// var openAiAudioApi = new OpenAiAudioApi(baseUrl, apiKey, restClientBuilder,
	// webClientBuilder,
	// responseErrorHandler);
	//
	// return new OpenAiAudioSpeechModel(openAiAudioApi, speechProperties.getOptions());
	// }

	@Bean
	@ConditionalOnMissingBean
	public FunctionCallbackContext springAiFunctionManager(ApplicationContext context) {
		FunctionCallbackContext manager = new FunctionCallbackContext();
		manager.setApplicationContext(context);
		return manager;
	}

	/**
	 * Setting the API key.
	 * @param connectionProperties {@link DashScopeConnectionProperties}
	 */
	private void settingApiKey(DashScopeConnectionProperties connectionProperties) {
		String apiKey;
		try {
			// It is recommended to set the key by defining the api-key in an environment
			// variable.
			var envKey = System.getenv(DASHSCOPE_API_KEY);
			if (Objects.nonNull(envKey)) {
				Constants.apiKey = envKey;
				return;
			}
			if (Objects.nonNull(connectionProperties.getApiKey())) {
				apiKey = connectionProperties.getApiKey();
			}
			else {
				apiKey = ApiKey.getApiKey(null);
			}

			Constants.apiKey = apiKey;
		}
		catch (NoApiKeyException e) {
			throw new RuntimeException(e.getMessage());
		}

	}

}
