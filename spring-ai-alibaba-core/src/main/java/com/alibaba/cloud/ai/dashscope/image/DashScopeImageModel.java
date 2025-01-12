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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @author 北极星
 * @since 2024/8/16 11:29
 */
public class DashScopeImageModel implements ImageModel {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

    private static final ImageModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DashScopeImageModelObservationConvention();

    /**
     * The default model used for the image completion requests.
     */
    private static final String DEFAULT_MODEL = DashScopeImageApi.DEFAULT_IMAGE_MODEL;

    /**
     * Low-level access to the DashScope Image API.
     */
    private final DashScopeImageApi dashScopeImageApi;

    /**
     * Observation registry used for instrumentation.
     */
    private final ObservationRegistry observationRegistry;

    /**
     * The default options used for the image completion requests.
     */
    private final DashScopeImageOptions defaultOptions;

    /**
     * The retry template used to retry the OpenAI Image API calls.
     */
    private final RetryTemplate retryTemplate;

    /**
     * Conventions to use for generating observations.
     */
    private ImageModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    public DashScopeImageModel (DashScopeImageApi dashScopeImageApi) {

        this.defaultOptions = DashScopeImageOptions.builder().withModel(DashScopeImageApi.DEFAULT_IMAGE_MODEL).build();
        this.dashScopeImageApi = dashScopeImageApi;
        this.retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
        this.observationRegistry = ObservationRegistry.NOOP;
    }

    public DashScopeImageModel (DashScopeImageApi dashScopeImageApi, ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
        this.defaultOptions = DashScopeImageOptions.builder().withModel(DashScopeImageApi.DEFAULT_IMAGE_MODEL).build();
        this.dashScopeImageApi = dashScopeImageApi;
        this.retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
    }

    public DashScopeImageModel (DashScopeImageApi dashScopeImageApi, ObservationRegistry observationRegistry, DashScopeImageOptions options, RetryTemplate retryTemplate) {
        this.observationRegistry = observationRegistry;
        this.defaultOptions = options;
        this.dashScopeImageApi = dashScopeImageApi;
        this.retryTemplate = retryTemplate;
    }

