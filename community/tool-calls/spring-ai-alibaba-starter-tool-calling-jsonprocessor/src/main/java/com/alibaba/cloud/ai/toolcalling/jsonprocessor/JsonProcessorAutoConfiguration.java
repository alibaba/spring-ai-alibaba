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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@Configuration
@EnableConfigurationProperties(JsonProcessorProperties.class)
@ConditionalOnClass({ JsonProcessorInsertService.class, JsonProcessorRemoveService.class,
		JsonProcessorReplaceService.class, JsonProcessorParseService.class })
@ConditionalOnProperty(value = JsonProcessorConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class JsonProcessorAutoConfiguration {

	@Bean(name = JsonProcessorConstants.INSERT_TOOL_NAME)
	@Description("Use Gson to insert a jsonObject property field .")
	@ConditionalOnMissingBean
	public JsonProcessorInsertService jsonInsertPropertyField(JsonParseTool jsonParseTool) {
		return new JsonProcessorInsertService(jsonParseTool);
	}

	@Bean(name = JsonProcessorConstants.PARSE_TOOL_NAME)
	@Description("Use Gson to parse String JsonObject .")
	@ConditionalOnMissingBean
	public JsonProcessorParseService jsonParseProperty(JsonParseTool jsonParseTool) {
		return new JsonProcessorParseService(jsonParseTool);
	}

	@Bean(name = JsonProcessorConstants.REMOVE_TOOL_NAME)
	@Description("Use Gson to remove JsonObject property field .")
	@ConditionalOnMissingBean
	public JsonProcessorRemoveService jsonRemovePropertyField(JsonParseTool jsonParseTool) {
		return new JsonProcessorRemoveService(jsonParseTool);
	}

	@Bean(name = JsonProcessorConstants.REPLACE_TOOL_NAME)
	@Description("Use Gson to replace JsonObject Field Value .")
	@ConditionalOnMissingBean
	public JsonProcessorReplaceService jsonReplacePropertyFiledValue(JsonParseTool jsonParseTool) {
		return new JsonProcessorReplaceService(jsonParseTool);
	}

}
