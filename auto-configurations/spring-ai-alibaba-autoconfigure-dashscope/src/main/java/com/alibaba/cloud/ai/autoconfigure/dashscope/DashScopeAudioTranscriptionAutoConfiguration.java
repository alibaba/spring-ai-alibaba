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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.model.SpringAIAlibabaModels;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

// @formatter:off
@ConditionalOnClass(DashScopeAudioTranscriptionApi.class)
@ConditionalOnDashScopeEnabled
@AutoConfiguration(after = {
		RestClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnProperty(name = SpringAIModelProperties.AUDIO_TRANSCRIPTION_MODEL, havingValue = SpringAIAlibabaModels.DASHSCOPE,
		matchIfMissing = true)
@EnableConfigurationProperties({
		DashScopeConnectionProperties.class,
		DashScopeAudioTranscriptionProperties.class })
@ImportAutoConfiguration(classes = {
		SpringAiRetryAutoConfiguration.class,
		RestClientAutoConfiguration.class })
// @formatter:on
public class DashScopeAudioTranscriptionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel(
			DashScopeConnectionProperties commonProperties,
			DashScopeAudioTranscriptionProperties audioTranscriptionProperties,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider, ResponseErrorHandler responseErrorHandle
	// todo Instead of using retryTemplate, use webSocket
	// RetryTemplate retryTemplate
	) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties,
				audioTranscriptionProperties, "audio.transcription");

		var dashScopeAudioTranscriptionApi = DashScopeAudioTranscriptionApi.builder()
			.baseUrl(resolved.baseUrl())
			.apiKey(new SimpleApiKey(resolved.apiKey()))
			.model(audioTranscriptionProperties.getOptions().getModel())
			.workSpaceId(resolved.workspaceId())
			.restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
			.headers(resolved.headers())
			.responseErrorHandler(responseErrorHandle)
			.build();

		return new DashScopeAudioTranscriptionModel(dashScopeAudioTranscriptionApi,
				audioTranscriptionProperties.getOptions());
	}

}
