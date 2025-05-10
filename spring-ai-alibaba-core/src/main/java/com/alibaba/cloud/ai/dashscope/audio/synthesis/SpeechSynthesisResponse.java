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

import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ResponseMetadata;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the response from a speech synthesis operation. This class contains the
 * synthesized audio data and any additional metadata.
 */
public class SpeechSynthesisResponse implements ModelResponse<SpeechSynthesisResult> {

	private final SpeechSynthesisResult result;

	private final Map<String, Object> metadata;

	public SpeechSynthesisResponse(SpeechSynthesisResult result) {
		this(result, null);
	}

	public SpeechSynthesisResponse(SpeechSynthesisResult result, Map<String, Object> metadata) {
		this.result = result;
		this.metadata = metadata;
	}

	@Override
	public SpeechSynthesisResult getResult() {
		return null;
	}

	@Override
	public List<SpeechSynthesisResult> getResults() {
		return null;
	}

	@Override
	public ResponseMetadata getMetadata() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpeechSynthesisResponse that))
			return false;
		return Objects.equals(result, that.result) && Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(result, metadata);
	}

	public static class SpeechSynthesisOutput {

		private final ByteBuffer audio;

		private final String format;

		private final Integer sampleRate;

		public SpeechSynthesisOutput(ByteBuffer audio) {
			this(audio, null, null);
		}

		public SpeechSynthesisOutput(ByteBuffer audio, String format, Integer sampleRate) {
			this.audio = audio;
			this.format = format;
			this.sampleRate = sampleRate;
		}

		public ByteBuffer getAudio() {
			return audio;
		}

		public String getFormat() {
			return format;
		}

		public Integer getSampleRate() {
			return sampleRate;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof SpeechSynthesisOutput that))
				return false;
			return Objects.equals(audio, that.audio) && Objects.equals(format, that.format)
					&& Objects.equals(sampleRate, that.sampleRate);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audio, format, sampleRate);
		}

	}

}