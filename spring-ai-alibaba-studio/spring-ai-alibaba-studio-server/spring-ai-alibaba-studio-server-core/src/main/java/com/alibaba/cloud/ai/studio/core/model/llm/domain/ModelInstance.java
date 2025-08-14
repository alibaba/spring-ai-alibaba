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
package com.alibaba.cloud.ai.studio.core.model.llm.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * Model instance
 *
 * @since 1.0.0.3
 */
@Data
@Accessors(chain = true)
public class ModelInstance implements Serializable {

	/**
	 * Provider name
	 */
	private String provider;

	/**
	 * Default model configuration including default parameters
	 */
	private ModelConfigInfo defaultModelConfig;

	/**
	 * Get parameter rules
	 * @return Parameter rules
	 */
	public List<ParameterRule> getParameterRules() {
		// Implementation to be provided by concrete subclasses
		return null;
	}

	/**
	 * Get token count
	 * @param text Text content
	 * @return Number of tokens
	 */
	public int getNumTokens(String text) {
		// Implementation to be provided by concrete subclasses
		return 0;
	}

	/**
	 * Model response
	 */
	@Data
	@Accessors(chain = true)
	public static class ModelResponse implements Serializable {

		/**
		 * Response content
		 */
		private String content;

		/**
		 * Token usage
		 */
		private Usage usage;

		/**
		 * Success flag
		 */
		private boolean success;

		/**
		 * Error code
		 */
		private String errorCode;

		/**
		 * Error message
		 */
		private String errorMsg;

	}

	/**
	 * Token usage statistics
	 */
	@Data
	@Accessors(chain = true)
	public static class Usage implements Serializable {

		/**
		 * Number of prompt tokens
		 */
		private int promptTokens;

		/**
		 * Number of completion tokens
		 */
		private int completionTokens;

		/**
		 * Total number of tokens
		 */
		private int totalTokens;

	}

}
