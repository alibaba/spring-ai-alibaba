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

package com.alibaba.cloud.ai.mcp.gateway.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class McpGatewayOAuthConfigValidator {

	private static final Logger logger = LoggerFactory.getLogger(McpGatewayOAuthConfigValidator.class);

	/**
	 * 验证OAuth配置的完整性
	 */
	public static ValidationResult validateOAuthProperties(McpGatewayOAuthProperties properties) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		if (properties == null) {
			errors.add("OAuth 属性为空");
			return new ValidationResult(false, errors, warnings);
		}

		if (!properties.isEnabled()) {
			logger.debug("OAuth 配置未开启");
			return new ValidationResult(true, errors, warnings);
		}

		McpGatewayOAuthProperties.OAuthProvider provider = properties.getProvider();
		if (provider == null) {
			errors.add("OAuth配置为空");
		}
		else {
			List<String> providerErrors = validateProvider(provider);
			errors.addAll(providerErrors);
		}

		if (properties.getTokenCache() != null) {
			validateTokenCacheConfig(properties.getTokenCache(), warnings);
		}

		if (properties.getRetry() != null) {
			validateRetryConfig(properties.getRetry(), warnings);
		}

		boolean isValid = errors.isEmpty();
		return new ValidationResult(isValid, errors, warnings);
	}

	/**
	 * 验证OAuth提供商配置
	 */
	private static List<String> validateProvider(McpGatewayOAuthProperties.OAuthProvider provider) {
		List<String> errors = new ArrayList<>();

		if (provider == null) {
			errors.add("提供商配置为空");
			return errors;
		}

		// 进行字段的验证
		if (!StringUtils.hasText(provider.getClientId())) {
			errors.add("clientId是必需的");
		}

		if (!StringUtils.hasText(provider.getClientSecret())) {
			errors.add("clientSecret是必需的");
		}

		if (!StringUtils.hasText(provider.getTokenUri())) {
			errors.add("tokenUri是必需的");
		}
		else {
			// 验证tokenUri格式
			try {
				URI.create(provider.getTokenUri());
			}
			catch (Exception e) {
				errors.add("tokenUri格式无效 - " + e.getMessage());
			}
		}

		String grantType = provider.getGrantType();
		if (StringUtils.hasText(grantType)) {
			if (!"client_credentials".equals(grantType) && !"authorization_code".equals(grantType)
					&& !"password".equals(grantType) && !"refresh_token".equals(grantType)) {
				errors.add("不支持的授权类型 '" + grantType + "'");
			}
		}

		if (StringUtils.hasText(provider.getAuthorizationUri())) {
			try {
				URI.create(provider.getAuthorizationUri());
			}
			catch (Exception e) {
				errors.add("authorizationUri格式无效 - " + e.getMessage());
			}
		}

		if (StringUtils.hasText(provider.getUserInfoUri())) {
			try {
				URI.create(provider.getUserInfoUri());
			}
			catch (Exception e) {
				errors.add("userInfoUri格式无效 - " + e.getMessage());
			}
		}

		return errors;
	}

	/**
	 * 验证Token缓存配置
	 */
	private static void validateTokenCacheConfig(McpGatewayOAuthProperties.TokenCache cacheConfig,
			List<String> warnings) {
		if (cacheConfig.getMaxSize() <= 0) {
			warnings.add("Token缓存最大大小应该是正数，当前值: " + cacheConfig.getMaxSize());
		}

		if (cacheConfig.getRefreshBeforeExpiry() != null) {
			long refreshSeconds = cacheConfig.getRefreshBeforeExpiry().getSeconds();
			if (refreshSeconds < 0) {
				warnings.add("Token缓存刷新时间不应为负数");
			}
			else if (refreshSeconds > 3600) {
				warnings.add("Token缓存刷新时间过大(> 1小时)，建议减小该值");
			}
		}
	}

	/**
	 * 验证重试配置
	 */
	private static void validateRetryConfig(McpGatewayOAuthProperties.Retry retryConfig, List<String> warnings) {
		if (retryConfig.getMaxAttempts() < 1) {
			warnings.add("重试最大尝试次数应至少为1，当前值: " + retryConfig.getMaxAttempts());
		}

		if (retryConfig.getBackoff() != null) {
			long backoffMillis = retryConfig.getBackoff().toMillis();
			if (backoffMillis < 0) {
				warnings.add("重试退避时间不应为负数");
			}
			else if (backoffMillis > 30000) {
				warnings.add("重试退避时间过大，建议减小该值");
			}
		}
	}

	/**
	 * 验证结果类
	 */
	public static class ValidationResult {

		private final boolean valid;

		private final List<String> errors;

		private final List<String> warnings;

		public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
			this.valid = valid;
			this.errors = errors != null ? errors : new ArrayList<>();
			this.warnings = warnings != null ? warnings : new ArrayList<>();
		}

		public boolean isValid() {
			return valid;
		}

		public List<String> getErrors() {
			return errors;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public boolean hasWarnings() {
			return !warnings.isEmpty();
		}

		public void logResults() {
			if (!valid) {
				logger.error("OAuth configuration validation failed with {} errors", errors.size());
				for (int i = 0; i < errors.size(); i++) {
					logger.error("Error {}: {}", (i + 1), errors.get(i));
				}
			}
			else {
				logger.info("OAuth configuration validation passed");
			}

			if (hasWarnings()) {
				logger.warn("OAuth configuration has {} warnings", warnings.size());
				for (int i = 0; i < warnings.size(); i++) {
					logger.warn("Warning {}: {}", (i + 1), warnings.get(i));
				}
			}
		}

	}

}
