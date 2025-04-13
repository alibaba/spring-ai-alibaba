package com.alibaba.cloud.ai.toolcalling.commontoolcall;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RestClientService {

	private final RestClient restClient;

	private final JsonParseService jsonParseService;

	public RestClientService(JsonParseService jsonParseService) {
		this.jsonParseService = jsonParseService;
		this.restClient = RestClient.builder().build();
	}

	public RestClientService(String baseUrl, Consumer<HttpHeaders> httpHeadersConsumer,
			JsonParseService jsonParseService) {
		this.jsonParseService = jsonParseService;
		this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeaders(httpHeadersConsumer).build();
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
