package com.alibaba.cloud.ai.toolcalling.commontoolcall;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class WebClientService {

	private final WebClient webClient;

	private final JsonParseService jsonParseService;

	public WebClientService(JsonParseService jsonParseService) {
		this.jsonParseService = jsonParseService;
		this.webClient = WebClient.builder().build();
	}

	public WebClientService(String baseUrl, Consumer<HttpHeaders> httpHeadersConsumer,
			JsonParseService jsonParseService) {
		this.webClient = WebClient.builder().baseUrl(baseUrl).defaultHeaders(httpHeadersConsumer).build();
		this.jsonParseService = jsonParseService;
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
