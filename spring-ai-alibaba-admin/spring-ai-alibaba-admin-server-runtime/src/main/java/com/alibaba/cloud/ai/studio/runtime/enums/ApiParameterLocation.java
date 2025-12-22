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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing the location of API parameters in HTTP requests.
 *
 * @since 1.0.0.3
 */

@Getter
public enum ApiParameterLocation {

	/** Parameter in request body */
	BODY("body"),

	/** Parameter in URL path */
	PATH("path"),

	/** Parameter in URL query string */
	QUERY("query"),

	/** Parameter in HTTP header */
	HEADER("header");

	/** The string representation of the parameter location */
	private final String location;

	ApiParameterLocation(String location) {
		this.location = location;
	}

	/**
	 * Converts a string location to the corresponding enum value.
	 * @param location The string representation of the location
	 * @return The matching ApiParameterLocation enum value, or null if not found
	 */
	public static ApiParameterLocation of(String location) {
		if (StringUtils.isBlank(location)) {
			return null;
		}

		Optional<ApiParameterLocation> any = Arrays.stream(values())
			.filter(parameterLocation -> location.equalsIgnoreCase(parameterLocation.getLocation()))
			.findAny();

		return any.orElse(null);
	}

}
