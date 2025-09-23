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

package com.alibaba.cloud.ai.autoconfigure.mcp.gateway.security;

import com.alibaba.cloud.ai.mcp.gateway.core.security.McpGatewayOAuthConfigValidator;
import com.alibaba.cloud.ai.mcp.gateway.core.security.McpGatewayOAuthInterceptor;
import com.alibaba.cloud.ai.mcp.gateway.core.security.McpGatewayOAuthProperties;
import com.alibaba.cloud.ai.mcp.gateway.core.security.McpGatewayOAuthTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableConfigurationProperties(McpGatewayOAuthProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.mcp.gateway.oauth", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@ConditionalOnClass(WebClient.class)
public class McpGatewayOAuthAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpGatewayOAuthAutoConfiguration.class);

	@Bean
	@ConditionalOnBean(WebClient.Builder.class)
	@ConditionalOnMissingBean(McpGatewayOAuthTokenManager.class)
	public McpGatewayOAuthTokenManager mcpGatewayOAuthTokenManager(WebClient.Builder webClientBuilder,
			McpGatewayOAuthProperties oauthProperties) {

		McpGatewayOAuthConfigValidator.ValidationResult validation = McpGatewayOAuthConfigValidator
			.validateOAuthProperties(oauthProperties);

		if (!validation.isValid()) {
			validation.logResults();
			throw new IllegalStateException("OAuth configuration is invalid");
		}
		else if (validation.hasWarnings()) {
			validation.logResults();
		}

		return new McpGatewayOAuthTokenManager(webClientBuilder, oauthProperties);
	}

	@Bean
	@ConditionalOnBean(McpGatewayOAuthTokenManager.class)
	@ConditionalOnMissingBean(McpGatewayOAuthInterceptor.class)
	public McpGatewayOAuthInterceptor mcpGatewayOAuthInterceptor(McpGatewayOAuthTokenManager tokenManager,
			McpGatewayOAuthProperties oauthProperties) {

		return new McpGatewayOAuthInterceptor(tokenManager, oauthProperties);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void validateOAuthConfigurationOnStartup(ApplicationReadyEvent event) {
		try {
			McpGatewayOAuthProperties oauthProperties = event.getApplicationContext()
				.getBean(McpGatewayOAuthProperties.class);

			if (oauthProperties.isEnabled()) {

				McpGatewayOAuthConfigValidator.ValidationResult validation = McpGatewayOAuthConfigValidator
					.validateOAuthProperties(oauthProperties);

				validation.logResults();
			}
		}
		catch (Exception e) {
			logger.debug("OAuth配置验证已跳过: {}", e.getMessage());
		}
	}

}