    public DashScopeImageModel (DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options, RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {

        Assert.notNull(dashScopeImageApi, "DashScopeImageApi must not be null");
        Assert.notNull(options, "options must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");

        this.dashScopeImageApi = dashScopeImageApi;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public ImageResponse call (ImagePrompt prompt) {

        String taskId = submitImageGenTask(prompt);
        if (taskId == null) {
            return new ImageResponse(List.of());
        }

//        ImageModelObservationContext observationContext = ImageModelObservationContext.builder().imagePrompt(prompt).provider(DashScopeApiConstants.PROVIDER_NAME).requestOptions(prompt.getOptions() != null ? prompt.getOptions() : this.defaultOptions).build();

//        Observation observation = ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);

        //        ImageResponse imageResponse = observation.observe(() -> {
        int retryCount = 0;
        while (retryCount < 10) {
            DashScopeImageApi.DashScopeImageAsyncReponse getResultResponse = getImageGenTask(taskId);
            if (getResultResponse != null) {
                DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput output = getResultResponse.output();
                String taskStatus = output.taskStatus();
                switch (taskStatus) {
                    case "SUCCEEDED" -> {
                        return toImageResponse(output);
                    }
                    case "FAILED", "UNKNOWN" -> {
                        return new ImageResponse(List.of());
                    }
                }
            }
            try {
                Thread.sleep(15000L);
                retryCount++;
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new ImageResponse(null);
        //        });
        //        return imageResponse;
    }

    //        String taskId = submitImageGenTask(request);
    //        if (taskId == null) {
    //            return new ImageResponse(List.of());
    //        }

    //        return ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry).observe(() -> {
    //
    //            ImageResponse response = retryTemplate.execute(ctx -> {
    //                DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput imageGenTaskResult = Objects.requireNonNull(dashScopeImageApi.getImageGenTaskResult(taskId).getBody()).output();
    //                String taskStatus = imageGenTaskResult.taskStatus();
    //                if ("SUCCEEDED".equals(taskStatus)) {
    //                    return toImageResponse(imageGenTaskResult);
    //                }
    //                return new ImageResponse(List.of());
    //            });
    //
    //            //            var imageCompletion = completionEntity.getBody();
    //            //
    //            //            if (imageCompletion == null || imageCompletion.output() == null) {
    //            //                logger.warn("No Image completion returned for prompt: {}", prompt);
    //            //                return new ImageResponse(List.of());
    //            //            }
    //            //
    //            //            ImageResponse response = toImageResponse(imageCompletion.output());
    //            observationContext.setResponse(response);
    //            return response;
    //        });


    /**
     * Merge Image options. Notice: Programmatically set options parameters take
     * precedence
     */
    private DashScopeImageOptions toImageOptions (ImageOptions runtimeOptions) {

        // set default image model
        var currentOptions = DashScopeImageOptions.builder().withModel(DEFAULT_MODEL).build();

        if (Objects.nonNull(runtimeOptions)) {
            currentOptions = ModelOptionsUtils.copyToTarget(runtimeOptions, ImageOptions.class, DashScopeImageOptions.class);
        }

        currentOptions = ModelOptionsUtils.merge(currentOptions, this.defaultOptions, DashScopeImageOptions.class);

        return currentOptions;
    }

    private ImageResponse toImageResponse (DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput output) {
        List<DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult> genImageList = output.results();
        if (genImageList == null || genImageList.isEmpty()) {
            return new ImageResponse(List.of());
        }
        List<ImageGeneration> imageGenerationList = genImageList.stream().map(entry -> new ImageGeneration(new Image(entry.url(), null))).toList();

        return new ImageResponse(imageGenerationList);
    }

    private DashScopeImageApi.DashScopeImageRequest constructImageRequest (ImagePrompt imagePrompt, DashScopeImageOptions options) {

        return new DashScopeImageApi.DashScopeImageRequest(options.getModel(), new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestInput(imagePrompt.getInstructions().get(0).getText(), options.getNegativePrompt(), options.getRefImg()), new DashScopeImageApi.DashScopeImageRequest.DashScopeImageRequestParameter(options.getStyle(), options.getSize(), options.getN(), options.getSeed(), options.getRefStrength(), options.getRefMode()));
    }

    /**
     * Use the provided convention for reporting observation data
     *
     * @param observationConvention The provided convention
     */
    public void setObservationConvention (ImageModelObservationConvention observationConvention) {
        Assert.notNull(observationConvention, "observationConvention cannot be null");
        this.observationConvention = observationConvention;
    }

    public String submitImageGenTask (ImagePrompt request) {

        DashScopeImageOptions imageOptions = toImageOptions(request.getOptions());
        logger.debug("Image options: {}", imageOptions);

        DashScopeImageApi.DashScopeImageRequest dashScopeImageRequest = constructImageRequest(request, imageOptions);

        ResponseEntity<DashScopeImageApi.DashScopeImageAsyncReponse> submitResponse = dashScopeImageApi.submitImageGenTask(dashScopeImageRequest);

        if (submitResponse == null || submitResponse.getBody() == null) {
            logger.warn("Submit imageGen error,request: {}", request);
            return null;
        }

        return submitResponse.getBody().output().taskId();
    }

    public DashScopeImageApi.DashScopeImageAsyncReponse getImageGenTask (String taskId) {
        ResponseEntity<DashScopeImageApi.DashScopeImageAsyncReponse> getImageGenResponse = dashScopeImageApi.getImageGenTaskResult(taskId);
        if (getImageGenResponse == null || getImageGenResponse.getBody() == null) {
            logger.warn("No image response returned for taskId: {}", taskId);
            return null;
        }
        return getImageGenResponse.getBody();
    }
}
