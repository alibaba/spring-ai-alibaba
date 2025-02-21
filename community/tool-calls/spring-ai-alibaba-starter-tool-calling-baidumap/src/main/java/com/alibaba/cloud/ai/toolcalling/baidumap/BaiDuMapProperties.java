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
package com.alibaba.cloud.ai.toolcalling.baidumap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Carbon
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.functioncalling.baidumap")
public class BaiDuMapProperties {

	// Official Document URL： https://lbs.baidu.com/faq/api?title=webapi/ROS2/prepare
	private String webApiKey;

	public BaiDuMapProperties(String webApiKey) {
		this.webApiKey = webApiKey;
	}

	public String getWebApiKey() {
		return webApiKey;
	}

	public void setWebApiKey(String webApiKey) {
		this.webApiKey = webApiKey;
	}

}