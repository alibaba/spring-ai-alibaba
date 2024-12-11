/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.serpapi;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.alibaba.cloud.ai.plugin.serpapi.SerpApiProperties.SERP_API_URL;
import static com.alibaba.cloud.ai.plugin.serpapi.SerpApiProperties.USER_AGENT_VALUE;

/**
 * @author 北极星
 */
public class SerpApiService implements Function<SerpApiService.Request, SerpApiService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(SerpApiService.class);

	private final WebClient webClient;

	private final String apikey;

	private final String engine;

	public SerpApiService(SerpApiProperties properties) {
		this.apikey = properties.getApikey();
		this.engine = properties.getEngine();
		this.webClient = WebClient.builder()
			.baseUrl(SERP_API_URL)
			.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE)
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
			.build();
	}

	/**
	 * 使用serpai API 搜索数据
	 * @param request the function argument
	 * @return responseMono
	 */
	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.query)) {
			return null;
		}
		try {
			Mono<String> responseMono = webClient.method(HttpMethod.GET)
				.uri(uriBuilder -> uriBuilder.queryParam("api_key", apikey)
					.queryParam("engine", engine)
					.queryParam("q", request.query)
					.build())
				.retrieve()
				.bodyToMono(String.class);
			String response = responseMono.block();
			assert response != null;
			logger.info("serpapi search: {},result:{}", request.query, response);
			List<SearchResult> resultList = parseJson(response);
			for (SearchResult result : resultList) {
				logger.info("{}\n{}", result.title(), result.text());
			}
			return new Response(resultList);
		}
		catch (Exception e) {
			logger.error("failed to invoke serpapi search, caused by:{}", e.getMessage());
			return null;
		}
	}

	private List<SearchResult> parseJson(String jsonResponse) {
		JSONArray jsonArray = new JSONObject(jsonResponse).getJSONArray("organic_results");
		List<SearchResult> resultList = new ArrayList<>();
		for (JSONObject jsonObject : jsonArray.toList(JSONObject.class)) {
			String title = jsonObject.getStr("title");
			String link = jsonObject.getStr("link");
			try {
				Document document = Jsoup.connect(link).userAgent(USER_AGENT_VALUE).get();
				String textContent = document.body().text();
				resultList.add(new SearchResult(title, textContent));
			}
			catch (Exception e) {
				logger.error("failed to parse serpapi search link,caused by:{}", e.getMessage());
			}
		}
		return resultList;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("serpapi search request")
	record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query) {
	}

	@JsonClassDescription("serpapi search response")
	record Response(List<SearchResult> results) {

	}

	record SearchResult(String title, String text) {

	}

}
