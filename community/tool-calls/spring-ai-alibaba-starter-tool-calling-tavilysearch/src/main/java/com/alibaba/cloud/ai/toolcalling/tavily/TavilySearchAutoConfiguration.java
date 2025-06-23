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
package com.alibaba.cloud.ai.toolcalling.tavily;

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
import org.springframework.http.MediaType;

@Configuration
@EnableConfigurationProperties(TavilySearchProperties.class)
@ConditionalOnClass(TavilySearchService.class)
@ConditionalOnProperty(prefix = TavilySearchConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class TavilySearchAutoConfiguration {

	@Bean(name = TavilySearchConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Provides a TavilySearchService bean for performing searches using the Tavily search engine.")
	public TavilySearchService tavilySearch(TavilySearchProperties properties, JsonParseTool jsonParseTool) {
		WebClientTool webClientTool = WebClientTool.builder(jsonParseTool, properties)
			.httpHeadersConsumer(httpHeaders -> {
				httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
				httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			})
			.build();
		return new TavilySearchService(jsonParseTool, webClientTool);
	}

}
