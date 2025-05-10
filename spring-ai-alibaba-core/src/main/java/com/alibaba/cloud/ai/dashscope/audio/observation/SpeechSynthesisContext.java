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

package com.alibaba.cloud.ai.dashscope.audio.observation;

import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import io.micrometer.observation.Observation;

/**
 * Context class for speech synthesis observations. This class holds the context
 * information for speech synthesis operations.
 */
public class SpeechSynthesisContext extends Observation.Context {

	private String modelName;

	private String format;

	private Integer sampleRate;

	private Long inputLength;

	private Long outputLength;

	private Long duration;

	private SpeechSynthesisPrompt prompt;

	private SpeechSynthesisResponse response;

	private Throwable error;

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Integer getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	public Long getInputLength() {
		return inputLength;
	}

	public void setInputLength(Long inputLength) {
		this.inputLength = inputLength;
	}

	public Long getOutputLength() {
		return outputLength;
	}

	public void setOutputLength(Long outputLength) {
		this.outputLength = outputLength;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public SpeechSynthesisPrompt getPrompt() {
		return prompt;
	}

	public void setPrompt(SpeechSynthesisPrompt prompt) {
		this.prompt = prompt;
	}

	public SpeechSynthesisResponse getResponse() {
		return response;
	}

	public void setResponse(SpeechSynthesisResponse response) {
		this.response = response;
	}

	public Throwable getError() {
		return error;
	}

	public void setError(Throwable error) {
		this.error = error;
	}

}
