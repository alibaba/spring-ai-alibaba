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

package com.alibaba.cloud.ai.dashscope.audio.speech;

import java.util.Objects;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;

import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.model.ModelRequest;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

public class SpeechPrompt implements ModelRequest<SpeechMessage> {

	private final SpeechMessage message;

	private DashScopeAudioSpeechOptions speechOptions;

	public SpeechPrompt(String instructions) {

		this(new SpeechMessage(instructions), DashScopeAudioSpeechOptions.builder().build());
	}

	public SpeechPrompt(String instructions, DashScopeAudioSpeechOptions speechOptions) {

		this(new SpeechMessage(instructions), speechOptions);
	}

	public SpeechPrompt(SpeechMessage speechMessage) {
		this(speechMessage, DashScopeAudioSpeechOptions.builder().build());
	}

	public SpeechPrompt(SpeechMessage speechMessage, DashScopeAudioSpeechOptions speechOptions) {

		this.message = speechMessage;
		this.speechOptions = speechOptions;
	}

	@Override
	public SpeechMessage getInstructions() {
		return this.message;
	}

	@Override
	public ModelOptions getOptions() {
		return speechOptions;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {

			return true;
		}
		if (!(o instanceof SpeechPrompt that)) {

			return false;
		}

		return Objects.equals(speechOptions, that.speechOptions) && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {

		return Objects.hash(speechOptions, message);
	}

}
