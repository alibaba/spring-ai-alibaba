/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.audio.speech;

import java.nio.ByteBuffer;

import reactor.core.publisher.Flux;

import org.springframework.ai.model.StreamingModel;

/**
 * The {@link StreamingSpeechModel} interface provides a way to interact with the OpenAI
 * Text-to-Speech (TTS) API using a streaming approach, allowing you to receive the
 * generated audio in a real-time fashion.
 *
 */
@FunctionalInterface
public interface StreamingSpeechModel extends StreamingModel<SpeechPrompt, SpeechResponse> {

	/**
	 * Generates a stream of audio bytes from the provided text message.
	 * @param message the text message to be converted to audio
	 * @return a Flux of audio bytes representing the generated speech
	 */
	default Flux<byte[]> stream(String message) {
		SpeechPrompt prompt = new SpeechPrompt(message);
		return stream(prompt).map(SpeechResponse::getResult).map(Speech::getOutput);
	}

	/**
	 * Sends a speech request to the OpenAI TTS API and returns a stream of the resulting
	 * speech responses.
	 * @param prompt the speech prompt containing the input text and other parameters
	 * @return a Flux of speech responses, each containing a portion of the generated
	 * audio
	 */
	@Override
	Flux<SpeechResponse> stream(SpeechPrompt prompt);

}