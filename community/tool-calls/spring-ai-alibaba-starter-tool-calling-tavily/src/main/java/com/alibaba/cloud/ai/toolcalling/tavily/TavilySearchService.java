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
package com.alibaba.cloud.ai.toolcalling.tavily;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Function;

public class TavilySearchService implements Function<TavilySearchSchema.Request, TavilySearchSchema.Response> {

	private static final Logger logger = LoggerFactory.getLogger(TavilySearchService.class);

	private static final String TAVILY_SEARCH_API = "https://api.tavily.com/search";

	private final WebClient webClient;

	public TavilySearchService(TavilySearchProperties properties) {
		final Map<String, String> headers = Map.of("Authorization", "Bearer " + properties.getToken(), "Content-Type",
				"application/json");
		this.webClient = CommonToolCallUtils.buildWebClient(headers,
				CommonToolCallConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS,
				CommonToolCallConstants.DEFAULT_RESPONSE_TIMEOUT_SECONDS, CommonToolCallConstants.MAX_MEMORY_SIZE);
	}

	@Override
	public TavilySearchSchema.Response apply(TavilySearchSchema.Request request) {
		if (request == null || !StringUtils.hasText(request.query())) {
			return null;
		}

		try {
			return webClient.post()
				.uri(TAVILY_SEARCH_API)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(TavilySearchSchema.Response.class)
				.block();

		}
		catch (Exception ex) {
			logger.error("tavily search error: {}", ex.getMessage());
			return null;
		}
	}

}
