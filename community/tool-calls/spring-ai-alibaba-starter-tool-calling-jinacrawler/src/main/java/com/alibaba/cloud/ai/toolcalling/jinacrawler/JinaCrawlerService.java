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
package com.alibaba.cloud.ai.toolcalling.jinacrawler;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.function.Function;

/**
 * @author vlsmb
 */
public class JinaCrawlerService implements Function<JinaCrawlerService.Request, JinaCrawlerService.Response> {

	private static final Logger log = LoggerFactory.getLogger(JinaCrawlerService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	private final JinaCrawlerProperties properties;

	public JinaCrawlerService(JsonParseTool jsonParseTool, WebClientTool webClientTool,
			JinaCrawlerProperties properties) {
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
		this.properties = properties;
	}

	@Override
	public Response apply(Request request) {
		if (!CommonToolCallUtils.isValidUrl(request.url())) {
			throw new RuntimeException("Invalid url: " + request.url());
		}
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("Please set api key");
		}
		try {
			return new Response(webClientTool.post("/", request).block());
		}
		catch (Exception e) {
			log.error("Jina reader request failed: ", e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("get response from Jina Reader API")
	public record Request(@JsonPropertyDescription("url") String url) {
	}

	public record Response(String content) {
	}

}
