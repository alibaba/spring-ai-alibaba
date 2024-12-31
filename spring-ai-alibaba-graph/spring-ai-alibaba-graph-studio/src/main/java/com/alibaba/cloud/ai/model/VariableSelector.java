package com.alibaba.cloud.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * VariableSelector is the reference of a variable in State.
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class VariableSelector {

	/**
	 * An isolation domain of the variable, Could be the node id.
	 */
	private String namespace;

	/**
	 * Name of the variable.
	 */
	private String name;

	/**
	 * Label of the variable.
	 */
	private String label;

	/**
	 * Only namespace and name is required for a valid selector.
	 * @param namespace An isolation domain of the variable
	 * @param name Name of the variable
	 */
	public VariableSelector(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

}
