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
 * Enum representing the different states of an application.
 */
@Getter
@AllArgsConstructor
public enum AppStatus {

	@JsonProperty("deleted")
	DELETED(0, "deleted"),

	@JsonProperty("draft")
	DRAFT(1, "draft"),

	@JsonProperty("published")
	PUBLISHED(2, "published"),

	@JsonProperty("published_editing")
	PUBLISHED_EDITING(3, "published_editing"),;

	@EnumValue
	private final Integer status; // Numeric status code

	private final String value; // String representation of the status

	/**
	 * Converts a numeric status code to its corresponding AppStatus enum value.
	 * @param status Numeric status code
	 * @return Corresponding AppStatus enum value
	 * @throws IllegalArgumentException if the status code is invalid
	 */
	public static AppStatus of(Integer status) {
		for (AppStatus appStatus : AppStatus.values()) {
			if (appStatus.status.equals(status)) {
				return appStatus;
			}
		}

		throw new IllegalArgumentException("Invalid status: " + status);
	}

}
