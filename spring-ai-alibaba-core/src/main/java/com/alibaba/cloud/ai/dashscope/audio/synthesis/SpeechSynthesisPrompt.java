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

import org.springframework.ai.model.ModelRequest;

import java.util.Collections;
import java.util.List;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisPrompt implements ModelRequest<List<SpeechSynthesisMessage>> {

	private final List<SpeechSynthesisMessage> messages;

	private final SpeechSynthesisOptions options;

	public SpeechSynthesisPrompt(String contents) {
		this(new SpeechSynthesisMessage(contents));
	}

	public SpeechSynthesisPrompt(SpeechSynthesisMessage message) {
		this(Collections.singletonList(message));
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> messages) {
		this(messages, null);
	}

	public SpeechSynthesisPrompt(String contents, SpeechSynthesisOptions options) {
		this(new SpeechSynthesisMessage(contents), options);
	}

	public SpeechSynthesisPrompt(SpeechSynthesisMessage message, SpeechSynthesisOptions options) {
		this(Collections.singletonList(message), options);
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> messages, SpeechSynthesisOptions options) {
		this.messages = messages;
		this.options = options;
	}

	@Override
	public SpeechSynthesisOptions getOptions() {
		return this.options;
	}

	@Override
	public List<SpeechSynthesisMessage> getInstructions() {
		return this.messages;
	}

}
