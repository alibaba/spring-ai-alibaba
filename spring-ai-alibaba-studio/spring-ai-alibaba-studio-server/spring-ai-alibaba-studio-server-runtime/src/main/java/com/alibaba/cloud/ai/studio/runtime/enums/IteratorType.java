package com.alibaba.cloud.ai.studio.runtime.enums;

import lombok.Getter;

/**
 * Enumeration representing different types of iteration methods
 *
 * @author guning.lt
 * @since 2025/5/9
 */
@Getter
public enum IteratorType {

	/**
	 * Iterate through an array
	 */
	ByArray("byArray", "array"),

	/**
	 * Iterate by count
	 */
	ByCount("byCount", "count"),

	;

	/**
	 * The code identifier for the iteration type
	 */
	private final String Code;

	/**
	 * The status/description of the iteration type
	 */
	private final String Status;

	IteratorType(String code, String status) {
		Code = code;
		Status = status;
	}

	/**
	 * Returns the status message for this iteration type
	 * @return the status message
	 */
	public String getMessage() {
		return this.Status;
	}

}
