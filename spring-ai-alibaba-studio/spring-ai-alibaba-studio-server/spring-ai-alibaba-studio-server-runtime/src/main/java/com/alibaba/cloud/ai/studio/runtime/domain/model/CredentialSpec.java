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
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Credential parameter specification
 */
@Data
@Accessors(chain = true)
public class CredentialSpec implements Serializable {

	/**
	 * Parameter code
	 */
	private String code;

	/**
	 * Display name of the parameter
	 */
	@JsonProperty("display_name")
	private String displayName;

	/**
	 * Parameter description
	 */
	private String description;

	/**
	 * Placeholder text for the parameter
	 */
	@JsonProperty("place_holder")
	private String placeHolder;

	/**
	 * Whether the parameter is required
	 */
	private boolean required;

	/**
	 * Whether the parameter contains sensitive information (e.g., API keys)
	 */
	private boolean sensitive;

}
