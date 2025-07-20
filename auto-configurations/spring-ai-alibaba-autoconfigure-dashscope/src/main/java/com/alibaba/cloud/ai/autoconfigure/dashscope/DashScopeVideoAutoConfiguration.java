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

import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi;
import com.alibaba.cloud.ai.dashscope.video.DashScopeVideoModel;
import com.alibaba.cloud.ai.model.SpringAIAlibabaModelProperties;
import com.alibaba.cloud.ai.model.SpringAIAlibabaModels;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * DashScope Video Generation Auto Configuration.
 *
 * @author dashscope
 * @author yuluo
 * @since 1.0.0.3
 */

@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class })
@ConditionalOnDashScopeEnabled
@ConditionalOnClass({ DashScopeVideoApi.class })
@ConditionalOnProperty(name = SpringAIAlibabaModelProperties.VIDEO_MODEL, havingValue = SpringAIAlibabaModels.DASHSCOPE,
		matchIfMissing = true)
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeVideoProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class, })
public class DashScopeVideoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeVideoModel dashScopeVideoModel(DashScopeConnectionProperties commonProperties,
			DashScopeVideoProperties videoProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			RetryTemplate retryTemplate, ResponseErrorHandler responseErrorHandler) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, videoProperties, "image");

		var videoApi = DashScopeVideoApi.builder()
			.apiKey(resolved.apiKey())
			.baseUrl(resolved.baseUrl())
			.restClientBuilder(restClientBuilderProvider.getIfAvailable())
			.responseErrorHandler(responseErrorHandler)
			.build();

		// todo: add observation

		return DashScopeVideoModel.builder()
			.videoApi(videoApi)
			.defaultOptions(videoProperties.getOptions())
			.retryTemplate(retryTemplate)
			.build();
	}

}
