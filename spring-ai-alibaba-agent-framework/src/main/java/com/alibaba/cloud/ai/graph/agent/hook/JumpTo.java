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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration representing jump destinations in the agent workflow.
 * Supports serialization to string and deserialization from string.
 */
public enum JumpTo {
	tool,
	model,
	end;

	/**
	 * Converts this enum to its string representation.
	 * Used for JSON serialization via @JsonValue annotation.
	 * 
	 * @return the string representation of this enum (same as name())
	 */
	@Override
	@JsonValue
	public String toString() {
		return name();
	}

	/**
	 * Converts a string to a JumpTo enum instance.
	 * Supports case-insensitive matching.
	 * Used for JSON deserialization via @JsonCreator annotation.
	 * 
	 * @param value the string value to convert
	 * @return the corresponding JumpTo enum instance
	 * @throws IllegalArgumentException if the value does not match any enum constant
	 */
	@JsonCreator
	public static JumpTo fromString(String value) {
		if (value == null) {
			throw new IllegalArgumentException("JumpTo value cannot be null");
		}

		// Try case-insensitive matching first
		for (JumpTo jumpTo : values()) {
			if (jumpTo.name().equalsIgnoreCase(value)) {
				return jumpTo;
			}
		}

		// If no match found, throw exception with helpful message
		throw new IllegalArgumentException(
			"Unknown JumpTo value: " + value + ". Valid values are: tool, model, end");
	}

	/**
	 * Converts a string to a JumpTo enum instance, returning null if the value is null or invalid.
	 * This is a safe version that doesn't throw exceptions.
	 * 
	 * @param value the string value to convert
	 * @return the corresponding JumpTo enum instance, or null if value is null or invalid
	 */
	public static JumpTo fromStringOrNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return fromString(value);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
}

