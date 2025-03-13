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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class BaidutranslateService implements Function<BaidutranslateService.Request, BaidutranslateService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(BaidutranslateService.class);

	private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";

	private static final Random random = new Random();

	private final String appId;

	private final String secretKey;

	private final WebClient webClient;

	public BaidutranslateService(BaidutranslateProperties properties) {
		assert StringUtils.hasText(properties.getAppId());
		this.appId = properties.getAppId();
		assert StringUtils.hasText(properties.getSecretKey());
		this.secretKey = properties.getSecretKey();

		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
			.build();
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.q) || !StringUtils.hasText(request.from)
				|| !StringUtils.hasText(request.to)) {
			return null;
		}
		String salt = String.valueOf(random.nextInt(100000));
		String sign = DigestUtils.md5DigestAsHex((appId + request.q + salt + secretKey).getBytes());
		String url = UriComponentsBuilder.fromHttpUrl(TRANSLATE_HOST_URL).toUriString();
		try {
			MultiValueMap<String, String> body = constructRequestBody(request, salt, sign);
			Mono<String> responseMono = webClient.post().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);

			String responseData = responseMono.block();
			assert responseData != null;
			logger.info("Translation request: {}, response: {}", request.q, responseData);

			return parseResponse(responseData);
		}
		catch (Exception e) {
			logger.error("Failed to invoke translate API due to: {}", e.getMessage());
			return null;
		}
	}

	private MultiValueMap<String, String> constructRequestBody(Request request, String salt, String sign) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("q", request.q);
		body.add("from", request.from);
		body.add("to", request.to);
		body.add("appid", appId);
		body.add("salt", salt);
		body.add("sign", sign);
		return body;
	}

	private Response parseResponse(String responseData) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Map<String, String> translations = new HashMap<>();
			TranslationResponse responseList = mapper.readValue(responseData, TranslationResponse.class);
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
				Map<String, String> responseList = mapper.readValue(responseData,
						mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
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
