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
package com.alibaba.cloud.ai.studio.runtime.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request for updating model information
 */
@Data
public class UpdateModelRequest {

	/**
	 * Model ID
	 */
	@JsonProperty("model_id")
	private String modelId;

	/**
	 * Model name
	 */
	@JsonProperty("model_name")
	private String modelName;

	/**
	 * Model icon
	 */
	private String icon;

	/**
	 * Tags separated by commas
	 */
	private String tags;

	/**
	 * Model status
	 */
	private Boolean enable;

}
