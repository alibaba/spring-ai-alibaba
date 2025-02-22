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
package com.alibaba.cloud.ai.toolcalling.alitranslate;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.alimt20181012.AsyncClient;
import com.aliyun.sdk.service.alimt20181012.models.TranslateGeneralRequest;
import com.aliyun.sdk.service.alimt20181012.models.TranslateGeneralResponse;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import darabonba.core.client.ClientOverrideConfiguration;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author yunlong
 */
public class AliTranslateService implements Function<AliTranslateService.Request, AliTranslateService.Response> {

	private final AsyncClient client;

	/**
	 * version of the api
	 */
	public static final String SCENE = "general";

	/**
	 * FormatType text or html
	 */
	public static final String FORM_TYPE = "text";

	/**
	 * offline doc:
	 * https://help.aliyun.com/zh/machine-translation/support/supported-languages-and-codes?spm=api-workbench.api_explorer.0.0.37a94eecsclZw9
	 */
	public static final String LANGUAGE_CODE = "zh";

	public AliTranslateService(AliTranslateProperties properties) {
		assert StringUtils.hasText(properties.getRegion());
		assert StringUtils.hasText(properties.getAccessKeyId());
		assert StringUtils.hasText(properties.getAccessKeySecret());
		StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
			.accessKeyId(properties.getAccessKeyId())
			.accessKeySecret(properties.getAccessKeySecret())
			.build());

		this.client = AsyncClient.builder()
			.region(properties.getRegion()) // Region ID
			.credentialsProvider(provider)
			.overrideConfiguration(ClientOverrideConfiguration.create().setEndpointOverride("mt.aliyuncs.com"))
			.build();
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.text) || !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}

		TranslateGeneralRequest translateGeneralRequest = TranslateGeneralRequest.builder()
			.formatType(FORM_TYPE)
			.sourceLanguage(LANGUAGE_CODE)
			.targetLanguage(request.targetLanguage)
			.sourceText(request.text)
			.scene(SCENE)
			.build();

		CompletableFuture<TranslateGeneralResponse> response = client.translateGeneral(translateGeneralRequest);

		TranslateGeneralResponse resp = null;
		try {
			resp = response.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		// 假设 resp 是一个对象
		String jsonString = new Gson().toJson(resp);
		JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
		String result = jsonObject.get("body")
			.getAsJsonObject()
			.get("data")
			.getAsJsonObject()
			.get("translated")
			.toString();

		client.close();
		return new Response(result);

	}

	@JsonClassDescription("Request to alitranslate text to a target language")
	public record Request(
			@JsonProperty(required = true,
					value = "text") @JsonPropertyDescription("Content that needs to be translated") String text,
			@JsonProperty(required = false,
					value = "targetLanguage") @JsonPropertyDescription("Target language to alitranslate into") String targetLanguage) {

		public Request(@JsonProperty("text") String text) {
			this(text, "en"); // 默认目标语言为英语
		}
	}

	@JsonClassDescription("Response to alitranslate text to a target language")
	public record Response(String translatedTexts) {
	}

}
