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

package com.alibaba.cloud.ai.toolcalling.baidutranslate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.alibaba.cloud.ai.toolcalling.baidutranslate.BaiduTranslateProperties.BaiDuTranslatePrefix;

/**
 * @author SCMRCORE
 */
@ConfigurationProperties(prefix = BaiDuTranslatePrefix)
public class BaiduTranslateProperties {

	protected static final String BaiDuTranslatePrefix = "spring.ai.alibaba.tool-calling.baidu.translate";

	private boolean enabled = true;

	private String appId;

	private String secretKey;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

}
