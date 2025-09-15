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
package com.alibaba.cloud.ai.toolcalling.wikipedia;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Makoto
 */
public class WikipediaService implements SearchService, Function<WikipediaService.Request, WikipediaService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(WikipediaService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final WikipediaProperties properties;

	public WikipediaService(WebClientTool webClientTool, JsonParseTool jsonParseTool, WikipediaProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(new Request(query, properties.getLimit(), false));
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.query())) {
			logger.error("Invalid request: query is required.");
			return new Response("错误：搜索查询不能为空", new ArrayList<>(), properties.getLanguage());
		}

		try {
			logger.info("Searching Wikipedia for: {}", request.query());

			// Search for pages
			String path = "w/api.php";
			MultiValueMap<String, String> searchParams = CommonToolCallUtils.<String, String>multiValueMapBuilder()
				.add("action", "query")
				.add("format", "json")
				.add("list", "search")
				.add("srsearch", request.query())
				.add("srlimit", String.valueOf(request.limit() > 0 ? request.limit() : properties.getLimit()))
				.add("srprop", "snippet|titlesnippet|size|timestamp")
				.build();

			String searchJsonResponse = webClientTool.get(path, searchParams).block();
			Map<String, Object> searchResult = jsonParseTool.jsonToObject(searchJsonResponse, new TypeReference<>() {
			});

			List<WikiPage> pages = parseSearchResults(searchResult);

			if (pages.isEmpty()) {
				return new Response("未找到相关的Wikipedia页面", new ArrayList<>(), properties.getLanguage());
			}

			// Get content for the top results
			if (request.includeContent()) {
				// Only get detailed content of the first 3 pages
				enrichPagesWithContent(pages.subList(0, Math.min(3, pages.size())));
			}

			String summary = String.format("找到 %d 个相关页面", pages.size());
			return new Response(summary, pages, properties.getLanguage());

		}
		catch (Exception e) {
			logger.error("Failed to search Wikipedia: {}", e.getMessage(), e);
			return new Response("搜索Wikipedia时发生错误: " + e.getMessage(), new ArrayList<>(), properties.getLanguage());
		}
	}

	private List<WikiPage> parseSearchResults(Map<String, Object> searchResult) {
		List<WikiPage> pages = new ArrayList<>();

		try {
			Map<String, Object> query = (Map<String, Object>) searchResult.get("query");
			if (query != null) {
				List<Map<String, Object>> searchResults = (List<Map<String, Object>>) query.get("search");
				if (searchResults != null) {
					for (Map<String, Object> result : searchResults) {
						String title = (String) result.get("title");
						String snippet = (String) result.get("snippet");
						Integer pageId = (Integer) result.get("pageid");
						Integer size = (Integer) result.get("size");
						String timestamp = (String) result.get("timestamp");

						// Clean HTML tags from snippet
						if (snippet != null) {
							snippet = snippet.replaceAll("<[^>]*>", "");
						}

						pages.add(new WikiPage(title, snippet, null, pageId, size, timestamp));
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Error parsing search results: {}", e.getMessage());
		}

		return pages;
	}

	private void enrichPagesWithContent(List<WikiPage> pages) {
		for (WikiPage page : pages) {
			try {
				String path = "w/api.php";
				MultiValueMap<String, String> contentParams = CommonToolCallUtils.<String, String>multiValueMapBuilder()
					.add("action", "query")
					.add("format", "json")
					.add("prop", "extracts")
					.add("pageids", String.valueOf(page.pageId()))
					.add("exintro", "true")
					.add("explaintext", "true")
					.add("exchars", String.valueOf(properties.getExcerptLength()))
					.build();

				String contentJsonResponse = webClientTool.get(path, contentParams).block();
				Map<String, Object> contentResult = jsonParseTool.jsonToObject(contentJsonResponse,
						new TypeReference<>() {
						});

				String content = extractPageContent(contentResult, page.pageId());
				if (content != null) {
					pages.set(pages.indexOf(page), new WikiPage(page.title(), page.snippet(), content, page.pageId(),
							page.size(), page.timestamp()));
				}
			}
			catch (Exception e) {
				logger.warn("Failed to get content for page {}: {}", page.title(), e.getMessage());
			}
		}
	}

	private String extractPageContent(Map<String, Object> contentResult, Integer pageId) {
		try {
			Map<String, Object> query = (Map<String, Object>) contentResult.get("query");
			if (query != null) {
				Map<String, Object> pages = (Map<String, Object>) query.get("pages");
				if (pages != null) {
					Map<String, Object> pageData = (Map<String, Object>) pages.get(String.valueOf(pageId));
					if (pageData != null) {
						return (String) pageData.get("extract");
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Error extracting page content: {}", e.getMessage());
		}
		return null;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Wikipedia搜索请求")
	public record Request(@JsonProperty(required = true) @JsonPropertyDescription("搜索查询关键词") String query,
			@JsonProperty(defaultValue = "5") @JsonPropertyDescription("返回结果数量限制，默认5") int limit,
			@JsonProperty(defaultValue = "false") @JsonPropertyDescription("是否包含页面详细内容，默认false") boolean includeContent)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.query();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Wikipedia搜索响应")
	public record Response(@JsonProperty @JsonPropertyDescription("搜索结果摘要") String summary,
			@JsonProperty @JsonPropertyDescription("搜索到的页面列表") List<WikiPage> pages,
			String language) implements SearchService.Response {

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.pages()
				.stream()
				.map(page -> new SearchService.SearchContent(page.title(),
						// Use content (if available) or snippet as content
						page.content() != null ? page.content() : page.snippet(),
						// Wikipedia页面URL - 构建基于页面ID的URL更可靠
						buildWikipediaUrl(page.title()), null // Wikipedia没有特定图标
				))
				.toList());
		}

		private String buildWikipediaUrl(String title) {
			if (title == null) {
				return "https://" + (language != null ? language : "zh") + ".wikipedia.org/";
			}
			// URL编码处理特殊字符
			String encodedTitle = title.replace(" ", "_").replace("(", "%28").replace(")", "%29");
			return "https://" + (language != null ? language : "zh") + ".wikipedia.org/wiki/" + encodedTitle;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Wikipedia页面信息")
	public record WikiPage(@JsonProperty @JsonPropertyDescription("页面标题") String title,
			@JsonProperty @JsonPropertyDescription("页面摘要片段") String snippet,
			@JsonProperty @JsonPropertyDescription("页面详细内容（如果请求）") String content,
			@JsonProperty @JsonPropertyDescription("页面ID") Integer pageId,
			@JsonProperty @JsonPropertyDescription("页面大小（字节）") Integer size,
			@JsonProperty @JsonPropertyDescription("最后修改时间") String timestamp) {
	}

}
