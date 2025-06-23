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
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
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
 * @author nuocheng.lxm
 * @author yuluo
 * @since 2024/8/16 11:45
 *
 * Spring AI Alibaba DashScope Chat Configuration.
 */

// @formatter:off
@ConditionalOnClass(DashScopeApi.class)
@ConditionalOnDashScopeEnabled
@ConditionalOnProperty(prefix = DashScopeChatProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfiguration(after = {
		RestClientAutoConfiguration.class,
		SpringAiRetryAutoConfiguration.class,
        ToolCallingAutoConfiguration.class})
@ImportAutoConfiguration(classes = {
		SpringAiRetryAutoConfiguration.class,
		RestClientAutoConfiguration.class,
		ToolCallingAutoConfiguration.class,
		WebClientAutoConfiguration.class
})
@EnableConfigurationProperties({
		DashScopeConnectionProperties.class,
		DashScopeChatProperties.class,
})
public class DashScopeChatAutoConfiguration {


		@Bean
		@ConditionalOnMissingBean
		public DashScopeChatModel dashscopeChatModel(
				RetryTemplate retryTemplate,
				ToolCallingManager toolCallingManager,
				DashScopeChatProperties chatProperties,
				ResponseErrorHandler responseErrorHandler,
				DashScopeConnectionProperties commonProperties,
				ObjectProvider<ObservationRegistry> observationRegistry,
				ObjectProvider<WebClient.Builder> webClientBuilderProvider,
				ObjectProvider<RestClient.Builder> restClientBuilderProvider,
				ObjectProvider<ChatModelObservationConvention> observationConvention,
				ObjectProvider<ToolExecutionEligibilityPredicate> dashscopeToolExecutionEligibilityPredicate
		) {

			var dashscopeApi = dashscopeChatApi(
					commonProperties,
					chatProperties,
					restClientBuilderProvider.getIfAvailable(RestClient::builder),
					webClientBuilderProvider.getIfAvailable(WebClient::builder),
					responseErrorHandler,
					"chat"
			);

			var dashscopeModel = DashScopeChatModel.builder()
					.dashScopeApi(dashscopeApi)
					.retryTemplate(retryTemplate)
					.toolCallingManager(toolCallingManager)
					.defaultOptions(chatProperties.getOptions())
					.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
					.toolExecutionEligibilityPredicate(
							dashscopeToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
					.build();

			observationConvention.ifAvailable(dashscopeModel::setObservationConvention);

			return dashscopeModel;
		}

		private DashScopeApi dashscopeChatApi(
				DashScopeConnectionProperties commonProperties,
				DashScopeChatProperties chatProperties,
				RestClient.Builder restClientBuilder,
				WebClient.Builder webClientBuilder,
				ResponseErrorHandler responseErrorHandler,
				String modelType
		) {

			ResolvedConnectionProperties resolved = resolveConnectionProperties(
					commonProperties,
					chatProperties,
					modelType
			);

			return DashScopeApi.builder()
					.apiKey(resolved.apiKey())
					.headers(resolved.headers())
					.baseUrl(resolved.baseUrl())
					.webClientBuilder(webClientBuilder)
					.workSpaceId(resolved.workspaceId())
					.restClientBuilder(restClientBuilder)
					.responseErrorHandler(responseErrorHandler)
					.build();
		}

}
// @formatter:on
