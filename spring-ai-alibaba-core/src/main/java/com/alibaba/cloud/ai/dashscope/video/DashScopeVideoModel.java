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

package com.alibaba.cloud.ai.dashscope.video;

import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi.VideoGenerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.Objects;

import static com.alibaba.cloud.ai.dashscope.video.DashScopeVideoOptions.DEFAULT_MODEL;

/**
 * DashScope Video Generation Model.
 *
 * @author dashscope
 * @author yuluo
 * @since 1.0.0.3
 */

public class DashScopeVideoModel implements VideoModel {

	private final static Logger logger = LoggerFactory.getLogger(DashScopeVideoModel.class);

	private final DashScopeVideoApi dashScopeVideoApi;

	private final DashScopeVideoOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	public DashScopeVideoModel(DashScopeVideoApi dashScopeVideoApi, DashScopeVideoOptions defaultOptions,
			RetryTemplate retryTemplate) {

		Assert.notNull(dashScopeVideoApi, "DashScopeVideoApi must not be null");
		Assert.notNull(defaultOptions, "DashScopeVideoOptions must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");

		this.dashScopeVideoApi = dashScopeVideoApi;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Generate video from text prompt.
	 */
	@Override
	public VideoResponse call(VideoPrompt prompt) {

		// Video Prompt use template gen, can null.
		Assert.notNull(prompt, "Prompt must not be null");
		Assert.notEmpty(prompt.getInstructions(), "Prompt instructions must not be empty");

		System.out.println("Video generation task submitted with prompt: " + prompt.getOptions());

		String taskId = submitGenTask(prompt);
		if (Objects.isNull(taskId)) {
			return new VideoResponse(null);
		}

		// todo: add observation
		logger.warn("Video generation task submitted with taskId: {}", taskId);
		return this.retryTemplate.execute(context -> {

			var resp = getVideoTask(taskId);
			if (Objects.nonNull(resp)) {

				logger.debug(String.valueOf(resp));

				String status = resp.getOutput().getTaskStatus();
				switch (status) {
					// status enum SUCCEEDED, FAILED, PENDING, RUNNING
					case "SUCCEEDED" -> {
						logger.info("Video generation task completed successfully: {}", taskId);
						return toVideoResponse(resp);
					}
					case "FAILED" -> {
						logger.error("Video generation task failed: {}", resp.getOutput());
						return new VideoResponse(null);
					}
				}
			}
			throw new RuntimeException("Video generation still pending, retry ...");
		});
	}

	/**
	 * Generate video from text prompt with options.
	 */
	public String submitGenTask(VideoPrompt prompt) {

		DashScopeVideoApi.VideoGenerationRequest request = buildDashScopeVideoRequest(prompt);

		// send request to DashScope Video API
		VideoGenerationResponse response = this.dashScopeVideoApi.submitVideoGenTask(request).getBody();

		if (Objects.isNull(response) || Objects.isNull(response.getOutput().getTaskId())) {
			logger.warn("Failed to submit video generation task: {}", response);
			return null;
		}

		return response.getOutput().getTaskId();
	}

	private DashScopeVideoApi.VideoGenerationResponse getVideoTask(String taskId) {

		ResponseEntity<VideoGenerationResponse> videoGenerationResponseResponseEntity = this.dashScopeVideoApi
			.queryVideoGenTask(taskId);
		if (videoGenerationResponseResponseEntity.getStatusCode().is2xxSuccessful()) {
			return videoGenerationResponseResponseEntity.getBody();
		}
		else {
			logger.warn("Failed to query video task: {}", videoGenerationResponseResponseEntity.getStatusCode());
			return null;
		}
	}

	private VideoResponse toVideoResponse(DashScopeVideoApi.VideoGenerationResponse asyncResp) {

		// var output = asyncResp.getOutput();
		// var usage = asyncResp.getUsage();
		// var results = output.getVideoUrl();
		// todo: add metadata

		return new VideoResponse(asyncResp);
	}

	private DashScopeVideoApi.VideoGenerationRequest buildDashScopeVideoRequest(VideoPrompt prompt) {

		DashScopeVideoOptions options = toVideoOptions(prompt.getOptions());
		logger.debug("Submitting video generation task with options: {}", options);

		return DashScopeVideoApi.VideoGenerationRequest.builder()
			.model(options.getModel())
			.input(DashScopeVideoApi.VideoGenerationRequest.VideoInput.builder()
				.prompt(prompt.getInstructions().get(0).getText())
				.negativePrompt(options.getNegativePrompt())
				.imageUrl(options.getImageUrl())
				.firstFrameUrl(options.getFirstFrameUrl())
				.lastFrameUrl(options.getLastFrameUrl())
				.template(options.getTemplate())
				.build())
			.parameters(DashScopeVideoApi.VideoGenerationRequest.VideoParameters.builder()
				.duration(options.getDuration())
				.size(options.getSize())
				.seed(options.getSeed())
				.promptExtend(options.getPrompt())
				.build())
			.build();
	}

	/**
	 * Merge Video options. Notice: Programmatically(runtime) set options parameters take
	 * precedence.
	 */
	private DashScopeVideoOptions toVideoOptions(VideoOptions runtimeOptions) {

		// set default image model
		var currentOptions = DashScopeVideoOptions.builder().model(DEFAULT_MODEL).build();

		if (Objects.nonNull(runtimeOptions)) {
			currentOptions = ModelOptionsUtils.copyToTarget(runtimeOptions, VideoOptions.class,
					DashScopeVideoOptions.class);
		}

		currentOptions = ModelOptionsUtils.merge(currentOptions, this.defaultOptions, DashScopeVideoOptions.class);

		return currentOptions;
	}

	public static final class Builder {

		private DashScopeVideoApi videoApi;

		private DashScopeVideoOptions defaultOptions = DashScopeVideoOptions.builder().model(DEFAULT_MODEL).build();

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private Builder() {
		}

		public Builder videoApi(DashScopeVideoApi videoApi) {
			this.videoApi = videoApi;
			return this;
		}

		public Builder defaultOptions(DashScopeVideoOptions defaultOptions) {
			this.defaultOptions = defaultOptions;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public DashScopeVideoModel build() {
			return new DashScopeVideoModel(this.videoApi, this.defaultOptions, this.retryTemplate);
		}

	}

}
