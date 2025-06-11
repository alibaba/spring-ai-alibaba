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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.alibaba.cloud.ai.toolcalling.youdaotranslate.AuthTools.calculateSign;

/**
 * @author Yeaury
 * @author Allen Hu
 */
public class YoudaoTranslateService
		implements Function<YoudaoTranslateService.Request, YoudaoTranslateService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(YoudaoTranslateService.class);

	private final String appKey;

	private final String appSecret;

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	public YoudaoTranslateService(YoudaoTranslateProperties properties, JsonParseTool jsonParseTool,
			WebClientTool webClientTool) {
		this.appKey = properties.getAppId();
		this.appSecret = properties.getSecretKey();
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.text) || !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}
		String curtime = String.valueOf(System.currentTimeMillis() / 1000);
		String salt = UUID.randomUUID().toString();
		try {
			MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
				.add("q", request.text)
				.add("from", request.sourceLanguage)
				.add("to", request.targetLanguage)
				.add("appKey", appKey)
				.add("salt", salt)
				.add("sign", calculateSign(appKey, appSecret, request.text, salt, curtime))
				.add("signType", "v3")
				.add("curtime", curtime)
				.build();

			String responseData = webClientTool
				.post("api", CommonToolCallUtils.<String, String>multiValueMapBuilder().build(), Map.of(), params,
						MediaType.APPLICATION_FORM_URLENCODED)
				.block();

			assert responseData != null;
			logger.debug("Translation request: {}, response: {}", request.text, responseData);

			return jsonParseTool.jsonToObject(responseData, Response.class);
		}
		catch (Exception e) {
			logger.error("Failed to invoke Youdao translate API due to: {}", e.getMessage());
			return null;
		}
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
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(String errorCode, @JsonProperty("translation") List<String> translatedTexts) {
	}

}
