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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;

/**
 * @author KrakenZJC
 **/
public class BaiduSearchService
		implements SearchService, Function<BaiduSearchService.Request, BaiduSearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BaiduSearchService.class);

	private final WebClientTool webClientTool;

	private final BaiduSearchProperties properties;

	public BaiduSearchService(JsonParseTool jsonParseTool, BaiduSearchProperties properties,
			WebClientTool webClientTool) {
		this.webClientTool = webClientTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(new Request(query, null));
	}

	@Override
	public BaiduSearchService.Response apply(BaiduSearchService.Request request) {
		if (CommonToolCallUtils.isInvalidateRequestParams(request, request.query)) {
			return null;
		}

		return CommonToolCallUtils.handleServiceError("BaiduSearch", () -> {
			int limit = request.limit == null ? properties.getMaxResults() : request.limit;
			String url = properties.getBaseUrl() + request.query;

			String html = webClientTool.getWebClient()
				.get()
				.uri(url)
				.acceptCharset(StandardCharsets.UTF_8)
				.retrieve()
				.bodyToMono(String.class)
				.block();

			List<SearchResult> results = CommonToolCallUtils.handleResponse(html, this::parseHtml, logger);

			if (CollectionUtils.isEmpty(results)) {
				return null;
			}

			logger.info("baidu search: {},result number:{}", request.query, results.size());
			for (SearchResult d : results) {
				logger.info("{}\n{}\n{}", d.title(), d.abstractText(), d.sourceUrl());
			}
			return new Response(results.subList(0, Math.min(results.size(), limit)));
		}, logger);
	}

	private List<SearchResult> parseHtml(String htmlContent) {
		try {
			Document doc = Jsoup.parse(htmlContent);
			Element contentLeft = doc.selectFirst("div#content_left");
			Elements divContents = contentLeft.children();
			List<SearchResult> listData = new ArrayList<>();

			for (Element div : divContents) {
				if (!div.hasClass("c-container")) {
					continue;
				}
				String title = "";
				String abstractText = "";
				String sourceUrl = div.attr("mu");

				try {
					if (div.hasClass("xpath-log") || div.hasClass("result-op")) {
						if (div.selectFirst("h3") != null) {
							title = div.selectFirst("h3").text().trim();
						}
						else {
							title = div.text().trim().split("\n", 2)[0];
						}

						if (div.selectFirst("div.c-abstract") != null) {
							abstractText = div.selectFirst("div.c-abstract").text().trim();
						}
						else if (div.selectFirst("div") != null) {
							abstractText = div.selectFirst("div").text().trim();
						}
						else {
							abstractText = div.text().trim().split("\n", 2)[1].trim();
						}
					}
					else if ("se_com_default".equals(div.attr("tpl"))) {
						if (div.selectFirst("h3") != null) {
							title = div.selectFirst("h3").text().trim();
						}
						else {
							title = div.children().get(0).text().trim();
						}

						if (div.selectFirst("div.c-abstract") != null) {
							abstractText = div.selectFirst("div.c-abstract").text().trim();
						}
						else if (div.selectFirst("div") != null) {
							abstractText = div.selectFirst("div").text().trim();
						}
						else {
							abstractText = div.text().trim();
						}
					}
					else {
						continue;
					}
				}
				catch (Exception e) {
					logger.error("Failed to parse search result: {}", e.getMessage());
					continue;
				}

				listData.add(new SearchResult(title, abstractText, sourceUrl));
			}

			return listData;
		}
		catch (Exception e) {
			logger.error("Failed to parse HTML content: {}", e.getMessage());
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Baidu search API request")
	public record Request(
			@JsonProperty(required = true, value = "query") @JsonPropertyDescription("The search query") String query,
			@JsonProperty(required = false,
					value = "limit") @JsonPropertyDescription("Maximum number of results to return") Integer limit)
			implements
				SearchService.Request {
		@Override
		public String getQuery() {
			return this.query();
		}
	}

	/**
	 * Baidu search Function response.
	 */
	@JsonClassDescription("Baidu search API response")
	public record Response(List<SearchResult> results) implements SearchService.Response {
		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.abstractText(), item.sourceUrl()))
				.toList());
		}
	}

	public record SearchResult(String title, String abstractText, String sourceUrl) {

	}

}
