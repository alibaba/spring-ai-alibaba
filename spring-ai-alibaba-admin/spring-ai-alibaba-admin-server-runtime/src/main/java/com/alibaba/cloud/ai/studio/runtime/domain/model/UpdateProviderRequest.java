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

import java.util.Map;

/**
 * Request for updating provider information
 */
@Data
public class UpdateProviderRequest {

	/**
	 * Provider name
	 */
	private String name;

	/**
	 * Provider description
	 */
	private String description;

	/**
	 * Whether the provider is enabled
	 */
	private Boolean enable;

	/**
	 * Provider icon
	 */
	private String icon;

	/**
	 * Credential configuration for authentication
	 */
	@JsonProperty("credential_config")
	private Map<String, Object> credentialConfig;

	/**
	 * Provider protocol, defaults to OpenAI
	 */
	private String protocol = "OpenAI";

	/**
	 * Supported model types, comma-separated
	 */
	@JsonProperty("supported_model_types")
	private String supportedModelTypes;

}
