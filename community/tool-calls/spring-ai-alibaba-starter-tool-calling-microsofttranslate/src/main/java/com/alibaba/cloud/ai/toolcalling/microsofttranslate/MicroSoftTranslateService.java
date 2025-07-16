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
package com.alibaba.cloud.ai.toolcalling.microsofttranslate;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MicroSoftTranslateService
		implements Function<MicroSoftTranslateService.Request, MicroSoftTranslateService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(MicroSoftTranslateService.class);

	private static final String TRANSLATE_PATH = "/translate?api-version=3.0";

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public MicroSoftTranslateService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.text) || !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}
		String uri = UriComponentsBuilder.fromHttpUrl(TRANSLATE_PATH)
			.queryParam("to", request.targetLanguage)
			.toUriString();
		logger.info("Request uri: {}", uri);
		try {
			String body = constructRequestBody(request);
			logger.info("Request body: {}", body);

			String responseData = webClientTool.getWebClient()
				.post()
				.uri(uri)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.block();

			logger.info("Translation request: {}, response: {}", request.text, responseData);
			return parseResponse(responseData);
		}
		catch (Exception e) {
			logger.error("Failed to invoke microsofttranslate API due to: {}", e.getMessage());
			return null;
		}
	}

	private String constructRequestBody(Request request) {
		return "[{\"Text\": \"" + request.text + "\"}]";
	}

	private Response parseResponse(String responseData) {
		try {
			Map<String, String> translations = new HashMap<>();

			String firstElement = jsonParseTool.getFirstElementFromJsonArrayString(responseData);
			if (firstElement != null) {
				List<Map<String, Object>> translationsList = jsonParseTool.getFieldValue(firstElement,
						new TypeReference<List<Map<String, Object>>>() {
						}, "translations");
				for (Map<String, Object> translation : translationsList) {
					String to = (String) translation.get("to");
					String translatedText = (String) translation.get("text");
					translations.put(to, translatedText);
					logger.info("Translated text to {}: {}", to, translatedText);
				}
			}
			return new Response(translations);
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to parse response JSON: {}", responseData, e);
			throw new RuntimeException("JSON parsing failed", e);
		}
	}

	@JsonClassDescription("Request to microsofttranslate text to a target language")
	public record Request(
			@JsonProperty(required = true,
					value = "text") @JsonPropertyDescription("Content that needs to be translated") String text,
			@JsonProperty(required = true,
					value = "targetLanguage") @JsonPropertyDescription("Target language to microsofttranslate into") String targetLanguage) {
	}

	@JsonClassDescription("Response to microsofttranslate text to a target language")
	public record Response(Map<String, String> translatedTexts) {
	}

}
