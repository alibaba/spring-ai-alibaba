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

/**
 * DashScope Video Generation API client.
 *
 * @author dashscope
 */
public class DashScopeVideoApi {

	private final RestClient restClient;

	private final WebClient webClient;

	private final ResponseErrorHandler responseErrorHandler;

	public DashScopeVideoApi(RestClient restClient, WebClient webClient, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClient;
		this.webClient = webClient;
		this.responseErrorHandler = responseErrorHandler;
	}

	/**
	 * Submit video generation task.
	 */
	public ResponseEntity<VideoGenerationResponse> submitTask(VideoGenerationRequest request) {
		return this.restClient.post()
			.uri("/api/v1/services/aigc/text2video/generation")
			.body(request)
			.retrieve()
			.toEntity(VideoGenerationResponse.class);
	}

	/**
	 * Query video generation task status.
	 */
	public ResponseEntity<VideoGenerationResponse> queryTask(String taskId) {
		return this.restClient.get()
			.uri("/api/v1/services/aigc/text2video/generation/{taskId}", taskId)
			.retrieve()
			.toEntity(VideoGenerationResponse.class);
	}

	/**
	 * Video generation request.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class VideoGenerationRequest {

		@JsonProperty("model")
		private String model;

		@JsonProperty("input")
		private VideoInput input;

		@JsonProperty("parameters")
		private VideoParameters parameters;

		public VideoGenerationRequest() {
		}

		public VideoGenerationRequest(String model, VideoInput input, VideoParameters parameters) {
			this.model = model;
			this.input = input;
			this.parameters = parameters;
		}

		public String getModel() {
			return this.model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public VideoInput getInput() {
			return this.input;
		}

		public void setInput(VideoInput input) {
			this.input = input;
		}

		public VideoParameters getParameters() {
			return this.parameters;
		}

		public void setParameters(VideoParameters parameters) {
			this.parameters = parameters;
		}

		/**
		 * Video input parameters.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoInput {

			@JsonProperty("prompt")
			private String prompt;

			public VideoInput() {
			}

			public VideoInput(String prompt) {
				this.prompt = prompt;
			}

			public String getPrompt() {
				return this.prompt;
			}

			public void setPrompt(String prompt) {
				this.prompt = prompt;
			}

		}

		/**
		 * Video generation parameters.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoParameters {

			@JsonProperty("width")
			private Integer width;

			@JsonProperty("height")
			private Integer height;

			@JsonProperty("duration")
			private Integer duration;

			@JsonProperty("fps")
			private Integer fps;

			@JsonProperty("seed")
			private Long seed;

			@JsonProperty("num_frames")
			private Integer numFrames;

			public VideoParameters() {
			}

			public VideoParameters(Integer width, Integer height, Integer duration, Integer fps, Long seed,
					Integer numFrames) {
				this.width = width;
				this.height = height;
				this.duration = duration;
				this.fps = fps;
				this.seed = seed;
				this.numFrames = numFrames;
			}

			public Integer getWidth() {
				return this.width;
			}

			public void setWidth(Integer width) {
				this.width = width;
			}

			public Integer getHeight() {
				return this.height;
			}

			public void setHeight(Integer height) {
				this.height = height;
			}

			public Integer getDuration() {
				return this.duration;
			}

			public void setDuration(Integer duration) {
				this.duration = duration;
			}

			public Integer getFps() {
				return this.fps;
			}

			public void setFps(Integer fps) {
				this.fps = fps;
			}

			public Long getSeed() {
				return this.seed;
			}

			public void setSeed(Long seed) {
				this.seed = seed;
			}

			public Integer getNumFrames() {
				return this.numFrames;
			}

			public void setNumFrames(Integer numFrames) {
				this.numFrames = numFrames;
			}

		}

	}

	/**
	 * Video generation response.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class VideoGenerationResponse {

		@JsonProperty("request_id")
		private String requestId;

		@JsonProperty("code")
		private String code;

		@JsonProperty("message")
		private String message;

		@JsonProperty("output")
		private VideoOutput output;

		@JsonProperty("usage")
		private VideoUsage usage;

		public VideoGenerationResponse() {
		}

		public String getRequestId() {
			return this.requestId;
		}

		public void setRequestId(String requestId) {
			this.requestId = requestId;
		}

		public String getCode() {
			return this.code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public VideoOutput getOutput() {
			return this.output;
		}

		public void setOutput(VideoOutput output) {
			this.output = output;
		}

		public VideoUsage getUsage() {
			return this.usage;
		}

		public void setUsage(VideoUsage usage) {
			this.usage = usage;
		}

		/**
		 * Video output.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoOutput {

			@JsonProperty("task_id")
			private String taskId;

			@JsonProperty("task_status")
			private String taskStatus;

			@JsonProperty("results")
			private VideoResult[] results;

			public VideoOutput() {
			}

			public String getTaskId() {
				return this.taskId;
			}

			public void setTaskId(String taskId) {
				this.taskId = taskId;
			}

			public String getTaskStatus() {
				return this.taskStatus;
			}

			public void setTaskStatus(String taskStatus) {
				this.taskStatus = taskStatus;
			}

			public VideoResult[] getResults() {
				return this.results;
			}

			public void setResults(VideoResult[] results) {
				this.results = results;
			}

			/**
			 * Video result.
			 */
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public static class VideoResult {

				@JsonProperty("url")
				private String url;

				@JsonProperty("video_url")
				private String videoUrl;

				public VideoResult() {
				}

				public String getUrl() {
					return this.url;
				}

				public void setUrl(String url) {
					this.url = url;
				}

				public String getVideoUrl() {
					return this.videoUrl;
				}

				public void setVideoUrl(String videoUrl) {
					this.videoUrl = videoUrl;
				}

			}

		}

		/**
		 * Video usage.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoUsage {

			@JsonProperty("total_tokens")
			private Integer totalTokens;

			public VideoUsage() {
			}

			public Integer getTotalTokens() {
				return this.totalTokens;
			}

			public void setTotalTokens(Integer totalTokens) {
				this.totalTokens = totalTokens;
			}

		}

	}

}
