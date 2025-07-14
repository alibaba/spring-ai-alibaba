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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisOutput;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResult;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Audio Speech: input text, output audio.
 *
 * @author kevinlin09
 */
public class DashScopeAudioSpeechModel implements SpeechSynthesisModel {

	private final DashScopeAudioSpeechApi api;

	private final DashScopeAudioSpeechOptions options;

	private final RetryTemplate retryTemplate;

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi api) {
		this(api, DashScopeAudioSpeechOptions.builder().model("").build());
	}

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi api, DashScopeAudioSpeechOptions options) {
		this(api, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeAudioSpeechModel(DashScopeAudioSpeechApi api, DashScopeAudioSpeechOptions options,
			RetryTemplate retryTemplate) {
		this.api = api;
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public SpeechSynthesisResponse call(SpeechSynthesisPrompt prompt) {
		Flux<SpeechSynthesisResponse> flux = this.stream(prompt);
		return flux.reduce((resp1, resp2) -> {
			ByteBuffer combinedBuffer = ByteBuffer.allocate(resp1.getResult().getOutput().getAudio().remaining()
					+ resp2.getResult().getOutput().getAudio().remaining());
			combinedBuffer.put(resp1.getResult().getOutput().getAudio());
			combinedBuffer.put(resp2.getResult().getOutput().getAudio());
			combinedBuffer.flip();

			return new SpeechSynthesisResponse(new SpeechSynthesisResult(new SpeechSynthesisOutput(combinedBuffer)));
		}).block();
	}

	@Override
	public Flux<SpeechSynthesisResponse> stream(SpeechSynthesisPrompt prompt) {
		return this.retryTemplate.execute(ctx -> this.api.streamOut(createRequest(prompt))
			.map(SpeechSynthesisOutput::new)
			.map(SpeechSynthesisResult::new)
			.map(SpeechSynthesisResponse::new));
	}

	public DashScopeAudioSpeechApi.Request createRequest(SpeechSynthesisPrompt prompt) {
		DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder().build();
		if (prompt.getOptions() != null) {
			DashScopeAudioSpeechOptions runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(),
					SpeechSynthesisOptions.class, DashScopeAudioSpeechOptions.class);

			options = ModelOptionsUtils.merge(runtimeOptions, options, DashScopeAudioSpeechOptions.class);
		}

		options = ModelOptionsUtils.merge(options, this.options, DashScopeAudioSpeechOptions.class);

		return new DashScopeAudioSpeechApi.Request(
				new DashScopeAudioSpeechApi.Request.RequestHeader("run-task", UUID.randomUUID().toString(), "out"),
				new DashScopeAudioSpeechApi.Request.RequestPayload(options.getModel(), "audio", "tts",
						"SpeechSynthesizer",
						new DashScopeAudioSpeechApi.Request.RequestPayload.RequestPayloadInput(
								prompt.getInstructions().get(0).getText()),
						new DashScopeAudioSpeechApi.Request.RequestPayload.RequestPayloadParameters(options.getVolume(),
								options.getRequestTextType().getValue(), options.getVoice(), options.getSampleRate(),
								options.getSpeed(), options.getResponseFormat().getValue(), options.getPitch(),
								options.getEnablePhonemeTimestamp(), options.getEnableWordTimestamp())));
	}

	private SpeechSynthesisResponse toResponse(DashScopeAudioSpeechApi.Response apiResponse) {
		SpeechSynthesisOutput output = new SpeechSynthesisOutput(apiResponse.getAudio());
		SpeechSynthesisResult result = new SpeechSynthesisResult(output);
		return new SpeechSynthesisResponse(result);
	}

}
