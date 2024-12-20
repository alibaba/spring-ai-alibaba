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
