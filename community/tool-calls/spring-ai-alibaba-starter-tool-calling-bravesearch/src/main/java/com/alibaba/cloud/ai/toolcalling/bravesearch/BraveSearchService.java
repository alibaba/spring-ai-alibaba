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
package com.alibaba.cloud.ai.toolcalling.bravesearch;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Document:
 * https://api-dashboard.search.brave.com/app/documentation/web-search/get-started
 *
 * @author vlsmb
 */
public class BraveSearchService
		implements SearchService, Function<BraveSearchService.Request, BraveSearchService.Response> {

	private static final Logger log = LoggerFactory.getLogger(BraveSearchService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public BraveSearchService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(new BraveSearchService.Request(query));
	}

	@Override
	public Response apply(Request request) {
		try {
			String responseStr = webClientTool.get("/", MultiValueMap.fromSingleValue(Map.of("q", request.query())))
				.block();
			return jsonParseTool.jsonToObject(responseStr, new TypeReference<BraveSearchService.Response>() {
			});
		}
		catch (Exception e) {
			log.error("BraveSearchService apply exception: ", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Request Object. Currently, only the most basic queries are implemented
	 */
	@JsonClassDescription("Brave Search API request")
	public record Request(
			@JsonProperty(required = true, value = "query") @JsonPropertyDescription("The search query") String query)
			implements
				SearchService.Request {
		@Override
		public String getQuery() {
			return this.query();
		}
	}

	/**
	 * Response Object. Currently, only the most basic queries are implemented
	 */
	@JsonClassDescription("Brave Search API response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("type") String type,
			@JsonProperty("web") @JsonPropertyDescription("Web search results relevant to the query.") Response.Search web)
			implements
				SearchService.Response {

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.web()
				.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.description(), item.url()))
				.toList());
		}

		public record SearchResult(@JsonProperty("title") String title, @JsonProperty("url") String url,
				@JsonProperty("description") String description) {

		}

		public record Search(@JsonProperty("type") String type,
				@JsonProperty("results") List<Response.SearchResult> results) {

		}
	}

}
