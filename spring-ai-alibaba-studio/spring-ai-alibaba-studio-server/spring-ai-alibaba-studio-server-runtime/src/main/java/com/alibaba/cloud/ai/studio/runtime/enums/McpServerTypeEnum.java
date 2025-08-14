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

/**
 * Enum representing different types of MCP servers.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
public enum McpServerTypeEnum {

	/** Official MCP server */
	OFFICIAL,

	/** Customer's MCP server */
	CUSTOMER;

	/**
	 * Converts a string to the corresponding McpServerTypeEnum value.
	 * @param type The string representation of the server type
	 * @return The corresponding McpServerTypeEnum value, or null if not found
	 */
	public static McpServerTypeEnum of(String type) {
		for (McpServerTypeEnum typeEnum : McpServerTypeEnum.values()) {
			if (typeEnum.name().equals(type)) {
				return typeEnum;
			}
		}
		return null;
	}

}
