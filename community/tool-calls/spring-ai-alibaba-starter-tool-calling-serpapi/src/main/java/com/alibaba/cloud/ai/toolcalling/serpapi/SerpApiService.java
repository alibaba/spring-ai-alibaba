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
package com.alibaba.cloud.ai.toolcalling.serpapi;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author 北极星
 * @author sixiyida
 */
public class SerpApiService implements SearchService, Function<SerpApiService.Request, SerpApiService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(SerpApiService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	private final SerpApiProperties properties;

	public SerpApiService(SerpApiProperties properties, JsonParseTool jsonParseTool, WebClientTool webClientTool) {
		this.properties = properties;
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(new Request(query));
	}

	/**
	 * 使用serpai API 搜索数据
	 * @param request the function argument
	 * @return responseMono
	 */
	@Override
	public SerpApiService.Response apply(SerpApiService.Request request) {
		if (CommonToolCallUtils.isInvalidateRequestParams(request, request.query)) {
			return null;
		}

		return CommonToolCallUtils.handleServiceError("SerpApi", () -> {
			String response = webClientTool.getWebClient()
				.get()
				.uri(uriBuilder -> uriBuilder.queryParam("api_key", properties.getApiKey())
					.queryParam("engine", properties.getEngine())
					.queryParam("q", request.query)
					.build())
				.acceptCharset(StandardCharsets.UTF_8)
				.retrieve()
				.bodyToMono(String.class)
				.block();

			List<SearchResult> results = CommonToolCallUtils.handleResponse(response, this::parseJson, logger);

			if (CollectionUtils.isEmpty(results)) {
				return null;
			}

			logger.info("serpapi search: {},result:{}", request.query, response);

			for (SearchResult d : results) {
				logger.info("{}\n{}", d.title(), d.text());
			}
			return new Response(results);
		}, logger);
	}

	private List<SearchResult> parseJson(String jsonResponse) {
		List<SearchResult> resultList = new ArrayList<>();
		try {
			TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {
			};

			List<Map<String, Object>> organicResults = jsonParseTool.getFieldValue(jsonResponse, typeRef,
					"organic_results");

			for (Map<String, Object> result : organicResults) {
				String title = (String) result.get("title");
				String link = (String) result.get("link");

				try {
					Document document = Jsoup.connect(link).userAgent(SerpApiProperties.USER_AGENT_VALUE).get();
					String textContent = document.body().text();
					resultList.add(new SearchResult(title, textContent));
				}
				catch (Exception e) {
					logger.error("Failed to parse SERP API search link {}, caused by: {}", link, e.getMessage());
				}
			}
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to parse JSON response, caused by: {}", e.getMessage());
		}

		return resultList;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("serpapi search request")
	public record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("The query " + "keyword e.g. Alibaba") String query)
			implements
				SearchService.Request {
		@Override
		public String getQuery() {
			return this.query();
		}
	}

	@JsonClassDescription("serpapi search response")
	public record Response(List<SearchResult> results) implements SearchService.Response {
		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.text(), null))
				.toList());
		}
	}

	public record SearchResult(String title, String text) {
	}

}
