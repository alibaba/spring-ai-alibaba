package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Common Param for InputParams and OutputParams 公共参数，用于InputParams和OutputParams
 *
 * @since 1.0.0-beta
 */
@Data
public class CommonParam implements Serializable {

	// key is the name of the param
	private String key;

	// type is the type of the param
	private String type;

	// desc is the description of the param
	private String desc;

	// value is the value of the param
	private Object value;

	// valueFrom is the source of the value, such as infer and input
	@JsonProperty("value_from")
	private String valueFrom;

	// required is the required of the param, true is required, false is not required
	private Boolean required;

	// defaultValue is the default value of the param
	@JsonProperty("default_value")
	private Object defaultValue;

}
