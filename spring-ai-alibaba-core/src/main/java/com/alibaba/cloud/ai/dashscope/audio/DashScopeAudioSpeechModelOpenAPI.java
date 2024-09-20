/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.audio;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioApi;
import com.alibaba.cloud.ai.dashscope.audio.speech.Speech;
import com.alibaba.cloud.ai.dashscope.audio.speech.SpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.speech.SpeechPrompt;
import com.alibaba.cloud.ai.dashscope.audio.speech.SpeechResponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioApi.SpeechRequest.AudioResponseFormat;

import com.alibaba.cloud.ai.dashscope.audio.speech.StreamingSpeechModel;
import com.alibaba.cloud.ai.dashscope.metadata.audio.DashScopeAudioSpeechResponseMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import reactor.core.publisher.Flux;

/**
 * OpenAI audio speech client implementation for backed by {@link DashScopeAudioApi}.
 *
 */
public class DashScopeAudioSpeechModelOpenAPI implements SpeechModel, StreamingSpeechModel {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The default options used for the audio completion requests.
	 */
	private final DashScopeAudioSpeechOptions defaultOptions;

	/**
	 * The speed of the default voice synthesis.
	 * @see DashScopeAudioSpeechOptions
	 */
	private static final Float SPEED = 1.0f;

	/**
	 * The retry template used to retry the OpenAI Audio API calls.
	 */
	private final RetryTemplate retryTemplate;

	/**
	 * Low-level access to the OpenAI Audio API.
	 */
	private final DashScopeAudioApi audioApi;

	/**
	 * Initializes a new instance of the DashScopeAudioSpeechModel class with the provided
	 * DashScopeAudioApi. It uses the model tts-1, response format mp3, voice alloy, and
	 * the default speed of 1.0.
	 * @param audioApi The DashScopeAudioApi to use for speech synthesis.
	 */
	public DashScopeAudioSpeechModelOpenAPI(DashScopeAudioApi audioApi) {
		this(audioApi,
				DashScopeAudioSpeechOptions.builder()
					.withModel(DashScopeAudioApi.TtsModel.TTS_1.getValue())
					.withResponseFormat(AudioResponseFormat.MP3)
					.withSpeed(SPEED)
					.build());
	}

	/**
	 * Initializes a new instance of the DashScopeAudioSpeechModel class with the provided
	 * DashScopeAudioApi and options.
	 * @param audioApi The DashScopeAudioApi to use for speech synthesis.
	 * @param options The DashScopeAudioSpeechOptions containing the speech synthesis
	 * options.
	 */
	public DashScopeAudioSpeechModelOpenAPI(DashScopeAudioApi audioApi, DashScopeAudioSpeechOptions options) {
		this(audioApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * Initializes a new instance of the DashScopeAudioSpeechModel class with the provided
	 * DashScopeAudioApi and options.
	 * @param audioApi The DashScopeAudioApi to use for speech synthesis.
	 * @param options The DashScopeAudioSpeechOptions containing the speech synthesis
	 * options.
	 * @param retryTemplate The retry template.
	 */
	public DashScopeAudioSpeechModelOpenAPI(DashScopeAudioApi audioApi, DashScopeAudioSpeechOptions options,
			RetryTemplate retryTemplate) {
		Assert.notNull(audioApi, "DashScopeAudioApi must not be null");
		Assert.notNull(options, "DashScopeSpeechOptions must not be null");
		Assert.notNull(options, "RetryTemplate must not be null");
		this.audioApi = audioApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public byte[] call(String text) {
		SpeechPrompt speechRequest = new SpeechPrompt(text);
		return call(speechRequest).getResult().getOutput();
	}

	@Override
	public SpeechResponse call(SpeechPrompt speechPrompt) {
		DashScopeAudioApi.SpeechRequest speechRequest = createRequest(speechPrompt);

		ResponseEntity<byte[]> speechEntity = this.retryTemplate
			.execute(ctx -> this.audioApi.createSpeech(speechRequest));

		var speech = speechEntity.getBody();

		if (speech == null) {
			logger.warn("No speech response returned for speechRequest: {}", speechRequest);
			return new SpeechResponse(new Speech(new byte[0]));
		}

		// RateLimit rateLimits =
		// DashScopeResponseHeaderExtractor.extractAiResponseHeaders(speechEntity);

		return new SpeechResponse(new Speech(speech), new DashScopeAudioSpeechResponseMetadata());
	}

	/**
	 * Streams the audio response for the given speech prompt.
	 * @param speechPrompt The speech prompt containing the text and options for speech
	 * synthesis.
	 * @return A Flux of SpeechResponse objects containing the streamed audio and
	 * metadata.
	 */
	@Override
	public Flux<SpeechResponse> stream(SpeechPrompt speechPrompt) {

		DashScopeAudioApi.SpeechRequest speechRequest = createRequest(speechPrompt);

		Flux<ResponseEntity<byte[]>> speechEntity = this.retryTemplate
			.execute(ctx -> this.audioApi.stream(speechRequest));

		return speechEntity.map(
				entity -> new SpeechResponse(new Speech(entity.getBody()), new DashScopeAudioSpeechResponseMetadata()));
	}

	private DashScopeAudioApi.SpeechRequest createRequest(SpeechPrompt request) {
		DashScopeAudioSpeechOptions options = this.defaultOptions;

		if (request.getOptions() != null) {
			if (request.getOptions() instanceof DashScopeAudioSpeechOptions runtimeOptions) {
				options = this.merge(runtimeOptions, options);
			}
			else {
				throw new IllegalArgumentException("Prompt options are not of type SpeechOptions: "
						+ request.getOptions().getClass().getSimpleName());
			}
		}

		String input = ObjectUtils.isEmpty(options.getText()) ? options.getText() : request.getInstructions().getText();

		DashScopeAudioApi.SpeechRequest.Builder requestBuilder = DashScopeAudioApi.SpeechRequest.builder()
			.withModel(options.getModel())
			.withInput(input)
			.withResponseFormat(options.getResponseFormat())
			.withSpeed(options.getSpeed());

		return requestBuilder.build();
	}

	private DashScopeAudioSpeechOptions merge(DashScopeAudioSpeechOptions source, DashScopeAudioSpeechOptions target) {
		DashScopeAudioSpeechOptions.Builder mergedBuilder = DashScopeAudioSpeechOptions.builder();

		mergedBuilder.withModel(source.getModel() != null ? source.getModel() : target.getModel());
		mergedBuilder.withText(source.getText() != null ? source.getText() : target.getText());
		mergedBuilder.withPitch(source.getPitch() != null ? source.getPitch() : target.getPitch());
		mergedBuilder.withSampleRate(source.getSampleRate() == null ? target.getSampleRate() : source.getSampleRate());
		mergedBuilder.withVolume(source.getVolume() == null ? target.getVolume() : source.getVolume());
		mergedBuilder.withEnablePhonemeTimestamp(source.getEnablePhonemeTimestamp() != null
				? source.getEnablePhonemeTimestamp() : target.getEnablePhonemeTimestamp());
		mergedBuilder.withEnableWordTimestamp(source.getEnableWordTimestamp() != null ? source.getEnableWordTimestamp()
				: target.getEnableWordTimestamp());
		mergedBuilder.withResponseFormat(
				source.getResponseFormat() != null ? source.getResponseFormat() : target.getResponseFormat());
		mergedBuilder.withSpeed(source.getSpeed() != null ? source.getSpeed() : target.getSpeed());

		return mergedBuilder.build();
	}

}
