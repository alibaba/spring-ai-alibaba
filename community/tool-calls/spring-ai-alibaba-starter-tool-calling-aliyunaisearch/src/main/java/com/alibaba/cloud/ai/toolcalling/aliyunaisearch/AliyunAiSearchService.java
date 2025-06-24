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
package com.alibaba.cloud.ai.toolcalling.aliyunaisearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Aliyun AI Web Search
 * https://opensearch.console.aliyun.com/cn-shanghai/rag/server-market/detail?serverType=web-search
 *
 * @author vlsmb
 */
public class AliyunAiSearchService
		implements SearchService, Function<AliyunAiSearchService.Request, AliyunAiSearchService.Response> {

	private static final Logger log = LoggerFactory.getLogger(AliyunAiSearchService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final AliyunAiSearchProperties properties;

	public AliyunAiSearchService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			AliyunAiSearchProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simplyQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!CommonToolCallUtils.isValidUrl(properties.getBaseUrl())) {
			throw new RuntimeException("Service Base Url is Invalid.");
		}
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("Service Api Key is Invalid.");
		}
		try {
			String responseStr = webClientTool.post("/", request).block();
			log.debug("Response: {}", responseStr);
			return jsonParseTool.getFieldValue(responseStr, new TypeReference<Response>() {
			}, "result");
		}
		catch (Exception e) {
			log.error("Service AliyunAiSearch Request Error: ", e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("Aliyun AI Web Search Request. If you're unsure what to enter, fill in the default value.")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty(required = true, value = "query") @JsonPropertyDescription("Search query") String query,
			@JsonProperty(value = "way",
					defaultValue = "fast") @JsonPropertyDescription("Search result filtering modes: `normal` (applies vector-based filtering to results), `fast` (performs no vector-based filtering on results), `full` (uses large models to conduct evaluation and filtering of results).") String way,
			@JsonProperty(value = "query_rewrite",
					defaultValue = "true") @JsonPropertyDescription("Whether to enable LLM-based query rewriting. (Default value: `true`)") Boolean isRewrite,
			@JsonProperty(value = "top_k",
					defaultValue = "5") @JsonPropertyDescription("The number of search results returned. Default: 5") Integer topK,
			@JsonProperty(value = "history",
					defaultValue = "null") @JsonPropertyDescription("The conversation history between user and model uses a list of {\"role\": role, \"content\": content} elements, where role accepts `system`, `user`, or `assistant`; the optional system role can only appear as the first message (messages[0]) if present, while user and assistant roles must strictly alternate throughout the dialogue to simulate real conversation flow.") List<History> history,
			@JsonProperty(value = "content_type",
					defaultValue = "snippet") @JsonPropertyDescription("Search result content types: `snippet` (a brief description of webpage content) and `summary` (a text summary of webpage content, which takes longer to generate than a snippet).") String contentType)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.query();
		}

		public record History(
				@JsonProperty(value = "role",
						defaultValue = "system") @JsonPropertyDescription("Role: system, user, assistant") String role,
				@JsonProperty(value = "content") @JsonPropertyDescription("content") String content) {

		}

		public static Request simplyQuery(String query) {
			return new Request(query, "fast", true, 5, null, "snippet");
		}
	}

	@JsonClassDescription("Aliyun AI Web Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(
			@JsonProperty("search_result") List<SearchResult> results) implements SearchService.Response {

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.content(), item.link()))
				.toList());
		}

		public record SearchResult(String title, String link, String snippet, String content, Integer position) {

		}
	}

}
