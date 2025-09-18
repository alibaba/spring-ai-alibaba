/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.mcp.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP service configuration properties
 */
@Component
@ConfigurationProperties("mcp")
public class McpProperties {

	/**
	 * Maximum retry count
	 */
	private int maxRetries = 3;

	/**
	 * Connection timeout duration
	 */
	private Duration timeout = Duration.ofSeconds(60);

	/**
	 * Cache expiration time after access
	 */
	private Duration cacheExpireAfterAccess = Duration.ofMinutes(10);

	/**
	 * Retry wait time multiplier (seconds)
	 */
	private int retryWaitMultiplier = 1;

	/**
	 * SSE URL path suffix
	 */
	private String ssePathSuffix = "/sse";

	/**
	 * User agent
	 */
	private String userAgent = "MCP-Client/1.0.0";

	// Getters and Setters
	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Duration getCacheExpireAfterAccess() {
		return cacheExpireAfterAccess;
	}

	public void setCacheExpireAfterAccess(Duration cacheExpireAfterAccess) {
		this.cacheExpireAfterAccess = cacheExpireAfterAccess;
	}

	public int getRetryWaitMultiplier() {
		return retryWaitMultiplier;
	}

	public void setRetryWaitMultiplier(int retryWaitMultiplier) {
		this.retryWaitMultiplier = retryWaitMultiplier;
	}

	public String getSsePathSuffix() {
		return ssePathSuffix;
	}

	public void setSsePathSuffix(String ssePathSuffix) {
		this.ssePathSuffix = ssePathSuffix;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

}
