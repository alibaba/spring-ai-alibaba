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
package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ModelResult;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisResult implements ModelResult<SpeechSynthesisOutput> {

	private final SpeechSynthesisOutput output;

	private final SpeechSynthesisResultMetadata metadata;

	public SpeechSynthesisResult(SpeechSynthesisOutput output) {
		this.output = output;
		this.metadata = SpeechSynthesisResultMetadata.NULL;
	}

	public SpeechSynthesisResult(SpeechSynthesisOutput output, SpeechSynthesisResultMetadata metadata) {
		this.output = output;
		this.metadata = metadata;
	}

	@Override
	public SpeechSynthesisOutput getOutput() {
		return this.output;
	}

	@Override
	public SpeechSynthesisResultMetadata getMetadata() {
		return this.metadata;
	}

}
