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
package com.alibaba.cloud.ai.dashscope.audio;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.*;
import com.alibaba.cloud.ai.dashscope.audio.observation.SpeechSynthesisContext;
import com.alibaba.cloud.ai.dashscope.audio.observation.DashScopeAudioObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.springframework.util.Assert;

/**
 * @author kevinlin09
 */
public class DashScopeSpeechSynthesisModel implements SpeechSynthesisModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeSpeechSynthesisModel.class);

	private static final DashScopeAudioObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DashScopeAudioObservationConvention();

	private final DashScopeSpeechSynthesisApi api;

	private final DashScopeSpeechSynthesisOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final ObservationRegistry observationRegistry;

	private DashScopeAudioObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api) {
		this(api, DashScopeSpeechSynthesisOptions.builder().withModel("sambert-zhichu-v1").build());
	}

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api, DashScopeSpeechSynthesisOptions options) {
		this(api, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api,
			DashScopeSpeechSynthesisOptions defaultOptions, RetryTemplate retryTemplate) {
		this(api, defaultOptions, retryTemplate, ObservationRegistry.NOOP);
	}

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api,
			DashScopeSpeechSynthesisOptions defaultOptions, RetryTemplate retryTemplate,
			ObservationRegistry observationRegistry) {
		this.api = api;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public SpeechSynthesisResponse call(SpeechSynthesisPrompt prompt) {
		SpeechSynthesisContext context = new SpeechSynthesisContext();
		context.setPrompt(prompt);
		context.setModelName(defaultOptions.getModel());
		context.setFormat(defaultOptions.getResponseFormat().toString());
		context.setSampleRate(defaultOptions.getSampleRate());
		if (!prompt.getInstructions().isEmpty()) {
			context.setInputLength((long) prompt.getInstructions().get(0).getText().length());
		}

		return Observation.createNotStarted(observationConvention, () -> {
			try {
				SpeechSynthesisResponse response = retryTemplate.execute(retryContext -> {
					DashScopeSpeechSynthesisApi.Request request = createRequest(prompt);
					DashScopeSpeechSynthesisApi.Response apiResponse = api.call(request);
					SpeechSynthesisOutput output = new SpeechSynthesisOutput(apiResponse.getAudio());
					SpeechSynthesisResult result = new SpeechSynthesisResult(output);
					return new SpeechSynthesisResponse(result);
				});

				context.setResponse(response);
				if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
					context.setOutputLength((long) response.getResult().getOutput().getAudio().remaining());
				}
				return context;
			}
			catch (Exception e) {
				context.setError(e);
				throw e;
			}
		}, observationRegistry).observe(() -> {
			try {
				return retryTemplate.execute(retryContext -> {
					DashScopeSpeechSynthesisApi.Request request = createRequest(prompt);
					DashScopeSpeechSynthesisApi.Response apiResponse = api.call(request);
					SpeechSynthesisOutput output = new SpeechSynthesisOutput(apiResponse.getAudio());
					SpeechSynthesisResult result = new SpeechSynthesisResult(output);
					return new SpeechSynthesisResponse(result);
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Flux<SpeechSynthesisResponse> stream(SpeechSynthesisPrompt prompt) {
		SpeechSynthesisContext context = new SpeechSynthesisContext();
		context.setPrompt(prompt);
		context.setModelName(defaultOptions.getModel());
		context.setFormat(defaultOptions.getResponseFormat().toString());
		context.setSampleRate(defaultOptions.getSampleRate());
		if (!prompt.getInstructions().isEmpty()) {
			context.setInputLength((long) prompt.getInstructions().get(0).getText().length());
		}

		return Observation.createNotStarted(observationConvention, () -> context, observationRegistry).observe(() -> {
			try {
				DashScopeSpeechSynthesisApi.Request request = createRequest(prompt);
				return api.streamOut(request).map(audioBuffer -> {
					SpeechSynthesisOutput output = new SpeechSynthesisOutput(audioBuffer);
					SpeechSynthesisResult result = new SpeechSynthesisResult(output);
					SpeechSynthesisResponse response = new SpeechSynthesisResponse(result);
					if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
						context.setOutputLength((long) response.getResult().getOutput().getAudio().remaining());
					}
					return response;
				});
			}
			catch (Exception e) {
				context.setError(e);
				throw new RuntimeException(e);
			}
		});
	}

	public DashScopeSpeechSynthesisApi.Request createRequest(SpeechSynthesisPrompt prompt) {
		DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder().build();
		if (prompt.getOptions() != null) {
			options = ModelOptionsUtils.merge(prompt.getOptions(), options, DashScopeSpeechSynthesisOptions.class);
		}
		options = ModelOptionsUtils.merge(options, this.defaultOptions, DashScopeSpeechSynthesisOptions.class);

		return new DashScopeSpeechSynthesisApi.Request(
				new DashScopeSpeechSynthesisApi.Request.RequestHeader("run-task", UUID.randomUUID().toString(), "out"),
				new DashScopeSpeechSynthesisApi.Request.RequestPayload(options.getModel(), "audio", "tts",
						"SpeechSynthesizer",
						new DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadInput(
								prompt.getInstructions().get(0).getText()),
						new DashScopeSpeechSynthesisApi.Request.RequestPayload.RequestPayloadParameters(
								options.getVolume(), options.getRequestTextType().getValue(), options.getVoice(),
								options.getSampleRate(), options.getSpeed(), options.getResponseFormat().getValue(),
								options.getPitch(), options.getEnablePhonemeTimestamp(),
								options.getEnableWordTimestamp())));
	}

	public void setObservationConvention(DashScopeAudioObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

}
