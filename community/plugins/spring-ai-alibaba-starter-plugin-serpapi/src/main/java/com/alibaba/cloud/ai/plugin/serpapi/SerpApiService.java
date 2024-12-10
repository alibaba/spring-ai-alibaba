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

public class SerpApiService implements Function<SerpApiService.Request, SerpApiService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(SerpApiService.class);

	public static final String SERP_API_URL = "https://serpapi.com/search.json";

	public static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

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
	public record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query) {

	}

	@JsonClassDescription("serpapi search response")
	public record Response(List<SearchResult> results) {

	}

	public record SearchResult(String title, String text) {

	}

}
