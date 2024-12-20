package com.alibaba.cloud.ai.dashscope.metadata.audio;

import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.google.gson.JsonObject;

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
