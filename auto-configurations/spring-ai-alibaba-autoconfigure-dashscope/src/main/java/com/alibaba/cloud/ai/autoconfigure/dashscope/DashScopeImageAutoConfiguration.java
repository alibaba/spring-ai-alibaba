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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@AutoConfiguration(after = { RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class })
@ConditionalOnClass(DashScopeApi.class)
@ConditionalOnDashScopeEnabled
@ConditionalOnProperty(prefix = DashScopeImageProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeImageProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class DashScopeImageAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeImageModel dashScopeImageModel(DashScopeConnectionProperties commonProperties,
			DashScopeImageProperties imageProperties, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, RetryTemplate retryTemplate, ResponseErrorHandler responseErrorHandler,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<ImageModelObservationConvention> observationConvention) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, imageProperties, "image");

		var dashScopeImageApi = new DashScopeImageApi(resolved.baseUrl(), resolved.apiKey(), resolved.workspaceId(),
				restClientBuilder, webClientBuilder, responseErrorHandler);

		DashScopeImageModel dashScopeImageModel = new DashScopeImageModel(dashScopeImageApi,
				imageProperties.getOptions(), retryTemplate,
				observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		observationConvention.ifAvailable(dashScopeImageModel::setObservationConvention);

		return dashScopeImageModel;
	}

}
