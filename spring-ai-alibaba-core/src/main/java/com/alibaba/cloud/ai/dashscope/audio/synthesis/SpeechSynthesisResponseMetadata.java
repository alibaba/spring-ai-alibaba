package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.chat.metadata.EmptyRateLimit;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.model.MutableResponseMetadata;
import org.springframework.lang.Nullable;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisResponseMetadata extends MutableResponseMetadata {

	protected static final String AI_METADATA_STRING = "{ @type: %1$s, requestsLimit: %2$s }";

	public static final SpeechSynthesisResponseMetadata NULL = new SpeechSynthesisResponseMetadata() {
	};

	@Nullable
	private RateLimit rateLimit;

	public SpeechSynthesisResponseMetadata() {
		this(null);
	}

	public SpeechSynthesisResponseMetadata(@Nullable RateLimit rateLimit) {
		this.rateLimit = rateLimit;
	}

	@Nullable
	public RateLimit getRateLimit() {
		RateLimit rateLimit = this.rateLimit;
		return rateLimit != null ? rateLimit : new EmptyRateLimit();
	}

	public SpeechSynthesisResponseMetadata withRateLimit(RateLimit rateLimit) {
		this.rateLimit = rateLimit;
		return this;
	}

	@Override
	public String toString() {
		return AI_METADATA_STRING.formatted(getClass().getName(), getRateLimit());
	}

}
