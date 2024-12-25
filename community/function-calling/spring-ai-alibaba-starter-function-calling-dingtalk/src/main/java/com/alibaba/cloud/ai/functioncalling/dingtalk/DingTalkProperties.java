/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
