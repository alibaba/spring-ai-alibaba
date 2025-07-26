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
package com.alibaba.cloud.ai.toolcalling.nationalstatistics;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * 国家统计局工具自动配置类
 *
 * @author Makoto
 */
@Configuration
@ConditionalOnClass(NationalStatisticsService.class)
@EnableConfigurationProperties(NationalStatisticsProperties.class)
@ConditionalOnProperty(prefix = NationalStatisticsConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class NationalStatisticsAutoConfiguration {

	@Bean(name = NationalStatisticsConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("使用国家统计局官网获取统计数据信息")
	public NationalStatisticsService getNationalStatisticsService(NationalStatisticsProperties properties,
			JsonParseTool jsonParseTool) {

		WebClientTool webClientTool = WebClientTool.builder(jsonParseTool, properties).httpHeadersConsumer(headers -> {
			headers.add("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
			headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			headers.add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
			headers.add("Accept-Encoding", "gzip, deflate");
			headers.add("Connection", "keep-alive");
			headers.add("Upgrade-Insecure-Requests", "1");
		}).build();

		return new NationalStatisticsService(webClientTool, jsonParseTool, properties);
	}

}
