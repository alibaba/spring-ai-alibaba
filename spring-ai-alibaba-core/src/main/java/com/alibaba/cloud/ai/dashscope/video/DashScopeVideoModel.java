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
import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi.VideoGenerationRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi.VideoGenerationResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Objects;

/**
 * DashScope Video Generation Model.
 *
 * @author dashscope
 */
public class DashScopeVideoModel {

	/**
	 * Default video model.
	 */
	public static final String DEFAULT_MODEL = "text2video-synthesis";

	/**
	 * Default polling interval.
	 */
	public static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(5);

	/**
	 * Default timeout.
	 */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

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

	/**
	 * Generate video from text prompt.
	 */
	public VideoGenerationResponse generate(String prompt) {
		return generate(prompt, null);
	}

	/**
	 * Generate video from text prompt with options.
	 */
	public VideoGenerationResponse generate(String prompt, VideoOptions runtimeOptions) {
		Assert.hasText(prompt, "Prompt must not be empty");

		DashScopeVideoOptions options = toVideoOptions(runtimeOptions);

		VideoGenerationRequest request = new VideoGenerationRequest(options.getModel(),
				new VideoGenerationRequest.VideoInput(prompt),
				new VideoGenerationRequest.VideoParameters(options.getWidth(), options.getHeight(),
						options.getDuration(), options.getFps(), options.getSeed(), options.getNumFrames()));

		VideoGenerationResponse response = this.dashScopeVideoApi.submitTask(request).getBody();

		if (response != null && response.getOutput() != null && response.getOutput().getTaskId() != null) {
			return pollForCompletion(response.getOutput().getTaskId());
		}

		return response;
	}

	/**
	 * Poll for task completion.
	 */
	private VideoGenerationResponse pollForCompletion(String taskId) {
		long startTime = System.currentTimeMillis();
		long timeout = DEFAULT_TIMEOUT.toMillis();

		while (System.currentTimeMillis() - startTime < timeout) {
			VideoGenerationResponse response = this.dashScopeVideoApi.queryTask(taskId).getBody();

			if (response != null && response.getOutput() != null) {
				String status = response.getOutput().getTaskStatus();

				if ("SUCCEEDED".equals(status)) {
					return response;
				}
				else if ("FAILED".equals(status)) {
					throw new RuntimeException("Video generation failed: " + response.getMessage());
				}
				// PENDING, RUNNING - continue polling
			}

			try {
				Thread.sleep(DEFAULT_POLLING_INTERVAL.toMillis());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Video generation interrupted", e);
			}
		}

		throw new RuntimeException("Video generation timeout after " + DEFAULT_TIMEOUT);
	}

	/**
	 * Merge Video options. Notice: Programmatically set options parameters take
	 * precedence
	 */
	private DashScopeVideoOptions toVideoOptions(VideoOptions runtimeOptions) {

		// set default video model
		var currentOptions = DashScopeVideoOptions.builder().withModel(DEFAULT_MODEL).build();

		if (Objects.nonNull(runtimeOptions)) {
			// Copy runtime options to current options
			if (runtimeOptions.getModel() != null) {
				currentOptions.setModel(runtimeOptions.getModel());
			}
			if (runtimeOptions.getWidth() != null) {
				currentOptions.setWidth(runtimeOptions.getWidth());
			}
			if (runtimeOptions.getHeight() != null) {
				currentOptions.setHeight(runtimeOptions.getHeight());
			}
			if (runtimeOptions.getDuration() != null) {
				currentOptions.setDuration(runtimeOptions.getDuration());
			}
			if (runtimeOptions.getFps() != null) {
				currentOptions.setFps(runtimeOptions.getFps());
			}
			if (runtimeOptions.getSeed() != null) {
				currentOptions.setSeed(runtimeOptions.getSeed());
			}
			if (runtimeOptions.getNumFrames() != null) {
				currentOptions.setNumFrames(runtimeOptions.getNumFrames());
			}
		}

		// Merge with default options
		if (this.defaultOptions.getModel() != null && currentOptions.getModel().equals(DEFAULT_MODEL)) {
			currentOptions.setModel(this.defaultOptions.getModel());
		}
		if (this.defaultOptions.getWidth() != null && currentOptions.getWidth() == null) {
			currentOptions.setWidth(this.defaultOptions.getWidth());
		}
		if (this.defaultOptions.getHeight() != null && currentOptions.getHeight() == null) {
			currentOptions.setHeight(this.defaultOptions.getHeight());
		}
		if (this.defaultOptions.getDuration() != null && currentOptions.getDuration() == null) {
			currentOptions.setDuration(this.defaultOptions.getDuration());
		}
		if (this.defaultOptions.getFps() != null && currentOptions.getFps() == null) {
			currentOptions.setFps(this.defaultOptions.getFps());
		}
		if (this.defaultOptions.getSeed() != null && currentOptions.getSeed() == null) {
			currentOptions.setSeed(this.defaultOptions.getSeed());
		}
		if (this.defaultOptions.getNumFrames() != null && currentOptions.getNumFrames() == null) {
			currentOptions.setNumFrames(this.defaultOptions.getNumFrames());
		}

		return currentOptions;
	}

	/**
	 * Video options interface.
	 */
	public interface VideoOptions {

		String getModel();

		Integer getWidth();

		Integer getHeight();

		Integer getDuration();

		Integer getFps();

		Long getSeed();

		Integer getNumFrames();

	}

}
