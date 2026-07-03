/*
 * Copyright 2026-2027 the original author or authors.
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
package com.alibaba.cloud.ai.agent.nacos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;

import org.springframework.ai.chat.prompt.ChatOptions;

final class ObservationMetadataChatOptions implements ChatOptions, ObservationMetadataAwareOptions {

	private final ChatOptions delegate;

	private Map<String, String> observationMetadata;

	private ObservationMetadataChatOptions(ChatOptions delegate, Map<String, String> observationMetadata) {
		this(delegate, copyObservationMetadata(observationMetadata), true);
	}

	private ObservationMetadataChatOptions(ChatOptions delegate, Map<String, String> observationMetadata,
			boolean sharedObservationMetadata) {
		this.delegate = delegate;
		this.observationMetadata = sharedObservationMetadata ? useObservationMetadata(observationMetadata)
				: copyObservationMetadata(observationMetadata);
	}

	static ObservationMetadataChatOptions from(ChatOptions delegate, Map<String, String> observationMetadata) {
		return new ObservationMetadataChatOptions(delegate, observationMetadata);
	}

	static ObservationMetadataChatOptions metadataOnly(Map<String, String> observationMetadata) {
		return from(ChatOptions.builder().build(), observationMetadata);
	}

	@Override
	public String getModel() {
		return this.delegate.getModel();
	}

	@Override
	public Double getFrequencyPenalty() {
		return this.delegate.getFrequencyPenalty();
	}

	@Override
	public Integer getMaxTokens() {
		return this.delegate.getMaxTokens();
	}

	@Override
	public Double getPresencePenalty() {
		return this.delegate.getPresencePenalty();
	}

	@Override
	public List<String> getStopSequences() {
		return this.delegate.getStopSequences();
	}

	@Override
	public Double getTemperature() {
		return this.delegate.getTemperature();
	}

	@Override
	public Integer getTopK() {
		return this.delegate.getTopK();
	}

	@Override
	public Double getTopP() {
		return this.delegate.getTopP();
	}

	@Override
	public Builder mutate() {
		return new Builder(this.delegate.mutate(), this.observationMetadata);
	}

	@Override
	public Map<String, String> getObservationMetadata() {
		return this.observationMetadata;
	}

	@Override
	public void setObservationMetadata(Map<String, String> observationMetadata) {
		this.observationMetadata = copyObservationMetadata(observationMetadata);
	}

	private static Map<String, String> copyObservationMetadata(Map<String, String> source) {
		return source != null ? new HashMap<>(source) : new HashMap<>();
	}

	private static Map<String, String> useObservationMetadata(Map<String, String> source) {
		return source != null ? source : new HashMap<>();
	}

	static final class Builder implements ChatOptions.Builder<Builder> {

		private final ChatOptions.Builder<?> delegateBuilder;

		private Map<String, String> observationMetadata;

		private Builder(ChatOptions.Builder<?> delegateBuilder, Map<String, String> observationMetadata) {
			this.delegateBuilder = delegateBuilder;
			this.observationMetadata = useObservationMetadata(observationMetadata);
		}

		@Override
		public Builder clone() {
			return new Builder(this.delegateBuilder.clone(), this.observationMetadata);
		}

		@Override
		public Builder model(String model) {
			this.delegateBuilder.model(model);
			return this;
		}

		@Override
		public Builder frequencyPenalty(Double frequencyPenalty) {
			this.delegateBuilder.frequencyPenalty(frequencyPenalty);
			return this;
		}

		@Override
		public Builder maxTokens(Integer maxTokens) {
			this.delegateBuilder.maxTokens(maxTokens);
			return this;
		}

		@Override
		public Builder presencePenalty(Double presencePenalty) {
			this.delegateBuilder.presencePenalty(presencePenalty);
			return this;
		}

		@Override
		public Builder stopSequences(List<String> stopSequences) {
			this.delegateBuilder.stopSequences(stopSequences);
			return this;
		}

		@Override
		public Builder temperature(Double temperature) {
			this.delegateBuilder.temperature(temperature);
			return this;
		}

		@Override
		public Builder topK(Integer topK) {
			this.delegateBuilder.topK(topK);
			return this;
		}

		@Override
		public Builder topP(Double topP) {
			this.delegateBuilder.topP(topP);
			return this;
		}

		@Override
		public ChatOptions build() {
			return new ObservationMetadataChatOptions(this.delegateBuilder.build(), this.observationMetadata, true);
		}

		@Override
		public Builder combineWith(ChatOptions.Builder<?> other) {
			ChatOptions.Builder<?> builder = other;
			if (other instanceof Builder observationMetadataBuilder) {
				builder = observationMetadataBuilder.delegateBuilder;
				this.observationMetadata = observationMetadataBuilder.observationMetadata;
			}
			this.delegateBuilder.combineWith(builder);
			return this;
		}

	}

}
