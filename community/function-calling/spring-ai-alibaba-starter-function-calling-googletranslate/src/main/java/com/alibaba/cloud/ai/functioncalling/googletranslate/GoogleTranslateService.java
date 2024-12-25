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

package com.alibaba.cloud.ai.functioncalling.googletranslate;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author erasernoob
 */
public class GoogleTranslateService implements Function<GoogleTranslateService.Request, GoogleTranslateService.Response> {

    private static final Logger log = LoggerFactory.getLogger(GoogleTranslateService.class);

    private static final String TRANSLATE_HOST = "https://translation.googleapis.com";

    private static final String TRANSLATE_PATH = "/language/translate/v2";

    private final GoogleTranslateProperties properties;

    private final WebClient webClient;

    public GoogleTranslateService(GoogleTranslateProperties properties) {
        assert StringUtils.hasText(properties.getApiKey());
        this.properties = properties;
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    @Override
    public Response apply(Request request) {
        if ( request == null || !StringUtils.hasText(properties.getApiKey()) ||
                !StringUtils.hasText(request.text) || !StringUtils.hasText(request.targetLanguage)
        ) {return null;}

        String requestUrl = UriComponentsBuilder.fromHttpUrl(TRANSLATE_HOST + TRANSLATE_PATH)
                .queryParam("key", properties.getApiKey())
                .queryParam("target", request.targetLanguage)
                .queryParam("q", request.text)
                .queryParam("format", "text")
                .toUriString();
        try {
            Mono<String> responseMono = webClient.post().uri(requestUrl).retrieve().bodyToMono(String.class);

            String responseData = responseMono.block();
            assert responseData != null;
            log.info("GoogleTranslation request: {}, response: {}", request.text, responseData);
            return parseResponseData(responseData, request.text);
        } catch (Exception e) {
            log.error("Using the googleTranslate service failed due to {}", e.getMessage());
        }
        return null;
    }

    private Response parseResponseData(String responseData, String q) {
        Gson gson = new Gson();
        Map<String, Object> response = gson.fromJson(responseData,
                new TypeToken<Map<String, Object>>() {}.getType());
        if (response.containsKey("error")) {
            Map<String, Object> errorMap = (Map<String, Object>) response.get("error");
            log.error("GoogleTranslation service failed due to {}", errorMap.get("message").toString());
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, String>> translationsList = (List<Map<String, String>>) data.get("translation");
        if ( translationsList == null || translationsList.isEmpty()) {
            log.error("Invalid response format: 'translation' list is empty.");
            return null;
        }

        Map<String, String> translationResult = new HashMap<>();
        for (Map<String, String> translation : translationsList) {
            translationResult.put(q, translation.get("translatedText"));
        }
        return new Response(translationResult);
    }

    public record Request(
            @JsonProperty(required = true,
                    value = "text")@JsonPropertyDescription("Content that needs to be translated") String text,
            @JsonProperty(required = true, value = "targetLanguage")
            @JsonPropertyDescription("the target language to translate into")
            String targetLanguage
    ) {}

    @JsonClassDescription("Response to translate text to the target language")
    public record Response(Map<String, String> translatedTexts) {}
}
