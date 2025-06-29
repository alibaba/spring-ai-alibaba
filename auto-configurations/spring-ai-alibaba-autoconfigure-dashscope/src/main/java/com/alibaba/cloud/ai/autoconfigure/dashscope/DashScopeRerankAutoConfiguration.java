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
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
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
@ConditionalOnProperty(prefix = DashScopeRerankProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeRerankProperties.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
public class DashScopeRerankAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeRerankModel dashscopeRerankModel(DashScopeConnectionProperties commonProperties,
			DashScopeRerankProperties rerankProperties, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, RetryTemplate retryTemplate,
			ResponseErrorHandler responseErrorHandler) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, rerankProperties,
				"rerank");

		var dashScopeApi = DashScopeApi.builder()
			.apiKey(resolved.apiKey())
			.headers(resolved.headers())
			.baseUrl(resolved.baseUrl())
			.webClientBuilder(webClientBuilder)
			.workSpaceId(resolved.workspaceId())
			.restClientBuilder(restClientBuilder)
			.responseErrorHandler(responseErrorHandler)
			.build();

		return new DashScopeRerankModel(dashScopeApi, rerankProperties.getOptions(), retryTemplate);
	}

}
