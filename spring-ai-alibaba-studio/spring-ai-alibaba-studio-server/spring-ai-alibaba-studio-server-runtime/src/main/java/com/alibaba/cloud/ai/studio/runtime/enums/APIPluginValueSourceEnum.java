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
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration for API plugin value sources
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Getter
public enum APIPluginValueSourceEnum {

	/**
	 * Model recognition source
	 */
	MODEL("model", "Model Recognition", "aiVars"),

	/**
	 * Business pass-through source
	 */
	BIZ("biz", "Business Pass-through", "bizVars");

	/**
	 * Source code identifier
	 */
	private final String code;

	/**
	 * Source description
	 */
	private final String desc;

	/**
	 * SDK field mapping
	 */
	private String sdkMapping;

	APIPluginValueSourceEnum(String code, String desc, String sdkMapping) {
		this.code = code;
		this.desc = desc;
		this.sdkMapping = sdkMapping;
	}

	/**
	 * Get enum value by code
	 * @param code source code
	 * @return corresponding enum value or null if not found
	 */
	public static APIPluginValueSourceEnum valueOfByCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		Optional<APIPluginValueSourceEnum> anyOptional = Arrays.stream(values())
			.filter(valueFrom -> valueFrom.getCode().equals(code))
			.findAny();
		return anyOptional.orElse(null);
	}

}
