package com.alibaba.cloud.ai.plugin.translate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zhang
 * @date 2024/11/27
 * @Description
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.translate")
public class TranslateProperties {

    public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";


    // translate api key for Ocp-Apim-Subscription-Key
    // https://learn.microsoft.com/en-us/azure/ai-services/translator/text-sdk-overview?tabs=java
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }



}
