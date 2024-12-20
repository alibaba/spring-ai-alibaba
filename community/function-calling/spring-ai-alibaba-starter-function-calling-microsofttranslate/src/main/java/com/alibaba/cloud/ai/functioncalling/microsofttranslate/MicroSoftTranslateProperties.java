package com.alibaba.cloud.ai.functioncalling.microsofttranslate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 31445
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.microsofttranslate")
public class MicroSoftTranslateProperties {

	public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

	private String apiKey;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}
