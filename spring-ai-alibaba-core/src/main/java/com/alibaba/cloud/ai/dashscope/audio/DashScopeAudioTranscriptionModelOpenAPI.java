/*
 * Copyright 2023-2024 the original author or authors.
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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

public class DashScopeAudioTranscriptionModelOpenAPI
		implements Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final DashScopeAudioTranscriptionOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final DashScopeAudioApi audioApi;

	/**
	 * DashScopeAudioTranscriptionModel is a client class used to interact with the OpenAI
	 * Audio Transcription API.
	 * @param audioApi The DashScopeAudioApi instance to be used for making API calls.
	 */
	public DashScopeAudioTranscriptionModelOpenAPI(DashScopeAudioApi audioApi) {
		this(audioApi,
				DashScopeAudioTranscriptionOptions.builder()
					.withModel(DashScopeAudioApi.WhisperModel.WHISPER_1.getValue())
					// .withResponseFormat(DashScopeAudioApi.TranscriptResponseFormat.JSON)
					// .withTemperature(0.7f)
					.build());
	}

	/**
	 * DashScopeAudioTranscriptionModel is a client class used to interact with the OpenAI
	 * Audio Transcription API.
	 * @param audioApi The DashScopeAudioApi instance to be used for making API calls.
	 * @param options The DashScopeAudioTranscriptionOptions instance for configuring the
	 * audio transcription.
	 */
	public DashScopeAudioTranscriptionModelOpenAPI(DashScopeAudioApi audioApi,
			DashScopeAudioTranscriptionOptions options) {
		this(audioApi, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * DashScopeAudioTranscriptionModel is a client class used to interact with the OpenAI
	 * Audio Transcription API.
	 * @param audioApi The DashScopeAudioApi instance to be used for making API calls.
	 * @param options The DashScopeAudioTranscriptionOptions instance for configuring the
	 * audio transcription.
	 * @param retryTemplate The RetryTemplate instance for retrying failed API calls.
	 */
	public DashScopeAudioTranscriptionModelOpenAPI(DashScopeAudioApi audioApi,
			DashScopeAudioTranscriptionOptions options, RetryTemplate retryTemplate) {
		Assert.notNull(audioApi, "DashScopeAudioApi must not be null");
		Assert.notNull(options, "DashScopeTranscriptionOptions must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		this.audioApi = audioApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
	}

	public String call(Resource audioResource) {
		org.springframework.ai.audio.transcription.AudioTranscriptionPrompt transcriptionRequest = new org.springframework.ai.audio.transcription.AudioTranscriptionPrompt(
				audioResource);
		return call(transcriptionRequest).getResult().getOutput();
	}

	@Override
	public org.springframework.ai.audio.transcription.AudioTranscriptionResponse call(
			org.springframework.ai.audio.transcription.AudioTranscriptionPrompt transcriptionPrompt) {

		Resource audioResource = transcriptionPrompt.getInstructions();

		// DashScopeAudioApi.TranscriptionRequest request =
		// createRequest(transcriptionPrompt);

		return null;

		// if (request.responseFormat().isJsonType()) {
		//
		// ResponseEntity<DashScopeAudioApi.StructuredResponse> transcriptionEntity =
		// this.retryTemplate
		// .execute(ctx -> this.audioApi.createTranscription(request,
		// DashScopeAudioApi.StructuredResponse.class));
		//
		// var transcription = transcriptionEntity.getBody();
		//
		// if (transcription == null) {
		// logger.warn("No transcription returned for request: {}", audioResource);
		// return new
		// org.springframework.ai.audio.transcription.AudioTranscriptionResponse(null);
		// }
		//
		// AudioTranscription transcript = new AudioTranscription(transcription.text());
		//
		// RateLimit rateLimits =
		// DashScopeResponseHeaderExtractor.extractAiResponseHeaders(transcriptionEntity);
		//
		// return new
		// org.springframework.ai.audio.transcription.AudioTranscriptionResponse(transcript,
		// DashScopeAudioTranscriptionResponseMetadata.from(transcriptionEntity.getBody())
		// .withRateLimit(rateLimits));
		//
		// }
		// else {
		//
		// ResponseEntity<String> transcriptionEntity = this.retryTemplate
		// .execute(ctx -> this.audioApi.createTranscription(request, String.class));
		//
		// var transcription = transcriptionEntity.getBody();
		//
		// if (transcription == null) {
		// logger.warn("No transcription returned for request: {}", audioResource);
		// return new
		// org.springframework.ai.audio.transcription.AudioTranscriptionResponse(null);
		// }
		//
		// AudioTranscription transcript = new AudioTranscription(transcription);
		//
		// RateLimit rateLimits =
		// DashScopeResponseHeaderExtractor.extractAiResponseHeaders(transcriptionEntity);
		//
		// return new AudioTranscriptionResponse(transcript,
		// DashScopeAudioTranscriptionResponseMetadata.from(transcriptionEntity.getBody())
		// .withRateLimit(rateLimits));
		// }
	}

	// DashScopeAudioApi.TranscriptionRequest createRequest(AudioTranscriptionPrompt
	// transcriptionPrompt) {
	//
	// DashScopeAudioTranscriptionOptions options = this.defaultOptions;
	//
	// if (transcriptionPrompt.getOptions() != null) {
	// if (transcriptionPrompt.getOptions() instanceof DashScopeAudioTranscriptionOptions
	// runtimeOptions) {
	// options = this.merge(runtimeOptions, options);
	// }
	// else {
	// throw new IllegalArgumentException("Prompt options are not of type
	// TranscriptionOptions: "
	// + transcriptionPrompt.getOptions().getClass().getSimpleName());
	// }
	// }
	//
	// return DashScopeAudioApi.TranscriptionRequest.builder()
	// .withFile(toBytes(transcriptionPrompt.getInstructions()))
	// .withResponseFormat(options.getResponseFormat())
	// .withPrompt(options.getPrompt())
	// .withTemperature(options.getTemperature())
	// .withLanguage(options.getLanguage())
	// .withModel(options.getModel())
	// .withGranularityType(options.getGranularityType())
	// .build();
	// }
	//
	// private byte[] toBytes(Resource resource) {
	// try {
	// return resource.getInputStream().readAllBytes();
	// }
	// catch (Exception e) {
	// throw new IllegalArgumentException("Failed to read resource: " + resource, e);
	// }
	// }
	//
	// private DashScopeAudioTranscriptionOptions merge(DashScopeAudioTranscriptionOptions
	// source,
	// DashScopeAudioTranscriptionOptions target) {
	//
	// if (source == null) {
	// source = new DashScopeAudioTranscriptionOptions();
	// }
	//
	// DashScopeAudioTranscriptionOptions merged = new
	// DashScopeAudioTranscriptionOptions();
	// merged.setLanguage(source.getLanguage() != null ? source.getLanguage() :
	// target.getLanguage());
	// merged.setModel(source.getModel() != null ? source.getModel() : target.getModel());
	// merged.setPrompt(source.getPrompt() != null ? source.getPrompt() :
	// target.getPrompt());
	// merged.setResponseFormat(
	// source.getResponseFormat() != null ? source.getResponseFormat() :
	// target.getResponseFormat());
	// merged.setTemperature(source.getTemperature() != null ? source.getTemperature() :
	// target.getTemperature());
	// merged.setGranularityType(
	// source.getGranularityType() != null ? source.getGranularityType() :
	// target.getGranularityType());
	// return merged;
	// }

}
