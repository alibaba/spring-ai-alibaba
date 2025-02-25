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
package com.alibaba.cloud.ai.toolcalling.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.util.Assert;

/**
 * @author yuluo
 */
@EnableConfigurationProperties({ CrawlerJinaProperties.class, CrawlerFirecrawlProperties.class })
@ConditionalOnProperty(prefix = CrawlerJinaProperties.JINA_PROPERTIES_PREFIX, name = "enabled", havingValue = "true")
public class CrawlerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Jina Reader Service Plugin.")
	public CrawlerJinaServiceImpl jinaFunction(CrawlerJinaProperties jinaProperties, ObjectMapper objectMapper) {

		Assert.notNull(jinaProperties, "Jina reader api token must not be empty");
		return new CrawlerJinaServiceImpl(jinaProperties, objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("Firecrawl Service Plugin.")
	public CrawlerFirecrawlServiceImpl firecrawlFunction(CrawlerFirecrawlProperties firecrawlProperties,
			ObjectMapper objectMapper) {

		Assert.notNull(firecrawlProperties.getToken(), "Firecrawl api token must not be empty");
		return new CrawlerFirecrawlServiceImpl(firecrawlProperties, objectMapper);
	}

}
