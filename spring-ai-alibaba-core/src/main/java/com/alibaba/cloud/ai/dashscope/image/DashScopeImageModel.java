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
package com.alibaba.cloud.ai.dashscope.image;

import java.util.List;
import java.util.Objects;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @since 2024/8/16 11:29
 */
public class DashScopeImageModel implements ImageModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

	/**
	 * The default model used for the image completion requests.
	 */
	private static final String DEFAULT_MODEL = "wanx-v1";

	/**
	 * Low-level access to the DashScope Image API.
	 */
	private final DashScopeImageApi dashScopeImageApi;

	/**
	 * The default options used for the image completion requests.
	 */
	private DashScopeImageOptions defaultOptions;

	/**
	 * The retry template used to retry the OpenAI Image API calls.
	 */
	private final RetryTemplate retryTemplate;

	private static final int MAX_RETRY_COUNT = 10;

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi) {
		this(dashScopeImageApi, DashScopeImageOptions.builder().build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options,
			RetryTemplate retryTemplate) {

		Assert.notNull(dashScopeImageApi, "DashScopeImageApi must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");

		this.dashScopeImageApi = dashScopeImageApi;
		this.defaultOptions = options;
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_RETRY_COUNT);
		FixedBackOffPolicy backOff = new FixedBackOffPolicy();
		backOff.setBackOffPeriod(15_000L);
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(backOff);
		this.retryTemplate = retryTemplate;
	}

	@Override
	public ImageResponse call(ImagePrompt request) {
		Assert.notNull(request, "Prompt must not be null");
		Assert.isTrue(!CollectionUtils.isEmpty(request.getInstructions()), "Prompt messages must not be empty");

		String taskId = submitImageGenTask(request);
		if (taskId == null) {
			return new ImageResponse(List.of(), toMetadataEmpty());
		}

		return retryTemplate.execute(ctx -> {
			DashScopeImageApi.DashScopeImageAsyncReponse resp = getImageGenTask(taskId);
			if (resp != null) {
				String status = resp.output().taskStatus();
				if ("SUCCEEDED".equals(status)) {
					return toImageResponse(resp);
				}
				if ("FAILED".equals(status) || "UNKNOWN".equals(status)) {
					return new ImageResponse(List.of(), toMetadata(resp));
				}
			}
			throw new RuntimeException("Image generation still pending");
		}, context -> {
			return new ImageResponse(List.of(), toMetadataTimeout(taskId));
		});
	}

	public String submitImageGenTask(ImagePrompt request) {

		DashScopeImageOptions imageOptions = toImageOptions(request.getOptions());
		logger.debug("Image options: {}", imageOptions);

		DashScopeImageApi.DashScopeImageRequest dashScopeImageRequest = constructImageRequest(request, imageOptions);

		ResponseEntity<DashScopeImageApi.DashScopeImageAsyncReponse> submitResponse = dashScopeImageApi
			.submitImageGenTask(dashScopeImageRequest);

		if (submitResponse == null || submitResponse.getBody() == null) {
			logger.warn("Submit imageGen error,request: {}", request);
			return null;
		}

		return submitResponse.getBody().output().taskId();
	}

	/**
	 * Merge Image options. Notice: Programmatically set options parameters take
	 * precedence
	 */
	private DashScopeImageOptions toImageOptions(ImageOptions runtimeOptions) {

		// set default image model
		var currentOptions = DashScopeImageOptions.builder().withModel(DEFAULT_MODEL).build();

		if (Objects.nonNull(runtimeOptions)) {
			currentOptions = ModelOptionsUtils.copyToTarget(runtimeOptions, ImageOptions.class,
					DashScopeImageOptions.class);
		}

		currentOptions = ModelOptionsUtils.merge(currentOptions, this.defaultOptions, DashScopeImageOptions.class);

		return currentOptions;
	}

	public DashScopeImageApi.DashScopeImageAsyncReponse getImageGenTask(String taskId) {
		ResponseEntity<DashScopeImageApi.DashScopeImageAsyncReponse> getImageGenResponse = dashScopeImageApi
			.getImageGenTaskResult(taskId);
		if (getImageGenResponse == null || getImageGenResponse.getBody() == null) {
			logger.warn("No image response returned for taskId: {}", taskId);
			return null;
		}
		return getImageGenResponse.getBody();
	}

	public DashScopeImageOptions getOptions() {
		return this.defaultOptions;
	}

	public void setOptions(DashScopeImageOptions options) {
		this.defaultOptions = options;
	}

	private ImageResponse toImageResponse(DashScopeImageApi.DashScopeImageAsyncReponse asyncResp) {
		var output = asyncResp.output();
		var results = output.results();
		ImageResponseMetadata md = toMetadata(asyncResp);
		List<ImageGeneration> gens = results == null ? List.of()
				: results.stream().map(r -> new ImageGeneration(new Image(r.url(), null))).toList();

		return new ImageResponse(gens, md);
	}

	private DashScopeImageApi.DashScopeImageRequest constructImageRequest(ImagePrompt imagePrompt,
			DashScopeImageOptions options) {

		return new DashScopeImageApi.DashScopeImageRequest(options.getModel(),
				new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput(
						imagePrompt.getInstructions().get(0).getText(), options.getNegativePrompt(),
						options.getRefImg(), options.getFunction(), options.getBaseImageUrl(),
						options.getMaskImageUrl(), options.getSketchImageUrl()),
				new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter(options.getStyle(),
						options.getSize(), options.getN(), options.getSeed(), options.getRefStrength(),
						options.getRefMode(), options.getPromptExtend(), options.getWatermark(),
						options.getSketchWeight(), options.getSketchExtraction(), options.getSketchColor(),
						options.getMaskColor()));
	}

	private ImageResponseMetadata toMetadata(DashScopeImageApi.DashScopeImageAsyncReponse re) {
		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput out = re.output();
		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseTaskMetrics tm = out.taskMetrics();
		DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseUsage usage = re.usage();

		ImageResponseMetadata md = new ImageResponseMetadata();

		if (usage != null) {
			md.put("imageCount", usage.imageCount());
		}
		if (tm != null) {
			md.put("taskTotal", tm.total());
			md.put("taskSucceeded", tm.SUCCEEDED());
			md.put("taskFailed", tm.FAILED());
		}
		md.put("requestId", re.requestId());
		md.put("taskStatus", out.taskStatus());
		md.put("code", out.code());
		md.put("message", out.message());

		return md;
	}

	private ImageResponseMetadata toMetadataEmpty() {
		ImageResponseMetadata md = new ImageResponseMetadata();
		md.put("taskStatus", "NO_TASK_ID");
		return md;
	}

	private ImageResponseMetadata toMetadataTimeout(String taskId) {
		ImageResponseMetadata md = new ImageResponseMetadata();
		md.put("taskId", taskId);
		md.put("taskStatus", "TIMED_OUT");
		return md;
	}

}
