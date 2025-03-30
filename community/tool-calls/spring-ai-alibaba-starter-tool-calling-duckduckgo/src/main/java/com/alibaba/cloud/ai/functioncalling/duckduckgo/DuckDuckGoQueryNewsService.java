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
package com.alibaba.cloud.ai.functioncalling.duckduckgo;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Function;

/**
 * @author 北极星
 */
public class DuckDuckGoQueryNewsService
		implements Function<DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest, Map<String, Object>> {

	private static final Logger logger = LoggerFactory.getLogger(DuckDuckGoQueryNewsService.class);

	private final WebClient webClient;

	private final DuckDuckGoProperties properties;

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	private static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	public DuckDuckGoQueryNewsService(DuckDuckGoProperties properties) {
		this.webClient = WebClient.builder()
			.defaultHeader("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " + "AppleWebKit/537.36 "
							+ "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE))
			.build();
		this.properties = properties;
	}

	@Override
	public Map<String, Object> apply(DuckDuckGoQueryNewsRequest request) {
		try {
			return webClient.method(HttpMethod.GET)
				.uri(uriBuilder -> uriBuilder.queryParam("secret_api_key", properties.getApiKey())
					.queryParam("engine", "duckduckgo_news")
					.queryParam("q", request.q())
					.queryParam("kl", request.kl())
					.build())
				.retrieve()
				.bodyToMono(Map.class)
				.block();
		}
		catch (RuntimeException e) {
			logger.error("failed to invoke duckduckgo search, caused by:{}", e.getMessage());
			return Map.of();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("duckduckgo search request")
	record DuckDuckGoQueryNewsRequest(
			@JsonProperty(required = true,
					value = "q") @JsonPropertyDescription("The query " + "keyword e.g. spring-ai-alibaba") String q,
			@JsonProperty(defaultValue = "en-us") String kl) {
	}

}
