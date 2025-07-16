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
import com.alibaba.cloud.ai.dashscope.video.DashScopeVideoOptions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * DashScope Video Generation Auto Configuration.
 *
 * @author dashscope
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ DashScopeVideoApi.class, DashScopeVideoModel.class })
@EnableConfigurationProperties(DashScopeVideoProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.dashscope.video", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class DashScopeVideoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeVideoApi dashScopeVideoApi(RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		return new DashScopeVideoApi(restClientBuilder.build(), webClientBuilder.build(), responseErrorHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public DashScopeVideoOptions dashScopeVideoOptions(DashScopeVideoProperties properties) {
		return DashScopeVideoOptions.builder()
			.withModel(properties.getModel())
			.withWidth(properties.getWidth())
			.withHeight(properties.getHeight())
			.withDuration(properties.getDuration())
			.withFps(properties.getFps())
			.withSeed(properties.getSeed())
			.withNumFrames(properties.getNumFrames())
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public DashScopeVideoModel dashScopeVideoModel(DashScopeVideoApi dashScopeVideoApi,
			DashScopeVideoOptions dashScopeVideoOptions, RetryTemplate retryTemplate) {
		return new DashScopeVideoModel(dashScopeVideoApi, dashScopeVideoOptions, retryTemplate);
	}

}
