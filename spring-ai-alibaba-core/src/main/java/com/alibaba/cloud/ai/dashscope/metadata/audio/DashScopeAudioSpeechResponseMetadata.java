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

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisUsage;
import com.alibaba.dashscope.audio.tts.timestamp.Sentence;

import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.model.MutableResponseMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

public class DashScopeAudioSpeechResponseMetadata extends MutableResponseMetadata {

	/**
	 * NULL objects.
	 */
	public static final DashScopeAudioSpeechResponseMetadata NULL = new DashScopeAudioSpeechResponseMetadata() {
	};

	protected static final String AI_METADATA_STRING = "{ @type: %1$s, requestsLimit: %2$s }";

	private SpeechSynthesisUsage usage;

	private String requestId;

	private Sentence time;

	@Nullable
	private RateLimit rateLimit;

	public DashScopeAudioSpeechResponseMetadata() {

		this(null);
	}

	public DashScopeAudioSpeechResponseMetadata(@Nullable RateLimit rateLimit) {

		this.rateLimit = rateLimit;
	}

	public static DashScopeAudioSpeechResponseMetadata from(SpeechSynthesisResult result) {

		Assert.notNull(result, "DashScope AI speech must not be null");
		DashScopeAudioSpeechResponseMetadata speechResponseMetadata = new DashScopeAudioSpeechResponseMetadata();

		return speechResponseMetadata;
	}

	public static DashScopeAudioSpeechResponseMetadata from(String result) {

		Assert.notNull(result, "DashScope AI speech must not be null");
		DashScopeAudioSpeechResponseMetadata speechResponseMetadata = new DashScopeAudioSpeechResponseMetadata();

		return speechResponseMetadata;
	}

	@Nullable
	public RateLimit getRateLimit() {

		RateLimit rateLimit = this.rateLimit;
		return rateLimit != null ? rateLimit : new EmptyRateLimit();
	}

	public DashScopeAudioSpeechResponseMetadata withRateLimit(RateLimit rateLimit) {

		this.rateLimit = rateLimit;
		return this;
	}

	public DashScopeAudioSpeechResponseMetadata withUsage(SpeechSynthesisUsage usage) {

		this.usage = usage;
		return this;
	}

	public DashScopeAudioSpeechResponseMetadata withRequestId(String id) {

		this.requestId = id;
		return this;
	}

	public DashScopeAudioSpeechResponseMetadata withSentence(Sentence sentence) {

		this.time = sentence;
		return this;
	}

	public SpeechSynthesisUsage getUsage() {
		return usage;
	}

	public String getRequestId() {
		return requestId;
	}

	public Sentence getTime() {
		return time;
	}

	@Override
	public String toString() {
		return AI_METADATA_STRING.formatted(getClass().getName(), getRateLimit());
	}

}
