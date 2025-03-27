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
package com.alibaba.cloud.ai.toolcalling.bingsearch;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author KrakenZJC
 **/
public class BingSearchService implements Function<BingSearchService.Request, BingSearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BingSearchService.class);

	private static final String BING_SEARCH_HOST_URL = "https://api.bing.microsoft.com";

	private static final String BING_SEARCH_PATH = "/v7.0/search";

	private final WebClient webClient;

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	private static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	public BingSearchService(BingSearchProperties properties) {
		assert StringUtils.hasText(properties.getToken()) && properties.getToken().length() == 32;
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT,
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
			.defaultHeader(BingSearchProperties.OCP_APIM_SUBSCRIPTION_KEY, properties.getToken())
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE))
			.build();
	}

	@Override
	public BingSearchService.Response apply(BingSearchService.Request request) {
		if (request == null || !StringUtils.hasText(request.query)) {
			return null;
		}

		String url = BING_SEARCH_HOST_URL + BING_SEARCH_PATH + "?responseFilter=webPages&q="
				+ URLEncoder.encode(request.query, StandardCharsets.UTF_8);

		try {
			Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
			String responseData = responseMono.block();
			assert responseData != null;
			logger.info("bing search: {},result:{}", request.query, responseData);

			Gson gson = new Gson();
			Map<String, Object> responseMap = gson.fromJson(responseData, new TypeToken<Map<String, Object>>() {
			}.getType());
			if (responseMap.containsKey("webPages")) {
				Map<String, Object> webPages = (Map<String, Object>) responseMap.get("webPages");
				if (webPages.containsKey("value")) {
					List<Map<String, Object>> valueList = (List<Map<String, Object>>) webPages.get("value");
					if (!valueList.isEmpty()) {
						Map<String, Object> value = valueList.get(0);
						if (value.containsKey("snippet")) {
							String snippet = (String) value.get("snippet");
							return new Response(snippet);
						}
					}
				}
			}
			return new Response(responseData);
		}
		catch (Exception e) {
			logger.error("failed to invoke bing search caused by:{}", e.getMessage());
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Bing search API request")
	public record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query) {

	}

	/**
	 * Bing search Function response.
	 */
	@JsonClassDescription("Bing search API response")
	public record Response(String data) {

	}

}
