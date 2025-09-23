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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * OAuth Token管理器 负责Token的获取、缓存和刷新
 */
public class McpGatewayOAuthTokenManager {

	private static final Logger logger = LoggerFactory.getLogger(McpGatewayOAuthTokenManager.class);

	private final WebClient webClient;

	private final McpGatewayOAuthProperties oauthProperties;

	private final ObjectMapper objectMapper;

	private volatile CachedToken cachedToken;

	private final static Integer DEFAULT_EXPIRED_TIME = 3600;

	public McpGatewayOAuthTokenManager(WebClient.Builder webClientBuilder, McpGatewayOAuthProperties oauthProperties) {
		this.webClient = webClientBuilder.build();
		this.oauthProperties = oauthProperties;
		this.objectMapper = new ObjectMapper();
		this.cachedToken = null;
	}

	/**
	 * 获取访问Token
	 */
	public Mono<String> getAccessToken() {
		if (!oauthProperties.isEnabled()) {
			return Mono.empty();
		}

		return Mono.fromCallable(() -> {
			// 检查缓存
			if (oauthProperties.getTokenCache().isEnabled()) {
				if (cachedToken != null && !isTokenExpiring(cachedToken)) {
					logger.debug("使用缓存的token");
					return cachedToken.getAccessToken();
				}
			}
			return null;
		}).onErrorResume(throwable -> {
			logger.error("获取访问token失败", throwable);
			return Mono.empty();
		}).switchIfEmpty(fetchNewToken());
	}

	/**
	 * 获取新的访问Token
	 */
	private Mono<String> fetchNewToken() {
		McpGatewayOAuthProperties.OAuthProvider provider = oauthProperties.getProvider();
		if (provider == null) {
			return Mono.error(new IllegalArgumentException("OAuth 未配置"));
		}

		logger.info("获取新的访问token");

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("grant_type", provider.getGrantType());
		formData.add("client_id", provider.getClientId());
		formData.add("client_secret", provider.getClientSecret());

		if (StringUtils.hasText(provider.getScope())) {
			formData.add("scope", provider.getScope());
		}

		return webClient.post()
			.uri(provider.getTokenUri())
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
			.body(BodyInserters.fromFormData(formData))
			.retrieve()
			.bodyToMono(String.class)
			.map(responseBody -> parseTokenResponse(responseBody))
			.doOnNext(token -> logger.info("成功获取访问token"))
			.retry(oauthProperties.getRetry().getMaxAttempts() - 1)
			.onErrorMap(throwable -> {
				logger.error("获取访问token失败", throwable);
				return new RuntimeException("OAuth token获取失败", throwable);
			});
	}

	/**
	 * 解析Token响应
	 */
	private String parseTokenResponse(String responseBody) {
		try {
			TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);

			if (!StringUtils.hasText(tokenResponse.getAccessToken())) {
				throw new RuntimeException("响应中未找到访问token");
			}

			// 缓存Token
			if (oauthProperties.getTokenCache().isEnabled()) {
				cachedToken = new CachedToken(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(),
						Instant.now()
							.plusSeconds(tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : DEFAULT_EXPIRED_TIME),
						tokenResponse.getTokenType());
				logger.debug("缓存访问token");
			}

			return tokenResponse.getAccessToken();
		}
		catch (Exception e) {
			logger.error("解析token响应失败，响应内容: {}", responseBody, e);
			throw new RuntimeException("解析OAuth token响应失败", e);
		}
	}

	/**
	 * 检查Token是否即将过期
	 */
	private boolean isTokenExpiring(CachedToken cachedToken) {
		if (cachedToken.getExpiresAt() == null) {
			return false;
		}

		Instant refreshThreshold = cachedToken.getExpiresAt()
			.minus(oauthProperties.getTokenCache().getRefreshBeforeExpiry());

		return Instant.now().isAfter(refreshThreshold);
	}

	/**
	 * 强制刷新访问Token
	 */
	public Mono<String> refreshAccessToken() {
		if (!oauthProperties.isEnabled()) {
			return Mono.empty();
		}

		logger.info("强制刷新访问token");

		// 清除缓存的token
		clearCachedToken();

		// 获取新的token
		return fetchNewToken();
	}

	/**
	 * 清除缓存Token
	 */
	public void clearCachedToken() {
		if (cachedToken != null) {
			logger.info("清除缓存的token ");
			cachedToken = null;
		}
		else {
			logger.debug("没有找到要清除的缓存token");
		}
	}

	/**
	 * Token响应信息
	 */
	public static class TokenResponse {

		@JsonProperty("access_token")
		private String accessToken;

		@JsonProperty("refresh_token")
		private String refreshToken;

		@JsonProperty("expires_in")
		private Long expiresIn;

		@JsonProperty("token_type")
		private String tokenType;

		@JsonProperty("scope")
		private String scope;

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getRefreshToken() {
			return refreshToken;
		}

		public void setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
		}

		public Long getExpiresIn() {
			return expiresIn;
		}

		public void setExpiresIn(Long expiresIn) {
			this.expiresIn = expiresIn;
		}

		public String getTokenType() {
			return tokenType;
		}

		public void setTokenType(String tokenType) {
			this.tokenType = tokenType;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

	}

	/**
	 * 缓存的Token信息
	 */
	public static class CachedToken {

		private final String accessToken;

		private final String refreshToken;

		private final Instant expiresAt;

		private final String tokenType;

		public CachedToken(String accessToken, String refreshToken, Instant expiresAt, String tokenType) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
			this.expiresAt = expiresAt;
			this.tokenType = tokenType;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public String getRefreshToken() {
			return refreshToken;
		}

		public Instant getExpiresAt() {
			return expiresAt;
		}

		public String getTokenType() {
			return tokenType;
		}

	}

}
