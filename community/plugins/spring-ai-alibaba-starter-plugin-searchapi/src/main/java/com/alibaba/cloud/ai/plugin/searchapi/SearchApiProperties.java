package com.alibaba.cloud.ai.plugin.searchapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: superhandsomeg
 * @since : 2024-12-11
 **/

@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.searchapi")
public class SearchApiProperties {

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
