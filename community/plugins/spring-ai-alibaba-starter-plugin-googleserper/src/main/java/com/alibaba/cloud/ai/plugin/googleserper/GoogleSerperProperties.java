package com.alibaba.cloud.ai.plugin.googleserper;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: superhandsomeg
 * @since : 2024-12-11
 **/

@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.googleserper")
public class GoogleSerperProperties {

	private String apikey;

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}
}
