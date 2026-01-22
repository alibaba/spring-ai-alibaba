/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.rag.bailian;

/**
 * Rewrite configuration for multi-turn conversation in Bailian knowledge base retrieval.
 *
 * <p>Query rewriting improves retrieval effectiveness by automatically adjusting the
 * user's query based on conversation context.
 */
public class RewriteConfig {

	private final String modelName;

	private RewriteConfig(Builder builder) {
		this.modelName = builder.modelName;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Gets the rewrite model name.
	 *
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Builder for RewriteConfig.
	 */
	public static class Builder {
		private String modelName = "conv-rewrite-qwen-1.8b";

		private Builder() {
		}

		/**
		 * Sets the rewrite model name.
		 *
		 * <p>Currently only supports:
		 * <ul>
		 *   <li>conv-rewrite-qwen-1.8b (default)
		 * </ul>
		 *
		 * @param modelName the model name
		 * @return this builder
		 */
		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		/**
		 * Builds a new RewriteConfig.
		 *
		 * @return a new RewriteConfig instance
		 */
		public RewriteConfig build() {
			return new RewriteConfig(this);
		}
	}
}
