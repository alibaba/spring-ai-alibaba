/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.context;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * Controls the input budget after the model prompt and its tool definitions have been
 * assembled. The configured input limit already accounts for reserved output tokens.
 */
public final class ContextWindowPolicy {

	private static final TokenCounter MESSAGE_TOKENS = TokenCounter.approximateMsgCounter();

	private final int maxInputTokens;

	private final int targetInputTokens;

	private final ToIntFunction<Prompt> inputTokenEstimator;

	private final Predicate<Throwable> inputOverflowDetector;

	private ContextWindowPolicy(Builder builder) {
		this.maxInputTokens = builder.maxInputTokens;
		this.targetInputTokens = builder.targetInputTokens;
		this.inputTokenEstimator = builder.inputTokenEstimator;
		this.inputOverflowDetector = builder.inputOverflowDetector;
	}

	public static Builder builder(int maxInputTokens) {
		return new Builder(maxInputTokens);
	}

	public int maxInputTokens() {
		return this.maxInputTokens;
	}

	public int targetInputTokens() {
		return this.targetInputTokens;
	}

	public int estimateInputTokens(Prompt prompt) {
		int estimate = this.inputTokenEstimator.applyAsInt(prompt);
		if (estimate < 0) {
			throw new IllegalStateException("The estimated input token count must be non-negative");
		}
		return estimate;
	}

	public boolean isInputOverflow(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (this.inputOverflowDetector.test(cause)) {
				return true;
			}
		}
		return false;
	}

	private static int estimateTextInput(Prompt prompt) {
		int messages = prompt.getInstructions().size();
		long tokens = MESSAGE_TOKENS.countTokens(prompt.getInstructions()) + 4L * messages + 3;
		if (prompt.getOptions() instanceof ToolCallingChatOptions options && options.getToolCallbacks() != null) {
			for (var callback : options.getToolCallbacks()) {
				tokens += ToolCallbackUtils.estimateSchemaTokens(callback.getToolDefinition());
			}
		}
		if (prompt.getOptions() instanceof StructuredOutputChatOptions options) {
			tokens += textTokens(options.getOutputSchema());
		}
		return (int) Math.min(tokens, Integer.MAX_VALUE);
	}

	private static long textTokens(String text) {
		return text == null ? 0 : (text.length() + 3L) / 4;
	}

	public static final class Builder {

		private final int maxInputTokens;

		private int targetInputTokens;

		private ToIntFunction<Prompt> inputTokenEstimator = ContextWindowPolicy::estimateTextInput;

		private Predicate<Throwable> inputOverflowDetector = error -> false;

		private Builder(int maxInputTokens) {
			if (maxInputTokens < 1) {
				throw new IllegalArgumentException("maxInputTokens must be positive");
			}
			this.maxInputTokens = maxInputTokens;
			this.targetInputTokens = maxInputTokens;
		}

		/** Sets the input size at which the agent starts editing old tool results. */
		public Builder targetInputTokens(int targetInputTokens) {
			if (targetInputTokens < 1 || targetInputTokens > this.maxInputTokens) {
				throw new IllegalArgumentException("targetInputTokens must be between 1 and maxInputTokens");
			}
			this.targetInputTokens = targetInputTokens;
			return this;
		}

		/**
		 * Overrides the default text-based estimate. A model-specific estimator can
		 * account for additional inputs such as media or provider-defined tools.
		 */
		public Builder inputTokenEstimator(ToIntFunction<Prompt> estimator) {
			this.inputTokenEstimator = Objects.requireNonNull(estimator, "estimator must not be null");
			return this;
		}

		/**
		 * Recognizes provider errors that reject a request because its input exceeds
		 * the permitted context. The predicate is applied to the exception cause chain.
		 */
		public Builder inputOverflowDetector(Predicate<Throwable> detector) {
			this.inputOverflowDetector = Objects.requireNonNull(detector, "detector must not be null");
			return this;
		}

		public ContextWindowPolicy build() {
			return new ContextWindowPolicy(this);
		}

	}

}
