/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Model DTO class for representing AI model information.
 *
 * @since 1.0.0.3
 */
@Data
public class Model implements Serializable {

	/** Unique identifier for the model */
	private Long id;

	/** External model identifier */
	@JsonProperty("model_id")
	private String modelId;

	/** Model type/category */
	private String type;

	/** Model status */
	private Integer status;

	/** Model name */
	private String name;

	/** Model description */
	private String description;

	/** Model configuration */
	private Config config;

	/** Context window size */
	private Integer context;

	/** Creation timestamp */
	private Long gmtCreate;

	/** Last modification timestamp */
	private Long gmtModified;

	/** Creator of the model */
	private String creator;

	/** Last modifier of the model */
	private String modifier;

	/**
	 * Configuration class for model parameters
	 */
	@Data
	public static class Config implements Serializable {

		/** Maximum number of input tokens */
		@JsonProperty("max_input_tokens")
		private Integer maxInputTokens;

		/** Maximum number of output tokens */
		@JsonProperty("max_output_tokens")
		private Integer maxOutputTokens;

	}

}
