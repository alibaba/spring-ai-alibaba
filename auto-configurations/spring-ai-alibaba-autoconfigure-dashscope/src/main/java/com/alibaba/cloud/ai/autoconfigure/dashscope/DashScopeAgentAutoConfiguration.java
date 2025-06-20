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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;

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
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionUtils.resolveConnectionProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@ConditionalOnClass(DashScopeApi.class)
@ConditionalOnDashScopeEnabled
@ConditionalOnProperty(prefix = DashScopeAgentProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@AutoConfiguration(after = { RestClientAutoConfiguration.class, SpringAiRetryAutoConfiguration.class,
		ToolCallingAutoConfiguration.class })
@ImportAutoConfiguration(classes = { SpringAiRetryAutoConfiguration.class, RestClientAutoConfiguration.class,
		ToolCallingAutoConfiguration.class, WebClientAutoConfiguration.class })
@EnableConfigurationProperties({ DashScopeConnectionProperties.class, DashScopeAgentProperties.class, })
public class DashScopeAgentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DashScopeAgentApi dashscopeAgentApi(DashScopeConnectionProperties commonProperties,
			DashScopeAgentProperties agentProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			ObjectProvider<WebClient.Builder> webClientBuilderProvider, ResponseErrorHandler responseErrorHandler) {

		ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, agentProperties, "agent");

		return new DashScopeAgentApi(resolved.baseUrl(), resolved.apiKey(), resolved.workspaceId(),
				restClientBuilderProvider.getIfAvailable(RestClient::builder),
				webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler);
	}

}
