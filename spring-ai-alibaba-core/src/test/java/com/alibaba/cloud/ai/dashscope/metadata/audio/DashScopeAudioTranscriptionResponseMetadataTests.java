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

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.RateLimit;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test cases for DashScopeAudioTranscriptionResponseMetadata. Tests cover constructor,
 * factory methods, rate limit handling, and toString representation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeAudioTranscriptionResponseMetadataTests {

	@Test
	void testDefaultConstructor() {
		// Test default constructor
		DashScopeAudioTranscriptionResponseMetadata metadata = new DashScopeAudioTranscriptionResponseMetadata();

		// Verify default rate limit is EmptyRateLimit
		assertThat(metadata.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

	@Test
	void testConstructorWithRateLimit() {
		// Test constructor with rate limit
		RateLimit rateLimit = mock(RateLimit.class);
		DashScopeAudioTranscriptionResponseMetadata metadata = new DashScopeAudioTranscriptionResponseMetadata(
				rateLimit);

		// Verify rate limit is set correctly
		assertThat(metadata.getRateLimit()).isEqualTo(rateLimit);
	}

	@Test
	void testFromString() {
		// Test factory method with String
		String result = "test result";
		DashScopeAudioTranscriptionResponseMetadata metadata = DashScopeAudioTranscriptionResponseMetadata.from(result);

		// Verify metadata is created successfully
		assertThat(metadata).isNotNull();
		assertThat(metadata.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

	@Test
	void testFromWithNullResult() {
		// Test factory method with null String
		assertThatThrownBy(() -> DashScopeAudioTranscriptionResponseMetadata.from(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("OpenAI Transcription must not be null");
	}

	@Test
	void testWithRateLimit() {
		// Test withRateLimit method
		RateLimit rateLimit = mock(RateLimit.class);
		DashScopeAudioTranscriptionResponseMetadata metadata = new DashScopeAudioTranscriptionResponseMetadata();

		// Set rate limit using withRateLimit
		DashScopeAudioTranscriptionResponseMetadata updatedMetadata = metadata.withRateLimit(rateLimit);

		// Verify rate limit is updated and method returns same instance
		assertThat(updatedMetadata).isSameAs(metadata);
		assertThat(updatedMetadata.getRateLimit()).isEqualTo(rateLimit);
	}

	@Test
	void testToString() {
		// Test toString method
		RateLimit rateLimit = mock(RateLimit.class);
		DashScopeAudioTranscriptionResponseMetadata metadata = new DashScopeAudioTranscriptionResponseMetadata(
				rateLimit);

		String toString = metadata.toString();

		// Verify toString contains essential information based on AI_METADATA_STRING
		// format
		assertThat(toString).contains("{ @type: " + DashScopeAudioTranscriptionResponseMetadata.class.getName())
			.contains("rateLimit: " + rateLimit.toString())
			.contains(" }");
	}

	@Test
	void testNullConstant() {
		// Test NULL constant
		assertThat(DashScopeAudioTranscriptionResponseMetadata.NULL).isNotNull();
		assertThat(DashScopeAudioTranscriptionResponseMetadata.NULL.getRateLimit()).isInstanceOf(EmptyRateLimit.class);
	}

}
