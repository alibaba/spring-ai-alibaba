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
