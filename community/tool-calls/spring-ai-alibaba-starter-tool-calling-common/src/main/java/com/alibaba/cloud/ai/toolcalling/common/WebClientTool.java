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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author vlsmb
 */
public class WebClientTool {

	private static final Logger log = LoggerFactory.getLogger(WebClientTool.class);

	private final WebClient webClient;

	private final JsonParseTool jsonParseTool;

	private final CommonToolCallProperties properties;

	private ReactorClientHttpConnector createHttpConnector() {
		return new ReactorClientHttpConnector(
				HttpClient.create().responseTimeout(Duration.ofMinutes(properties.getNetworkTimeout())));
	}

	private ExchangeFilterFunction logRequest() {
		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			if (log.isDebugEnabled()) {
				log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());

				clientRequest.headers()
					.forEach(
							(name, values) -> values.forEach(value -> log.debug("Request Header: {}={}", name, value)));
			}
			return Mono.just(clientRequest);
		});
	}

	private ExchangeFilterFunction logResponse() {
		return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
			if (log.isDebugEnabled()) {
				log.debug("Response Status: {}", clientResponse.statusCode());

				clientResponse.headers()
					.asHttpHeaders()
					.forEach((name, values) -> values
						.forEach(value -> log.debug("Response Header: {}={}", name, value)));
			}
			return Mono.just(clientResponse);
		});
	}

	/**
	 * default webClient
	 */
	public WebClientTool(JsonParseTool jsonParseTool, CommonToolCallProperties properties) {
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
		this.webClient = WebClient.builder()
			.clientConnector(createHttpConnector())
			.baseUrl(properties.getBaseUrl())
			.defaultStatusHandler(HttpStatusCode::is4xxClientError,
					CommonToolCallConstants.DEFAULT_WEBCLIENT_4XX_EXCEPTION)
			.defaultStatusHandler(HttpStatusCode::is5xxServerError,
					CommonToolCallConstants.DEFAULT_WEBCLIENT_5XX_EXCEPTION)
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(CommonToolCallConstants.MAX_MEMORY_SIZE))
			.build();
	}

	/**
	 * default webClient with customized HeaderConsumer
	 */
	public WebClientTool(Consumer<HttpHeaders> httpHeadersConsumer, JsonParseTool jsonParseTool,
			CommonToolCallProperties properties) {
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
		this.webClient = WebClient.builder()
			.clientConnector(createHttpConnector())
			.baseUrl(properties.getBaseUrl())
			.defaultHeaders(httpHeadersConsumer)
			.defaultStatusHandler(HttpStatusCode::is4xxClientError,
					CommonToolCallConstants.DEFAULT_WEBCLIENT_4XX_EXCEPTION)
			.defaultStatusHandler(HttpStatusCode::is5xxServerError,
					CommonToolCallConstants.DEFAULT_WEBCLIENT_5XX_EXCEPTION)
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(CommonToolCallConstants.MAX_MEMORY_SIZE))
			.filter(logRequest())
			.filter(logResponse())
			.build();
	}

	/**
	 * Creates webClient with customized HeaderConsumer and ExceptionFunction
	 */
	public WebClientTool(Consumer<HttpHeaders> httpHeadersConsumer,
			Function<ClientResponse, Mono<? extends Throwable>> is4xxException,
			Function<ClientResponse, Mono<? extends Throwable>> is5xxException, CommonToolCallProperties properties,
			JsonParseTool jsonParseTool) {
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
		this.webClient = WebClient.builder()
			.clientConnector(createHttpConnector())
			.baseUrl(properties.getBaseUrl())
			.defaultHeaders(httpHeadersConsumer)
			.defaultStatusHandler(HttpStatusCode::is4xxClientError, is4xxException)
			.defaultStatusHandler(HttpStatusCode::is5xxServerError, is5xxException)
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(CommonToolCallConstants.MAX_MEMORY_SIZE))
			.filter(logRequest())
			.filter(logResponse())
			.build();
	}

	public Mono<String> get(String uri, MultiValueMap<String, String> params, Map<String, ?> variables) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path(uri).queryParams(params).build(variables))
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
					response -> Mono
						.error(new RuntimeException("Client error, code: " + response.statusCode().value())))
			.onStatus(HttpStatusCode::is5xxServerError,
					response -> Mono
						.error(new RuntimeException("Server error, code: " + response.statusCode().value())))
			.bodyToMono(String.class);
	}

	public Mono<String> get(String uri, MultiValueMap<String, String> params) {
		return this.get(uri, params, new HashMap<>());
	}

	public Mono<String> get(String uri, Map<String, ?> variables) {
		return this.get(uri, new LinkedMultiValueMap<>(), variables);
	}

	public Mono<String> get(String uri) {
		return this.get(uri, new HashMap<>());
	}

	public <T> Mono<String> post(String uri, MultiValueMap<String, String> params, Map<String, ?> variables, T value,
			MediaType mediaType) {
		return Mono.fromCallable(() -> {
			if (mediaType.equals(MediaType.APPLICATION_JSON))
				return jsonParseTool.objectToJson(value);
			else
				return value;
		})
			.flatMap(json -> webClient.post()
				.uri(uriBuilder -> uriBuilder.path(uri).queryParams(params).build(variables))
				.contentType(mediaType)
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

	/**
	 * post json object
	 */
	public <T> Mono<String> post(String uri, MultiValueMap<String, String> params, Map<String, ?> variables, T value) {
		return this.post(uri, params, variables, value, MediaType.APPLICATION_JSON);
	}

	/**
	 * post json object
	 */
	public <T> Mono<String> post(String uri, Map<String, ?> variables, T value) {
		return this.post(uri, new LinkedMultiValueMap<>(), variables, value);
	}

	/**
	 * post json object
	 */
	public <T> Mono<String> post(String uri, MultiValueMap<String, String> params, T value) {
		return this.post(uri, params, new HashMap<>(), value);
	}

	/**
	 * post json object
	 */
	public <T> Mono<String> post(String uri, T value) {
		return this.post(uri, new HashMap<>(), value);
	}

	public WebClient getWebClient() {
		return webClient;
	}

}
