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
package com.alibaba.cloud.ai.functioncalling.alitranslate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yunlong
 * offline doc: https://help.aliyun.com/zh/machine-translation/developer-reference/api-alimt-2018-10-12-translategeneral?spm=a2c4g.11186623.help-menu-30396.d_4_1_2_0_0.54882fa7VX75w3&scm=20140722.H_2505929._.OR_help-T_cn~zh-V_1
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.alitranslate")
public class AliTranslateProperties {

	private String region;

	private String accessKeyId;

	private String accessKeySecret;


	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getAccessKeySecret() {
		return accessKeySecret;
	}

	public void setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
}
