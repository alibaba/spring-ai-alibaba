package com.alibaba.cloud.ai.studio.runtime.domain.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Schema definition for tool input parameters.
 *
 * @author guning.lt
 * @since 1.0.0-M1
 */
@Data
public class InputSchema implements Serializable {

	/**
	 * The type of the input schema
	 */
	private String type;

	/**
	 * Properties of the input schema
	 */
	private Map<String, Object> properties;

	/**
	 * List of required property names
	 */
	private List<String> required;

	/**
	 * Whether additional properties are allowed
	 */
	@JsonProperty("additional_properties")
	private Boolean additionalProperties;

}
