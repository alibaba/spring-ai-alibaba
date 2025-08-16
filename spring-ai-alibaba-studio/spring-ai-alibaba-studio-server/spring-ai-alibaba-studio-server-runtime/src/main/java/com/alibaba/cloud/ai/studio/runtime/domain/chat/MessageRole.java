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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Enum representing different roles in a chat message. Defines the possible roles that
 * can be assigned to messages in a conversation.
 *
 * @since 1.0.0.3
 */
@Getter
public enum MessageRole {

	/** Represents a message from the user */
	@JsonProperty("user")
	USER("user"),

	/** Represents a message from the AI assistant */
	@JsonProperty("assistant")
	ASSISTANT("assistant"),

	/** Represents a system-level message */
	@JsonProperty("system")
	SYSTEM("system"),

	/** Represents a message from a tool */
	@JsonProperty("tool")
	TOOL("tool");

	/** The string value of the message role */
	private final String value;

	MessageRole(String value) {
		this.value = value;
	}

	/**
	 * Converts a string value to its corresponding MessageRole enum.
	 * @param value The string value to convert
	 * @return The corresponding MessageRole enum
	 * @throws IllegalArgumentException if the value is invalid
	 */
	public static MessageRole of(String value) {
		for (MessageRole messageRole : MessageRole.values()) {
			if (messageRole.getValue().equals(value)) {
				return messageRole;
			}
		}

		throw new IllegalArgumentException("Invalid MessageType value: " + value);
	}

}
