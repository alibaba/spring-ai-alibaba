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
package com.alibaba.cloud.ai.toolcalling.youdaotranslate;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.alibaba.cloud.ai.toolcalling.youdaotranslate.AuthTools.calculateSign;

/**
 * @author Yeaury
 */
public class YoudaoTranslateService
		implements Function<YoudaoTranslateService.Request, YoudaoTranslateService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(YoudaoTranslateService.class);

	private static final String YOUDAO_TRANSLATE_HOST_URL = "https://openapi.youdao.com/api";

	private final String appKey;

	private final String appSecret;

	private final WebClient webClient;

	private static final int MEMORY_SIZE = 5;

	private static final int BYTE_SIZE = 1024;

	private static final int MAX_MEMORY_SIZE = MEMORY_SIZE * BYTE_SIZE * BYTE_SIZE;

	public YoudaoTranslateService(YoudaoTranslateProperties properties) {
		this.appKey = properties.getAppKey();
		this.appSecret = properties.getAppSecret();
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,ja;q=0.8")
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE))
			.build();
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.text) || !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}
		String curtime = String.valueOf(System.currentTimeMillis() / 1000);
		String salt = UUID.randomUUID().toString();
		try {
			String url = UriComponentsBuilder.fromHttpUrl(YOUDAO_TRANSLATE_HOST_URL)
				.queryParam("q", request.text)
				.queryParam("from", request.sourceLanguage)
				.queryParam("to", request.targetLanguage)
				.queryParam("appKey", appKey)
				.queryParam("salt", salt)
				.queryParam("sign", calculateSign(appKey, appSecret, request.text, salt, curtime))
				.queryParam("signType", "v3")
				.queryParam("curtime", curtime)
				.build()
				.toUriString();

			Mono<String> response = webClient.get().uri(url).retrieve().bodyToMono(String.class);
			String responseData = response.block();
			System.out.println(responseData);
			assert responseData != null;
			logger.info("Translation request: {}, response: {}", request.text, responseData);
			Gson gson = new Gson();
			Map<String, Object> responseMap = gson.fromJson(responseData, new TypeToken<Map<String, Object>>() {
			}.getType());
			if (responseMap.containsKey("translation")) {
				return new Response((List<String>) responseMap.get("translation"));
			}
		}
		catch (Exception e) {
			logger.error("Failed to invoke Youdao translate API due to: {}", e.getMessage());
			return null;
		}
		return null;
	}

	@JsonClassDescription("Request to translate text to a target language")
	public record Request(
			@JsonProperty(required = true,
					value = "text") @JsonPropertyDescription("Content that needs to be translated") String text,

			@JsonProperty(required = true,
					value = "sourceLanguage") @JsonPropertyDescription("Source language") String sourceLanguage,

			@JsonProperty(required = true,
					value = "targetLanguage") @JsonPropertyDescription("Target language to translate into") String targetLanguage) {
	}

	@JsonClassDescription("Response to translate text to a target language")
	public record Response(List<String> translatedTexts) {
	}

}
