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

package com.alibaba.cloud.ai.dashscope.metadata.audio;

import java.util.HashMap;

import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.google.gson.JsonObject;

import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.model.MutableResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

public class DashScopeAudioTranscriptionResponseMetadata extends MutableResponseMetadata {

	/**
	 * NULL objects.
	 */
	public static final DashScopeAudioTranscriptionResponseMetadata NULL = new DashScopeAudioTranscriptionResponseMetadata() {
	};

	protected static final String AI_METADATA_STRING = "{ @type: %1$s, rateLimit: %4$s }";

	@Nullable
	private RateLimit rateLimit;

	private JsonObject usage;

	protected DashScopeAudioTranscriptionResponseMetadata() {

		this(null, new JsonObject());
	}

	protected DashScopeAudioTranscriptionResponseMetadata(JsonObject usage) {

		this(null, usage);
	}

	protected DashScopeAudioTranscriptionResponseMetadata(@Nullable RateLimit rateLimit, JsonObject usage) {

		this.rateLimit = rateLimit;
		this.usage = usage;
	}

	public static DashScopeAudioTranscriptionResponseMetadata from(TranscriptionResult result) {

		Assert.notNull(result, "DashScope Transcription must not be null");
		return new DashScopeAudioTranscriptionResponseMetadata(result.getUsage());
	}

	@Nullable
	public RateLimit getRateLimit() {

		return this.rateLimit != null ? this.rateLimit : new EmptyRateLimit();
	}

	public void setRateLimit(@Nullable RateLimit rateLimit) {
		this.rateLimit = rateLimit;
	}

	public JsonObject getUsage() {
		return usage;
	}

	public void setUsage(JsonObject usage) {
		this.usage = usage;
	}

	@Override
	public String toString() {

		return AI_METADATA_STRING.formatted(getClass().getName(), getRateLimit());
	}

}
