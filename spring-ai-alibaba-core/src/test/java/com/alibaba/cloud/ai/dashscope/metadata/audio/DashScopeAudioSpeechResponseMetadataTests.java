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
package com.alibaba.cloud.ai.dashscope.metadata.audio;

import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.RateLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test cases for DashScopeAudioSpeechResponseMetadata. Tests cover constructor, factory
 * methods, rate limit handling, and toString representation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeAudioSpeechResponseMetadataTests {

	@Test
	void testDefaultConstructor() {
		// Test default constructor
		DashScopeAudioSpeechResponseMetadata metadata = new DashScopeAudioSpeechResponseMetadata();

		// Verify default rate limit is EmptyRateLimit
		assertThat(metadata.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

	@Test
	void testConstructorWithRateLimit() {
		// Test constructor with rate limit
		RateLimit rateLimit = mock(RateLimit.class);
		DashScopeAudioSpeechResponseMetadata metadata = new DashScopeAudioSpeechResponseMetadata(rateLimit);

		// Verify rate limit is set correctly
		assertThat(metadata.getRateLimit()).isEqualTo(rateLimit);
	}

	@Test
	void testFromSpeechSynthesisResult() {
		// Test factory method with SpeechSynthesisResult
		SpeechSynthesisResult result = mock(SpeechSynthesisResult.class);
		DashScopeAudioSpeechResponseMetadata metadata = DashScopeAudioSpeechResponseMetadata.from(result);

		// Verify metadata is created successfully
		assertThat(metadata).isNotNull();
		assertThat(metadata.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

	@Test
	void testFromString() {
		// Test factory method with String
		String result = "test result";
		DashScopeAudioSpeechResponseMetadata metadata = DashScopeAudioSpeechResponseMetadata.from(result);

		// Verify metadata is created successfully
		assertThat(metadata).isNotNull();
		assertThat(metadata.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

	@Test
	void testFromWithNullResult() {
		// Test factory method with null SpeechSynthesisResult
		assertThatThrownBy(() -> DashScopeAudioSpeechResponseMetadata.from((SpeechSynthesisResult) null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("DashScope speech must not be null");

		// Test factory method with null String
		assertThatThrownBy(() -> DashScopeAudioSpeechResponseMetadata.from((String) null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("DashScope speech must not be null");
	}

	@Test
	void testWithRateLimit() {
		// Test withRateLimit method
		RateLimit rateLimit = mock(RateLimit.class);
		DashScopeAudioSpeechResponseMetadata metadata = new DashScopeAudioSpeechResponseMetadata();

		// Set rate limit using withRateLimit
		DashScopeAudioSpeechResponseMetadata updatedMetadata = metadata.withRateLimit(rateLimit);

		// Verify rate limit is updated and method returns same instance
		assertThat(updatedMetadata).isSameAs(metadata);
		assertThat(updatedMetadata.getRateLimit()).isEqualTo(rateLimit);
	}

	@Test
	void testToString() {
		// Test toString method
		DashScopeAudioSpeechResponseMetadata metadata = new DashScopeAudioSpeechResponseMetadata();
		String toString = metadata.toString();

		// Verify toString contains essential information
		assertThat(toString).contains(DashScopeAudioSpeechResponseMetadata.class.getName()).contains("requestsLimit");
	}

	@Test
	void testNullConstant() {
		// Test NULL constant
		assertThat(DashScopeAudioSpeechResponseMetadata.NULL).isNotNull();
		assertThat(DashScopeAudioSpeechResponseMetadata.NULL.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

}
