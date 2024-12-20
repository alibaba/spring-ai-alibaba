package com.alibaba.cloud.ai.functioncalling.serpapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 北极星
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.serpapi")
public class SerpApiProperties {

	public static final String SERP_API_URL = "https://serpapi.com/search.json";

	public static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

	private String apikey;

	private String engine;

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

}
