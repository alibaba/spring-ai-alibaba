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

import static com.alibaba.cloud.ai.toolcalling.baidumap.BaiDuMapProperties.BaiDuMapPrefix;

/**
 * @author Carbon
 */
@ConfigurationProperties(prefix = BaiDuMapPrefix)
public class BaiDuMapProperties {

	protected static final String BaiDuMapPrefix = "spring.ai.alibaba.tool-calling.baidu.map";

	private boolean enabled = true;

	/**
	 * Official Document URL：
	 * <a href="https://lbs.baidu.com/faq/api?title=webapi/ROS2/prepare">...</a>
	 */
	private String apiKey;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}