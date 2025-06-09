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
package com.alibaba.cloud.ai.toolcalling.microsofttranslate;

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

/**
 * @author 31445
 */
@Configuration
@ConditionalOnClass(MicroSoftTranslateService.class)
@EnableConfigurationProperties(MicroSoftTranslateProperties.class)
@ConditionalOnProperty(prefix = MicroSoftTranslateConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class MicroSoftTranslateAutoConfiguration {

	@Bean(name = MicroSoftTranslateConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Implement natural language translation capabilities.")
	public MicroSoftTranslateService microSoftTranslateFunction(MicroSoftTranslateProperties properties,
			JsonParseTool jsonParseTool) {
		WebClientTool webClientTool = WebClientTool.builder(jsonParseTool, properties)
			.httpHeadersConsumer((headers) -> {
				headers.add("Ocp-Apim-Subscription-Key", properties.getApiKey());
				headers.set("Ocp-Apim-Subscription-Region", properties.getRegion());
				headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
			})
			.build();
		return new MicroSoftTranslateService(webClientTool, jsonParseTool);
	}

}
