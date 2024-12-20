package com.alibaba.cloud.ai.functioncalling.bingsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author: KrakenZJC
 **/
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.bing")
public class BingSearchProperties {

	public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
