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

package com.alibaba.cloud.ai.plugin.serpapi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnClass(SerpApiService.class)
@EnableConfigurationProperties(SerpApiProperties.class)
public class SerpApiAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Use SerpApi search to query for the latest news.")
	@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.serpapi", name = "enabled", havingValue = "true")
	public SerpApiService serpApiService(SerpApiProperties properties) {
		return new SerpApiService(properties);
	}

}
