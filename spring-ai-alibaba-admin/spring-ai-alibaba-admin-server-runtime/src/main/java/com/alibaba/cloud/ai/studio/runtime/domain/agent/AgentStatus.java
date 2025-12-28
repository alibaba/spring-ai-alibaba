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

package com.alibaba.cloud.ai.studio.runtime.domain.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the status of an agent in the system.
 *
 * @since 1.0.0.3
 */

@Getter
@AllArgsConstructor
public enum AgentStatus {

	/** Indicates that the agent has completed its task successfully */
	@JsonProperty("completed")
	COMPLETED("completed"),

	/** Indicates that the agent's task has failed */
	@JsonProperty("failed")
	FAILED("failed"),

	/** Indicates that the agent is currently processing its task */
	@JsonProperty("in_progress")
	IN_PROGRESS("in_progress"),

	/** Indicates that the agent's task is incomplete */
	@JsonProperty("incomplete")
	INCOMPLETE("incomplete"),;

	/** The string value representing the status */
	private final String value;

	/**
	 * Converts a finish reason string to the corresponding AgentStatus.
	 * @param finishReason The reason for task completion
	 * @return The corresponding AgentStatus
	 */
	public static AgentStatus toAgentStatus(String finishReason) {
		if (finishReason == null || finishReason.isEmpty()) {
			return AgentStatus.IN_PROGRESS;
		}

		finishReason = finishReason.toLowerCase();
		return switch (finishReason) {
			case "stop", "length" -> AgentStatus.COMPLETED;
			case "tool_calls" -> AgentStatus.IN_PROGRESS;
			default -> AgentStatus.FAILED;
		};
	}

}
