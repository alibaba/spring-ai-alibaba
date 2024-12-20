package com.alibaba.cloud.ai.functioncalling.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 31445
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.weather")
public class WeatherProperties {

	private String apiKey;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}
