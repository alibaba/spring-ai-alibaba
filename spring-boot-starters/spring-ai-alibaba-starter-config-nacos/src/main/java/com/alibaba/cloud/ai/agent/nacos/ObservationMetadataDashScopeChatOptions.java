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
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;

final class ObservationMetadataDashScopeChatOptions extends DashScopeChatOptions
		implements ObservationMetadataAwareOptions {

	private Map<String, String> observationMetadata;

	private ObservationMetadataDashScopeChatOptions(DashScopeChatOptions options,
			Map<String, String> observationMetadata) {
		this(options, copyObservationMetadata(observationMetadata), true);
	}

	private ObservationMetadataDashScopeChatOptions(DashScopeChatOptions options, Map<String, String> observationMetadata,
			boolean sharedObservationMetadata) {
		super(options.getModel(), options.getStream(), options.getTemperature(), options.getTopP(), options.getTopK(),
				options.getEnableThinking(), options.getPreserveThinking(), options.getThinkingBudget(),
				options.getReasoningEffort(), options.getToolStream(), options.getEnableCodeInterpreter(),
				options.getRepetitionPenalty(), options.getPresencePenalty(), options.getVlHighResolutionImages(),
				options.getVlEnableImageHwOutput(), options.getMaxCompletionTokens(), options.getSeed(),
				options.getIncrementalOutput(), options.getResponseFormat(), options.getResultFormat(),
				options.getLogprobs(), options.getTopLogprobs(), options.getN(), options.getStop(),
				options.getTools(), options.getToolChoice(), options.getParallelToolCalls(), options.getEnableSearch(),
				options.getSearchOptions(), options.getDataInspection(), options.getSkill(), options.getExtraBody(),
				options.getHttpHeaders(), options.getToolCallbacks(), options.getMultiModel(),
				options.getToolContext());
		this.observationMetadata = sharedObservationMetadata ? useObservationMetadata(observationMetadata)
				: copyObservationMetadata(observationMetadata);
	}

	static ObservationMetadataDashScopeChatOptions from(DashScopeChatOptions options,
			Map<String, String> observationMetadata) {
		return new ObservationMetadataDashScopeChatOptions(options, observationMetadata);
	}

	@Override
	public Map<String, String> getObservationMetadata() {
		return this.observationMetadata;
	}

	@Override
	public void setObservationMetadata(Map<String, String> observationMetadata) {
		this.observationMetadata = copyObservationMetadata(observationMetadata);
	}

	@Override
	public Builder mutate() {
		return new Builder(this, this.observationMetadata);
	}

	private static Map<String, String> copyObservationMetadata(Map<String, String> source) {
		return source != null ? new HashMap<>(source) : new HashMap<>();
	}

	private static Map<String, String> useObservationMetadata(Map<String, String> source) {
		return source != null ? source : new HashMap<>();
	}

	static final class Builder extends DashScopeChatOptions.Builder {

		private Map<String, String> observationMetadata;

		private Builder(DashScopeChatOptions options, Map<String, String> observationMetadata) {
			copyFrom(options);
			this.observationMetadata = useObservationMetadata(observationMetadata);
		}

		@Override
		public Builder clone() {
			return new Builder(build(), this.observationMetadata);
		}

		@Override
		public ObservationMetadataDashScopeChatOptions build() {
			return new ObservationMetadataDashScopeChatOptions(super.build(), this.observationMetadata, true);
		}

		private void copyFrom(DashScopeChatOptions options) {
			model(options.getModel());
			stream(options.getStream());
			temperature(options.getTemperature());
			topP(options.getTopP());
			topK(options.getTopK());
			enableThinking(options.getEnableThinking());
			preserveThinking(options.getPreserveThinking());
			thinkingBudget(options.getThinkingBudget());
			reasoningEffort(options.getReasoningEffort());
			toolStream(options.getToolStream());
			enableCodeInterpreter(options.getEnableCodeInterpreter());
			repetitionPenalty(options.getRepetitionPenalty());
			presencePenalty(options.getPresencePenalty());
			vlHighResolutionImages(options.getVlHighResolutionImages());
			vlEnableImageHwOutput(options.getVlEnableImageHwOutput());
			maxCompletionTokens(options.getMaxCompletionTokens());
			seed(options.getSeed());
			incrementalOutput(options.getIncrementalOutput());
			responseFormat(options.getResponseFormat());
			resultFormat(options.getResultFormat());
			logprobs(options.getLogprobs());
			topLogprobs(options.getTopLogprobs());
			n(options.getN());
			stop(options.getStop());
			tools(options.getTools());
			toolChoice(options.getToolChoice());
			parallelToolCalls(options.getParallelToolCalls());
			enableSearch(options.getEnableSearch());
			searchOptions(options.getSearchOptions());
			dataInspection(options.getDataInspection());
			skill(options.getSkill());
			extraBody(options.getExtraBody());
			httpHeaders(options.getHttpHeaders());
			toolCallbacks(options.getToolCallbacks());
			multiModel(options.getMultiModel());
			toolContext(options.getToolContext());
		}

	}

}
