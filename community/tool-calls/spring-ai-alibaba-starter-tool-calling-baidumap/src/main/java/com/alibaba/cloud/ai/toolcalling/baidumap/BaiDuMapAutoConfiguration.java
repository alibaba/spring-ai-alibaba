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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author Carbon
 */

@Configuration
@EnableConfigurationProperties(BaiDuMapProperties.class)
@ConditionalOnProperty(prefix = BaiDuMapProperties.BaiDuMapPrefix, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class BaiDuMapAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(BaiDuMapAutoConfiguration.class);

	@Bean
	@Description("Search for places using Baidu Maps API "
			+ "or Get detail information of a address and facility query with baidu map or "
			+ "Get address information of a place with baidu map or "
			+ "Get detailed information about a specific place with baidu map")
	public MapSearchService baiDuMapGetAddressInformationFunction(BaiDuMapProperties baiDuMapProperties,
			JsonParseTool jsonParseTool) {

		logger.debug("baiDuMapGetAddressInformationFunction is enabled.");
		return new MapSearchService(baiDuMapProperties, new WebClientTool(jsonParseTool, baiDuMapProperties),
				jsonParseTool);
	}

}
