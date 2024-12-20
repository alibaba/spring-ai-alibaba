package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ResultMetadata;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisResultMetadata implements ResultMetadata {

	public static final SpeechSynthesisResultMetadata NULL = SpeechSynthesisResultMetadata.create();

	/**
	 * Factory method used to construct a new {@link SpeechSynthesisResultMetadata}
	 * @return a new {@link SpeechSynthesisResultMetadata}
	 */
	static SpeechSynthesisResultMetadata create() {
		return new SpeechSynthesisResultMetadata() {
		};
	}

}
