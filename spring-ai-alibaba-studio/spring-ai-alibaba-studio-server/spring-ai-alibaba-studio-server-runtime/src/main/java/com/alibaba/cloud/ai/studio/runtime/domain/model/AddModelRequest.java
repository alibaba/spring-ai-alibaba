package com.alibaba.cloud.ai.studio.runtime.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request for adding a new model
 */
@Data
public class AddModelRequest {

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
	 * Model type
	 */
	private String type;

	/**
	 * Model tags (comma-separated)
	 */
	private String tags;

	/**
	 * Model icon
	 */
	private String icon;

}
