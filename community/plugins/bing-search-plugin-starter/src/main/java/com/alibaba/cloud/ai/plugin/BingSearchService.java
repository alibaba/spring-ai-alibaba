package com.alibaba.cloud.ai.plugin;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * .
 *
 * @author: KrakenZJC
 * @since : 2024-11-18
 **/

public class BingSearchService implements Function<BingSearchService.Request, BingSearchService.Response> {
    
    private static final Logger logger = LoggerFactory.getLogger(BingSearchService.class);
    
    private static final String BING_SEARCH_HOST_URL = "https://api.bing.microsoft.com";
    
    private static final String BING_SEARCH_PATH = "/v7.0/search";
    
    private final WebClient webClient;
    
    public BingSearchService(BingSearchProperties properties) {
        assert StringUtils.hasText(properties.getToken()) && properties.getToken().length() == 32;
        this.webClient = WebClient.builder().defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .defaultHeader(BingSearchProperties.OCP_APIM_SUBSCRIPTION_KEY, properties.getToken())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)).build();
    }
    
    @Override
    public BingSearchService.Response apply(BingSearchService.Request request) {
        if (request == null || !StringUtils.hasText(request.query)) {
            return null;
        }
        
        String url = BING_SEARCH_HOST_URL + BING_SEARCH_PATH + "?q=" + URLEncoder.encode(request.query,
                StandardCharsets.UTF_8);
        
        try {
            Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
            String responseData = responseMono.block();
            assert responseData != null;
            logger.info("bing search: {},result:{}", request.query, responseData);
            return new Response(responseData);
        } catch (Exception e) {
            logger.error("failed to invoke bing search caused by:{}", e.getMessage());
            return null;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Bing search API request")
    public record Request(
            @JsonProperty(required = true, value = "query") @JsonPropertyDescription("The query keyword e.g. Alibaba") String query) {
        
    }
    
    /**
     * Bing search Function response.
     */
    @JsonClassDescription("Bing search API response")
    public record Response(String data) {
    
    }
    
}
