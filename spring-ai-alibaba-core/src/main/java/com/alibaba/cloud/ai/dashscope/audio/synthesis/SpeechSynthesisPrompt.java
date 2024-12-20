package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ModelRequest;

import java.util.Collections;
import java.util.List;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisPrompt implements ModelRequest<List<SpeechSynthesisMessage>> {

	private final List<SpeechSynthesisMessage> messages;

	private final SpeechSynthesisOptions options;

	public SpeechSynthesisPrompt(String contents) {
		this(new SpeechSynthesisMessage(contents));
	}

	public SpeechSynthesisPrompt(SpeechSynthesisMessage message) {
		this(Collections.singletonList(message));
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> messages) {
		this(messages, null);
	}

	public SpeechSynthesisPrompt(String contents, SpeechSynthesisOptions options) {
		this(new SpeechSynthesisMessage(contents), options);
	}

	public SpeechSynthesisPrompt(SpeechSynthesisMessage message, SpeechSynthesisOptions options) {
		this(Collections.singletonList(message), options);
	}

	public SpeechSynthesisPrompt(List<SpeechSynthesisMessage> messages, SpeechSynthesisOptions options) {
		this.messages = messages;
		this.options = options;
	}

	@Override
	public SpeechSynthesisOptions getOptions() {
		return this.options;
	}

	@Override
	public List<SpeechSynthesisMessage> getInstructions() {
		return this.messages;
	}

}
