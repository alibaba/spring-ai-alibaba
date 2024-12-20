package com.alibaba.cloud.ai.functioncalling.dingtalk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author YunLong
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.dingtalk")
public class DingTalkProperties {

	private String customRobotAccessToken;

	private String customRobotSignature;

	public DingTalkProperties(String customRobotAccessToken, String customRobotSignature) {
		this.customRobotAccessToken = customRobotAccessToken;
		this.customRobotSignature = customRobotSignature;
	}

	public String getCustomRobotAccessToken() {
		return customRobotAccessToken;
	}

	public void setCustomRobotAccessToken(String customRobotAccessToken) {
		this.customRobotAccessToken = customRobotAccessToken;
	}

	public String getCustomRobotSignature() {
		return customRobotSignature;
	}

	public void setCustomRobotSignature(String customRobotSignature) {
		this.customRobotSignature = customRobotSignature;
	}

}
