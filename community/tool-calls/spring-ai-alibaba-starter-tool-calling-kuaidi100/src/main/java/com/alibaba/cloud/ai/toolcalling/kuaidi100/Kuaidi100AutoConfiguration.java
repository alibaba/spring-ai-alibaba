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
package com.alibaba.cloud.ai.toolcalling.kuaidi100;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.RestClientTool;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author XiaoYunTao
 * @since 2024/12/18
 */
@Configuration
@ConditionalOnClass(Kuaidi100AutoConfiguration.class)
@ConditionalOnProperty(prefix = Kuaidi100Constants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(Kuaidi100Properties.class)
public class Kuaidi100AutoConfiguration {

	@Bean(name = Kuaidi100Constants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Query courier tracking information")
	public Kuaidi100Service queryTrack(Kuaidi100Properties kuaidi100Properties) {
		JsonParseTool jsonParseTool = createJsonParseTool();
		RestClientTool restClientTool = RestClientTool.builder(jsonParseTool, kuaidi100Properties).build();
		return new Kuaidi100Service(kuaidi100Properties, jsonParseTool, restClientTool);
	}

	private JsonParseTool createJsonParseTool() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return new JsonParseTool(objectMapper);
	}

}
