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
package com.alibaba.cloud.ai.toolcalling.baidusearch;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * baidu AI Search
 * <a href="https://cloud.baidu.com/doc/AppBuilder/s/pmaxd1hvy">qianfan</a> This version
 * is relatively stable, but requires apiKey configuration, with 100 free queries per day
 *
 * @author HunterPorter
 * @author <a href="mailto:zongpeng_hzp@163.com">HunterPorter</a>
 */
public class BaiduAiSearchService
		implements SearchService, Function<BaiduAiSearchService.Request, BaiduAiSearchService.Response> {

	private static final Logger log = LoggerFactory.getLogger(BaiduAiSearchService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final BaiduAiSearchProperties properties;

	public BaiduAiSearchService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			BaiduAiSearchProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response query(String query) {
		return this.apply(Request.simplyQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(properties.getApiKey())) {
			throw new RuntimeException("Service Api Key is Invalid.");
		}
		try {
			String responseStr = webClientTool.post("/v2/ai_search/chat/completions", request).block();
			log.debug("Response: {}", responseStr);
			return jsonParseTool.jsonToObject(responseStr, Response.class);
		}
		catch (Exception e) {
			log.error("Service Baidu AI Request Error: ", e);
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("Return real-time search results from web and other data sources based on questions or keywords")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(@JsonProperty(required = true,
			value = "messages") @JsonPropertyDescription("Search input, including user query content") List<Message> messages,
			@JsonProperty(value = "edition",
					defaultValue = "standard") @JsonPropertyDescription("""
							Search version. Default is standard.
							Optional values:
							standard: Full version.
							lite: Standard version, a simplified version of the full version with better latency performance, but slightly weaker effect.""") String edition,
			@JsonProperty(value = "search_source",
					defaultValue = "baidu_search_v2") @JsonPropertyDescription("Search engine version used; fixed value: baidu_search_v2") String searchSource,
			@JsonProperty(
					value = "resource_type_filter") @JsonPropertyDescription("""
							Support setting web, video, image, and Aladdin search modalities. The maximum value of top_k for web is 50, for video is 10, for image is 30, and for Aladdin is 5. The default value is:
							[{"type": "web","top_k": 20},{"type": "video","top_k": 0},{"type": "image","top_k": 0},{"type": "aladdin","top_k": 0}]
							Notes when using Aladdin:
							1. Aladdin does not support site and timeliness filtering.
							2. It is recommended to use it with web modality to increase the number of search returns.
							3. Aladdin's return parameters are in beta version and may change in the future.""") List<SearchResource> resourceTypeFilter,
			@JsonProperty(
					value = "search_filter") @JsonPropertyDescription("Filter retrieval based on conditions") SearchFilter searchFilter,
			@JsonProperty(
					value = "block_websites") @JsonPropertyDescription("List of sites to be blocked") List<String> blockWebsites,
			@JsonProperty(value = "search_recency_filter") @JsonPropertyDescription("""
					Filter by web page publication time.
					Enumeration values:
					week: Last 7 days
					month: Last 30 days
					semiyear: Last 180 days
					year: Last 365 days""") String searchRecencyFilter) implements SearchService.Request {

		@Override
		public String getQuery() {
			if (messages != null && !messages.isEmpty()) {
				Message lastMessage = messages.get(messages.size() - 1);
				if ("user".equals(lastMessage.role)) {
					return lastMessage.content;
				}
			}
			return null;
		}

		public static Request simplyQuery(String query) {
			return new Request(List.of(new Message("user", query)), "standard", "baidu_search_v2",
					List.of(new SearchResource("web", 20)), null, null, null);
		}
	}

	public record Message(
			@JsonProperty("role") @JsonPropertyDescription("Role setting, optional values: user: user; assistant: model") String role,
			@JsonProperty("content") @JsonPropertyDescription("""
					When content is text, it corresponds to the dialog content, that is, the user's query question. Notes:
					1. Cannot be empty.
					2. In multi-round conversations, the last user input content cannot be empty characters, such as spaces, "\\n", "\\r", "\\f", etc.""") String content) {
	}

	public record SearchResource(@JsonProperty("type") @JsonPropertyDescription("""
			Search resource type. Optional values:
			web: Web page
			video: Video
			image: Image
			aladdin: Aladdin""") String type,
			@JsonProperty("top_k") @JsonPropertyDescription("Specify the maximum number of returns for the modality.") Integer topK) {
	}

	public record SearchFilter(@JsonProperty("match") @JsonPropertyDescription("Site condition query") Match match,
			@JsonProperty("range") @JsonPropertyDescription("Time range query") Range range) {
	}

	public record Match(
			@JsonProperty("site") @JsonPropertyDescription("Support setting search conditions for specified sites, that is, content search only in the set sites. Currently supports setting 20 sites. Example: [\"tieba.baidu.com\"]") List<String> site) {
	}

	public record Range(@JsonProperty("page_time") PageTime pageTime) {
	}

	public record PageTime(
			@JsonProperty("gte") @JsonPropertyDescription("Time query parameter, greater than or equal to. Supported time units: y (year), M (month), w (week), d (day), for example \"now-1w/d\", one week ago, rounded down") String gte,
			@JsonProperty("gt") @JsonPropertyDescription("Time query parameter, greater than. Supported time units: y (year), M (month), w (week), d (day), for example \"now-1w/d\", one week ago, rounded up") String gt,
			@JsonProperty("lte") @JsonPropertyDescription("Time query parameter, less than or equal to. Supported time units: y (year), M (month), w (week), d (day), for example \"now-1w/d\", one week ago, rounded up") String lte,
			@JsonProperty("lt") @JsonPropertyDescription("Time query parameter, less than. Supported time units: y (year), M (month), w (week), d (day), for example \"now-1w/d\", one week ago, rounded down") String lt) {
	}

	@JsonClassDescription("Baidu AI Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("requestId") @JsonPropertyDescription("Request ID") String requestId,
			@JsonProperty("code") @JsonPropertyDescription("Error code, returned when an exception occurs") String code,
			@JsonProperty("message") @JsonPropertyDescription("Error message, returned when an exception occurs") String message,
			@JsonProperty("references") @JsonPropertyDescription("Search result list") List<Reference> references)
			implements
				SearchService.Response {

		@Override
		public SearchResult getSearchResult() {
			if (references == null || references.isEmpty()) {
				return new SearchResult(List.of());
			}

			return new SearchResult(this.references()
				.stream()
				.map(item -> new SearchContent(item.title(), item.content(), item.url(), null))
				.toList());
		}

		public record Reference(@JsonProperty("icon") @JsonPropertyDescription("Website icon address") String icon,
				@JsonProperty("id") @JsonPropertyDescription("Reference number") Integer id,
				@JsonProperty("title") @JsonPropertyDescription("Title") String title,
				@JsonProperty("url") @JsonPropertyDescription("URL") String url,
				@JsonProperty("web_anchor") @JsonPropertyDescription("Anchor") String webAnchor,
				@JsonProperty("website") @JsonPropertyDescription("Website name") String website,
				@JsonProperty("content") @JsonPropertyDescription("Content") String content,
				@JsonProperty("date") @JsonPropertyDescription("Date") String date,
				@JsonProperty("type") @JsonPropertyDescription("""
						Retrieval resource type. Return values:
						web: Web page
						video: Video content
						image: Image
						aladdin: Aladdin""") String type,
				@JsonProperty("image") @JsonPropertyDescription("Image information") ImageDetail image,
				@JsonProperty("video") @JsonPropertyDescription("Video information") VideoDetail video,
				@JsonProperty("is_aladdin") @JsonPropertyDescription("Whether it is Aladdin content") Boolean isAladdin,
				@JsonProperty("aladdin") @JsonPropertyDescription("Aladdin content") Object aladdin) {
		}

		public record ImageDetail(String url, String height, String width) {
		}

		public record VideoDetail(String url, String height, String width, String size, String duration,
				String hoverPic) {
		}
	}

}
