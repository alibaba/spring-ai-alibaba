package com.alibaba.cloud.ai.plugin.googleserper;

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

/**
 * @author: superhandsomeg
 * @since : 2024-12-11
 **/

public class GoogleSerperService implements Function<GoogleSerperService.Request, GoogleSerperService.Response> {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSerperService.class);

    private static final String GOOGLE_SERPER_SEARCH_URL = "https://google.serper.dev/search";

    public static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final WebClient webClient;

    public GoogleSerperService(GoogleSerperProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl(GOOGLE_SERPER_SEARCH_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_VALUE)
                .defaultHeader("X-API-KEY", properties.getApikey())
                .defaultHeader("Content-Type", "application/json")
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
                    .uri(uriBuilder -> uriBuilder.queryParam("q", request.query).build()).retrieve().bodyToMono(String.class);
            String response = responseMono.block();
            assert response != null;
            logger.info("googleserper search: {},result:{}", request.query, response);
            List<SearchResult> resultList = parseJson(response);
            for (SearchResult result : resultList) {
                logger.info("{}\n{}", result.title(), result.text());
            }
            return new Response(resultList);
        } catch (Exception e) {
            logger.error("failed to invoke googleserper search, caused by:{}", e.getMessage());
            return null;
        }
    }

    private List<SearchResult> parseJson(String jsonResponse) {
        JSONArray jsonArray = new JSONObject(jsonResponse).getJSONArray("organic");
        List<SearchResult> resultList = new ArrayList<>();
        for (JSONObject jsonObject : jsonArray.toList(JSONObject.class)) {
            String title = jsonObject.getStr("title");
            String link = jsonObject.getStr("link");
            try {
                Document document = Jsoup.connect(link).userAgent(USER_AGENT_VALUE).get();
                String textContent = document.body().text();
                resultList.add(new SearchResult(title, textContent));
            } catch (Exception e) {
                resultList.add(new SearchResult(title, jsonObject.getStr("snippet")));
                logger.error("failed to parse searchapi search link,caused by:{}", e.getMessage());
            }
        }
        return resultList;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("googleserper search request")
    public record Request(@JsonProperty(required = true,
            value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query) {

    }

    @JsonClassDescription("googleserper search response")
    public record Response(List<SearchResult> results) {

    }

    public record SearchResult(String title, String text) {

    }
}
