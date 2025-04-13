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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author vlsmb
 */
public class WebClientService {

	private final WebClient webClient;

	private final JsonParseService jsonParseService;

	private final CommonToolCallProperties properties;

	public WebClientService(JsonParseService jsonParseService, CommonToolCallProperties properties) {
		this.jsonParseService = jsonParseService;
		this.properties = properties;
		this.webClient = WebClient.builder().build();
	}

	public WebClientService(Consumer<HttpHeaders> httpHeadersConsumer, CommonToolCallProperties properties,
			JsonParseService jsonParseService) {
		this.webClient = WebClient.builder()
			.baseUrl(properties.getBaseUrl())
			.defaultHeaders(httpHeadersConsumer)
			.build();
		this.jsonParseService = jsonParseService;
		this.properties = properties;
	}

	public Mono<String> get(String uri, Map<String, Object> variables) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path(uri).build(variables))
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
					response -> Mono
						.error(new RuntimeException("Client error, code: " + response.statusCode().value())))
			.onStatus(HttpStatusCode::is5xxServerError,
					response -> Mono
						.error(new RuntimeException("Server error, code: " + response.statusCode().value())))
			.bodyToMono(String.class);
	}

	public Mono<String> get(String uri) {
		return this.get(uri, new HashMap<>());
	}

	public <T> Mono<String> post(String uri, Map<String, Object> variables, T value) {
		return Mono.fromCallable(() -> jsonParseService.objectToJson(value))
			.flatMap(json -> webClient.post()
				.uri(uriBuilder -> uriBuilder.path(uri).build(variables))
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError,
						response -> Mono
							.error(new RuntimeException("Client error, code: " + response.statusCode().value())))
				.onStatus(HttpStatusCode::is5xxServerError,
						response -> Mono
							.error(new RuntimeException("Server error, code: " + response.statusCode().value())))
				.bodyToMono(String.class))
			.onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Serialization failed", e));
	}

	public <T> Mono<String> post(String uri, T value) {
		return this.post(uri, new HashMap<>(), value);
	}

}
