package com.alibaba.cloud.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author YunLong
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.gaode-map")
public class GaoDeProperties {

    private final String baseUrl = "https://restapi.amap.com/v3";

    private String webApiKey;

    public GaoDeProperties() {}

    public GaoDeProperties(String webApiKey) {
        this.webApiKey = webApiKey;
    }

    public String getWebApiKey() {
        return webApiKey;
    }

    public void setWebApiKey(String webApiKey) {
        this.webApiKey = webApiKey;
    }
}