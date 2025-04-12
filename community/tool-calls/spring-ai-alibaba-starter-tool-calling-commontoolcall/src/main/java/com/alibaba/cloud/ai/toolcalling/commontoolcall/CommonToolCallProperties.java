package com.alibaba.cloud.ai.toolcalling.commontoolcall;

public abstract class CommonToolCallProperties {

	private String apiKey;

	private String secretKey;

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

}
