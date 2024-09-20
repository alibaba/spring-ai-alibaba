package com.alibaba.cloud.ai.dashscope.image;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @date 2024/8/16 11:29
 */
public class DashScopeImageModel implements ImageModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

	/**
	 * Low-level access to the DashScope Image API.
	 */
	private final DashScopeImageApi dashScopeImageApi;

	/**
	 * The default options used for the image completion requests.
	 */
	private final DashScopeImageOptions options;

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
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public ImageResponse call(ImagePrompt request) {
		String taskId = submitImageGenTask(request);
		if (taskId == null) {
			return new ImageResponse(List.of());
		}
		int retryCount = 0;
		while (true && retryCount < MAX_RETRY_COUNT) {
			DashScopeImageApi.DashScopeImageAsyncReponse getResultResponse = getImageGenTask(taskId);
			if (getResultResponse != null) {
				DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput output = getResultResponse
					.output();
				String taskStatus = output.taskStatus();
				switch (taskStatus) {
					case "SUCCEEDED":
						return toImageResponse(output);
					case "FAILED":
					case "UNKNOWN":
						return new ImageResponse(List.of());
				}
			}
			try {
				Thread.sleep(15000l);
				retryCount++;
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return new ImageResponse(List.of());
	}

	public String submitImageGenTask(ImagePrompt request) {
		String instructions = request.getInstructions().get(0).getText();
		DashScopeImageApi.DashScopeImageRequest imageRequest = null;
		if (options != null) {
			imageRequest = new DashScopeImageApi.DashScopeImageRequest(options.getModel(),
					new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput(instructions,
							options.getNegativePrompt(), options.getRefImg()),
					new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter(options.getStyle(),
							options.getSize(), options.getN(), options.getSeed(), options.getRefStrength(),
							options.getRefMode()));
		}
		if (request.getOptions() != null) {
			DashScopeImageOptions options = toQianFanImageOptions(request.getOptions());
			imageRequest = new DashScopeImageApi.DashScopeImageRequest(options.getModel(),
					new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput(instructions,
							options.getNegativePrompt(), options.getRefImg()),
					new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter(options.getStyle(),
							options.getSize(), options.getN(), options.getSeed(), options.getRefStrength(),
							options.getRefMode()));
		}
		ResponseEntity<DashScopeImageApi.DashScopeImageAsyncReponse> submitResponse = dashScopeImageApi
			.submitImageGenTask(imageRequest);
		if (submitResponse == null || submitResponse.getBody() == null) {
			logger.warn("Submit imageGen error,request: {}", request);
			return null;
		}
		return submitResponse.getBody().output().taskId();
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

	private ImageResponse toImageResponse(
			DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput output) {
		List<DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult> genImageList = output
			.results();
		if (genImageList == null || genImageList.isEmpty()) {
			return new ImageResponse(List.of());
		}
		List<ImageGeneration> imageGenerationList = genImageList.stream()
			.map(entry -> new ImageGeneration(new Image(entry.url(), null)))
			.toList();

		return new ImageResponse(imageGenerationList);
	}

	private DashScopeImageOptions toQianFanImageOptions(ImageOptions runtimeImageOptions) {
		DashScopeImageOptions.Builder dashScopeImageOptionsBuilder = DashScopeImageOptions.builder();
		if (runtimeImageOptions != null) {
			if (runtimeImageOptions.getN() != null) {
				dashScopeImageOptionsBuilder.withN(runtimeImageOptions.getN());
			}
			if (runtimeImageOptions.getModel() != null) {
				dashScopeImageOptionsBuilder.withModel(runtimeImageOptions.getModel());
			}
			if (runtimeImageOptions.getWidth() != null) {
				dashScopeImageOptionsBuilder.withWidth(runtimeImageOptions.getWidth());
			}
			if (runtimeImageOptions.getHeight() != null) {
				dashScopeImageOptionsBuilder.withHeight(runtimeImageOptions.getHeight());
			}
			if (runtimeImageOptions instanceof DashScopeImageOptions) {
				DashScopeImageOptions dashScopeImageOptions = (DashScopeImageOptions) runtimeImageOptions;
				if (dashScopeImageOptions.getStyle() != null) {
					dashScopeImageOptionsBuilder.withStyle(dashScopeImageOptions.getStyle());
				}
				if (dashScopeImageOptions.getSeed() != null) {
					dashScopeImageOptionsBuilder.withSeed(dashScopeImageOptions.getSeed());
				}
				if (dashScopeImageOptions.getRefImg() != null) {
					dashScopeImageOptionsBuilder.withRefImg(dashScopeImageOptions.getRefImg());
				}
				if (dashScopeImageOptions.getRefMode() != null) {
					dashScopeImageOptionsBuilder.withRefMode(dashScopeImageOptions.getRefMode());
				}
				if (dashScopeImageOptions.getRefStrength() != null) {
					dashScopeImageOptionsBuilder.withRefStrength(dashScopeImageOptions.getRefStrength());
				}
				if (dashScopeImageOptions.getNegativePrompt() != null) {
					dashScopeImageOptionsBuilder.withNegativePrompt(dashScopeImageOptions.getNegativePrompt());
				}
			}
		}
		return dashScopeImageOptionsBuilder.build();
	}

}
