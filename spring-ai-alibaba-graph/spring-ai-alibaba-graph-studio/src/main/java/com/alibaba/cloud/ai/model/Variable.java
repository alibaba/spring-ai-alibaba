package com.alibaba.cloud.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Variable is often used to represent the output of a node, or pre-defined variables in
 * an App.
 */
@Data
@NoArgsConstructor
public class Variable {

	private String name;

	private String value;

	private String valueType;

	private String description;

	private Map<String, Object> extraProperties;

	/**
	 * Only name and valueType is required
	 * @param name a valid variable name
	 * @param valueType a {@link VariableType} value
	 */
	public Variable(String name, String valueType) {
		this.name = name;
		this.valueType = valueType;
	}

}
