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
package com.alibaba.cloud.ai.toolcalling.tushare;

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
 * @author HunterPorter
 */
@Configuration
@EnableConfigurationProperties(TushareProperties.class)
@ConditionalOnProperty(prefix = TushareConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class TushareAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TushareAutoConfiguration.class);

	@Bean(name = TushareConstants.STOCK_QUOTES_TOOL_NAME)
	@Description("根据股票代码或(和)日期获取股票日行情，每次最多6000条")
	public TushareStockQuotesService tushareGetStockQuotes(JsonParseTool jsonParseTool,
			TushareProperties tushareProperties) {
		logger.debug("TushareStockQuotesService is enabled.");
		return new TushareStockQuotesService(WebClientTool.builder(jsonParseTool, tushareProperties).build(),
				tushareProperties);
	}

}
