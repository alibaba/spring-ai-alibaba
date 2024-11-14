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

package com.alibaba.cloud.ai.dashscope.audio;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author kevinlin09
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeSpeechSynthesisOptions implements SpeechSynthesisOptions {

	// @formatter:off
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
	 * Voice, only for tts v2.
	 */
	@JsonProperty("voice")
	private String voice = null;

	/**
	 * Input Text type.
	 */
	@JsonProperty("request_text_type")
	private DashScopeSpeechSynthesisApi.RequestTextType requestTextType = DashScopeSpeechSynthesisApi.RequestTextType.PLAIN_TEXT;

    /**
     * synthesis audio sample rate.
     */
    @JsonProperty("sample_rate")
    private Integer sampleRate = 48000;

    /**
     * synthesis audio volume.
     */
    @JsonProperty("volume")
    private Integer volume = 50;

    /**
     * synthesis audio speed.
     */
    @JsonProperty("speed")
    private Double speed = 1.0;

    /**
     * synthesis audio pitch.
     */
    @JsonProperty("pitch")
    private Double pitch = 1.0;

    /**
     * enable word level timestamp.
     */
    @JsonProperty("enable_word_timestamp")
    private Boolean enableWordTimestamp = false;

    /**
     * enable phoneme level timestamp.
     */
    @JsonProperty("enable_phoneme_timestamp")
    private Boolean enablePhonemeTimestamp = false;

    /**
     * The format of the audio output. Supported formats are mp3, wav, and pcm. Defaults
     * to mp3.
     */
    @JsonProperty("response_format")
    private DashScopeSpeechSynthesisApi.ResponseFormat responseFormat = DashScopeSpeechSynthesisApi.ResponseFormat.MP3;

    // @formatter:on
	public static DashScopeSpeechSynthesisOptions.Builder builder() {
		return new DashScopeSpeechSynthesisOptions.Builder();
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

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public Double getPitch() {
		return pitch;
	}

	public void setPitch(Double pitch) {
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

	DashScopeSpeechSynthesisApi.ResponseFormat getResponseFormat() {
		return responseFormat;
	}

	void setResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	public DashScopeSpeechSynthesisApi.RequestTextType getRequestTextType() {
		return requestTextType;
	}

	public String getVoice() {
		return voice;
	}

	public void setVoice(String voice) {
		this.voice = voice;
	}

	/**
	 * Build an options instances.
	 */
	public static class Builder {

		private final DashScopeSpeechSynthesisOptions options = new DashScopeSpeechSynthesisOptions();

		public DashScopeSpeechSynthesisOptions.Builder withModel(String model) {
			options.model = model;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withText(String text) {
			options.text = text;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withVoice(String voice) {
			options.voice = voice;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withRequestText(
				DashScopeSpeechSynthesisApi.RequestTextType requestTextType) {
			options.requestTextType = requestTextType;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withSampleRate(Integer sampleRate) {
			options.sampleRate = sampleRate;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withVolume(Integer volume) {
			options.volume = volume;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withSpeed(Double speed) {
			options.speed = speed;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withResponseFormat(
				DashScopeSpeechSynthesisApi.ResponseFormat format) {
			options.responseFormat = format;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withPitch(Double pitch) {
			options.pitch = pitch;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withEnableWordTimestamp(Boolean enableWordTimestamp) {
			options.enableWordTimestamp = enableWordTimestamp;
			return this;
		}

		public DashScopeSpeechSynthesisOptions.Builder withEnablePhonemeTimestamp(Boolean enablePhonemeTimestamp) {
			options.enablePhonemeTimestamp = enablePhonemeTimestamp;
			return this;
		}

		public DashScopeSpeechSynthesisOptions build() {
			return options;
		}

	}

}
