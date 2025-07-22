package com.alibaba.cloud.ai.studio.runtime.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request for updating model information
 */
@Data
public class UpdateModelRequest {

	/**
	 * Model ID
	 */
	@JsonProperty("model_id")
	private String modelId;

	/**
	 * Model name
	 */
	@JsonProperty("model_name")
	private String modelName;

	/**
	 * Model icon
	 */
	private String icon;

	/**
	 * Tags separated by commas
	 */
	private String tags;

	/**
	 * Model status
	 */
	private Boolean enable;

}
