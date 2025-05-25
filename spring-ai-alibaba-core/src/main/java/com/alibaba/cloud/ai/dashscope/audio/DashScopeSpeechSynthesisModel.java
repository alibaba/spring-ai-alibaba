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

import java.nio.ByteBuffer;
import java.util.UUID;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisOutput;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author kevinlin09
 */
public class DashScopeSpeechSynthesisModel implements SpeechSynthesisModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeSpeechSynthesisModel.class);

	private final DashScopeSpeechSynthesisApi api;

	private final DashScopeSpeechSynthesisOptions options;

	private final RetryTemplate retryTemplate;

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api) {
		this(api, DashScopeSpeechSynthesisOptions.builder().model("").build());
	}

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api, DashScopeSpeechSynthesisOptions options) {
		this(api, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeSpeechSynthesisModel(DashScopeSpeechSynthesisApi api, DashScopeSpeechSynthesisOptions options,
			RetryTemplate retryTemplate) {
		this.api = api;
		this.options = options;
		this.retryTemplate = retryTemplate;
	}

	public enum DashScopeSpeechModel {

		SAMBERT_ZHICHU_V1("sambert-zhichu-v1"),

		COSYVOICE_V1("cosyvoice-v1");

		private final String model;

		DashScopeSpeechModel(String model) {
			this.model = model;
		}

		public String getModel() {
			return this.model;
		}

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

	public DashScopeSpeechSynthesisApi.Request createRequest(SpeechSynthesisPrompt prompt) {
		DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder().build();
		if (prompt.getOptions() != null) {
			DashScopeSpeechSynthesisOptions runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(),
					SpeechSynthesisOptions.class, DashScopeSpeechSynthesisOptions.class);

			options = ModelOptionsUtils.merge(runtimeOptions, options, DashScopeSpeechSynthesisOptions.class);
		}

		options = ModelOptionsUtils.merge(options, this.options, DashScopeSpeechSynthesisOptions.class);

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

	private SpeechSynthesisResponse toResponse(DashScopeSpeechSynthesisApi.Response apiResponse) {
		SpeechSynthesisOutput output = new SpeechSynthesisOutput(apiResponse.getAudio());
		SpeechSynthesisResult result = new SpeechSynthesisResult(output);
		return new SpeechSynthesisResponse(result);
	}

}
