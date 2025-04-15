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

package com.alibaba.cloud.ai.toolcalling.baidutranslate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.RestClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

public class BaiduTranslateService implements Function<BaiduTranslateService.Request, BaiduTranslateService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BaiduTranslateService.class);

	private static final Random random = new Random();

	private final RestClientTool restClientTool;

	private final JsonParseTool jsonParseTool;

	private final BaiduTranslateProperties properties;

	public BaiduTranslateService(BaiduTranslateProperties properties, RestClientTool restClientTool,
			JsonParseTool jsonParseTool) {

		assert StringUtils.hasText(properties.getAppId());
		assert StringUtils.hasText(properties.getSecretKey());
		this.restClientTool = restClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public Response apply(Request request) {

		if (request == null || !StringUtils.hasText(request.q) || !StringUtils.hasText(request.from)
				|| !StringUtils.hasText(request.to)) {
			return null;
		}

		String salt = String.valueOf(random.nextInt(100000));
		String sign = DigestUtils
			.md5DigestAsHex((properties.getAppId() + request.q + salt + properties.getSecretKey()).getBytes());
		try {
			MultiValueMap<String, String> body = CommonToolCallUtils.<String, String>multiValueMapBuilder()
				.add("q", request.q)
				.add("from", request.from)
				.add("to", request.to)
				.add("appid", properties.getAppId())
				.add("salt", salt)
				.add("sign", sign)
				.build();
			return parseResponse(restClientTool.post("/", new LinkedMultiValueMap<>(), new HashMap<>(), body,
					MediaType.APPLICATION_FORM_URLENCODED));
		}
		catch (Exception e) {
			logger.error("Error occurred: {}", e.getMessage());
			return null;
		}
	}

	private Response parseResponse(String responseData) {
		try {
			Map<String, String> translations = new HashMap<>();
			TranslationResponse responseList = jsonParseTool.jsonToObject(responseData,
					new TypeReference<TranslationResponse>() {
					});
			String to = responseList.to;
			List<TranslationResult> translationsList = responseList.trans_result;
			if (translationsList != null) {
				for (TranslationResult translation : translationsList) {
					String translatedText = translation.dst;
					translations.put(to, translatedText);
					logger.info("Translated text to {}: {}", to, translatedText);
				}
			}
			return new Response(translations);
		}
		catch (Exception e) {
			try {
				Map<String, String> responseList = jsonParseTool.jsonToMap(responseData, String.class);
				logger.info(
						"Translation exception, please inquire Baidu translation api documentation to info error_code:{}",
						responseList);
				return new Response(responseList);
			}
			catch (Exception ex) {
				logger.error("Failed to parse json due to: {}", ex.getMessage());
				return null;
			}
		}
	}

	@JsonClassDescription("Request to translate text to a target language")
	public record Request(
			@JsonProperty(required = true,
					value = "q") @JsonPropertyDescription("Content that needs to be translated") String q,
			@JsonProperty(required = true,
					value = "from") @JsonPropertyDescription("Source language that needs to be translated") String from,
			@JsonProperty(required = true,
					value = "to") @JsonPropertyDescription("Target language to translate into") String to) {
	}

	@JsonClassDescription("Response to translate text to a target language")
	public record Response(Map<String, String> translatedTexts) {
	}

	@JsonClassDescription("part of the response")
	public record TranslationResult(
			@JsonProperty(required = true, value = "src") @JsonPropertyDescription("Original Content") String src,
			@JsonProperty(required = true, value = "dst") @JsonPropertyDescription("Final Result") String dst) {
	}

	@JsonClassDescription("complete response")
	public record TranslationResponse(
			@JsonProperty(required = true,
					value = "from") @JsonPropertyDescription("Source language that needs to be translated") String from,
			@JsonProperty(required = true,
					value = "to") @JsonPropertyDescription("Target language to translate into") String to,
			@JsonProperty(required = true,
					value = "trans_result") @JsonPropertyDescription("part of the response") List<TranslationResult> trans_result) {
	}

}
