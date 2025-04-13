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
package com.alibaba.cloud.ai.toolcalling.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author vlsmb
 */
public class RestClientService {

	private final RestClient restClient;

	private final JsonParseService jsonParseService;

	private final CommonToolCallProperties properties;

	public RestClientService(JsonParseService jsonParseService, CommonToolCallProperties properties) {
		this.jsonParseService = jsonParseService;
		this.properties = properties;
		this.restClient = RestClient.builder().build();
	}

	public RestClientService(String baseUrl, Consumer<HttpHeaders> httpHeadersConsumer,
			CommonToolCallProperties properties, JsonParseService jsonParseService) {
		this.jsonParseService = jsonParseService;
		this.properties = properties;
		this.restClient = RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.defaultHeaders(httpHeadersConsumer)
			.build();
	}

	public String get(String uri, Map<String, Object> variables) {
		return restClient.get()
			.uri(uriBuilder -> uriBuilder.path(uri).build(variables))
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
				throw new RuntimeException("Client error, code: " + response.getStatusCode());
			})
			.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
				throw new RuntimeException("Server error, code: " + response.getStatusCode());
			})
			.body(String.class);
	}

	public String get(String uri) {
		return this.get(uri, new HashMap<>());
	}

	public <T> String post(String uri, Map<String, Object> variables, T value) {
		try {
			return restClient.post()
				.uri(uriBuilder -> uriBuilder.path(uri).build(variables))
				.contentType(MediaType.APPLICATION_JSON)
				.body(jsonParseService.objectToJson(value))
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					throw new RuntimeException("Client error, code: " + response.getStatusCode());
				})
				.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					throw new RuntimeException("Server error, code: " + response.getStatusCode());
				})
				.body(String.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Serialization failed", e);
		}
	}

	public <T> String post(String uri, T value) {
		return this.post(uri, new HashMap<>(), value);
	}

}
