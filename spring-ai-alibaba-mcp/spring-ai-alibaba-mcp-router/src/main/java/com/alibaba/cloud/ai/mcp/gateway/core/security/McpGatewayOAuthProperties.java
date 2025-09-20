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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * MCP Gateway OAuth认证配置属性
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.gateway.oauth")
public class McpGatewayOAuthProperties {

	/**
	 * 是否启用OAuth认证
	 */
	private boolean enabled = false;

	/**
	 * OAuth提供商配置
	 */
	private OAuthProvider provider = new OAuthProvider();

	/**
	 * Token缓存配置
	 */
	private TokenCache tokenCache = new TokenCache();

	/**
	 * 重试配置
	 */
	private Retry retry = new Retry();

	public static class OAuthProvider {

		/**
		 * 客户端ID
		 */
		private String clientId;

		/**
		 * 客户端密钥
		 */
		private String clientSecret;

		/**
		 * 授权URL
		 */
		private String authorizationUri;

		/**
		 * Token获取URL
		 */
		private String tokenUri;

		/**
		 * 用户信息URL
		 */
		private String userInfoUri;

		/**
		 * 授权范围
		 */
		private String scope = "read";

		/**
		 * 重定向URI
		 */
		private String redirectUri;

		/**
		 * 授权类型
		 */
		private String grantType = "client_credentials";

		/**
		 * 额外的请求参数
		 */
		private Map<String, String> additionalParameters;

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getAuthorizationUri() {
			return authorizationUri;
		}

		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		public String getTokenUri() {
			return tokenUri;
		}

		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		public String getUserInfoUri() {
			return userInfoUri;
		}

		public void setUserInfoUri(String userInfoUri) {
			this.userInfoUri = userInfoUri;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		public String getRedirectUri() {
			return redirectUri;
		}

		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}

		public String getGrantType() {
			return grantType;
		}

		public void setGrantType(String grantType) {
			this.grantType = grantType;
		}

		public Map<String, String> getAdditionalParameters() {
			return additionalParameters;
		}

		public void setAdditionalParameters(Map<String, String> additionalParameters) {
			this.additionalParameters = additionalParameters;
		}

	}

	public static class TokenCache {

		/**
		 * 是否启用Token缓存
		 */
		private boolean enabled = true;

		/**
		 * Token提前刷新时间
		 */
		private Duration refreshBeforeExpiry = Duration.ofMinutes(5);

		/**
		 * 最大缓存大小
		 */
		private int maxSize = 1000;

		// Getters and Setters
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Duration getRefreshBeforeExpiry() {
			return refreshBeforeExpiry;
		}

		public void setRefreshBeforeExpiry(Duration refreshBeforeExpiry) {
			this.refreshBeforeExpiry = refreshBeforeExpiry;
		}

		public int getMaxSize() {
			return maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

	}

	public static class Retry {

		/**
		 * 最大重试次数
		 */
		private int maxAttempts = 3;

		/**
		 * 重试间隔
		 */
		private Duration backoff = Duration.ofSeconds(1);

		// Getters and Setters
		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public Duration getBackoff() {
			return backoff;
		}

		public void setBackoff(Duration backoff) {
			this.backoff = backoff;
		}

	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public OAuthProvider getProvider() {
		return provider;
	}

	public void setProvider(OAuthProvider provider) {
		this.provider = provider;
	}

	public TokenCache getTokenCache() {
		return tokenCache;
	}

	public void setTokenCache(TokenCache tokenCache) {
		this.tokenCache = tokenCache;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

}
