/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.agent.context.ContextWindowExceededException;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.model.ModelTarget;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

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

	private final List<ModelTarget> fallbackModels;

	private ModelFallbackInterceptor(Builder builder) {
		this.fallbackModels = List.copyOf(builder.fallbackModels);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		try {
			return watchStream(request, handler, handler.call(request), 0);
		}
		catch (RuntimeException failure) {
			return fallback(request, handler, failure, 0);
		}
	}

	private ModelResponse fallback(ModelRequest request, ModelCallHandler handler, RuntimeException failure, int from) {
		for (int i = from; i < this.fallbackModels.size(); i++) {
			if (failure instanceof CancellationException) {
				break;
			}
			var target = this.fallbackModels.get(i);
			if ((request.getModelTarget() != null && request.getModelTarget().model() == target.model())
					|| (failure instanceof ContextWindowExceededException && target.contextWindowPolicy() == null)) {
				continue;
			}
			try {
				log.debug("Trying fallback model {} of {} after {}", i + 1, this.fallbackModels.size(),
						failure.getClass().getSimpleName());
				var alternative = ModelRequest.builder(request).modelTarget(target).build();
				return watchStream(request, handler, handler.call(alternative), i + 1);
			}
			catch (RuntimeException error) {
				failure = error;
			}
		}
		throw failure;
	}

	private ModelResponse watchStream(ModelRequest request, ModelCallHandler handler, ModelResponse response, int next) {
		if (!(response.getMessage() instanceof Flux<?> chunks)) {
			return response;
		}
		return ModelResponse.of(Flux.defer(() -> {
			var emitted = new AtomicBoolean();
			return chunks.cast(ChatResponse.class).doOnNext(chunk -> emitted.set(true))
				.onErrorResume(error -> {
					if (emitted.get()) {
						return Flux.error(error);
					}
					var alternative = fallback(request, handler, Exceptions.propagate(error), next);
					if (alternative.getMessage() instanceof Flux<?> stream) {
						return stream.cast(ChatResponse.class);
					}
					if (alternative.getChatResponse() != null) {
						return Flux.just(alternative.getChatResponse());
					}
					if (alternative.getMessage() instanceof AssistantMessage assistant) {
						return Flux.just(new ChatResponse(List.of(new Generation(assistant))));
					}
					return Flux.error(new IllegalStateException("Fallback returned no streaming response"));
				});
		}));
	}

	@Override
	public String getName() {
		return "ModelFallback";
	}

	public static class Builder {
		private final List<ModelTarget> fallbackModels = new ArrayList<>();

		public Builder addFallbackModel(ChatModel model) {
			return addFallbackModel(model, null);
		}

		/**
		 * Adds a fallback with its own input budget for recovery from context overflow.
		 */
		public Builder addFallbackModel(ChatModel model, ContextWindowPolicy policy) {
			this.fallbackModels.add(new ModelTarget(model, policy));
			return this;
		}

		public Builder fallbackModels(List<ChatModel> models) {
			models.forEach(this::addFallbackModel);
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

