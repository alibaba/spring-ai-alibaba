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
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a prompt for speech synthesis. This class contains the text to be
 * synthesized and any additional options.
 */
public class SpeechSynthesisPrompt implements ModelRequest<List<SpeechSynthesisMessage>> {

	private final List<SpeechSynthesisMessage> instructions;

	private final SpeechSynthesisOptions options;

	public SpeechSynthesisPrompt(String text) {
		this(text, null);
	}

	public SpeechSynthesisPrompt(String text, SpeechSynthesisOptions options) {
		this.instructions = new ArrayList<>();
		this.instructions.add(new SpeechSynthesisMessage(text));
		this.options = options;
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> instructions) {
		this(instructions, null);
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> instructions, SpeechSynthesisOptions options) {
		this.instructions = instructions;
		this.options = options;
	}

	@Override
	public List<SpeechSynthesisMessage> getInstructions() {
		return instructions;
	}

	public SpeechSynthesisOptions getOptions() {
		return options;
	}

	public static class SpeechSynthesisInstruction {

		private final String text;

		private final Resource audio;

		private final String voice;

		private final String language;

		private final Float speed;

		private final Float volume;

		public SpeechSynthesisInstruction(String text, Resource audio, String voice, String language, Float speed,
				Float volume) {
			this.text = text;
			this.audio = audio;
			this.voice = voice;
			this.language = language;
			this.speed = speed;
			this.volume = volume;
		}

		public String getText() {
			return text;
		}

		public Resource getAudio() {
			return audio;
		}

		public String getVoice() {
			return voice;
		}

		public String getLanguage() {
			return language;
		}

		public Float getSpeed() {
			return speed;
		}

		public Float getVolume() {
			return volume;
		}

	}

}
