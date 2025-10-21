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
package com.alibaba.cloud.ai.graph.agent.hook.modelfallback;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.BeforeModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic fallback to alternative models on errors.
 *
 * Retries failed model calls with alternative models in sequence until
 * success or all models exhausted.
 *
 * Example:
 * <pre>
 * ModelFallbackHook fallback = ModelFallbackHook.builder()
 *     .addFallbackModel(gpt4oMiniModel)
 *     .addFallbackModel(claude35SonnetModel)
 *     .build();
 * </pre>
 */
public class ModelFallbackHook extends BeforeModelHook {

	private static final Logger log = LoggerFactory.getLogger(ModelFallbackHook.class);

	private final List<ChatModel> fallbackModels;
	private ChatModel currentModel;

	private ModelFallbackHook(Builder builder) {
		this.fallbackModels = new ArrayList<>(builder.fallbackModels);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		// This hook wraps model calls and handles fallback logic
		// The actual implementation would integrate with the model execution pipeline
		return CompletableFuture.completedFuture(Map.of());
	}

	/**
	 * Attempts to call a model with fallback support.
	 *
	 * @param primaryModel The primary model to try first
	 * @param prompt The prompt to send
	 * @return The response from a successful model call
	 * @throws Exception if all models fail
	 */
	public ChatResponse callWithFallback(
			ChatModel primaryModel,
			Prompt prompt) throws Exception {

		Exception lastException = null;

		// Try primary model first
		try {
			return primaryModel.call(prompt);
		}
		catch (Exception e) {
			log.warn("Primary model failed: {}", e.getMessage());
			lastException = e;
		}

		// Try fallback models
		for (ChatModel fallbackModel : fallbackModels) {
			try {
				log.info("Trying fallback model: {}", fallbackModel.getClass().getSimpleName());
				return fallbackModel.call(prompt);
			}
			catch (Exception e) {
				log.warn("Fallback model failed: {}", e.getMessage());
				lastException = e;
			}
		}

		// All models failed
		throw lastException;
	}

	@Override
	public String getName() {
		return "ModelFallback";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
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

		public ModelFallbackHook build() {
			if (fallbackModels.isEmpty()) {
				throw new IllegalArgumentException("At least one fallback model must be specified");
			}
			return new ModelFallbackHook(this);
		}
	}
}

