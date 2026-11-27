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

/** A model request exceeded its configured or provider-enforced input limit. */
public final class ContextWindowExceededException extends RuntimeException {

	private final int estimatedInputTokens;

	private final int maxInputTokens;

	public ContextWindowExceededException(int estimatedInputTokens, int maxInputTokens, Throwable cause) {
		super("Model input exceeds the context window (estimated input: " + estimatedInputTokens
				+ " tokens, configured limit: " + maxInputTokens + " tokens)", cause);
		this.estimatedInputTokens = estimatedInputTokens;
		this.maxInputTokens = maxInputTokens;
	}

	public int getEstimatedInputTokens() {
		return this.estimatedInputTokens;
	}

	public int getMaxInputTokens() {
		return this.maxInputTokens;
	}

}
