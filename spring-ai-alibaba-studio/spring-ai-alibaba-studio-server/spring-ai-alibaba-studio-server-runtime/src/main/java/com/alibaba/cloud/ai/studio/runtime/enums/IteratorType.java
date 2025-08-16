/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
