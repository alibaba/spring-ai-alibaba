package com.alibaba.cloud.ai.agent.nacos.vo;

public class ModelVO {
	private String baseUrl;

	private String apiKey;

	private String model;

	private String temperature;

	private String maxTokens;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}

	public String getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(String maxTokens) {
		this.maxTokens = maxTokens;
	}
}
