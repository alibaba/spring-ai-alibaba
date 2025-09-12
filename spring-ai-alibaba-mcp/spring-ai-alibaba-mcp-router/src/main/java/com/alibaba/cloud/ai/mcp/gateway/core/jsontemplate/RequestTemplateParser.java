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

package com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestTemplateParser {

	public static final Pattern PATH_VARIABLES_PATTERN = Pattern.compile("(?<!\\{)\\{([^}]+)\\}(?!\\})");

	public static RequestTemplateInfo parseRequestTemplate(JsonNode requestTemplate, JsonNode argsPosition) {
		String url = requestTemplate.path("url").asText();
		String method = requestTemplate.path("method").asText();
		boolean argsToUrlParam = requestTemplate.path("argsToUrlParam").asBoolean(false);
		boolean argsToJsonBody = requestTemplate.path("argsToJsonBody").asBoolean(false);
		boolean argsToFormBody = requestTemplate.path("argsToFormBody").asBoolean(false);
		JsonNode headers = requestTemplate.path("headers");
		JsonNode body = requestTemplate.path("body");
		if (body.isMissingNode() && !argsToJsonBody && !argsToFormBody) {
			argsToUrlParam = true;
		}
		int optionCount = (argsToUrlParam ? 1 : 0) + (argsToJsonBody ? 1 : 0) + (argsToFormBody ? 1 : 0)
				+ (body.isMissingNode() ? 0 : 1);
		if (optionCount > 1) {
			throw new IllegalArgumentException(
					"Only one of urlToParam, argsToJsonBody, argsToFormBody, or argsToUrlParam should be specified");
		}
		return new RequestTemplateInfo(url, method, argsToUrlParam, argsToJsonBody, argsToFormBody, headers, body,
				argsPosition, requestTemplate);
	}

	public static URI buildUri(UriBuilder builder, String processedUrl, RequestTemplateInfo info,
			Map<String, Object> args) {
		// 检查URL是否包含查询参数
		if (processedUrl.contains("?")) {
			// 如果URL包含查询参数，需要分别处理路径和查询参数
			String[] urlParts = processedUrl.split("\\?", 2);
			String path = urlParts[0];
			String existingQuery = urlParts.length > 1 ? urlParts[1] : "";

			// 设置路径
			builder.path(path);

			// 解析现有的查询参数
			if (!existingQuery.isEmpty()) {
				String[] queryPairs = existingQuery.split("&");
				for (String pair : queryPairs) {
					if (!pair.isEmpty()) {
						String[] keyValue = pair.split("=", 2);
						if (keyValue.length == 2) {
							builder.queryParam(keyValue[0], keyValue[1]);
						}
						else if (keyValue.length == 1) {
							builder.queryParam(keyValue[0], "");
						}
					}
				}
			}
		}
		else {
			// 如果URL不包含查询参数，直接设置路径
			builder.path(processedUrl);
		}

		// 添加额外的查询参数
		for (Map.Entry<String, Object> entry : args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			boolean addToQuery = info.argsToUrlParam;
			if (info.argsPosition != null && info.argsPosition.has(key)) {
				String position = info.argsPosition.path(key).asText();
				addToQuery = "query".equals(position);
			}
			if (addToQuery && value != null) {
				if (value instanceof final Collection<?> collection) {
					for (Object item : collection) {
						builder.queryParam(key, item);
					}
				}
				else if (value instanceof Map<?, ?> map) {
					for (Map.Entry<?, ?> kvEntry : map.entrySet()) {
						if (kvEntry.getKey() != null && kvEntry.getValue() != null) {
							builder.queryParam(kvEntry.getKey().toString(), kvEntry.getValue());
						}
					}
				}
				else {
					builder.queryParam(key, value);
				}
			}
		}
		return builder.build();
	}

	private static void handleCookies(WebClient.RequestBodySpec requestSpec, MultiValueMap<String, String> headers,
			RequestTemplateInfo info, Map<String, Object> args) {

		// 从args中查找cookie相关参数
		StringBuilder cookieBuilder = new StringBuilder();

		for (Map.Entry<String, Object> entry : args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value != null) {
				// 检查是否指定了cookie位置
				if (info.argsPosition != null && info.argsPosition.has(key)) {
					String position = info.argsPosition.path(key).asText();
					if ("cookie".equals(position)) {
						if (!cookieBuilder.isEmpty()) {
							cookieBuilder.append("; ");
						}
						cookieBuilder.append(key).append("=").append(value.toString());
					}
				}
			}
		}
		// 如果有cookie，添加到请求头
		if (!cookieBuilder.isEmpty()) {
			String cookieHeader = cookieBuilder.toString();
			requestSpec.header("Cookie", cookieHeader);
			headers.add("Cookie", cookieHeader);
		}
	}

	public static MultiValueMap<String, String> addHeaders(WebClient.RequestBodySpec requestSpec,
			RequestTemplateInfo info, Map<String, Object> args,
			BiFunction<String, Map<String, Object>, String> templateProcessor) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		JsonNode headersNode = info.headers;
		if (headersNode != null && headersNode.isArray()) {
			for (JsonNode header : headersNode) {
				String key = header.path("key").asText();
				String valueTemplate = header.path("value").asText();
				Map<String, Object> params = new HashMap<>();
				params.put("args", args);
				params.put("extendedData", "");
				String value = templateProcessor.apply(valueTemplate, params);
				requestSpec.header(key, value);
				headers.add(key, value);
			}
		}

		handleCookies(requestSpec, headers, info, args);
		for (Map.Entry<String, Object> entry : args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			boolean addToHeader = false;
			if (info.argsPosition != null && info.argsPosition.has(key)) {
				String position = info.argsPosition.path(key).asText();
				addToHeader = "header".equals(position);
			}
			if (addToHeader && value != null) {
				if (value instanceof final Collection<?> collection) {
					for (Object item : collection) {
						requestSpec.header(key, String.valueOf(item));
						headers.add(key, String.valueOf(item));
					}
				}
				else {
					requestSpec.header(key, String.valueOf(value));
					headers.add(key, String.valueOf(value));
				}
			}
		}
		return headers;
	}

	public static String addPathVariables(String url, RequestTemplateInfo info, Map<String, Object> args) {
		if (url == null || url.isEmpty() || args == null || args.isEmpty()) {
			return url;
		}

		Matcher matcher = PATH_VARIABLES_PATTERN.matcher(url);

		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String variableName = matcher.group(1);
			if (info.argsPosition != null && info.argsPosition.has(variableName)) {
				String position = info.argsPosition.path(variableName).asText();
				if ("path".equals(position)) {
					Object value = args.get(variableName);
					String replacement = value != null ? value.toString() : matcher.group(0);
					matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
				}
			}
		}
		matcher.appendTail(result);

		return result.toString();
	}

	public static WebClient.RequestHeadersSpec<?> addRequestBody(WebClient.RequestBodySpec requestSpec,
			MultiValueMap<String, String> headers, RequestTemplateInfo info, Map<String, Object> args,
			java.util.function.BiFunction<String, Map<String, Object>, String> templateProcessor,
			ObjectMapper objectMapper, Logger logger) {
		boolean hasBody = info.body != null && !info.body.asText().isEmpty();
		int optionCount = (hasBody ? 1 : 0) + (info.argsToJsonBody ? 1 : 0) + (info.argsToFormBody ? 1 : 0)
				+ (info.argsToUrlParam ? 1 : 0);
		if (optionCount > 1) {
			throw new IllegalArgumentException(
					"Only one of body, argsToJsonBody, argsToFormBody, or argsToUrlParam should be specified");
		}
		if (hasBody) {
			String bodyTemplate = info.body.asText();
			Map<String, Object> params = new HashMap<>();
			params.put("args", args);
			params.put("extendedData", "");
			String processedBody = templateProcessor.apply(bodyTemplate, params);
			return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.bodyValue(processedBody);
		}
		else {
			String bodyType = info.argsToFormBody ? "form" : "json";
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			Map<String, Object> jsonData = new HashMap<>();
			if (!info.argsToJsonBody && !info.argsToFormBody) {
				String contentType = headers.getFirst("Content-Type");
				if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
					bodyType = "form";
				}
				else {
					bodyType = "json";
				}
			}
			for (Map.Entry<String, Object> entry : args.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (value == null) {
					continue;
				}
				boolean addToBody = info.argsToFormBody || info.argsToJsonBody;
				if (info.argsPosition != null && info.argsPosition.has(key)) {
					String position = info.argsPosition.path(key).asText();
					addToBody = "body".equals(position);
				}
				if (addToBody) {
					formData.add(key, value.toString());
					jsonData.put(key, value);
				}
			}
			if (formData.isEmpty()) {
				return requestSpec;
			}
			if ("json".equals(bodyType)) {
				try {
					String jsonBody = objectMapper.writeValueAsString(jsonData);
					return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.bodyValue(jsonBody);
				}
				catch (com.fasterxml.jackson.core.JsonProcessingException e) {
					logger.error("Failed to create JSON request body", e);
					return requestSpec;
				}
			}
			else {
				return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
					.body(BodyInserters.fromFormData(formData));
			}
		}
	}

}
