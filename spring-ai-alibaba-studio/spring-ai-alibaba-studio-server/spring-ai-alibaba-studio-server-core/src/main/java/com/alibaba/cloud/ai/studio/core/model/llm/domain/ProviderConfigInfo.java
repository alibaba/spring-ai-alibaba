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

import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Configuration information for model provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderConfigInfo {

	/**
	 * Provider ID
	 */
	private String provider;

	/**
	 * Provider name
	 */
	private String name;

	/**
	 * Provider description
	 */
	private String description;

	/**
	 * Provider icon
	 */
	private String icon;

	/**
	 * Authentication credentials in JSON format
	 */
	private ModelCredential credential;

	/**
	 * Whether the provider is enabled
	 */
	private Boolean enable;

	/**
	 * Source of the provider
	 */
	private String source;

	/**
	 * Protocol type, defaults to OpenAI protocol
	 */
	private String protocol = "openai";

	/**
	 * Creation time
	 */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/**
	 * Last modification time
	 */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/**
	 * Creator of the provider
	 */
	private String creator;

	/**
	 * Last modifier
	 */
	private String modifier;

	/**
	 * List of credential specifications
	 */
	@JsonProperty("credential_specs")
	private List<CredentialSpec> credentialSpecs;

	/**
	 * List of supported model types
	 */
	@JsonProperty("supported_model_types")
	private List<String> supportedModelTypes;

	/**
	 * Number of models available
	 */
	@JsonProperty("model_count")
	private Integer modelCount;

}
