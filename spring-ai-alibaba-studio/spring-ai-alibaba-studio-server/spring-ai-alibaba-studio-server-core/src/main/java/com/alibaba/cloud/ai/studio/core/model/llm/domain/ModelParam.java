package com.alibaba.cloud.ai.studio.core.model.llm.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Model parameter configuration
 */
@Data
public class ModelParam implements Serializable {

	/**
	 * Parameter key name
	 */
	private String key;

	/**
	 * Parameter type
	 */
	private String type;

	/**
	 * Default value
	 */
	private Object defaultValue;

	/**
	 * Parameter value
	 */
	private Object value;

	/**
	 * Parameter switch flag
	 */
	private Boolean paramSwitch = false;

}
