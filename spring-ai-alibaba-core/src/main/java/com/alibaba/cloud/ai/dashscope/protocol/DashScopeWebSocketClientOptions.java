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
package com.alibaba.cloud.ai.dashscope.protocol;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;

/**
 * @author kevinlin09
 */
public class DashScopeWebSocketClientOptions {

	private String url = DashScopeApiConstants.DEFAULT_WEBSOCKET_URL;

	private String apiKey;

	private String workSpaceId = null;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getWorkSpaceId() {
		return workSpaceId;
	}

	public void setWorkSpaceId(String workSpaceId) {
		this.workSpaceId = workSpaceId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected DashScopeWebSocketClientOptions options;

		public Builder() {
			this.options = new DashScopeWebSocketClientOptions();
		}

		public Builder(DashScopeWebSocketClientOptions options) {
			this.options = options;
		}

		public Builder withUrl(String baseUrl) {
			options.setUrl(baseUrl);
			return this;
		}

		public Builder withApiKey(String apiKey) {
			options.setApiKey(apiKey);
			return this;
		}

		public Builder withWorkSpaceId(String workSpaceId) {
			options.setWorkSpaceId(workSpaceId);
			return this;
		}

		public DashScopeWebSocketClientOptions build() {
			return options;
		}

	}

}
