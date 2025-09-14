package com.alibaba.cloud.ai.memory.mem0.core;

/**
 * @author yingzi
 * @since 2025/9/14
 */

public class Mem0Client {

	private String baseUrl = "http://localhost:8888";

	private boolean enableCache = true;

	private int timeoutSeconds = 30;

	private int maxRetryAttempts = 3;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public boolean isEnableCache() {
		return enableCache;
	}

	public void setEnableCache(boolean enableCache) {
		this.enableCache = enableCache;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getMaxRetryAttempts() {
		return maxRetryAttempts;
	}

	public void setMaxRetryAttempts(int maxRetryAttempts) {
		this.maxRetryAttempts = maxRetryAttempts;
	}

}
