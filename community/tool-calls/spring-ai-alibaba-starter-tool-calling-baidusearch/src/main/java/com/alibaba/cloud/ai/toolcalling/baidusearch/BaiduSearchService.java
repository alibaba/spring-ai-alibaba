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
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author KrakenZJC
 **/
public class BaiduSearchService implements Function<BaiduSearchService.Request, BaiduSearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BaiduSearchService.class);

	private static final String BAIDU_SEARCH_API_URL = "https://www.baidu.com/s?wd=";

	private static final int MAX_RESULTS = 20;

	private static final int Memory_Size = 5;

	private static final int Memory_Unit = 1024;

	private static final int Max_Memory_In_MB = Memory_Size * Memory_Unit * Memory_Unit;

	private final WebClient webClient;

	public BaiduSearchService() {
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT,
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
			.defaultHeader(HttpHeaders.ACCEPT,
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
			.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
			.defaultHeader(HttpHeaders.REFERER, "https://www.baidu.com/")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,ja;q=0.8")
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(Max_Memory_In_MB))
			.build();
	}

	@Override
	public BaiduSearchService.Response apply(BaiduSearchService.Request request) {
		if (request == null || !StringUtils.hasText(request.query)) {
			return null;
		}

		int limit = request.limit == 0 ? MAX_RESULTS : request.limit;

		String url = BAIDU_SEARCH_API_URL + request.query;
		try {
			Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
			String html = responseMono.block();
			assert html != null;

			List<SearchResult> results = parseHtml(html);
			if (CollectionUtils.isEmpty(results)) {
				return null;
			}

			logger.info("baidu search: {},result number:{}", request.query, results.size());
			for (SearchResult d : results) {
				logger.info(d.title() + "\n" + d.abstractText());
			}
			return new Response(results.subList(0, Math.min(results.size(), limit)));
		}
		catch (Exception e) {
			logger.error("failed to invoke baidu search caused by:{}", e.getMessage());
			return null;
		}

	}

	private List<SearchResult> parseHtml(String htmlContent) {
		try {
			Document doc = Jsoup.parse(htmlContent);
			Element contentLeft = doc.selectFirst("div#content_left"); // 选择具有特定 ID 的 div
			// 元素
			Elements divContents = contentLeft.children();
			List<SearchResult> listData = new ArrayList<>();

			for (Element div : divContents) {
				if (!div.hasClass("c-container")) {
					continue;
				}
				String title = "";
				String abstractText = "";

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
					logger.error("failed to parse baidu search html,caused by:{}", e.getMessage());
					continue;
				}

				listData.add(new SearchResult(title, abstractText));
			}

			return listData;
		}
		catch (Exception e) {
			logger.error("failed to parse baidu search html,caused by:{}", e.getMessage());
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Baidu search API request")
	public record Request(
			@JsonProperty(required = true,
					value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query,
			@JsonProperty(required = true,
					value = "limit") @JsonPropertyDescription("The limit count of the number of returned results e.g. 20") int limit) {

	}

	/**
	 * Baidu search Function response.
	 */
	@JsonClassDescription("Baidu search API response")
	public record Response(List<SearchResult> results) {

	}

	public record SearchResult(String title, String abstractText) {

	}

}
