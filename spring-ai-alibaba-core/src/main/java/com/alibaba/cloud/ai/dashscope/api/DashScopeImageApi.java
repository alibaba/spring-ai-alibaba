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
package com.alibaba.cloud.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.ENABLED;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_ASYNC;

/**
 * @author nuocheng.lxm
 * @author yuluo-yx
 * @author Soryu
 */

public class DashScopeImageApi {

	public static final String DEFAULT_IMAGE_MODEL = ImageModel.WANX_V1.getValue();

	private final RestClient restClient;

	// format: off
	public DashScopeImageApi(String baseUrl, String apiKey, String workSpaceId, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId))
			.defaultStatusHandler(responseErrorHandler)
			.build();
	}

	public ResponseEntity<DashScopeImageAsyncReponse> submitImageGenTask(DashScopeImageRequest request) {

		String baseUrl = "/api/v1/services/aigc/";
		String model = request.model();
		String endpoint = model.equals(ImageModel.WANX2_1_IMAGE_EDIT.getValue())
				|| model.equals(ImageModel.WANX_X_PAINTING.getValue())
				|| model.equals(ImageModel.WANX_SKETCH_TO_IMAGE_LITE.getValue())
				|| model.equals(ImageModel.IMAGE_OUT_PAINTING.getValue()) ? "image2image" : "text2image";

		String url = baseUrl + endpoint + "/image-synthesis";

		return this.restClient.post()
			.uri(url)
			// todo: add workspaceId header
			.header(HEADER_ASYNC, ENABLED)
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

	public enum ImageModel {

		// WANX V1 models.
		WANX_V1("wanx-v1"),

		// WANX V2 models.
		WANX2_1_T2I_TURBO("wanx2.1-t2i-turbo"), WANX2_1_T2I_PLUS("wanx2.1-t2i-plus"),
		WANX2_0_T2I_TURBO("wanx2.0-t2i-turbo"),

		// WANX Image edit model.
		WANX2_1_IMAGE_EDIT("wanx2.1-imageedit"),

		// Images doodle painting
		WANX_SKETCH_TO_IMAGE_LITE("wanx-sketch-to-image-lite"),

		// Image partial repainting
		WANX_X_PAINTING("wanx-x-painting"),

		// Image screen expansion.
		IMAGE_OUT_PAINTING("image-out-painting");

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
				@JsonProperty("negative_prompt") String negativePrompt, @JsonProperty("ref_img") String refImg,
				@JsonProperty("function") String function, @JsonProperty("base_image_url") String baseImageUrl,
				@JsonProperty("mask_image_url") String maskImageUrl,
				@JsonProperty("sketch_image_url") String sketchImageUrl) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DashScopeImageRequestParameter(@JsonProperty("style") String style,
				@JsonProperty("size") String size, @JsonProperty("n") Integer n, @JsonProperty("seed") Integer seed,
				@JsonProperty("ref_strength") Float refStrength, @JsonProperty("ref_mode") String refMode,
				@JsonProperty("prompt_extend") Boolean promptExtend, @JsonProperty("watermark") Boolean watermark,

				@JsonProperty("sketch_weight") Integer sketchWeight,
				@JsonProperty("sketch_extraction") Boolean sketchExtraction,
				@JsonProperty("sketch_color") Integer[][] sketchColor,
				@JsonProperty("mask_color") Integer[][] maskColor) {
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
	// format: on

	// todo: add image options builder.

}
