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
package com.alibaba.cloud.ai.toolcalling.sinanews;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.DEFAULT_USER_AGENTS;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * @author XiaoYunTao
 * @since 2024/12/18
 */
@Configuration
@ConditionalOnClass(SinaNewsService.class)
@ConditionalOnProperty(prefix = SinaNewsConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SinaNewsProperties.class)
public class SinaNewsAutoConfiguration {

	@Bean(name = SinaNewsConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Get the news from the Sina news (获取新浪新闻).")
	public SinaNewsService getSinaNews(JsonParseTool jsonParseTool, SinaNewsProperties properties) {
		Consumer<HttpHeaders> consumer = headers -> {
			headers.add(HttpHeaders.USER_AGENT,
					DEFAULT_USER_AGENTS[ThreadLocalRandom.current().nextInt(DEFAULT_USER_AGENTS.length)]);
			headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
			headers.add(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,ja;q=0.8");
			headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
		};

		return new SinaNewsService(jsonParseTool, properties,
				WebClientTool.builder(jsonParseTool, properties).httpHeadersConsumer(consumer).build());
	}

}
