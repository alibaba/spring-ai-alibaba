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
package com.alibaba.cloud.ai.toolcalling.yuque;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
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
@ConditionalOnProperty(prefix = YuqueConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@ConditionalOnClass
@EnableConfigurationProperties(YuqueProperties.class)
public class YuqueAutoConfiguration {

	@Bean(name = YuqueConstants.CREATE_BOOK_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to create a book.")
	public YuqueCreateBookService createYuqueBook(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueCreateBookService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.QUERY_BOOK_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to query a book.")
	public YuqueQueryBookService queryYuqueBook(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueQueryBookService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.UPDATE_BOOK_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to update a book.")
	public YuqueUpdateBookService updateYuqueBook(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueUpdateBookService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.DELETE_BOOK_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to delete a book.")
	public YuqueDeleteBookService deleteYuqueBook(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueDeleteBookService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.CREATE_DOC_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to create a doc.")
	public YuqueCreateDocService createYuqueDoc(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueCreateDocService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.QUERY_DOC_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to query a doc.")
	public YuqueQueryDocService queryYuqueDoc(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueQueryDocService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.UPDATE_DOC_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to update your doc.")
	public YuqueUpdateDocService updateDocService(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueUpdateDocService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

	@Bean(name = YuqueConstants.DELETE_DOC_TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Use yuque api to invoke a http request to delete your doc.")
	public YuqueDeleteDocService deleteDocService(YuqueProperties yuqueProperties, JsonParseTool jsonParseTool) {
		return new YuqueDeleteDocService(WebClientTool.builder(jsonParseTool, yuqueProperties)
			.httpHeadersConsumer(headers -> headers.set("X-Auth-Token", yuqueProperties.getToken()))
			.build(), jsonParseTool);
	}

}
