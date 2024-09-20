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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author xYLiu
 * @author yuluo
 * @since 2023.0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeAudioTranscriptionOptions implements AudioTranscriptionOptions {

	@JsonProperty("model")
	private String model;

	@NestedConfigurationProperty
	@JsonProperty("file_urls")
	private List<String> fileUrls = new ArrayList<>();

	@JsonProperty("phrase_id")
	private String phraseId = null;

	@NestedConfigurationProperty
	@JsonProperty("channel_id")
	private List<Integer> channelId = Collections.singletonList(0);

	@JsonProperty("diarization_enabled")
	private Boolean diarizationEnabled = false;

	@JsonProperty("speaker_count")
	private Integer speakerCount = null;

	@JsonProperty("disfluency_removal_enabled")
	private Boolean disfluencyRemovalEnabled = false;

	@JsonProperty("timestamp_alignment_enabled")
	private Boolean timestampAlignmentEnabled = false;

	@JsonProperty("special_word_filter")
	private String specialWordFilter = "";

	@JsonProperty("audio_event_detection_enabled")
	private Boolean audioEventDetectionEnabled = false;

	public static Builder builder() {

		return new Builder();
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<String> getFileUrls() {
		return fileUrls;
	}

	public void setFileUrls(List<String> fileUrls) {
		this.fileUrls = fileUrls;
	}

	public String getPhraseId() {
		return phraseId;
	}

	public void setPhraseId(String phraseId) {
		this.phraseId = phraseId;
	}

	public List<Integer> getChannelId() {
		return channelId;
	}

	public void setChannelId(List<Integer> channelId) {
		this.channelId = channelId;
	}

	public Boolean getDiarizationEnabled() {
		return diarizationEnabled;
	}

	public void setDiarizationEnabled(Boolean diarizationEnabled) {
		this.diarizationEnabled = diarizationEnabled;
	}

	public Integer getSpeakerCount() {
		return speakerCount;
	}

	public void setSpeakerCount(Integer speakerCount) {
		this.speakerCount = speakerCount;
	}

	public Boolean getDisfluencyRemovalEnabled() {
		return disfluencyRemovalEnabled;
	}

	public void setDisfluencyRemovalEnabled(Boolean disfluencyRemovalEnabled) {
		this.disfluencyRemovalEnabled = disfluencyRemovalEnabled;
	}

	public Boolean getTimestampAlignmentEnabled() {
		return timestampAlignmentEnabled;
	}

	public void setTimestampAlignmentEnabled(Boolean timestampAlignmentEnabled) {
		this.timestampAlignmentEnabled = timestampAlignmentEnabled;
	}

	public String getSpecialWordFilter() {
		return specialWordFilter;
	}

	public void setSpecialWordFilter(String specialWordFilter) {
		this.specialWordFilter = specialWordFilter;
	}

	public Boolean getAudioEventDetectionEnabled() {
		return audioEventDetectionEnabled;
	}

	public void setAudioEventDetectionEnabled(Boolean audioEventDetectionEnabled) {
		this.audioEventDetectionEnabled = audioEventDetectionEnabled;
	}

	/**
	 * Builder class for constructing DashScopeAudioTranscriptionOptions instances.
	 */
	public static class Builder {

		private final DashScopeAudioTranscriptionOptions options = new DashScopeAudioTranscriptionOptions();

		public Builder withModel(String model) {
			options.model = model;
			return this;
		}

		public Builder withFileUrls(List<String> fileUrls) {
			options.fileUrls = fileUrls;
			return this;
		}

		public Builder withPhraseId(String phraseId) {
			options.phraseId = phraseId;
			return this;
		}

		public Builder withChannelId(List<Integer> channelId) {
			options.channelId = channelId;
			return this;
		}

		public Builder withDiarizationEnabled(Boolean diarizationEnabled) {
			options.diarizationEnabled = diarizationEnabled;
			return this;
		}

		public Builder withSpeakerCount(Integer speakerCount) {
			options.speakerCount = speakerCount;
			return this;
		}

		public Builder withDisfluencyRemovalEnabled(Boolean disfluencyRemovalEnabled) {
			options.disfluencyRemovalEnabled = disfluencyRemovalEnabled;
			return this;
		}

		public Builder withTimestampAlignmentEnabled(Boolean timestampAlignmentEnabled) {
			options.timestampAlignmentEnabled = timestampAlignmentEnabled;
			return this;
		}

		public Builder withSpecialWordFilter(String specialWordFilter) {
			options.specialWordFilter = specialWordFilter;
			return this;
		}

		public Builder withAudioEventDetectionEnabled(Boolean audioEventDetectionEnabled) {
			options.audioEventDetectionEnabled = audioEventDetectionEnabled;
			return this;
		}

		public DashScopeAudioTranscriptionOptions build() {
			// Perform any necessary validation here before returning the built object
			return options;
		}

	}

}
