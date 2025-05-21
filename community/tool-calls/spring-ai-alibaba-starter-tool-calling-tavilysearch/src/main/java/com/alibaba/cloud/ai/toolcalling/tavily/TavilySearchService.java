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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * TavilySearch Service
 *
 * @author Allen Hu
 */
public class TavilySearchService implements Function<TavilySearchService.Request, TavilySearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(TavilySearchService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	public TavilySearchService(JsonParseTool jsonParseTool, WebClientTool webClientTool) {
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
	}

	@Override
	public TavilySearchService.Response apply(TavilySearchService.Request request) {
		if (request == null || !StringUtils.hasText(request.query())) {
			return null;
		}

		try {
			String responseData = webClientTool.post("search", request).block();
			return jsonParseTool.jsonToObject(responseData, Response.class);
		}
		catch (Exception ex) {
			logger.error("tavily search error: {}", ex.getMessage());
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(@JsonProperty("query") String query, @JsonProperty("topic") String topic,
			@JsonProperty("search_depth") String searchDepth,
			@JsonProperty("chunks_per_source") Integer chunksPerSource, @JsonProperty("max_results") Integer maxResults,
			@JsonProperty("time_range") String timeRange, @JsonProperty("days") Integer days,
			@JsonProperty("include_answer") Boolean includeAnswer,
			@JsonProperty("include_raw_content") Boolean includeRawContent,
			@JsonProperty("include_images") Boolean includeImages,
			@JsonProperty("include_image_descriptions") Boolean includeImageDescriptions,
			@JsonProperty("include_domains") List<String> includeDomains,
			@JsonProperty("exclude_domains") List<String> excludeDomains) implements Serializable {

		public static Request simpleQuery(String query) {
			return new Request(query, null, null, null, null, null, null, null, null, null, null, null, null);
		}
	}

	public record ResponseImage(@JsonProperty("url") String url,
			@JsonProperty("description") String description) implements Serializable {
	}

	public record ResponseResult(@JsonProperty("title") String title, @JsonProperty("url") String url,
			@JsonProperty("content") String content, @JsonProperty("score") Double score,
			@JsonProperty("raw_content") String rawContent) implements Serializable {
	}

	public record ResponseErrorDetail(@JsonProperty("error") String error) implements Serializable {
	}

	public record Response(@JsonProperty("query") String query, @JsonProperty("answer") String answer,
			@JsonProperty("images") List<ResponseImage> images, @JsonProperty("results") List<ResponseResult> results,
			@JsonProperty("response_time") String responseTime,
			@JsonProperty("detail") ResponseErrorDetail detail) implements Serializable {
	}

}
