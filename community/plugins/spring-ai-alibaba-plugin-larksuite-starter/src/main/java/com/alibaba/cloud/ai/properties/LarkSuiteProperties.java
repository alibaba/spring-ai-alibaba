package com.alibaba.cloud.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 北极星
 */

@ConfigurationProperties("spring.ai.alibaba.plugin.larksuite")
public class LarkSuiteProperties {

    /**
     * AccessToken
     */
    private String appId;

    /**
     * AccessToken
     */
    private String appSecret;

    public LarkSuiteProperties() {
    }

    public LarkSuiteProperties(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public LarkSuiteProperties setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public LarkSuiteProperties setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }
}
