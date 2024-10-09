package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.Model;

import reactor.core.publisher.Flux;

/**
 * @author kevinlin09
 */
public interface SpeechSynthesisModel extends Model<SpeechSynthesisPrompt, SpeechSynthesisResponse> {

	@Override
	SpeechSynthesisResponse call(SpeechSynthesisPrompt prompt);

	Flux<SpeechSynthesisResponse> stream(SpeechSynthesisPrompt prompt);

}