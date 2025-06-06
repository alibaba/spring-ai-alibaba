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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.jsontemplate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public class RequestTemplateParser {

	public static RequestTemplateInfo parseRequestTemplate(JsonNode requestTemplate) {
		String url = requestTemplate.path("url").asText();
		String method = requestTemplate.path("method").asText();
		boolean argsToUrlParam = requestTemplate.path("argsToUrlParam").asBoolean(false);
		boolean argsToJsonBody = requestTemplate.path("argsToJsonBody").asBoolean(false);
		boolean argsToFormBody = requestTemplate.path("argsToFormBody").asBoolean(false);
		JsonNode headers = requestTemplate.path("headers");
		JsonNode body = requestTemplate.path("body");
		return new RequestTemplateInfo(url, method, argsToUrlParam, argsToJsonBody, argsToFormBody, headers, body,
				requestTemplate);
	}

	public static URI buildUri(UriBuilder builder, String processedUrl, RequestTemplateInfo info,
			Map<String, Object> args) {
		builder.path(processedUrl);
		if (info.argsToUrlParam) {
			for (Map.Entry<String, Object> entry : args.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (value != null) {
					if (value instanceof final Collection<?> collection) {
						for (Object item : collection) {
							builder.queryParam(key, item);
						}
					}
					else {
						builder.queryParam(key, value);
					}
				}
			}
		}
		return builder.build();
	}

	public static void addHeaders(WebClient.RequestBodySpec requestSpec, JsonNode headersNode, Map<String, Object> args,
			java.util.function.BiFunction<String, Map<String, Object>, String> templateProcessor) {
		if (headersNode != null && headersNode.isArray()) {
			for (JsonNode header : headersNode) {
				String key = header.path("key").asText();
				String valueTemplate = header.path("value").asText();
				String value = templateProcessor.apply(valueTemplate, args);
				requestSpec.header(key, value);
			}
		}
	}

	public static WebClient.RequestHeadersSpec<?> addRequestBody(WebClient.RequestBodySpec requestSpec,
			RequestTemplateInfo info, Map<String, Object> args,
			java.util.function.BiFunction<String, Map<String, Object>, String> templateProcessor,
			com.fasterxml.jackson.databind.ObjectMapper objectMapper, org.slf4j.Logger logger) {
		boolean hasBody = info.body != null && !info.body.asText().isEmpty();
		int optionCount = (hasBody ? 1 : 0) + (info.argsToJsonBody ? 1 : 0) + (info.argsToFormBody ? 1 : 0)
				+ (info.argsToUrlParam ? 1 : 0);
		if (optionCount > 1) {
			throw new IllegalArgumentException(
					"Only one of body, argsToJsonBody, argsToFormBody, or argsToUrlParam should be specified");
		}
		if (hasBody) {
			String bodyTemplate = info.body.asText();
			String processedBody = templateProcessor.apply(bodyTemplate, args);
			return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.bodyValue(processedBody);
		}
		else if (info.argsToJsonBody) {
			try {
				String jsonBody = objectMapper.writeValueAsString(args);
				return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON).bodyValue(jsonBody);
			}
			catch (com.fasterxml.jackson.core.JsonProcessingException e) {
				logger.error("Failed to create JSON request body", e);
				return requestSpec;
			}
		}
		else if (info.argsToFormBody) {
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			args.forEach((key, value) -> {
				if (value != null) {
					formData.add(key, value.toString());
				}
			});
			return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(formData));
		}
		return requestSpec;
	}

}
