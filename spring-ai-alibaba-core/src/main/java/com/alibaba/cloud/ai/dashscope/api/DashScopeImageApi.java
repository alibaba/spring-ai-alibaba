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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public class DashScopeImageApi {

	public static final String DEFAULT_IMAGE_MODEL = ImageModel.WANX_V1.getValue();

	private final RestClient restClient;

	public DashScopeImageApi(String apiKey) {
		this(DEFAULT_BASE_URL, apiKey, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeImageApi(String apiKey, String workSpaceId) {
		this(DEFAULT_BASE_URL, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeImageApi(String baseUrl, String apiKey, String workSpaceId) {
		this(baseUrl, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeImageApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey))
			.defaultStatusHandler(responseErrorHandler)
			.build();
	}

	public DashScopeImageApi(String baseUrl, String apiKey, String workSpaceId, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId))
			.defaultStatusHandler(responseErrorHandler)
			.build();
	}

	public ResponseEntity<DashScopeImageAsyncReponse> submitImageGenTask(DashScopeImageRequest request) {
		return this.restClient.post()
			.uri("/api/v1/services/aigc/text2image/image-synthesis")
			// issue: https://github.com/alibaba/spring-ai-alibaba/issues/29
			.header("X-DashScope-Async","enable")
			.body(request)
			.retrieve()
			.toEntity(DashScopeImageAsyncReponse.class);
	}

	public ResponseEntity<DashScopeImageAsyncReponse> getImageGenTaskResult(String taskId) {
		return this.restClient.get()
			.uri("/api/v1/tasks/{task_id}", taskId)
			.retrieve()
			.toEntity(DashScopeImageAsyncReponse.class);
	}

	/*******************************************
	 * Embedding相关
	 **********************************************/

	public enum ImageModel {

		WANX_V1("wanx-v1");

		public final String value;

		ImageModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DashScopeImageRequest(@JsonProperty("model") String model,
			@JsonProperty("input") DashScopeImageRequestInput input,
			@JsonProperty("parameters") DashScopeImageRequestParameter parameters

	) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageRequestInput(@JsonProperty("prompt") String prompt,
				@JsonProperty("negative_prompt") String negativePrompt, @JsonProperty("ref_img") String refImg) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageRequestParameter(@JsonProperty("style") String style,
				@JsonProperty("size") String size, @JsonProperty("n") Integer n, @JsonProperty("seed") Integer seed,
				@JsonProperty("ref_strength") Float refStrength, @JsonProperty("ref_mode") String refMode) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DashScopeImageAsyncReponse(@JsonProperty("request_id") String requestId,
			@JsonProperty("output") DashScopeImageAsyncReponseOutput output,
			@JsonProperty("usage") DashScopeImageAsyncReponseUsage usage) {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageAsyncReponseOutput(@JsonProperty("task_id") String taskId,
				@JsonProperty("task_status") String taskStatus,
				@JsonProperty("results") List<DashScopeImageAsyncReponseResult> results,
				@JsonProperty("task_metrics") DashScopeImageAsyncReponseTaskMetrics taskMetrics,
				@JsonProperty("code") String code, @JsonProperty("message") String message) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageAsyncReponseTaskMetrics(@JsonProperty("TOTAL") Integer total,
				@JsonProperty("SUCCEEDED") Integer SUCCEEDED, @JsonProperty("FAILED") Integer FAILED) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageAsyncReponseUsage(@JsonProperty("image_count") Integer imageCount) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageAsyncReponseResult(@JsonProperty("url") String url) {
		}
	}

}
