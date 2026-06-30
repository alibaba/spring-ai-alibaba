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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;

import org.springframework.ai.openai.OpenAiChatOptions;

final class ObservationMetadataOpenAiChatOptions extends OpenAiChatOptions implements ObservationMetadataAwareOptions {

	private Map<String, String> observationMetadata;

	private ObservationMetadataOpenAiChatOptions(OpenAiChatOptions options, Map<String, String> observationMetadata) {
		this(options, copyObservationMetadata(observationMetadata), true);
	}

	private ObservationMetadataOpenAiChatOptions(OpenAiChatOptions options, Map<String, String> observationMetadata,
			boolean sharedObservationMetadata) {
		super(options.getBaseUrl(), options.getApiKey(), options.getCredential(), options.getModel(),
				options.getMicrosoftDeploymentName(), options.getMicrosoftFoundryServiceVersion(),
				options.getOrganizationId(), options.isMicrosoftFoundry(), options.isGitHubModels(),
				options.getTimeout(), options.getMaxRetries(), options.getProxy(), copyMap(options.getCustomHeaders()),
				options.getFrequencyPenalty(), options.getMaxTokens(), options.getPresencePenalty(),
				copyList(options.getStopSequences()), options.getTemperature(), options.getTopP(),
				copyList(options.getToolCallbacks()), copyMap(options.getToolContext()), copyMap(options.getLogitBias()),
				options.getLogprobs(), options.getTopLogprobs(), options.getMaxCompletionTokens(), options.getN(),
				copyList(options.getOutputModalities()), options.getOutputAudio(), options.getResponseFormat(),
				options.getStreamOptions(), options.getSeed(), options.getToolChoice(), options.getUser(),
				options.getParallelToolCalls(), options.getStore(), copyMap(options.getMetadata()),
				options.getReasoningEffort(), options.getVerbosity(), options.getServiceTier(),
				options.getPromptCacheKey(), copyMap(options.getExtraBody()));
		this.observationMetadata = sharedObservationMetadata ? useObservationMetadata(observationMetadata)
				: copyObservationMetadata(observationMetadata);
	}

	static ObservationMetadataOpenAiChatOptions from(OpenAiChatOptions options,
			Map<String, String> observationMetadata) {
		return new ObservationMetadataOpenAiChatOptions(options, observationMetadata);
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

	private static <T> List<T> copyList(List<T> source) {
		return source != null ? new ArrayList<>(source) : null;
	}

	private static <K, V> Map<K, V> copyMap(Map<K, V> source) {
		return source != null ? new HashMap<>(source) : null;
	}

	static final class Builder extends OpenAiChatOptions.Builder {

		private Map<String, String> observationMetadata;

		private Builder(OpenAiChatOptions options, Map<String, String> observationMetadata) {
			copyFrom(options);
			this.observationMetadata = useObservationMetadata(observationMetadata);
		}

		Builder observationMetadata(Map<String, String> observationMetadata) {
			this.observationMetadata = copyObservationMetadata(observationMetadata);
			return this;
		}

		@Override
		public Builder clone() {
			return new Builder(build(), this.observationMetadata);
		}

		@Override
		public ObservationMetadataOpenAiChatOptions build() {
			return new ObservationMetadataOpenAiChatOptions(super.build(), this.observationMetadata, true);
		}

		private void copyFrom(OpenAiChatOptions options) {
			this.model = options.getModel();
			this.frequencyPenalty = options.getFrequencyPenalty();
			this.maxTokens = options.getMaxTokens();
			this.presencePenalty = options.getPresencePenalty();
			this.stopSequences = copyList(options.getStopSequences());
			this.temperature = options.getTemperature();
			this.topK = options.getTopK();
			this.topP = options.getTopP();
			this.toolCallbacks = copyList(options.getToolCallbacks());
			this.toolContext = copyMap(options.getToolContext());
			this.baseUrl = options.getBaseUrl();
			this.apiKey = options.getApiKey();
			this.credential = options.getCredential();
			this.microsoftDeploymentName = options.getMicrosoftDeploymentName();
			this.microsoftFoundryServiceVersion = options.getMicrosoftFoundryServiceVersion();
			this.organizationId = options.getOrganizationId();
			this.isMicrosoftFoundry = options.isMicrosoftFoundry();
			this.isGitHubModels = options.isGitHubModels();
			this.timeout = options.getTimeout();
			this.maxRetries = options.getMaxRetries();
			this.proxy = options.getProxy();
			this.customHeaders = copyMap(options.getCustomHeaders());
			this.logitBias = copyMap(options.getLogitBias());
			this.logprobs = options.getLogprobs();
			this.topLogprobs = options.getTopLogprobs();
			this.maxCompletionTokens = options.getMaxCompletionTokens();
			this.n = options.getN();
			this.outputModalities = copyList(options.getOutputModalities());
			this.outputAudio = options.getOutputAudio();
			this.responseFormat = options.getResponseFormat();
			this.streamOptions = options.getStreamOptions();
			this.seed = options.getSeed();
			this.toolChoice = options.getToolChoice();
			this.user = options.getUser();
			this.parallelToolCalls = options.getParallelToolCalls();
			this.store = options.getStore();
			this.metadata = copyMap(options.getMetadata());
			this.reasoningEffort = options.getReasoningEffort();
			this.verbosity = options.getVerbosity();
			this.serviceTier = options.getServiceTier();
			this.promptCacheKey = options.getPromptCacheKey();
			this.extraBody = copyMap(options.getExtraBody());
		}

	}

}
