package com.alibaba.cloud.ai.studio.core.model.llm;

import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ParameterRule;

import java.util.List;
import java.util.Map;

/**
 * Interface for LLM model providers
 *
 * @since 1.0.0-beta
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
