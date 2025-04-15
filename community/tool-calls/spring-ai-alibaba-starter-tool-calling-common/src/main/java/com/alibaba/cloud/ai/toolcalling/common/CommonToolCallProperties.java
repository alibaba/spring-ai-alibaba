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
package com.alibaba.cloud.ai.toolcalling.common;

/**
 * The properties of the specific tool call need to inherit from this class
 *
 * @author vlsmb
 */
public class CommonToolCallProperties {

	private String apiKey;

	private String secretKey;

	private String baseUrl;

	private Integer networkTimeout;

	private String appId;

	private String token;

	private boolean enabled = false;

	public CommonToolCallProperties() {
		this.baseUrl = CommonToolCallConstants.DEFAULT_BASE_URL;
		this.networkTimeout = CommonToolCallConstants.DEFAULT_NETWORK_TIMEOUT;
	}

	public CommonToolCallProperties(String baseUrl) {
		this.baseUrl = baseUrl;
		this.networkTimeout = CommonToolCallConstants.DEFAULT_NETWORK_TIMEOUT;
	}

	public CommonToolCallProperties(String baseUrl, Integer networkTimeout) {
		this.baseUrl = baseUrl;
		this.networkTimeout = networkTimeout;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Integer getNetworkTimeout() {
		return networkTimeout;
	}

	public void setNetworkTimeout(Integer networkTimeout) {
		this.networkTimeout = networkTimeout;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
