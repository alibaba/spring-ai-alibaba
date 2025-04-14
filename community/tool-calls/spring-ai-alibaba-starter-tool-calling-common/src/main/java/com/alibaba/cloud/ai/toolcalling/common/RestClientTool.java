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
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author vlsmb
 */
public class RestClientTool {

	private final RestClient restClient;

	private final JsonParseTool jsonParseTool;

	private final CommonToolCallProperties properties;

	private HttpComponentsClientHttpRequestFactory createRequestFactory() {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(Timeout.of(properties.getNetworkTimeout(), TimeUnit.MINUTES))
			.setConnectTimeout(Timeout.of(properties.getNetworkTimeout(), TimeUnit.MINUTES))
			.setResponseTimeout(Timeout.of(properties.getNetworkTimeout(), TimeUnit.MINUTES))
			.build();
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	/**
	 * default restClient
	 */
	public RestClientTool(JsonParseTool jsonParseTool, CommonToolCallProperties properties) {
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
		this.restClient = RestClient.builder()
			.requestFactory(createRequestFactory())
			.baseUrl(properties.getBaseUrl())
			.defaultStatusHandler(CommonToolCallConstants.DEFAULT_RESTCLIENT_ERROR_HANDLER)
			.build();
	}

	/**
	 * Creates restClient with customized HeaderConsumer and ErrorHandler
	 */
	public RestClientTool(Consumer<HttpHeaders> httpHeadersConsumer, ResponseErrorHandler errorHandler,
			CommonToolCallProperties properties, JsonParseTool jsonParseTool) {
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
		this.restClient = RestClient.builder()
			.requestFactory(createRequestFactory())
			.baseUrl(properties.getBaseUrl())
			.defaultHeaders(httpHeadersConsumer)
			.defaultStatusHandler(errorHandler)
			.build();
	}

	public String get(String uri, MultiValueMap<String, String> params, Map<String, ?> variables) {
		return restClient.get()
			.uri(uriBuilder -> uriBuilder.path(uri).queryParams(params).build(variables))
			.retrieve()
			.body(String.class);
	}

	public String get(String uri, MultiValueMap<String, String> params) {
		return this.get(uri, params, new HashMap<>());
	}

	public String get(String uri, Map<String, ?> variables) {
		return this.get(uri, new LinkedMultiValueMap<>(), variables);
	}

	public String get(String uri) {
		return this.get(uri, new HashMap<>());
	}

	public <T> String post(String uri, MultiValueMap<String, String> params, Map<String, ?> variables, T value,
			MediaType mediaType) {
		return restClient.post()
			.uri(uriBuilder -> uriBuilder.path(uri).queryParams(params).build(variables))
			.contentType(mediaType)
			.body(value)
			.retrieve()
			.body(String.class);
	}

	public <T> String post(String uri, MultiValueMap<String, String> params, Map<String, ?> variables, T value) {
		try {
			return this.post(uri, params, variables, jsonParseTool.objectToJson(value), MediaType.APPLICATION_JSON);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Serialization failed", e);
		}
	}

	/**
	 * post json object
	 */
	public <T> String post(String uri, Map<String, ?> variables, T value) {
		return this.post(uri, new LinkedMultiValueMap<>(), variables, value);
	}

	/**
	 * post json object
	 */
	public <T> String post(String uri, MultiValueMap<String, String> params, T value) {
		return this.post(uri, params, new HashMap<>(), value);
	}

	/**
	 * post json object
	 */
	public <T> String post(String uri, T value) {
		return this.post(uri, new HashMap<>(), value);
	}

}
