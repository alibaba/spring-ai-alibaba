/*
 * Copyright 2024-2025 the original author or authors.
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
package io.agentscope.core.model;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;

public class OpenAIChatModel implements Model {

	private final String baseUrl;

	private final String apiKey;

	private final String modelName;

	public OpenAIChatModel(String baseUrl, String apiKey, String modelName) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.modelName = modelName;
	}

	@Override
	public ChatModel chatModel() {
		return org.springframework.ai.openai.OpenAiChatModel.builder()
			.openAiApi(OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build())
			.defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
			.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String baseUrl = OpenAiApiConstants.DEFAULT_BASE_URL;

		private String apiKey;

		private String modelName;

		private Builder() {
		}

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		public OpenAIChatModel build() {
			return new OpenAIChatModel(baseUrl, apiKey, modelName);
		}

	}

}
