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

package com.alibaba.cloud.ai.toolcalling.googletrends;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Function;

public class GoogleTrendsService implements Function<GoogleTrendsService.Request, GoogleTrendsService.Response> {

	private static final Logger log = LoggerFactory.getLogger(GoogleTrendsService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	private final GoogleTrendsProperties properties;

	public GoogleTrendsService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			GoogleTrendsProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new IllegalStateException("Please config apiKey in application config file.");
		}
		if (request == null || !StringUtils.hasText(request.query())) {
			throw new IllegalArgumentException("request is empty");
		}
		try {
			String response = webClientTool
				.get("/",
						MultiValueMap.fromSingleValue(Map.of("engine", "google_trends", "api_key",
								properties.getApiKey(), "q", request.query())))
				.block();
			return new Response(jsonParseTool.jsonToMap(response, Object.class));
		}
		catch (Exception e) {
			log.error("Google Trends service exception: {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("Google Trends Search API Request")
	public record Request(@JsonProperty(required = true,
			value = "q") @JsonPropertyDescription("Parameter defines the query or queries you want to search. You can use anything that you would use in a regular Google Trends search."
					+ "When passing multiple queries you need to use a comma `,` to separate them (e.g. `coffee,pizza,dark chocolate,bread`)."
					+ "Maximum length for each query is 100 characters.") String query) {

	}

	@JsonClassDescription("Google Trends Search API Response")
	public record Response(Map<String, Object> response) {

	}

}
