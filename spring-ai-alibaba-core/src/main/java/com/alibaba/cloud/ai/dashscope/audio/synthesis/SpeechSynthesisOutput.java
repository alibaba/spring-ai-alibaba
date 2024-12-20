package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import java.nio.ByteBuffer;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisOutput {

	private final ByteBuffer audio;

	public SpeechSynthesisOutput(ByteBuffer audio) {
		this.audio = audio;
	}

	public ByteBuffer getAudio() {
		return audio;
	}

}
