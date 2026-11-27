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

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Token limits for a model deployment. A separate input cap can be supplied when the
 * provider imposes one in addition to the shared input and output window.
 * @param contextWindowTokens the shared context window
 * @param maxInputTokens the input cap, or zero when the provider has no separate cap
 * @param reservedOutputTokens the minimum output reservation
 */
public record ModelCapacity(int contextWindowTokens, int maxInputTokens, int reservedOutputTokens) {

	public ModelCapacity {
		if (contextWindowTokens < 1 || maxInputTokens < 0 || reservedOutputTokens < 0
				|| reservedOutputTokens >= contextWindowTokens) {
			throw new IllegalArgumentException("Model capacity must leave room for input tokens");
		}
	}

	public int inputLimit(ChatOptions options) {
		Integer configuredOutput = options == null ? null : options.getMaxTokens();
		int reserved = configuredOutput == null ? this.reservedOutputTokens
				: Math.max(configuredOutput, this.reservedOutputTokens);
		int remaining = Math.max(0, this.contextWindowTokens - reserved);
		return this.maxInputTokens == 0 ? remaining : Math.min(remaining, this.maxInputTokens);
	}

}
