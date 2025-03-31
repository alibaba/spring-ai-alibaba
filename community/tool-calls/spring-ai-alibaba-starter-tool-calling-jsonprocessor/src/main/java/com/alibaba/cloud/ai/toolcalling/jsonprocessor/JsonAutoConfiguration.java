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
package com.alibaba.cloud.ai.toolcalling.jsonprocessor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@Configuration
@ConditionalOnClass({ JsonInsertService.class, JsonRemoveService.class, JsonReplaceService.class,
		JsonParseService.class })
@ConditionalOnProperty(value = "spring.ai.alibaba.toolcalling.jsonprocessor", name = "enabled", havingValue = "true")
public class JsonAutoConfiguration {

	@Bean
	@Description("use Gson to insert a jsonObject property field .")
	@ConditionalOnMissingBean
	public JsonInsertService jsonInsertPropertyFieldFunction() {
		return new JsonInsertService();
	}

	@Bean
	@Description("use Gson to parse String JsonObject .")
	@ConditionalOnMissingBean
	public JsonParseService jsonParsePropertyFunction() {
		return new JsonParseService();
	}

	@Bean
	@Description("use Gson to remove JsonObject property field .")
	@ConditionalOnMissingBean
	public JsonRemoveService jsonRemovePropertyFieldFunction() {
		return new JsonRemoveService();
	}

	@Bean
	@Description("use Gson to replace JsonObject Field Value .")
	@ConditionalOnMissingBean
	public JsonReplaceService jsonReplacePropertyFiledValueFunction() {
		return new JsonReplaceService();
	}

}
