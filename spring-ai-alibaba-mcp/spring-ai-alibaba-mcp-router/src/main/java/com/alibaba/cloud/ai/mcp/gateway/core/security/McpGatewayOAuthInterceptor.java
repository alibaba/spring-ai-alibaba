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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * OAuth认证拦截器 自动为MCP Gateway的HTTP请求添加OAuth认证头
 */
public class McpGatewayOAuthInterceptor implements ExchangeFilterFunction {

	private static final Logger logger = LoggerFactory.getLogger(McpGatewayOAuthInterceptor.class);

	private final McpGatewayOAuthTokenManager tokenManager;

	private final McpGatewayOAuthProperties oauthProperties;

	public McpGatewayOAuthInterceptor(McpGatewayOAuthTokenManager tokenManager,
			McpGatewayOAuthProperties oauthProperties) {
		this.tokenManager = tokenManager;
		this.oauthProperties = oauthProperties;
	}

	@NotNull
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (!oauthProperties.isEnabled()) {
			logger.debug("OAuth 身份验证未开启");
			return next.exchange(request);
		}

		logger.debug("OAuth 认证 URL: {}", request.url());

		return tokenManager.getAccessToken().flatMap(accessToken -> {
			ClientRequest authenticatedRequest = ClientRequest.from(request)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.build();

			logger.debug("成功添加 OAuth 认证");
			return next.exchange(authenticatedRequest);
		}).switchIfEmpty(Mono.defer(() -> {
			logger.warn("没有可用的OAuth token，继续执行无认证请求");
			return next.exchange(request);
		})).onErrorResume(throwable -> {
			logger.error("OAuth认证失败，error: {}", throwable.getMessage(), throwable);
			return next.exchange(request);
		}).doOnNext(response -> {
			if (response.statusCode().is4xxClientError() && response.statusCode().value() == 401) {
				logger.warn("收到401未授权响应，OAuth token 无效");
				try {
					tokenManager.clearCachedToken();
					logger.info("已清除无效的缓存token");
				}
				catch (Exception e) {
					logger.debug("清除缓存token失败", e);
				}
			}
		});
	}

}
