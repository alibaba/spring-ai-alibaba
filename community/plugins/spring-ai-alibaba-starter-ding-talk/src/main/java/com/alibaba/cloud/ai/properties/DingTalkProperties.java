package com.alibaba.cloud.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author YunLong
 */
@ConfigurationProperties("spring.ai.alibaba.plugin.ding-talk")
public class DingTalkProperties {

    private CustomRobot customRobot;

    public DingTalkProperties() {
    }

    public DingTalkProperties(CustomRobot customRobot) {
        this.customRobot = customRobot;
    }

    public CustomRobot getCustomRobot() {
        return customRobot;
    }

    public void setCustomRobot(CustomRobot customRobot) {
        this.customRobot = customRobot;
    }

    public static class CustomRobot {

        private String accessToken;

        private String signature;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }
}
