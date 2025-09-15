/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.domain.audio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audio response format enum. Defines supported audio formats for responses.
 *
 * @since 1.0.0.3
 */
public enum AudioResponseFormat {

	/**
	 * MP3 audio format
	 */
	@JsonProperty("mp3")
	MP3,
	/**
	 * FLAC audio format
	 */
	@JsonProperty("flac")
	FLAC,
	/**
	 * OPUS audio format
	 */
	@JsonProperty("opus")
	OPUS,
	/**
	 * PCM16 audio format
	 */
	@JsonProperty("pcm16")
	PCM16,
	/**
	 * WAV audio format
	 */
	@JsonProperty("wav")
	WAV

}
