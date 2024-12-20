package com.alibaba.cloud.ai.dashscope.audio.transcription;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;

import org.springframework.ai.model.Model;
import reactor.core.publisher.Flux;

/**
 * @author kevinlin09
 */
public interface AudioTranscriptionModel extends Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> {

	@Override
	AudioTranscriptionResponse call(AudioTranscriptionPrompt prompt);

	AudioTranscriptionResponse asyncCall(AudioTranscriptionPrompt prompt);

	AudioTranscriptionResponse fetch(String taskId);

	Flux<AudioTranscriptionResponse> stream(AudioTranscriptionPrompt prompt);

}
