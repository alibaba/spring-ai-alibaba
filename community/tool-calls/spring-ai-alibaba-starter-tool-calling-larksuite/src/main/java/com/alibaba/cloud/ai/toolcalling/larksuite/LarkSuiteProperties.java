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
package com.alibaba.cloud.ai.toolcalling.larksuite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 北极星
 */

@ConfigurationProperties("spring.ai.alibaba.toolcalling.larksuite")
public class LarkSuiteProperties {

	/**
	 * AppId
	 */
	private String appId;

	/**
	 * AppSecret
	 */
	private String appSecret;

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
