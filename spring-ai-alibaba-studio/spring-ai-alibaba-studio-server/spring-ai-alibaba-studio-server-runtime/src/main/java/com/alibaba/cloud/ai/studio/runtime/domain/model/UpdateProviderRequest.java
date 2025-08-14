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
