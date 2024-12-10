/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.crawler;

import com.alibaba.cloud.ai.plugin.crawler.service.impl.CrawlerFirecrawlServiceImpl;
import com.alibaba.cloud.ai.plugin.crawler.service.impl.CrawlerJinaServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.util.Assert;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Configuration
@EnableConfigurationProperties({ CrawlerJinaProperties.class, CrawlerFirecrawlProperties.class })
public class CrawlerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Jina Reader Service Plugin")
	@ConditionalOnProperty(prefix = CrawlerJinaProperties.JINA_PROPERTIES_PREFIX, name = "enabled",
			havingValue = "true")
	public CrawlerJinaServiceImpl jinaService(CrawlerJinaProperties jinaProperties, ObjectMapper objectMapper) {

		Assert.notNull(jinaProperties, "Jina reader api token must not be empty");
		return new CrawlerJinaServiceImpl(jinaProperties, objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("Firecrawl Service Plugin")
	@ConditionalOnProperty(prefix = CrawlerJinaProperties.JINA_PROPERTIES_PREFIX, name = "enabled",
			havingValue = "true")
	public CrawlerFirecrawlServiceImpl firecrawlService(CrawlerFirecrawlProperties firecrawlProperties,
			ObjectMapper objectMapper) {

		Assert.notNull(firecrawlProperties.getToken(), "Firecrawl api token must not be empty");
		return new CrawlerFirecrawlServiceImpl(firecrawlProperties, objectMapper);
	}

}
