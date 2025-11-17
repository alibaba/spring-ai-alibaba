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
package com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic fallback to alternative models on errors.
 *
 * Retries failed model calls with alternative models in sequence until
 * success or all models exhausted.
 *
 * Example:
 * ModelFallbackInterceptor interceptor = ModelFallbackInterceptor.builder()
 *     .addFallbackModel(gpt4oMiniModel)
 *     .addFallbackModel(claude35SonnetModel)
 *     .build();
 */
public class ModelFallbackInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ModelFallbackInterceptor.class);

	private final List<ChatModel> fallbackModels;

	private ModelFallbackInterceptor(Builder builder) {
		this.fallbackModels = new ArrayList<>(builder.fallbackModels);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		Exception lastException = null;

		// Try primary model first
		try {
			ModelResponse modelResponse = handler.call(request);
			Message message = (Message) modelResponse.getMessage();
			
			// Check if response contains error indicator
			if (message.getText() != null && message.getText().contains("Exception:")) {
				throw new RuntimeException(message.getText());
			}
			
			// Return successful response
			return modelResponse;
		}
		catch (Exception e) {
			log.warn("Primary model failed: {}", e.getMessage());
			lastException = e;
		}

		// Try fallback models in sequence
		for (int i = 0; i < fallbackModels.size(); i++) {
			ChatModel fallbackModel = fallbackModels.get(i);
			try {
				log.info("Trying fallback model {} of {}", i + 1, fallbackModels.size());

				// Call the fallback model directly
				Prompt prompt = new Prompt(request.getMessages(), request.getOptions());
				var response = fallbackModel.call(prompt);

				return ModelResponse.of(response.getResult().getOutput());
			}
			catch (Exception e) {
				log.warn("Fallback model {} failed: {}", i + 1, e.getMessage());
				lastException = e;
			}
		}

		// All models failed
		throw new RuntimeException("All models failed after " + (fallbackModels.size() + 1) + " attempts", lastException);
	}

	@Override
	public String getName() {
		return "ModelFallback";
	}

	public static class Builder {
		private final List<ChatModel> fallbackModels = new ArrayList<>();

		public Builder addFallbackModel(ChatModel model) {
			this.fallbackModels.add(model);
			return this;
		}

		public Builder fallbackModels(List<ChatModel> models) {
			this.fallbackModels.addAll(models);
			return this;
		}

		public ModelFallbackInterceptor build() {
			if (fallbackModels.isEmpty()) {
				throw new IllegalArgumentException("At least one fallback model must be specified");
			}
			return new ModelFallbackInterceptor(this);
		}
	}
}

