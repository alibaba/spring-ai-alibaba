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
package com.alibaba.cloud.ai.dashscope.metadata.observation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.observation.AudioTranscriptionContext;
import com.alibaba.cloud.ai.dashscope.audio.observation.DashScopeAudioObservationConvention;
import com.alibaba.cloud.ai.dashscope.audio.observation.SpeechSynthesisContext;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeAudioObservationConvention. Tests cover both audio
 * transcription and speech synthesis scenarios, including observation names, audio
 * parameters handling, and key value generation.
 *
 * @author Inlines10
 * @author <a href="mailto:yuluo08290126@gmail.com">Inlines10</a>
 * @since 1.0.0-M5.1
 */
class DashScopeAudioObservationConventionTests {

	private DashScopeAudioObservationConvention convention;

	private AudioTranscriptionContext transcriptionContext;

	private SpeechSynthesisContext synthesisContext;

	@BeforeEach
	void setUp() {
		convention = new DashScopeAudioObservationConvention();

		// Set up audio transcription context
		transcriptionContext = new AudioTranscriptionContext();
		transcriptionContext.setModelName("speech-translation-v1");
		transcriptionContext.setFormat("wav");
		transcriptionContext.setSampleRate(16000);
		transcriptionContext.setPrompt(new AudioTranscriptionPrompt(new ByteArrayResource(new byte[1024])));

		// Set up speech synthesis context
		synthesisContext = new SpeechSynthesisContext();
		synthesisContext.setModelName("sambert-zhichu-v1");
		synthesisContext.setFormat("mp3");
		synthesisContext.setSampleRate(16000);
		synthesisContext.setPrompt(new SpeechSynthesisPrompt("Test text"));
	}

	@Test
	void testGetName() {
		assertThat(convention.getName()).isEqualTo("spring.ai.alibaba.audio");
	}

	@Test
	void testGetContextualName() {
		// Test audio transcription context name
		assertThat(convention.getContextualName(transcriptionContext)).isEqualTo("spring.ai.audio.transcription");

		// Test speech synthesis context name
		assertThat(convention.getContextualName(synthesisContext)).isEqualTo("spring.ai.audio.synthesis");

		// Test unknown context
		assertThat(convention.getContextualName(new Observation.Context())).isEqualTo("spring.ai.alibaba.audio");
	}

	@Test
	void testLowCardinalityKeyValuesForTranscription() {
		// Set transcription options
		transcriptionContext.setPrompt(new AudioTranscriptionPrompt(new ByteArrayResource(new byte[1024]),
				DashScopeAudioTranscriptionOptions.builder()
					.withModel("speech-translation-v1")
					.withFormat(DashScopeAudioTranscriptionOptions.AudioFormat.WAV)
					.withSampleRate(16000)
					.build()));

		KeyValues keyValues = convention.getLowCardinalityKeyValues(transcriptionContext);

		assertThat(keyValues).hasSize(3)
			.contains(KeyValue.of("model", "speech-translation-v1"))
			.contains(KeyValue.of("format", "wav"))
			.contains(KeyValue.of("sample_rate", "16000"));
	}

	@Test
	void testLowCardinalityKeyValuesForSynthesis() {
		// Set synthesis options
		synthesisContext.setPrompt(new SpeechSynthesisPrompt("Test text",
				DashScopeSpeechSynthesisOptions.builder()
					.withModel("sambert-zhichu-v1")
					.withResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.MP3) // 使用字符串表示格式
					.withSampleRate(16000)
					.withVoice("female")
					.withVolume(50)
					.withSpeed(1.0)
					.build()));

		KeyValues keyValues = convention.getLowCardinalityKeyValues(synthesisContext);

		assertThat(keyValues).hasSize(3)
			.contains(KeyValue.of("model", "sambert-zhichu-v1"))
			.contains(KeyValue.of("format", "mp3"))
			.contains(KeyValue.of("sample_rate", "16000"));
	}

	@Test
	void testHighCardinalityKeyValuesForTranscription() {
		// Set high cardinality data for transcription context
		transcriptionContext.setInputLength(1024L);
		transcriptionContext.setOutputLength(2048L);
		transcriptionContext.setDuration(5000L);

		KeyValues keyValues = convention.getHighCardinalityKeyValues(transcriptionContext);

		assertThat(keyValues).hasSize(3)
			.contains(KeyValue.of("input_length", "1024"))
			.contains(KeyValue.of("output_length", "2048"))
			.contains(KeyValue.of("duration", "5000"));
	}

	@Test
	void testHighCardinalityKeyValuesForSynthesis() {
		// Set high cardinality data for synthesis context
		synthesisContext.setInputLength(100L);
		synthesisContext.setOutputLength(1000L);
		synthesisContext.setDuration(3000L);

		KeyValues keyValues = convention.getHighCardinalityKeyValues(synthesisContext);

		assertThat(keyValues).hasSize(3)
			.contains(KeyValue.of("input_length", "100"))
			.contains(KeyValue.of("output_length", "1000"))
			.contains(KeyValue.of("duration", "3000"));
	}

	@Test
	void testErrorHandling() {
		// Test error handling - transcription
		transcriptionContext.setError(new RuntimeException("Transcription error"));
		KeyValues transcriptionKeyValues = convention.getHighCardinalityKeyValues(transcriptionContext);
		assertThat(transcriptionKeyValues).hasSize(1).contains(KeyValue.of("error", "Transcription error"));

		// Test error handling - synthesis
		synthesisContext.setError(new RuntimeException("Synthesis error"));
		KeyValues synthesisKeyValues = convention.getHighCardinalityKeyValues(synthesisContext);
		assertThat(synthesisKeyValues).hasSize(1).contains(KeyValue.of("error", "Synthesis error"));
	}

	@Test
	void testSupportsContext() {
		// Test supported context types
		assertThat(convention.supportsContext(transcriptionContext)).isTrue();
		assertThat(convention.supportsContext(synthesisContext)).isTrue();
		assertThat(convention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void testEmptyContext() {
		// Test empty context
		AudioTranscriptionContext emptyTranscriptionContext = new AudioTranscriptionContext();
		SpeechSynthesisContext emptySynthesisContext = new SpeechSynthesisContext();

		KeyValues transcriptionKeyValues = convention.getLowCardinalityKeyValues(emptyTranscriptionContext);
		KeyValues synthesisKeyValues = convention.getLowCardinalityKeyValues(emptySynthesisContext);

		assertThat(transcriptionKeyValues).isEmpty();
		assertThat(synthesisKeyValues).isEmpty();
	}

	@Test
	void testStreamingContext() {
		// Test streaming context - transcription
		transcriptionContext.setStreaming(true);
		transcriptionContext.setChunkCount(5);
		transcriptionContext.setTotalChunks(10);

		KeyValues transcriptionKeyValues = convention.getHighCardinalityKeyValues(transcriptionContext);
		assertThat(transcriptionKeyValues).hasSize(2)
			.contains(KeyValue.of("chunk_count", "5"))
			.contains(KeyValue.of("total_chunks", "10"));

		// Test streaming context - synthesis
		synthesisContext.setStreaming(true);
		synthesisContext.setChunkCount(3);
		synthesisContext.setTotalChunks(6);

		KeyValues synthesisKeyValues = convention.getHighCardinalityKeyValues(synthesisContext);
		assertThat(synthesisKeyValues).hasSize(2)
			.contains(KeyValue.of("chunk_count", "3"))
			.contains(KeyValue.of("total_chunks", "6"));
	}

}

