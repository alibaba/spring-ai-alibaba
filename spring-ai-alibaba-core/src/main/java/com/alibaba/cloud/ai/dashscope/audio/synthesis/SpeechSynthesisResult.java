package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ModelResult;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResultMetadata;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisResult implements ModelResult<SpeechSynthesisOutput> {

	private final SpeechSynthesisOutput output;

	private final SpeechSynthesisResultMetadata metadata;

	public SpeechSynthesisResult(SpeechSynthesisOutput output) {
		this.output = output;
		this.metadata = SpeechSynthesisResultMetadata.NULL;
	}

	public SpeechSynthesisResult(SpeechSynthesisOutput output, SpeechSynthesisResultMetadata metadata) {
		this.output = output;
		this.metadata = metadata;
	}

	@Override
	public SpeechSynthesisOutput getOutput() {
		return this.output;
	}

	@Override
	public SpeechSynthesisResultMetadata getMetadata() {
		return this.metadata;
	}

}
