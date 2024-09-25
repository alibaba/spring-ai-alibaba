/*
 * Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

import java.util.List;

/**
 * @author linkesheng.lks
 * @since 1.0.0-M2
 */
public class DashScopeAgentApi {

	private final RestClient restClient;

	private final WebClient webClient;

	public DashScopeAgentApi(String apiKey) {
		this(DEFAULT_BASE_URL, apiKey, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeAgentApi(String apiKey, String workSpaceId) {
		this(DEFAULT_BASE_URL, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeAgentApi(String baseUrl, String apiKey, String workSpaceId) {
		this(baseUrl, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeAgentApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey))
			.defaultStatusHandler(responseErrorHandler)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, null, true))
			.build();
	}

	public DashScopeAgentApi(String baseUrl, String apiKey, String workSpaceId, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId))
			.defaultStatusHandler(responseErrorHandler)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId, true))
			.build();
	}

	public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest request) {
		String uri = "/api/v1/apps/" + request.app_id() + "/completion";
		return restClient.post().uri(uri).body(request).retrieve().toEntity(DashScopeAgentResponse.class);
	}

	public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest request) {
		String uri = "/api/v1/apps/" + request.app_id() + "/completion";
		return webClient.post()
			.uri(uri)
			.body(Mono.just(request), DashScopeAgentResponse.class)
			.retrieve()
			.bodyToFlux(DashScopeAgentResponse.class)
			.handle((data, sink) -> {
				sink.next(data);
			});
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DashScopeAgentRequest(String app_id, @JsonProperty("input") DashScopeAgentRequestInput input,
			@JsonProperty("parameters") DashScopeAgentRequestParameters parameters) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeAgentRequestInput(@JsonProperty("prompt") String prompt,
				@JsonProperty("session_id") String sessionId, @JsonProperty("memory_id") String memoryId,
				@JsonProperty("biz_params") JsonNode bizParams) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeAgentRequestParameters(@JsonProperty("has_thoughts") Boolean hasThoughts,
				@JsonProperty("incremental_output") Boolean incrementalOutput

		) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DashScopeAgentResponse(@JsonProperty("status_code") Integer statusCode,
			@JsonProperty("request_id") String requestId, @JsonProperty("code") String code,
			@JsonProperty("message") String message, @JsonProperty("output") DashScopeAgentResponseOutput output,
			@JsonProperty("usage") DashScopeAgentResponseUsage usage) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeAgentResponseOutput(@JsonProperty("text") String text,
				@JsonProperty("finish_reason") String finishReason, @JsonProperty("session_id") String sessionId,
				@JsonProperty("thoughts") List<DashScopeAgentResponseOutputThoughts> thoughts,
				@JsonProperty("doc_references") List<DashScopeAgentResponseOutputDocReference> docReferences) {
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record DashScopeAgentResponseOutputThoughts(@JsonProperty("thought") String thought,
					@JsonProperty("action_type") String actionType, @JsonProperty("action_name") String actionName,
					@JsonProperty("action") String action,
					@JsonProperty("action_input_stream") String actionInputStream,
					@JsonProperty("action_input") String actionInput, @JsonProperty("response") String response,
					@JsonProperty("observation") String observation) {
			}

			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record DashScopeAgentResponseOutputDocReference(@JsonProperty("index_id") String indexId,
					@JsonProperty("title") String title, @JsonProperty("doc_id") String docId,
					@JsonProperty("doc_name") String docName, @JsonProperty("text") String text,
					@JsonProperty("images") List<String> images) {
			}
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeAgentResponseUsage(
				@JsonProperty("models") List<DashScopeAgentResponseUsageModels> models) {
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record DashScopeAgentResponseUsageModels(@JsonProperty("model_id") String modelId,
					@JsonProperty("input_tokens") Integer inputTokens,
					@JsonProperty("output_tokens") Integer outputTokens) {
			}
		}
	}

}
