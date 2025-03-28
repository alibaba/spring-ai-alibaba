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

import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.protocol.DashScopeWebSocketClient;
import org.springframework.ai.audio.transcription.*;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.transcription.AudioTranscriptionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;

public class DashScopeAudioTranscriptionModel implements AudioTranscriptionModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAudioTranscriptionModel.class);

	public static final String REQUEST_ID = "request_id";

	public static final String TASK_ID = "task_id";

	public static final String STATUS_CODE = "status_code";

	public static final String CODE = "code";

	public static final String USAGE = "usage";

	public static final String OUTPUT = "output";

	public static final String MESSAGE = "message";

	private final DashScopeAudioTranscriptionApi api;

	private final DashScopeAudioTranscriptionOptions options;

	private final RetryTemplate retryTemplate;

	public DashScopeAudioTranscriptionModel(DashScopeAudioTranscriptionApi api) {
		this(api, DashScopeAudioTranscriptionOptions.builder().build());
	}

	public DashScopeAudioTranscriptionModel(DashScopeAudioTranscriptionApi api,
			DashScopeAudioTranscriptionOptions options) {
		this(api, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeAudioTranscriptionModel(DashScopeAudioTranscriptionApi api,
			DashScopeAudioTranscriptionOptions options, RetryTemplate retryTemplate) {
		this.api = api;
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public AudioTranscriptionResponse asyncCall(AudioTranscriptionPrompt prompt) {
		DashScopeAudioTranscriptionApi.Request request = createRequest(prompt);

		ResponseEntity<DashScopeAudioTranscriptionApi.Response> response = this.api.call(request);

		if (response == null || response.getBody() == null) {
			logger.warn("app call error: request: {}", request);
			return null;
		}

		return toResponse(response.getBody());
	}

	@Override
	public AudioTranscriptionResponse fetch(String taskId) {
		AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(null);

		DashScopeAudioTranscriptionApi.Request request = createRequest(prompt);

		ResponseEntity<DashScopeAudioTranscriptionApi.Response> response = this.api.callWithTaskId(request, taskId);

		return toResponse(Objects.requireNonNull(response.getBody()));
	}

	@Override
	public AudioTranscriptionResponse call(AudioTranscriptionPrompt prompt) {
		DashScopeAudioTranscriptionApi.Request request = createRequest(prompt);

		ResponseEntity<DashScopeAudioTranscriptionApi.Response> submitResponse = this.api.call(request);

		String taskId = Objects.requireNonNull(submitResponse.getBody()).output().taskId();

		int waitMilliseconds = 1000;
		int maxWaitMilliseconds = 5 * 1000;
		int incrementSteps = 3;
		int step = 0;
		while (true) {
			DashScopeAudioTranscriptionApi.Response fetchResponse = (this.api.callWithTaskId(request, taskId))
				.getBody();

			DashScopeAudioTranscriptionApi.TaskStatus taskStatus = Objects.requireNonNull(fetchResponse)
				.output()
				.taskStatus();

			if (taskStatus == DashScopeAudioTranscriptionApi.TaskStatus.FAILED
					|| taskStatus == DashScopeAudioTranscriptionApi.TaskStatus.CANCELED
					|| taskStatus == DashScopeAudioTranscriptionApi.TaskStatus.UNKNOWN) {
				logger.error("task failed");
				return toResponse(fetchResponse);
			}
			else if (taskStatus == DashScopeAudioTranscriptionApi.TaskStatus.SUCCEEDED) {
				logger.info("task succeeded");
				return toResponse(fetchResponse);
			}
			else {
				step += 1;
				if (waitMilliseconds < maxWaitMilliseconds && step % incrementSteps == 0) {
					waitMilliseconds = Math.min(waitMilliseconds * 2, maxWaitMilliseconds);
				}
				try {
					Thread.sleep(waitMilliseconds);
				}
				catch (InterruptedException ignored) {
				}
			}
		}
	}

	@Override
	public Flux<AudioTranscriptionResponse> stream(AudioTranscriptionPrompt prompt) {
		DashScopeAudioTranscriptionApi.RealtimeRequest run_request = createRealtimeRequest(prompt,
				DashScopeWebSocketClient.EventType.RUN_TASK);

		logger.info("send run-task");
		this.api.realtimeControl(run_request);

		Resource resource = prompt.getInstructions();

		Flux<ByteBuffer> audio = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 16384)
			.map(dataBuffer -> {
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
				DataBufferUtils.release(dataBuffer);
				return byteBuffer;
			})
			.delayElements(Duration.ofMillis(100), Schedulers.boundedElastic())
			.doOnComplete(() -> {
				DashScopeAudioTranscriptionApi.RealtimeRequest finish_request = createRealtimeRequest(prompt,
						DashScopeWebSocketClient.EventType.FINISH_TASK);

				logger.info("send finish-task");
				this.api.realtimeControl(finish_request);
			});

		return this.api.realtimeStream(audio).map(this::toResponse);
	}

	private DashScopeAudioTranscriptionApi.Request createRequest(AudioTranscriptionPrompt prompt) {
		DashScopeAudioTranscriptionOptions options = mergeOptions(prompt);

		List<String> fileUrls = List.of();
		try {
			if (prompt.getInstructions() != null) {
				fileUrls = List.of(prompt.getInstructions().getURL().toString());
			}
			return new DashScopeAudioTranscriptionApi.Request(options.getModel(),
					new DashScopeAudioTranscriptionApi.Request.Input(fileUrls),
					new DashScopeAudioTranscriptionApi.Request.Parameters(options.getChannelId(),
							options.getVocabularyId(), options.getPhraseId(), options.getDisfluencyRemovalEnabled(),
							options.getLanguageHints()));
		}
		catch (IOException e) {
			throw new DashScopeException("failed to get file urls", e);
		}
	}

	private DashScopeAudioTranscriptionApi.RealtimeRequest createRealtimeRequest(AudioTranscriptionPrompt prompt,
			DashScopeWebSocketClient.EventType action) {
		DashScopeAudioTranscriptionOptions options = mergeOptions(prompt);

		return new DashScopeAudioTranscriptionApi.RealtimeRequest(
				new DashScopeAudioTranscriptionApi.RealtimeRequest.Header(action, UUID.randomUUID().toString(),
						"duplex"),
				new DashScopeAudioTranscriptionApi.RealtimeRequest.Payload(options.getModel(), "audio", "asr",
						"recognition", new DashScopeAudioTranscriptionApi.RealtimeRequest.Payload.Input(),
						new DashScopeAudioTranscriptionApi.RealtimeRequest.Payload.Parameters(options.getSampleRate(),
								options.getFormat(), options.getDisfluencyRemovalEnabled())));
	}

	private DashScopeAudioTranscriptionOptions mergeOptions(AudioTranscriptionPrompt prompt) {
		DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder().build();

		if (prompt.getOptions() != null) {
			DashScopeAudioTranscriptionOptions runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(),
					AudioTranscriptionOptions.class, DashScopeAudioTranscriptionOptions.class);

			options = ModelOptionsUtils.merge(runtimeOptions, options, DashScopeAudioTranscriptionOptions.class);
		}

		options = ModelOptionsUtils.merge(options, this.options, DashScopeAudioTranscriptionOptions.class);
		return options;
	}

	private AudioTranscriptionResponse toResponse(DashScopeAudioTranscriptionApi.Response apiResponse) {
		DashScopeAudioTranscriptionApi.Response.Output apiOutput = apiResponse.output();
		List<DashScopeAudioTranscriptionApi.Response.Output.Result> apiResults = apiOutput.results();

		DashScopeAudioTranscriptionApi.TaskStatus taskStatus = apiOutput.taskStatus();

		String text = null;
		if (apiResults != null && !apiResults.isEmpty()) {
			String transcriptionUrl = apiResults.get(0).transcriptionUrl();
			DashScopeAudioTranscriptionApi.Outcome outcome = this.api.getOutcome(transcriptionUrl);
			if (!outcome.transcripts().isEmpty()) {
				text = outcome.transcripts().get(0).text();
			}
		}

		AudioTranscription result = new AudioTranscription(text);

		AudioTranscriptionResponseMetadata responseMetadata = new AudioTranscriptionResponseMetadata();
		if (apiResponse.statusCode() != null) {
			responseMetadata.put(STATUS_CODE, apiResponse.statusCode());
		}
		if (apiResponse.requestId() != null) {
			responseMetadata.put(REQUEST_ID, apiResponse.requestId());
		}
		if (apiResponse.code() != null) {
			responseMetadata.put(CODE, apiResponse.code());
		}
		if (apiResponse.message() != null) {
			responseMetadata.put(MESSAGE, apiResponse.message());
		}
		if (apiResponse.usage() != null) {
			responseMetadata.put(USAGE, apiResponse.usage());
		}
		responseMetadata.put(OUTPUT, apiOutput);

		return new AudioTranscriptionResponse(result, responseMetadata);
	}

	private AudioTranscriptionResponse toResponse(DashScopeAudioTranscriptionApi.RealtimeResponse apiResponse) {
		DashScopeAudioTranscriptionApi.RealtimeResponse.Payload apiPayload = apiResponse.payload();
		DashScopeAudioTranscriptionApi.RealtimeResponse.Payload.Output apiOutput = apiPayload.output();
		DashScopeAudioTranscriptionApi.RealtimeResponse.Payload.Output.Sentence sentence = apiOutput.sentence();
		String taskId = apiResponse.header().taskId();

		String text = null;
		if (sentence != null) {
			text = sentence.text();
		}

		AudioTranscription result = new AudioTranscription(text);

		AudioTranscriptionResponseMetadata responseMetadata = new AudioTranscriptionResponseMetadata();

		responseMetadata.put(TASK_ID, taskId);
		responseMetadata.put(OUTPUT, apiOutput);
		if (apiPayload.usage() != null) {
			responseMetadata.put(USAGE, apiPayload.usage());
		}

		return new AudioTranscriptionResponse(result, responseMetadata);
	}

}
