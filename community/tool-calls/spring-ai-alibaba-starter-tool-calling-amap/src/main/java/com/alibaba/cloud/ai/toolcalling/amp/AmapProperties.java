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
package com.alibaba.cloud.ai.toolcalling.amp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

/**
 * @author YunLong
 */
@ConfigurationProperties(prefix = AmapProperties.AMAP_PREFIX)
public class AmapProperties extends CommonToolCallProperties {

	protected static final String AMAP_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".amap";

	public AmapProperties() {
		super("https://restapi.amap.com/v3");
		this.setPropertiesFromEnv("GAODE_AMAP_API_KEY", null, null, null);
	}

	public String getWebApiKey() {
		return getApiKey();
	}

	@Deprecated
	public void setWebApiKey(String webApiKey) {
		this.setApiKey(webApiKey);
	}

}
