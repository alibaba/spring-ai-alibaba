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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

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

	private final GoogleTranslateProperties properties;

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonPraseTool;

	public GoogleTranslateService(GoogleTranslateProperties properties, WebClientTool webClientTool,
			JsonParseTool jsonPraseTool) {
		assert StringUtils.hasText(properties.getApiKey());
		this.properties = properties;
		this.webClientTool = webClientTool;
		this.jsonPraseTool = jsonPraseTool;
	}

	@Override
	public Response apply(Request request) {
		if (CommonToolCallUtils.isInvalidateRequestParams(request, request.text, request.targetLanguage)) {
			return null;
		}

		return CommonToolCallUtils.handleServiceError("GoogleTranslate", () -> {
			try {
				MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
					.add("key", properties.getApiKey())
					.add("target", request.targetLanguage)
					.add("q", jsonPraseTool.objectToJson(request.text))
					.add("format", "text")
					.build();
				String responseData = webClientTool.post("/", params, "").block();
				return CommonToolCallUtils.handleResponse(responseData, data -> parseResponseData(data, request.text),
						log);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, log);
	}

	private Response parseResponseData(String responseData, List<String> query) {
		Map<String, String> translationResult = new HashMap<>();
		try {
			String translationStr = jsonPraseTool.getDepthFieldValueAsString(responseData, "data", "translations");
			List<String> translations = jsonPraseTool.jsonToList(translationStr, String.class);
			assert translations != null;
			assert query.size() == translations.size();
			for (int i = 0; i < translations.size(); i++) {
				translationResult.put(query.get(i), translations.get(i));
			}
			return new Response(translationResult);
		}
		catch (Exception e) {
			log.error("Failed to parse response data: {}", e.getMessage());
			return null;
		}
	}

	private void translateFailed(JsonNode rootNode) {
		String errorMessage = rootNode.path("error").path("message").asText();
		String code = rootNode.path("error").path("code").asText();
		log.info("Translate Text Failed. message:{} code:{}", errorMessage, code);
	}

	@JsonClassDescription("Request to translate text to the target language")
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
