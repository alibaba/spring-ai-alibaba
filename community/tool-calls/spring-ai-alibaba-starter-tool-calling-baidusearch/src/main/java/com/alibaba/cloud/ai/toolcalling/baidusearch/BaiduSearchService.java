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
import io.netty.channel.ChannelOption;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * @author KrakenZJC
 **/
public class BaiduSearchService implements Function<BaiduSearchService.Request, BaiduSearchService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BaiduSearchService.class);

	private static final String BAIDU_SEARCH_API_URL = "https://www.baidu.com/s?wd=";

	private static final int MAX_RESULTS = 20;

	private static final int MEMORY_SIZE = 5;

	private static final int MEMORY_UNIT = 1024;

	private static final int MAX_MEMORY_IN_MB = MEMORY_SIZE * MEMORY_UNIT * MEMORY_UNIT;

	private static final String[] USER_AGENTS = {
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36" };

	private static final int CONNECT_TIMEOUT_MILLIS = 5000;

	private static final int RESPONSE_TIMEOUT_SECONDS = 10;

	private final Random random = new Random();

	private final WebClient webClient;

	public BaiduSearchService() {
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENTS[random.nextInt(USER_AGENTS.length)])
			.defaultHeader(HttpHeaders.REFERER, "https://www.baidu.com/")
			.defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9")
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_IN_MB))
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
				.responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))))
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
			Mono<String> responseMono = webClient.get()
				.uri(url)
				.acceptCharset(Charset.forName("UTF-8"))
				.retrieve()
				.bodyToMono(String.class);
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
			// Select a div with a specific ID
			Element contentLeft = doc.selectFirst("div#content_left");
			// element
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
