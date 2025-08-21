/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.utils;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for handling HTTP response errors from different model providers.
 * Provides standardized error handling for DashScope and OpenAI API responses.
 *
 * @since 1.0.0.3
 */
@Slf4j
public class ErrorHandlerUtils {

	/**
	 * Error handler for DashScope API responses. Converts HTTP error responses into
	 * standardized BizException.
	 */
	public static final ResponseErrorHandler DASHSCOPE_RESPONSE_ERROR_HANDLER = new ResponseErrorHandler() {

		@Override
		public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
			return response.getStatusCode().isError();
		}

		@Override
		public void handleError(@NonNull ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().isError()) {
				String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
				JsonNode node = JsonUtils.fromJson(body);

				String code = "ModelCall";
				if (node.has("code")) {
					code = node.get("code").asText();
				}

				String message;
				if (node.has("message")) {
					message = node.get("message").asText();
				}
				else {
					message = body;
				}

				Error error = Error.builder()
					.statusCode(response.getStatusCode().value())
					.code(code)
					.message(message)
					.build();
				throw new BizException(error);
			}
		}
	};

	/**
	 * Error handler for OpenAI API responses. Converts HTTP error responses into
	 * standardized BizException.
	 */
	public static final ResponseErrorHandler OPENAI_RESPONSE_ERROR_HANDLER = new ResponseErrorHandler() {

		@Override
		public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
			return response.getStatusCode().isError();
		}

		@Override
		public void handleError(@NonNull ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().isError()) {
				String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
				log.error("failed to call openai model, code: {}, body: {}", response.getStatusCode(), body);

				Error error = parseOpenAiError(response.getStatusCode().value(), body);
				throw new BizException(error);
			}
		}
	};

	public static Error parseOpenAiError(int statusCode, String body) {
		JsonNode node = JsonUtils.fromJson(body);

		String code = "ModelCall";
		String message = "";
		if (node.has("error")) {
			node = node.get("error");
			if (node.has("code")) {
				code = node.get("code").asText();
			}
			if (node.has("message")) {
				message = node.get("message").asText();
			}
		}
		else {
			message = body;
		}

		return Error.builder().statusCode(statusCode).code(code).message(message).build();
	}

}
