package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

/**
 * Enum defining the source of values in workflow processing.
 */
public enum ValueFromEnum {

	/**
	 * Value is referenced from another source or variable.
	 */
	refer,

	/**
	 * Value is directly input by the user.
	 */
	input,

	/**
	 * Value should be cleared or reset.
	 */
	clear

}
