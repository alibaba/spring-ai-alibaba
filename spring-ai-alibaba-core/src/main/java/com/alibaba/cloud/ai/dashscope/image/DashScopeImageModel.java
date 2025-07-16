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

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.image.observation.DashScopeImageModelObservationConvention;
import com.alibaba.cloud.ai.dashscope.image.observation.DashScopeImagePromptContentObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @author polaris
 * @since 2024/8/16 11:29
 */
public class DashScopeImageModel implements ImageModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

	/**
	 * The default model used for the image completion requests.
	 */
	private static final String DEFAULT_MODEL = "wanx-v1";

	private static final int MAX_RETRY_COUNT = 10;

	/**
	 * Low-level access to the DashScope Image API.
	 */
	private final DashScopeImageApi dashScopeImageApi;

	/**
	 * The default options used for the image completion requests.
	 */
	private final DashScopeImageOptions defaultOptions;

	/**
	 * The retry template used to retry the OpenAI Image API calls.
	 */
	private final RetryTemplate retryTemplate;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	/**
	 * Conventions to use for generating observations.
	 */
	private ImageModelObservationConvention observationConvention = new DefaultImageModelObservationConvention();

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options,
			RetryTemplate retryTemplate) {
		this(dashScopeImageApi, options, retryTemplate, ObservationRegistry.NOOP);
	}

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi) {
		this(dashScopeImageApi,
				DashScopeImageOptions.builder().withModel(DashScopeImageApi.DEFAULT_IMAGE_MODEL).build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE, ObservationRegistry.NOOP);
	}

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options) {
		this(dashScopeImageApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE, ObservationRegistry.NOOP);
	}

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, ObservationRegistry observationRegistry) {
		this(dashScopeImageApi,
				DashScopeImageOptions.builder().withModel(DashScopeImageApi.DEFAULT_IMAGE_MODEL).build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE, observationRegistry);
	}

	public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options,
			RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {

		Assert.notNull(dashScopeImageApi, "DashScopeImageApi must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		Assert.notNull(observationRegistry, "observationRegistry must not be null");

		this.dashScopeImageApi = dashScopeImageApi;
		this.defaultOptions = options;
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_RETRY_COUNT);
		FixedBackOffPolicy backOff = new FixedBackOffPolicy();
		backOff.setBackOffPeriod(15_000L);
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(backOff);
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;

		this.observationRegistry.observationConfig()
			.observationHandler(new DashScopeImagePromptContentObservationHandler());

		this.observationConvention = new DashScopeImageModelObservationConvention();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ImageResponse call(ImagePrompt request) {
		Assert.notNull(request, "Prompt must not be null");
		Assert.isTrue(!CollectionUtils.isEmpty(request.getInstructions()), "Prompt messages must " + "not be empty");

		String taskId = submitImageGenTask(request);
		if (taskId == null) {
			return new ImageResponse(List.of(), toMetadataEmpty());
		}

		ImageModelObservationContext observationContext = ImageModelObservationContext.builder()
			.imagePrompt(request)
			.provider(DashScopeApiConstants.PROVIDER_NAME)
			.build();

		Observation observation = ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION.observation(
				observationConvention, new DefaultImageModelObservationConvention(), () -> observationContext,
				this.observationRegistry);

		return observation.observe(() ->

		retryTemplate.execute(ctx -> {
			observation.lowCardinalityKeyValue("retry.attempt", String.valueOf(ctx.getRetryCount()));

			DashScopeImageApi.DashScopeImageAsyncReponse resp = getImageGenTask(taskId);
			if (resp != null) {
				String status = resp.output().taskStatus();
				observation.lowCardinalityKeyValue("task.status", status);

				switch (status) {
					case "SUCCEEDED":
						return toImageResponse(resp);
					case "FAILED":
					case "UNKNOWN":
						return new ImageResponse(List.of(), toMetadata(resp));
				}
			}
			throw new RuntimeException("Image generation still pending");
		}, context -> {
			observation.lowCardinalityKeyValue("timeout", "true");
			return new ImageResponse(List.of(), toMetadataTimeout(taskId));
		}));
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
		var out = re.output();
		var tm = out.taskMetrics();
		var usage = re.usage();

		ImageResponseMetadata md = new ImageResponseMetadata();

		Optional.ofNullable(usage)
			.map(DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseUsage::imageCount)
			.ifPresent(count -> md.put("imageCount", count));
		Optional.ofNullable(tm).ifPresent(metrics -> {
			md.put("taskTotal", metrics.total());
			md.put("taskSucceeded", metrics.SUCCEEDED());
			md.put("taskFailed", metrics.FAILED());
		});
		md.put("requestId", re.requestId());
		md.put("taskStatus", out.taskStatus());
		Optional.ofNullable(out.code()).ifPresent(code -> md.put("code", code));
		Optional.ofNullable(out.message()).ifPresent(msg -> md.put("message", msg));

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

	/**
	 * Use the provided convention for reporting observation data
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(ImageModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

	public static final class Builder {

		private DashScopeImageApi dashScopeImageApi;

		private DashScopeImageOptions defaultOptions = DashScopeImageOptions.builder()
			.withModel(DEFAULT_MODEL)
			.withN(1)
			.build();

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		private ImageModelObservationConvention observationConvention = new DashScopeImageModelObservationConvention();

		private ObservationHandler<ImageModelObservationContext> promptHandler = new DashScopeImagePromptContentObservationHandler();

		private Builder() {
		}

		public DashScopeImageModel.Builder dashScopeApi(DashScopeImageApi dashScopeImageApi) {
			this.dashScopeImageApi = dashScopeImageApi;
			return this;
		}

		public Builder defaultOptions(DashScopeImageOptions defaultOptions) {
			this.defaultOptions = defaultOptions;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public Builder observationConvention(ImageModelObservationConvention observationConvention) {
			this.observationConvention = observationConvention;
			return this;
		}

		public Builder promptHandler(ObservationHandler<ImageModelObservationContext> promptHandler) {
			this.promptHandler = promptHandler;
			return this;
		}

		public DashScopeImageModel build() {
			DashScopeImageModel model = new DashScopeImageModel(dashScopeImageApi, defaultOptions, retryTemplate,
					observationRegistry);

			model.setObservationConvention(this.observationConvention);
			this.observationRegistry.observationConfig().observationHandler(this.promptHandler);
			return model;
		}

	}

}
