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
