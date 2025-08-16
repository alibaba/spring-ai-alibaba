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
package com.alibaba.cloud.ai.studio.core.model.llm;

import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ParameterRule;

import java.util.List;
import java.util.Map;

/**
 * Interface for LLM model providers
 *
 * @since 1.0.0.3
 */
public interface ModelProvider {

	/**
	 * Get provider code
	 * @return provider code
	 */
	String getCode();

	/**
	 * Get provider name
	 * @return provider name
	 */
	String getName();

	/**
	 * Get provider description
	 * @return provider description
	 */
	String getDescription();

	/**
	 * Get list of preset models for this provider
	 * @return list of preset models
	 */
	List<ModelConfigInfo> getPresetModels();

	/**
	 * Get model protocol
	 * @return protocol
	 */
	String getProtocol();

	/**
	 * Get provider endpoint
	 * @return endpoint URL
	 */
	String getEndpoint();

	/**
	 * Validate provider credentials
	 * @param credentialSpecs credential specifications
	 * @param credentialMap credential values
	 * @return validation result
	 */
	boolean validateCredentials(List<CredentialSpec> credentialSpecs, Map<String, Object> credentialMap);

	/**
	 * Get credential specifications
	 * @return list of credential specifications
	 */
	List<CredentialSpec> getCredentialSpecs();

	/**
	 * Get parameter rules for a specific model
	 * @param modelId model identifier
	 * @param modelType model type
	 * @return list of parameter rules
	 */
	List<ParameterRule> getParameterRules(String modelId, String modelType);

}
