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
package com.alibaba.cloud.ai.toolcalling.googletranslate;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class GoogleTranslateService
		implements Function<GoogleTranslateService.Request, GoogleTranslateService.Response> {

	private static final Logger log = LoggerFactory.getLogger(GoogleTranslateService.class);

	private static final String TRANSLATE_HOST = "https://translation.googleapis.com";

	private static final String TRANSLATE_PATH = "/language/translate/v2";

	private final GoogleTranslateProperties properties;

	private final WebClient webClient;

	public GoogleTranslateService(GoogleTranslateProperties properties) {
		assert StringUtils.hasText(properties.getApiKey());
		this.properties = properties;
		this.webClient = WebClient.builder().defaultHeader("Content-Type", "application/json").build();
	}

	@Override
	public Response apply(Request request) {
		if (request == null || request.text == null || request.text.isEmpty()
				|| !StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}

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
		}
		catch (Exception e) {
			log.error("Using the googleTranslate service failed due to {}", e.getMessage());
		}
		return null;
	}

	private Response parseResponseData(String responseData, List<String> query) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> translationResult = new HashMap<>();
		try {
			JsonNode rootNode = mapper.readTree(responseData);
			JsonNode data = rootNode.path("data");
			if (data == null || data.isNull()) {
				translateFailed(rootNode);
				return null;
			}
			JsonNode translations = data.path("translations");
			assert translations != null;
			assert query.size() == translations.size();
			for (int i = 0; i < translations.size(); i++) {
				translationResult.put(query.get(i), translations.get(i).asText());
			}
			return new Response(translationResult);
		}
		catch (Exception e) {
			log.error("failed to convert the response to json object  due to {}", e.getMessage());
			return null;
		}
	}

	private void translateFailed(JsonNode rootNode) {
		String errorMessage = rootNode.path("error").path("message").asText();
		String code = rootNode.path("error").path("code").asText();
		log.info("Translate Text Failed. message:{} code:{}", errorMessage, code);
	}

	public record Request(
			@JsonProperty(required = true,
					value = "text") @JsonPropertyDescription("Content that needs to be translated") List<String> text,
			@JsonProperty(required = true,
					value = "targetLanguage") @JsonPropertyDescription("the target language to translate into") String targetLanguage) {
	}

	@JsonClassDescription("Response to translate text to the target language")
	public record Response(Map<String, String> translatedTexts) {
	}

}
