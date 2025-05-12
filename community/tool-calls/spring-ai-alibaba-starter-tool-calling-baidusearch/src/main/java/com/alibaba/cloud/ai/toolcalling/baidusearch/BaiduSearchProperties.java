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
package com.alibaba.cloud.ai.toolcalling.baidusearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

/**
 * @author vlsmb
 */
@ConfigurationProperties(prefix = BaiduSearchProperties.BAIDU_SEARCH_PREFIX)
public class BaiduSearchProperties extends CommonToolCallProperties {

	protected static final String BAIDU_SEARCH_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".baidu.search";

	public BaiduSearchProperties() {
		super("https://www.baidu.com/s?wd=");
	}

}
