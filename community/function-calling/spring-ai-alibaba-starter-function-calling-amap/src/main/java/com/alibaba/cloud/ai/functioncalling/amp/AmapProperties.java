package com.alibaba.cloud.ai.functioncalling.amp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author YunLong
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.gaode")
public class AmapProperties {

	// Official Document Address： https://lbs.amap.com/api/webservice/summary
	private String webApiKey;

	public AmapProperties() {
	}

	public AmapProperties(String webApiKey) {
		this.webApiKey = webApiKey;
	}

	public String getWebApiKey() {
		return webApiKey;
	}

	public void setWebApiKey(String webApiKey) {
		this.webApiKey = webApiKey;
	}

}