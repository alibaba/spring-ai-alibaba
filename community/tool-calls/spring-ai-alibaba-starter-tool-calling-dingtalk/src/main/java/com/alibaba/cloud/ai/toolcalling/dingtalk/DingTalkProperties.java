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
package com.alibaba.cloud.ai.toolcalling.dingtalk;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author YunLong
 */
@ConfigurationProperties(prefix = DingTalkConstants.CONFIG_PREFIX)
public class DingTalkProperties extends CommonToolCallProperties {

	private String customRobotAccessToken;

	private String customRobotSignature;

	public DingTalkProperties(String customRobotAccessToken, String customRobotSignature) {
		this.customRobotAccessToken = customRobotAccessToken;
		this.customRobotSignature = customRobotSignature;
		if (!StringUtils.hasText(customRobotAccessToken)) {
			this.customRobotAccessToken = System.getenv(DingTalkConstants.ACCESS_TOKEN_ENV);
		}
		if (!StringUtils.hasText(customRobotSignature)) {
			this.customRobotSignature = System.getenv(DingTalkConstants.SIGNATURE_ENV);
		}
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
