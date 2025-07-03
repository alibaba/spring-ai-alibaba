/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.alibaba.cloud.ai.service.simple.SimpleNl2SqlService;
import com.alibaba.cloud.ai.service.simple.SimpleSchemaService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhangshenghang
 */
@Configuration
public class BaseDefaultConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Nl2sqlConfiguration.class);

	@Autowired
	private DbAccessor dbAccessor;

	@Autowired
	private DbConfig dbConfig;

	@Bean("nl2SqlServiceImpl")
	@ConditionalOnMissingBean(name = "nl2SqlServiceImpl")
	public BaseNl2SqlService defaultNl2SqlService(
			@Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService,
			@Qualifier("simpleSchemaService") BaseSchemaService schemaService, LlmService aiService) {
		logger.info("Creating default BaseNl2SqlService implementation");
		return new SimpleNl2SqlService(vectorStoreService, schemaService, aiService, dbAccessor, dbConfig);
	}

	@Bean("schemaServiceImpl")
	@ConditionalOnMissingBean(name = "schemaServiceImpl")
	public BaseSchemaService defaultSchemaService(
			@Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService, DbConfig dbConfig,
			Gson gson) {
		logger.info("Creating default BaseSchemaService implementation");
		return new SimpleSchemaService(dbConfig, gson, vectorStoreService);
	}

}
