/*
 * Copyright 2024 the original author or authors.
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
