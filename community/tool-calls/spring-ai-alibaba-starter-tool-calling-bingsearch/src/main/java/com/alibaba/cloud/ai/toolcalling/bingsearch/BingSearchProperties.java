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
package com.alibaba.cloud.ai.toolcalling.bingsearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

/**
 * @author KrakenZJC
 **/
@EnableConfigurationProperties
@ConfigurationProperties(prefix = BingSearchProperties.BING_SEARCH_PREFIX)
public class BingSearchProperties extends CommonToolCallProperties {

	public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

	protected static final String BING_SEARCH_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".bingsearch";

	public static final String BING_SEARCH_PATH = "/v7.0/search";

	public BingSearchProperties() {
		super("https://api.bing.microsoft.com");
		this.setPropertiesFromEnv(null, null, null, "BING_SEARCH_TOKEN");
	}

}
