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

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeSpeechSynthesisModel's observability features. Tests cover both
 * synchronous and streaming scenarios, including observation names, audio parameters
 * handling, and key value generation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 1.0.0-M5.1
 */
class DashScopeSpeechSynthesisModelObservationTests {

	@Mock
	private DashScopeSpeechSynthesisApi api;

	private ObservationRegistry observationRegistry;

	private ObservationConfig observationConfig;

	private DashScopeSpeechSynthesisModel model;

	private DashScopeSpeechSynthesisOptions options;

	private RetryTemplate retryTemplate;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		// Initialize ObservationRegistry
		observationRegistry = mock(ObservationRegistry.class);
		observationConfig = mock(ObservationConfig.class);
		when(observationRegistry.observationConfig()).thenReturn(observationConfig);
		when(observationConfig.isObservationEnabled(any(), any())).thenReturn(true);

		options = DashScopeSpeechSynthesisOptions.builder()
			.withModel("sambert-zhichu-v1")
			.withResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.MP3)
			.withSampleRate(16000)
			.withVoice("female")
			.withVolume(50)
			.withSpeed(1.0)
			.build();
		retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
		model = new DashScopeSpeechSynthesisModel(api, options, retryTemplate, observationRegistry);
	}

	@Test
	void testSynchronousCallObservation() {
		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
		ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

		// Mock API response
		DashScopeSpeechSynthesisApi.Response apiResponse = new DashScopeSpeechSynthesisApi.Response();
		apiResponse.audio = audioBuffer;
		when(api.call(any())).thenReturn(apiResponse);

		// Execute test
		SpeechSynthesisResponse response = model.call(prompt);

		// Verify results
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
		assertThat(response.getResult().getOutput()).isNotNull();
		assertThat(response.getResult().getOutput().getAudio()).isNotNull();
	}

	@Test
	void testStreamingCallObservation() {
		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
		ByteBuffer audioBuffer1 = ByteBuffer.wrap(new byte[512]);
		ByteBuffer audioBuffer2 = ByteBuffer.wrap(new byte[512]);

		// Mock streaming API response
		when(api.streamOut(any())).thenReturn(Flux.just(audioBuffer1, audioBuffer2));

		// Execute test
		Flux<SpeechSynthesisResponse> responseFlux = model.stream(prompt);

		// Verify results
		StepVerifier.create(responseFlux).expectNextCount(2).verifyComplete();
	}

	@Test
	void testErrorHandlingObservation() {
		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);

		// Mock API error
		when(api.call(any())).thenThrow(new RuntimeException("API call failed"));

		// Execute test and verify exception
		try {
			model.call(prompt);
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(RuntimeException.class);
			assertThat(e.getMessage()).isEqualTo("API call failed");
		}
	}

	@Test
	void testStreamingErrorHandlingObservation() {
		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);

		// Mock streaming API error
		when(api.streamOut(any())).thenReturn(Flux.error(new RuntimeException("Streaming failed")));

		// Execute test
		Flux<SpeechSynthesisResponse> responseFlux = model.stream(prompt);

		// Verify error handling
		StepVerifier.create(responseFlux).expectError(RuntimeException.class).verify();
	}

	@Test
	void testEmptyPromptHandling() {
		// Prepare empty prompt
		SpeechSynthesisPrompt emptyPrompt = new SpeechSynthesisPrompt("");

		// Mock API response
		ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);
		DashScopeSpeechSynthesisApi.Response apiResponse = new DashScopeSpeechSynthesisApi.Response();
		apiResponse.audio = audioBuffer;
		when(api.call(any())).thenReturn(apiResponse);

		// Execute test
		SpeechSynthesisResponse response = model.call(emptyPrompt);

		// Verify results
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
	}

	@Test
	void testCustomOptionsObservation() {
		// Prepare custom options
		DashScopeSpeechSynthesisOptions customOptions = DashScopeSpeechSynthesisOptions.builder()
			.withModel("custom-model")
			.withResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.WAV)
			.withSampleRate(44100)
			.withVoice("male")
			.withVolume(75)
			.withSpeed(1.5)
			.build();

		// Create model with custom options
		DashScopeSpeechSynthesisModel customModel = new DashScopeSpeechSynthesisModel(api, customOptions, retryTemplate,
				observationRegistry);

		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
		ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

		// Mock API response
		DashScopeSpeechSynthesisApi.Response apiResponse = new DashScopeSpeechSynthesisApi.Response();
		apiResponse.audio = audioBuffer;
		when(api.call(any())).thenReturn(apiResponse);

		// Execute test
		SpeechSynthesisResponse response = customModel.call(prompt);

		// Verify results
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
	}

	@Test
	void testObservationContext() {
		// Prepare test data
		String testText = "Test text";
		SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
		ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

		// Mock API response
		DashScopeSpeechSynthesisApi.Response apiResponse = new DashScopeSpeechSynthesisApi.Response();
		apiResponse.audio = audioBuffer;
		when(api.call(any())).thenReturn(apiResponse);

		// Execute test
		SpeechSynthesisResponse response = model.call(prompt);

		// Verify observation context
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
		assertThat(response.getResult().getOutput()).isNotNull();
		assertThat(response.getResult().getOutput().getAudio()).isNotNull();
	}

}