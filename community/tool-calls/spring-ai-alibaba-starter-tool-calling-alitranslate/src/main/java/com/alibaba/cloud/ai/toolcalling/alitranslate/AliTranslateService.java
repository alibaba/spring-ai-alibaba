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
import com.aliyun.sdk.service.alimt20181012.models.TranslateGeneralResponseBody;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import darabonba.core.client.ClientOverrideConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author yunlong
 * @author Allen Hu
 */
public class AliTranslateService
		implements Function<AliTranslateService.Request, AliTranslateService.Response>, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AliTranslateService.class);

	private final AsyncClient client;

	public AliTranslateService(AliTranslateProperties properties) {
		StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
			.accessKeyId(properties.getAccessKeyId())
			.accessKeySecret(properties.getSecretKey())
			.build());

		this.client = AsyncClient.builder()
			.credentialsProvider(provider)
			.overrideConfiguration(ClientOverrideConfiguration.create().setEndpointOverride("mt.aliyuncs.com"))
			.build();
	}

	@Override
	public Response apply(Request request) {
		if (request == null || !StringUtils.hasText(request.text) || !StringUtils.hasText(request.sourceLanguage)
				|| !StringUtils.hasText(request.targetLanguage)) {
			return null;
		}

		TranslateGeneralRequest translateGeneralRequest = TranslateGeneralRequest.builder()
			.formatType(AliTranslateConstants.FORM_TYPE)
			.sourceLanguage(request.sourceLanguage)
			.targetLanguage(request.targetLanguage)
			.sourceText(request.text)
			.scene(AliTranslateConstants.SCENE)
			.build();

		CompletableFuture<TranslateGeneralResponse> response = client.translateGeneral(translateGeneralRequest);

		try {
			TranslateGeneralResponse resp = response.get();
			String translated = getTranslatedText(resp);
			return new Response(translated);
		}
		catch (Exception e) {
			logger.error("Failed to invoke alitranslate caused by:{}", e.getMessage());
			return null;
		}
	}

	@Override
	public void close() {
		client.close();
	}

	private String getTranslatedText(TranslateGeneralResponse resp) throws IllegalStateException {
		if (null == resp || !Objects.equals(200, resp.getStatusCode())) {
			throw new IllegalStateException("Failed to invoke alitranslate, caused response error.");
		}

		TranslateGeneralResponseBody body = resp.getBody();
		if (null == body) {
			throw new IllegalStateException("Failed to invoke alitranslate, caused body is null.");
		}
		Integer code = body.getCode();
		if (!Objects.equals(200, code)) {
			throw new IllegalStateException(code + " - " + body.getMessage());
		}

		TranslateGeneralResponseBody.Data data = body.getData();
		if (null == data) {
			throw new IllegalStateException("Failed to invoke alitranslate, caused data is null.");
		}
		return data.getTranslated();
	}

	@JsonClassDescription("Request to alitranslate text to a target language")
	public record Request(
			@JsonProperty(required = true,
					value = "text") @JsonPropertyDescription("Content that needs to be translated") String text,

			@JsonProperty(required = false,
					value = "sourceLanguage") @JsonPropertyDescription("Source language of the text, default is zh") String sourceLanguage,

			@JsonProperty(required = false,
					value = "targetLanguage") @JsonPropertyDescription("Target language to alitranslate into, default is en") String targetLanguage) {

		public Request(@JsonProperty("text") String text) {
			this(text, AliTranslateConstants.LANGUAGE_CODE_ZH, AliTranslateConstants.LANGUAGE_CODE_EN);
		}
	}

	@JsonClassDescription("Response to alitranslate text to a target language")
	public record Response(String translatedTexts) {
	}

}
