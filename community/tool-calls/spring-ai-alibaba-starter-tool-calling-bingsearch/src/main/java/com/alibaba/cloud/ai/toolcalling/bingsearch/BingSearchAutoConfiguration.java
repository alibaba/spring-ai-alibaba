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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * @author KrakenZJC
 **/
@Configuration
@ConditionalOnClass(BingSearchService.class)
@EnableConfigurationProperties(BingSearchProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.toolcalling.bingsearch", name = "enabled", havingValue = "true")
public class BingSearchAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use bing search engine to query for the latest news.")
	public BingSearchService bingSearchFunction(JsonParseTool jsonParseTool, BingSearchProperties properties) {
		return new BingSearchService(
				WebClientTool.builder(jsonParseTool, properties).httpHeadersConsumer(httpHeaders -> {
					httpHeaders.add(HttpHeaders.USER_AGENT,
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
					if (!StringUtils.hasText(properties.getToken())) {
						throw new IllegalArgumentException("token is empty");
					}
					httpHeaders.add(BingSearchProperties.OCP_APIM_SUBSCRIPTION_KEY, properties.getToken());
				}).build(), properties, jsonParseTool);
	}

}
