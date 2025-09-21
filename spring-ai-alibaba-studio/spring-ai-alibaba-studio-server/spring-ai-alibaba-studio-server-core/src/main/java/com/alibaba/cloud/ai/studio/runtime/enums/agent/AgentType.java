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

package com.alibaba.cloud.ai.studio.runtime.enums.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of agent types supported by the system.
 *
 * @since 1.0.0.3
 */
public enum AgentType {

	/** React agent that can use tools and interact with environments */
	REACT_AGENT("ReactAgent"),

	/** Parallel agent that executes multiple agents concurrently */
	PARALLEL_AGENT("ParallelAgent"),

	/** Sequential agent that executes agents in a specific order */
	SEQUENTIAL_AGENT("SequentialAgent"),

	/**
	 * LLM routing agent that routes requests to different agents based on LLM decisions
	 */
	LLM_ROUTING_AGENT("LLMRoutingAgent"),

	/** Loop agent that repeatedly executes agents based on conditions */
	LOOP_AGENT("LoopAgent");

	private final String code;

	AgentType(String code) {
		this.code = code;
	}

	@JsonValue
	public String getCode() {
		return code;
	}

	@JsonCreator
	public static AgentType fromCode(String code) {
		for (AgentType type : values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown agent type code: " + code);
	}

}
