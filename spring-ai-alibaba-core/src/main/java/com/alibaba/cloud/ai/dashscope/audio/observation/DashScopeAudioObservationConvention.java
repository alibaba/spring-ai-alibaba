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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * DashScope conventions to populate observations for audio model operations.
 */
public class DashScopeAudioObservationConvention implements ObservationConvention<Observation.Context> {

	private static final String NAME = "spring.ai.alibaba.audio";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getContextualName(Observation.Context context) {
		if (context instanceof AudioTranscriptionContext) {
			return "spring.ai.audio.transcription";
		}
		else if (context instanceof SpeechSynthesisContext) {
			return "spring.ai.audio.synthesis";
		}
		return NAME;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(Observation.Context context) {
		KeyValues keyValues = KeyValues.empty();

		if (context instanceof AudioTranscriptionContext audioContext) {
			keyValues = keyValues.and(KeyValue.of("model", audioContext.getModelName()))
				.and(KeyValue.of("format", audioContext.getFormat()))
				.and(KeyValue.of("sample_rate", String.valueOf(audioContext.getSampleRate())));
		}
		else if (context instanceof SpeechSynthesisContext synthesisContext) {
			keyValues = keyValues.and(KeyValue.of("model", synthesisContext.getModelName()))
				.and(KeyValue.of("format", synthesisContext.getFormat()))
				.and(KeyValue.of("sample_rate", String.valueOf(synthesisContext.getSampleRate())));
		}

		return keyValues;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(Observation.Context context) {
		KeyValues keyValues = KeyValues.empty();

		if (context instanceof AudioTranscriptionContext audioContext) {
			if (Boolean.TRUE.equals(audioContext.getStreaming())) {
				keyValues = keyValues.and(KeyValue.of("chunk_count", String.valueOf(audioContext.getChunkCount())))
					.and(KeyValue.of("total_chunks", String.valueOf(audioContext.getTotalChunks())));
			}
			else {
				keyValues = keyValues.and(KeyValue.of("input_length", String.valueOf(audioContext.getInputLength())))
					.and(KeyValue.of("output_length", String.valueOf(audioContext.getOutputLength())))
					.and(KeyValue.of("duration", String.valueOf(audioContext.getDuration())));
			}
		}
		else if (context instanceof SpeechSynthesisContext synthesisContext) {
			if (Boolean.TRUE.equals(synthesisContext.getStreaming())) {
				keyValues = keyValues.and(KeyValue.of("chunk_count", String.valueOf(synthesisContext.getChunkCount())))
					.and(KeyValue.of("total_chunks", String.valueOf(synthesisContext.getTotalChunks())));
			}
			else {
				keyValues = keyValues
					.and(KeyValue.of("input_length", String.valueOf(synthesisContext.getInputLength())))
					.and(KeyValue.of("output_length", String.valueOf(synthesisContext.getOutputLength())))
					.and(KeyValue.of("duration", String.valueOf(synthesisContext.getDuration())));
			}
		}

		return keyValues;
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof AudioTranscriptionContext || context instanceof SpeechSynthesisContext;
	}

}