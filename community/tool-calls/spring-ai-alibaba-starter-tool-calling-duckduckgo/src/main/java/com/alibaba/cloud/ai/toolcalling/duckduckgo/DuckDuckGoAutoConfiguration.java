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
package com.alibaba.cloud.ai.toolcalling.duckduckgo;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.DEFAULT_USER_AGENTS;

/**
 * @author 北极星
 * @author sixiyida
 */

@Configuration
@EnableConfigurationProperties(DuckDuckGoProperties.class)
@ConditionalOnProperty(prefix = DuckDuckGoConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class DuckDuckGoAutoConfiguration {

	@Bean(name = DuckDuckGoConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use DuckDuckGo search to query for the latest news.")
	public DuckDuckGoQueryNewsService duckDuckGoQueryNews(JsonParseTool jsonParseTool,
			DuckDuckGoProperties duckDuckGoProperties) {
		Consumer<HttpHeaders> consumer = headers -> {
			headers.add(HttpHeaders.USER_AGENT,
					DEFAULT_USER_AGENTS[ThreadLocalRandom.current().nextInt(DEFAULT_USER_AGENTS.length)]);
			headers.add(HttpHeaders.CONNECTION, "keep-alive");
		};
		WebClientTool webClientTool = WebClientTool.builder(jsonParseTool, duckDuckGoProperties)
			.httpHeadersConsumer(consumer)
			.build();
		return new DuckDuckGoQueryNewsService(duckDuckGoProperties, webClientTool);
	}

}
