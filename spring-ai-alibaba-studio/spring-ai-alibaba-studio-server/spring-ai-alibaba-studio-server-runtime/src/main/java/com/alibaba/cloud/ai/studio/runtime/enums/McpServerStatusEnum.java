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

/**
 * Enum representing the status of MCP server
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Getter
public enum McpServerStatusEnum {

	/** Server is not available */
	Unavailable(0, "unavailable"),

	/** Server is running normally */
	Normal(1, "normal"),

	/** Server has been deleted */
	Deleted(3, "deleted");

	/** Status code */
	private final Integer Code;

	/** Status description */
	private final String Status;

	McpServerStatusEnum(Integer code, String status) {
		Code = code;
		Status = status;
	}

	/**
	 * @return the status message
	 */
	public String getMessage() {
		return this.Status;
	}

}
