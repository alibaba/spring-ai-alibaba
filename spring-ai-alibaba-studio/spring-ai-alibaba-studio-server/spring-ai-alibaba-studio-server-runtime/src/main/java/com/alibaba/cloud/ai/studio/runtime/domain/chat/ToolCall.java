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

package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a tool call in the chat system. Contains information about the tool being
 * called and its execution details.
 *
 * @since 1.0.0.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall implements Serializable {

	/** Unique identifier for the tool call */
	private String id;

	/** Type of the tool call */
	private ToolCallType type;

	/** Index of the tool call in a sequence */
	private Integer index;

	/** Function details of the tool call */
	private Function function;

	/**
	 * Represents the function details of a tool call.
	 */
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Function implements Serializable {

		/** Name of the function */
		private String name;

		/** Arguments passed to the function */
		private String arguments;

		/** Output result of the function execution */
		private String output;

	}

}
