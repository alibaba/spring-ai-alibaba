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

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelDescription;
import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.ResultMetadata;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.ENABLED;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.HEADER_ASYNC;

/**
 * DashScope Video Generation API client.
 *
 * @author windWheel
 * @author yuluo
 * @since 1.0.0.3
 */

public class DashScopeVideoApi {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeVideoApi.class);

	public static final String DEFAULT_VIDEO_MODEL = VideoModel.WANX2_1_T2V_TURBO.getValue();

	private final String baseUrl;

	private final ApiKey apiKey;

	private final RestClient restClient;

	private final ResponseErrorHandler responseErrorHandler;

	public Builder mutate() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public DashScopeVideoApi(String baseUrl, ApiKey apiKey, RestClient.Builder restClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.responseErrorHandler = responseErrorHandler;

		// Check API Key in headers.
		Consumer<HttpHeaders> finalHeaders = h -> {
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}
			h.setContentType(MediaType.APPLICATION_JSON);
		};

		this.restClient = restClientBuilder.clone()
			.baseUrl(baseUrl)
			.defaultHeaders(finalHeaders)
			.defaultStatusHandler(responseErrorHandler)
			.build();
	}

	/**
	 * Submit video generation task.
	 */
	public ResponseEntity<VideoGenerationResponse> submitVideoGenTask(VideoGenerationRequest request) {

		logger.debug("Submitting video generation task with options: {}", request);

		String baseUrl = "/api/v1/services/aigc";
		String firstAndLAst = "/image2video/video-synthesis";
		String normal = "/video-generation/video-synthesis";

		// Use unused uri paths based on the head and tail frames
		if (request.input.getFirstFrameUrl() != null || request.input.getLastFrameUrl() != null) {
			baseUrl += firstAndLAst;
		}
		else {
			baseUrl += normal;
		}

		return this.restClient.post()
			.uri(baseUrl)
			.body(request)
			.header(HEADER_ASYNC, ENABLED)
			.retrieve()
			.toEntity(VideoGenerationResponse.class);
	}

	/**
	 * Query video generation task status.
	 */
	public ResponseEntity<VideoGenerationResponse> queryVideoGenTask(String taskId) {
		return this.restClient.get()
			.uri("/api/v1/tasks/{taskId}", taskId)
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

			/**
			 * Reverse prompt words are used to describe content that you do not want to
			 * see in the video screen, and can limit the video screen.
			 */
			@JsonProperty("negative_prompt")
			private String negativePrompt;

			@JsonProperty("img_url")
			private String imageUrl;

			@JsonProperty("template")
			private String template;

			@JsonProperty("first_frame_url")
			private String firstFrameUrl;

			@JsonProperty("last_frame_url")
			private String lastFrameUrl;

			public VideoInput(String prompt) {
				this.prompt = prompt;
			}

			public VideoInput(String prompt, String negativePrompt) {
				this.prompt = prompt;
				this.negativePrompt = negativePrompt;
			}

			public VideoInput(String prompt, String negativePrompt, String imageUrl, String template) {
				this.prompt = prompt;
				this.negativePrompt = negativePrompt;
				this.imageUrl = imageUrl;
				this.template = template;
			}

			public VideoInput(String prompt, String negativePrompt, String imageUrl, String template,
					String firstFrameUrl, String lastFrameUrl) {
				this.prompt = prompt;
				this.negativePrompt = negativePrompt;
				this.imageUrl = imageUrl;
				this.template = template;
				this.firstFrameUrl = firstFrameUrl;
				this.lastFrameUrl = lastFrameUrl;
			}

			public String getFirstFrameUrl() {
				return firstFrameUrl;
			}

			public void setFirstFrameUrl(String firstFrameUrl) {
				this.firstFrameUrl = firstFrameUrl;
			}

			public String getLastFrameUrl() {
				return lastFrameUrl;
			}

			public void setLastFrameUrl(String lastFrameUrl) {
				this.lastFrameUrl = lastFrameUrl;
			}

			public String getNegativePrompt() {
				return this.negativePrompt;
			}

			public void setNegativePrompt(String negativePrompt) {
				this.negativePrompt = negativePrompt;
			}

			public String getImageUrl() {
				return imageUrl;
			}

			public void setImageUrl(String imageUrl) {
				this.imageUrl = imageUrl;
			}

			public String getTemplate() {
				return template;
			}

			public void setTemplate(String template) {
				this.template = template;
			}

			public String getPrompt() {
				return this.prompt;
			}

			public void setPrompt(String prompt) {
				this.prompt = prompt;
			}

			public static Builder builder() {
				return new Builder();
			}

			public static class Builder {

				private String prompt;

				private String negativePrompt;

				private String imageUrl;

				private String template;

				private String firstFrameUrl;

				private String lastFrameUrl;

				public Builder() {
				}

				public Builder prompt(String prompt) {
					this.prompt = prompt;
					return this;
				}

				public Builder negativePrompt(String negativePrompt) {
					this.negativePrompt = negativePrompt;
					return this;
				}

				public Builder imageUrl(String imageUrl) {
					this.imageUrl = imageUrl;
					return this;
				}

				public Builder template(VideoTemplate template) {
					this.template = Objects.nonNull(template) ? template.getValue() : "";
					return this;
				}

				public Builder firstFrameUrl(String firstFrameUrl) {
					this.firstFrameUrl = firstFrameUrl;
					return this;
				}

				public Builder lastFrameUrl(String lastFrameUrl) {
					this.lastFrameUrl = lastFrameUrl;
					return this;
				}

				public VideoInput build() {
					return new VideoInput(prompt, negativePrompt, imageUrl, template, firstFrameUrl, lastFrameUrl);
				}

			}

		}

		/**
		 * Video generation parameters.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoParameters {

			@JsonProperty("resolution")
			private String resolution;

			@JsonProperty("size")
			private String size;

			@JsonProperty("duration")
			private Integer duration;

			/**
			 * Random number seeds are used to control the randomness of the content
			 * generated by the model. The value range is [0, 2147483647].
			 */
			@JsonProperty("seed")
			private Long seed;

			/**
			 * Whether to enable prompt intelligent rewriting. After turning on, use the
			 * big model to intelligently rewrite the input prompt. The generation effect
			 * of shorter prompts is significantly improved, but it will increase
			 * time-consuming.
			 */
			@JsonProperty("prompt_extend")
			private Boolean promptExtend;

			public VideoParameters(String size, Integer duration, Long seed, String resolution, Boolean promptExtend) {
				this.promptExtend = promptExtend;
				this.resolution = resolution;
				this.size = size;
				this.seed = seed;
				this.duration = duration;
			}

			public String getResolution() {
				return resolution;
			}

			public void setResolution(String resolution) {
				this.resolution = resolution;
			}

			public Integer getDuration() {
				return this.duration;
			}

			public void setDuration(Integer duration) {
				this.duration = duration;
			}

			public Long getSeed() {
				return this.seed;
			}

			public void setSeed(Long seed) {
				this.seed = seed;
			}

			public Boolean getPromptExtend() {
				return this.promptExtend;
			}

			public void setPromptExtend(Boolean promptExtend) {
				this.promptExtend = promptExtend;
			}

			public String getSize() {
				return this.size;
			}

			public void setSize(String size) {
				this.size = size;
			}

			public static Builder builder() {
				return new Builder();
			}

			public static class Builder {

				private String size;

				private Integer duration;

				private Long seed;

				private Boolean promptExtend;

				private String resolution;

				public Builder() {
				}

				public Builder size(String size) {
					this.size = size;
					return this;
				}

				public Builder duration(Integer duration) {
					this.duration = duration;
					return this;
				}

				public Builder seed(Long seed) {
					this.seed = seed;
					return this;
				}

				public Builder promptExtend(Boolean promptExtend) {
					this.promptExtend = promptExtend;
					return this;
				}

				public Builder resolution(String resolution) {
					this.resolution = resolution;
					return this;
				}

				public VideoParameters build() {
					return new VideoParameters(this.size, this.duration, this.seed, this.resolution, this.promptExtend);
				}

			}

		}

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {

			private String model;

			private VideoInput input;

			private VideoParameters parameters;

			public Builder() {
			}

			public Builder model(String model) {
				this.model = model;
				return this;
			}

			public Builder input(VideoInput input) {
				this.input = input;
				return this;
			}

			public Builder parameters(VideoParameters parameters) {
				this.parameters = parameters;
				return this;
			}

			public VideoGenerationRequest build() {
				return new VideoGenerationRequest(this.model, this.input, this.parameters);
			}

		}

	}

	/**
	 * https://help.aliyun.com/zh/model-studio/text-to-video-api-reference
	 */
	public enum VideoModel implements ModelDescription {

		/**
		 * Text to Video, faster generation speed and balanced performance.
		 */
		WANX2_1_T2V_TURBO("wanx2.1-t2v-turbo"),

		/**
		 * Text to Video, The generated details are richer and the picture is more
		 * textured.
		 */
		WANX2_1_T2V_PLUS("wanx2.1-t2v-plus"),

		/**
		 * Picture-generated video, based on the first frame. The generation speed is
		 * faster, taking only one-third of the plus model, and it has a higher
		 * cost-effectiveness.
		 */
		WANX2_1_I2V_TURBO("wanx2.1-i2v-turbo"),

		/**
		 * Picture-generated video, The generated details are richer and the picture is
		 * more textured.
		 */
		WANX2_1_I2V_PLUS("wanx2.1-i2v-plus"),

		/**
		 * Generate video based on the beginning and end frames
		 */
		WANX2_1_KF2V_PLUS("wanx2.1-kf2v-plus");

		public final String value;

		VideoModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.value;
		}

	}

	/**
	 * Video generation response.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class VideoGenerationResponse implements ModelResult<VideoGenerationResponse.VideoOutput> {

		@JsonProperty("request_id")
		private String requestId;

		@JsonProperty("output")
		private VideoOutput output;

		@JsonProperty("usage")
		private VideoUsage usage;

		public VideoGenerationResponse() {
		}

		public VideoUsage getUsage() {
			return usage;
		}

		public void setUsage(VideoUsage usage) {
			this.usage = usage;
		}

		public String getRequestId() {
			return this.requestId;
		}

		public void setRequestId(String requestId) {
			this.requestId = requestId;
		}

		public VideoOutput getOutput() {
			return this.output;
		}

		@Override
		public ResultMetadata getMetadata() {

			// todo: add metadata
			return null;
		}

		public void setOutput(VideoOutput output) {
			this.output = output;
		}

		@Override
		public String toString() {
			return "VideoGenerationResponse{" + "requestId='" + requestId + '\'' + ", output=" + output + ", usage="
					+ usage + '}';
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class VideoUsage {

			@JsonProperty("video_duration")
			private Integer videoDuration;

			@JsonProperty("video_ratio")
			private String videoRatio;

			@JsonProperty("video_count")
			private Integer videoCount;

			public VideoUsage() {
			}

			public Integer getVideoDuration() {
				return this.videoDuration;
			}

			public void setVideoDuration(Integer videoDuration) {
				this.videoDuration = videoDuration;
			}

			public String getVideoRatio() {
				return this.videoRatio;
			}

			public void setVideoRatio(String videoRatio) {
				this.videoRatio = videoRatio;
			}

			public Integer getVideoCount() {
				return this.videoCount;
			}

			public void setVideoCount(Integer videoCount) {
				this.videoCount = videoCount;
			}

			@Override
			public String toString() {
				return "VideoUsage{" + "videoDuration=" + videoDuration + ", videoRatio='" + videoRatio + '\''
						+ ", videoCount=" + videoCount + '}';
			}

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

			@JsonProperty("submit_time")
			private String submitTimes;

			@JsonProperty("end_time")
			private String endTime;

			@JsonProperty("scheduled_time")
			private String scheduledTime;

			@JsonProperty("video_url")
			private String videoUrl;

			@JsonProperty("orig_prompt")
			private String origPrompt;

			@JsonProperty("actual_prompt")
			private String actualPrompt;

			@JsonProperty("code")
			private String code;

			@JsonProperty("message")
			private String message;

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

			public String getSubmitTimes() {
				return this.submitTimes;
			}

			public void setSubmitTimes(String submitTimes) {
				this.submitTimes = submitTimes;
			}

			public String getEndTime() {
				return this.endTime;
			}

			public void setEndTime(String endTime) {
				this.endTime = endTime;
			}

			public String getScheduledTime() {
				return this.scheduledTime;
			}

			public void setScheduledTime(String scheduledTime) {
				this.scheduledTime = scheduledTime;
			}

			public String getVideoUrl() {
				return this.videoUrl;
			}

			public void setVideoUrl(String videoUrl) {
				this.videoUrl = videoUrl;
			}

			public String getOrigPrompt() {
				return this.origPrompt;
			}

			public void setOrigPrompt(String origPrompt) {
				this.origPrompt = origPrompt;
			}

			public String getActualPrompt() {
				return this.actualPrompt;
			}

			public void setActualPrompt(String actualPrompt) {
				this.actualPrompt = actualPrompt;
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

			@Override
			public String toString() {
				return "VideoOutput{" + "taskId='" + taskId + '\'' + ", taskStatus='" + taskStatus + '\''
						+ ", submitTimes='" + submitTimes + '\'' + ", endTime='" + endTime + '\'' + ", scheduledTime='"
						+ scheduledTime + '\'' + ", videoUrl='" + videoUrl + '\'' + ", origPrompt='" + origPrompt + '\''
						+ ", actualPrompt='" + actualPrompt + '\'' + ", code='" + code + '\'' + ", message='" + message
						+ '\'' + '}';
			}

		}

	}

	public enum VideoTemplate {

		// 通用特效

		/**
		 * 解压捏捏
		 */
		SQUISH("squish"),

		/**
		 * 戳戳乐
		 */
		POKE("poke"),

		/**
		 * 转圈圈
		 */
		ROTATION("rotation"),

		/**
		 * 气球膨胀
		 */
		INFLATE("inflate"),

		/**
		 * 分子扩散
		 */
		DISSOLVE("dissolve"),

		// 单人特效

		/**
		 * 时光木马
		 */
		CAROUSEL("carousel"),

		/**
		 * 爱你哟
		 */
		SINGLEHEART("singleheart"),

		/**
		 * 摇摆时刻
		 */
		DANCE1("dance1"),

		/**
		 * 头号甩舞
		 */
		DANCE2("dance2"),

		/**
		 * 星摇时刻
		 */
		DANCE3("dance3"),

		/**
		 * 人鱼觉醒
		 */
		MERMAID("mermaid"),

		/**
		 * 学术加冕
		 */
		GRADUATION("graduation"),

		/**
		 * 巨兽追袭
		 */
		DEAGON("dragon"),

		/**
		 * 财从天降
		 */
		MONEY("money"),

		// 单人或动物特效

		/**
		 * 魔法悬浮
		 */
		FLYING("flying"),

		/**
		 * 赠人玫瑰
		 */
		ROSE("rose"),

		/**
		 * 闪亮玫瑰
		 */
		CRYSTALROSE("crystalrose"),

		// 双人特效

		/**
		 * 爱的抱抱
		 */
		HUG("hug"),

		/**
		 * 唇齿相依
		 */
		FRENCHKISS("frenchkiss"),

		/**
		 * 双倍心动
		 */
		COUPLEHEART("coupleheart");

		private final String value;

		VideoTemplate(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	String getBaseUrl() {
		return this.baseUrl;
	}

	ApiKey getApiKey() {
		return this.apiKey;
	}

	RestClient getRestClient() {
		return this.restClient;
	}

	ResponseErrorHandler getResponseErrorHandler() {
		return this.responseErrorHandler;
	}

	public static class Builder {

		public Builder() {
		}

		// Copy constructor for mutate()
		public Builder(DashScopeVideoApi api) {
			this.baseUrl = api.getBaseUrl();
			this.apiKey = api.getApiKey();
			this.restClientBuilder = api.restClient != null ? api.restClient.mutate() : RestClient.builder();
			this.responseErrorHandler = api.getResponseErrorHandler();
		}

		private String baseUrl = DashScopeApiConstants.DEFAULT_BASE_URL;

		private ApiKey apiKey;

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder baseUrl(String baseUrl) {

			Assert.notNull(baseUrl, "Base URL cannot be null");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder apiKey(String simpleApiKey) {
			Assert.notNull(simpleApiKey, "Simple api key cannot be null");
			this.apiKey = new SimpleApiKey(simpleApiKey);
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			Assert.notNull(restClientBuilder, "Rest client builder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			Assert.notNull(responseErrorHandler, "Response error handler cannot be null");
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		public DashScopeVideoApi build() {

			Assert.notNull(apiKey, "API key cannot be null");

			return new DashScopeVideoApi(this.baseUrl, this.apiKey, this.restClientBuilder, this.responseErrorHandler);
		}

	}

}
