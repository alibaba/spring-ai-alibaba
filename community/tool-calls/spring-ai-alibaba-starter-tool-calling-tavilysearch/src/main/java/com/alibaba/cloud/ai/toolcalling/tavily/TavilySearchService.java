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
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * TavilySearch Service
 *
 * @author Allen Hu
 */
public class TavilySearchService
		implements SearchService, Function<TavilySearchService.Request, TavilySearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(TavilySearchService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	public TavilySearchService(JsonParseTool jsonParseTool, WebClientTool webClientTool) {
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simpleQuery(query));
	}

	@Override
	public TavilySearchService.Response apply(TavilySearchService.Request request) {
		if (request == null || !StringUtils.hasText(request.query())) {
			return Response.errorResponse(request != null ? request.query : "", "query is empty");
		}

		try {
			String responseData = webClientTool.post("search", request).block();
			return jsonParseTool.jsonToObject(responseData, new TypeReference<Response>() {
			});
		}
		catch (Exception ex) {
			logger.error("tavily search error: {}", ex.getMessage());
			return Response.errorResponse(request.query, ex.getMessage());
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("This is the parameter entity class for TavilySearchService; values must be strictly populated according to field requirements.")
	public record Request(
			@JsonProperty(value = "query",
					required = true) @JsonPropertyDescription("The search query to execute with Tavily.") String query,
			@JsonProperty(value = "topic",
					defaultValue = "general") @JsonPropertyDescription("The category of the search.news is useful for retrieving real-time updates, particularly about politics, sports, and major current events covered by mainstream media sources. general is for broader, more general-purpose searches that may include a wide range of sources.\n"
							+ "Available options: general, news ") String topic,
			@JsonProperty(value = "search_depth",
					defaultValue = "basic") @JsonPropertyDescription("The depth of the search. advanced search is tailored to retrieve the most relevant sources and content snippets for your query, while basic search provides generic content snippets from each source. A basic search costs 1 API Credit, while an advanced search costs 2 API Credits.\n"
							+ "Available options: basic, advanced") String searchDepth,
			@JsonProperty(value = "chunks_per_source",
					defaultValue = "3") @JsonPropertyDescription("Chunks are short content snippets (maximum 500 characters each) pulled directly from the source. Use chunks_per_source to define the maximum number of relevant chunks returned per source and to control the content length. Chunks will appear in the content field as: <chunk 1> [...] <chunk 2> [...] <chunk 3>. Available only when search_depth is advanced.\n"
							+ "Required range: 1 <= x <= 3") Integer chunksPerSource,
			@JsonProperty(value = "max_results",
					defaultValue = "5") @JsonPropertyDescription("The maximum number of search results to return.\n"
							+ "Required range: 0 <= x <= 20") Integer maxResults,
			@JsonProperty(value = "time_range",
					defaultValue = "year") @JsonPropertyDescription("The time range back from the current date to filter results. Useful when looking for sources that have published data.\n"
							+ "Available options: day," + "week, " + "month, " + "year, " + "d, " + "w, " + "m, "
							+ "y ") String timeRange,
			@JsonProperty(value = "days",
					defaultValue = "7") @JsonPropertyDescription("Number of days back from the current date to include. Available only if topic is news.\n"
							+ "Required range: x >= 1") Integer days,
			@JsonProperty(value = "include_answer",
					defaultValue = "false") @JsonPropertyDescription("Include an LLM-generated answer to the provided query. basic or true returns a quick answer. advanced returns a more detailed answer.") Boolean includeAnswer,
			@JsonProperty(value = "include_raw_content",
					defaultValue = "false") @JsonPropertyDescription("Include the cleaned and parsed HTML content of each search result.") Boolean includeRawContent,
			@JsonProperty(value = "include_images",
					defaultValue = "false") @JsonPropertyDescription("Also perform an image search and include the results in the response.") Boolean includeImages,
			@JsonProperty(value = "include_image_descriptions",
					defaultValue = "false") @JsonPropertyDescription("When include_images is true, also add a descriptive text for each image.") Boolean includeImageDescriptions,
			@JsonProperty(value = "include_domains",
					defaultValue = "[]") @JsonPropertyDescription("A list of domains to specifically include in the search results.") List<String> includeDomains,
			@JsonProperty(value = "exclude_domains",
					defaultValue = "[]") @JsonPropertyDescription("A list of domains to specifically exclude from the search results.") List<String> excludeDomains)
			implements
				Serializable,
				SearchService.Request {

		public static Request simpleQuery(String query) {
			return new Request(query, null, null, null, null, null, null, null, null, null, null, null, null);
		}

		@Override
		public String getQuery() {
			return this.query();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("query") String query, @JsonProperty("answer") String answer,
			@JsonProperty("images") List<ImageInfo> images, @JsonProperty("results") List<ResultInfo> results,
			@JsonProperty("response_time") String responseTime) implements SearchService.Response {
		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonDeserialize(using = ImageInfoDeserializer.class)
		public record ImageInfo(@JsonProperty("url") String url, @JsonProperty("description") String description) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record ResultInfo(@JsonProperty("title") String title, @JsonProperty("url") String url,
				@JsonProperty("content") String content, @JsonProperty("score") String score,
				@JsonProperty("raw_content") String raw_content) {
		}

		public static Response errorResponse(String query, String errorMsg) {
			return new Response(query, errorMsg, null, null, null);
		}

		@Override
		public SearchResult getSearchResult() {
			return new SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.content(), item.url()))
				.toList());
		}
	}

}

class ImageInfoDeserializer extends JsonDeserializer<TavilySearchService.Response.ImageInfo> {

	@Override
	public TavilySearchService.Response.ImageInfo deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.isTextual()) {
			return new TavilySearchService.Response.ImageInfo(node.asText(), null);
		}
		else if (node.isObject()) {
			String url = node.has("url") ? node.get("url").asText() : null;
			String description = node.has("description") ? node.get("description").asText() : null;
			return new TavilySearchService.Response.ImageInfo(url, description);
		}

		return null;
	}

}
