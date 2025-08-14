package com.alibaba.cloud.ai.studio.runtime.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Request for adding a provider
 */
@Data
public class AddProviderRequest {

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
	 * Credential configuration for authentication
	 */
	@JsonProperty("credential_config")
	private Map<String, Object> credentialConfig;

	/**
	 * Provider protocol, defaults to OpenAI
	 */
	private String protocol = "OpenAI";

	/**
	 * List of supported model types, comma-separated
	 */
	@JsonProperty("supported_model_types")
	private String supportedModelTypes;

}
