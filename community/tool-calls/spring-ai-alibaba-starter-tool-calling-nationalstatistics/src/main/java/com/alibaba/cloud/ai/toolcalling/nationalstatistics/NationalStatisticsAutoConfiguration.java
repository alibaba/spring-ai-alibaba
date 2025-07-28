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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 国家统计局工具自动配置类
 *
 * @author Makoto
 */
@Configuration
@EnableConfigurationProperties(NationalStatisticsProperties.class)
@ConditionalOnClass(NationalStatisticsService.class)
@ConditionalOnProperty(prefix = NationalStatisticsConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class NationalStatisticsAutoConfiguration {



	/**
	 * 注册国家统计局服务Bean
	 */
	@Bean(name = NationalStatisticsConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("查询中国国家统计局的各类统计数据，包括人口、经济、社会等统计指标")
	public NationalStatisticsService nationalStatistics(NationalStatisticsProperties properties, JsonParseTool jsonParseTool) {
		// 临时切换为HTTP方式避免SSL问题
		NationalStatisticsProperties modifiedProperties = new NationalStatisticsProperties();
		modifiedProperties.setBaseUrl("http://data.stats.gov.cn");
		modifiedProperties.setEnabled(properties.isEnabled());
		modifiedProperties.setMaxResults(properties.getMaxResults());
		modifiedProperties.setNetworkTimeout(properties.getNetworkTimeout());
		
		WebClientTool webClientTool = WebClientTool.builder(jsonParseTool, modifiedProperties)
			.httpHeadersConsumer(httpHeaders -> {
				httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
				httpHeaders.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
				httpHeaders.add(HttpHeaders.REFERER, modifiedProperties.getBaseUrl());
			})
			.build();
		return new NationalStatisticsService(jsonParseTool, webClientTool);
	}

} 