/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.audio;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioApi.SpeechRequest.AudioResponseFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.model.ModelOptions;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeAudioSpeechOptions implements ModelOptions {

	/**
	 * Audio Speech models.
	 */
	@JsonProperty("model")
	private String model;

	/**
	 * Text content.
	 */
	@JsonProperty("text")
	private String text;

	/**
	 * synthesis audio sample rate.
	 */
	@JsonProperty("sample_rate")
	private Integer sampleRate = 16000;

	/**
	 * synthesis audio volume.
	 */
	@JsonProperty("volume")
	private Integer volume = 50;

	/**
	 * synthesis audio speed.
	 */
	@JsonProperty("speed")
	private Float speed = 1.0f;

	/**
	 * synthesis audio pitch.
	 */
	@JsonProperty("pitch")
	private Float pitch = 1.0f;

	/**
	 * enable word level timestamp.
	 */
	@JsonProperty("enable_word_timestamp")
	private Boolean enableWordTimestamp;

	/**
	 * enable phoneme level timestamp.
	 */
	@JsonProperty("enable_phoneme_timestamp")
	private Boolean enablePhonemeTimestamp;

	/**
	 * The format of the audio output. Supported formats are mp3, wav, and pcm. Defaults
	 * to mp3.
	 */
	@JsonProperty("response_format")
	private AudioResponseFormat responseFormat;

	public static Builder builder() {
		return new Builder();
	}

	public String getModel() {

		return model;
	}

	public void setModel(String model) {

		this.model = model;
	}

	public String getText() {

		return text;
	}

	public void setText(String text) {

		this.text = text;
	}

	public Integer getSampleRate() {

		return sampleRate;
	}

	public void setSampleRate(Integer sampleRate) {

		this.sampleRate = sampleRate;
	}

	public Integer getVolume() {

		return volume;
	}

	public void setVolume(Integer volume) {

		this.volume = volume;
	}

	public Float getSpeed() {

		return speed;
	}

	public void setSpeed(Float speed) {

		this.speed = speed;
	}

	public Float getPitch() {

		return pitch;
	}

	public void setPitch(Float pitch) {

		this.pitch = pitch;
	}

	Boolean getEnableWordTimestamp() {
		return enableWordTimestamp;
	}

	public Boolean isEnableWordTimestamp() {
		return enableWordTimestamp != null && enableWordTimestamp;
	}

	public void setEnableWordTimestamp(Boolean enableWordTimestamp) {
		this.enableWordTimestamp = enableWordTimestamp;
	}

	Boolean getEnablePhonemeTimestamp() {
		return enablePhonemeTimestamp;
	}

	public Boolean isEnablePhonemeTimestamp() {
		return enablePhonemeTimestamp != null && enablePhonemeTimestamp;
	}

	public void setEnablePhonemeTimestamp(Boolean enablePhonemeTimestamp) {
		this.enablePhonemeTimestamp = enablePhonemeTimestamp;
	}

	AudioResponseFormat getResponseFormat() {
		return responseFormat;
	}

	void setResponseFormat(AudioResponseFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	/**
	 * Build a options instances.
	 */
	public static class Builder {

		private final DashScopeAudioSpeechOptions options = new DashScopeAudioSpeechOptions();

		public Builder withModel(String model) {

			options.model = model;
			return this;
		}

		public Builder withText(String text) {

			options.text = text;
			return this;
		}

		public Builder withSampleRate(Integer sampleRate) {

			options.sampleRate = sampleRate;
			return this;
		}

		public Builder withVolume(Integer volume) {

			options.volume = volume;
			return this;
		}

		// public Builder withRate(Float rate) {
		//
		// options.rate = rate;
		// return this;
		// }

		public Builder withSpeed(Float speed) {
			options.speed = speed;
			return this;
		}

		public Builder withResponseFormat(AudioResponseFormat format) {
			options.responseFormat = format;
			return this;
		}

		public Builder withPitch(Float pitch) {
			options.pitch = pitch;
			return this;
		}

		public Builder withEnableWordTimestamp(Boolean enableWordTimestamp) {

			options.enableWordTimestamp = enableWordTimestamp;
			return this;
		}

		public Builder withEnablePhonemeTimestamp(Boolean enablePhonemeTimestamp) {

			options.enablePhonemeTimestamp = enablePhonemeTimestamp;
			return this;
		}

		public DashScopeAudioSpeechOptions build() {

			return options;
		}

	}

}
