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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.alibaba.cloud.ai.toolcalling.bingsearch.BingSearchProperties.BING_SEARCH_PATH;

/**
 * @author KrakenZJC
 **/
public class BingSearchService implements Function<BingSearchService.Request, BingSearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BingSearchService.class);

	private final WebClientTool webClientTool;

	private final BingSearchProperties properties;

	private final JsonParseTool jsonParseTool;

	public BingSearchService(WebClientTool webClientTool, BingSearchProperties properties,
			JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.properties = properties;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public BingSearchService.Response apply(BingSearchService.Request request) {
		if (request == null || !StringUtils.hasText(request.query)) {
			return null;
		}
		try {
			Mono<String> responseMono = webClientTool.get(BING_SEARCH_PATH,
					MultiValueMap.fromSingleValue(Map.of("responseFilter", "webPages", "q", request.query)));
			String responseData = responseMono.block();
			assert responseData != null;
			logger.info("bing search: {},result:{}", request.query, responseData);

			try {
				String valueListStr = jsonParseTool.getDepthFieldValueAsString(responseData, "webPages", "value");
				String value = jsonParseTool.getFirstElementFromJsonArrayString(valueListStr);
				if (!StringUtils.hasText(value)) {
					throw new RuntimeException("bing search result is empty");
				}
				return new Response(jsonParseTool.getFieldValueAsString(value, "snippet"));
			}
			catch (Exception ignored) {
				return new Response(responseData);
			}
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
