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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Parameter rule definition
 */
@Data
@Accessors(chain = true)
public class ParameterRule implements Serializable {

	/**
	 * Parameter code
	 */
	private String code;

	/**
	 * Parameter name
	 */
	private String name;

	/**
	 * Parameter description
	 */
	private String description;

	/**
	 * Parameter type
	 */
	private String type;

	/**
	 * Default value
	 */
	@JsonProperty("default_value")
	private Object defaultValue;

	/**
	 * Minimum value (for numeric parameters)
	 */
	private Integer min;

	/**
	 * Maximum value (for numeric parameters)
	 */
	private Integer max;

	/**
	 * Precision
	 */
	private Integer precision;

	/**
	 * Available options (for enum parameters)
	 */
	private List<Object> options;

	/**
	 * Whether the parameter is required
	 */
	private boolean required;

	/**
	 * Help text
	 */
	private Map<String, String> help;

}
