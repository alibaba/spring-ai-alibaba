package com.alibaba.cloud.ai.functioncalling.yuque;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 北极星
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.functioncalling.yuque")
public class YuqueProperties {

    public static String BASE_URL = "https://www.yuque.com/api/v2/repo";

    private String authToken;

    public String getAuthToken () {
        return authToken;
    }

    public YuqueProperties setAuthToken (String authToken) {
        this.authToken = authToken;
        return this;
    }
}
