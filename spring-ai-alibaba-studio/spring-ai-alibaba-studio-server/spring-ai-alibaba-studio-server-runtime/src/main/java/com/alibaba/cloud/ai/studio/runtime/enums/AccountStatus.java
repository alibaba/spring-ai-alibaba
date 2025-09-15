/*
 * Copyright 2025 the original author or authors.
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

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Account status enum. Represents different states of an account in the system.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum AccountStatus {

	/** Account has been deleted */
	@JsonProperty("deleted")
	DELETED(0, "deleted"),

	/** Account is in normal state */
	@JsonProperty("normal")
	NORMAL(1, "normal"),

	/** Account is disabled */
	@JsonProperty("disabled")
	DISABLED(2, "disabled"),;

	/** Numeric status code */
	@EnumValue
	private final Integer status;

	/** String representation of the status */
	private final String value;

	/**
	 * Get AccountStatus by numeric status code
	 * @param status numeric status code
	 * @return corresponding AccountStatus
	 * @throws IllegalArgumentException if status is invalid
	 */
	public static AccountStatus of(Integer status) {
		for (AccountStatus accountStatus : AccountStatus.values()) {
			if (accountStatus.status.equals(status)) {
				return accountStatus;
			}
		}

		throw new IllegalArgumentException("Invalid status: " + status);
	}

}
