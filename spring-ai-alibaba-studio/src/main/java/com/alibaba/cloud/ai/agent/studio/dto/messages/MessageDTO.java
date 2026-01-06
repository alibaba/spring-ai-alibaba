/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.agent.studio.dto.messages;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for message DTOs with polymorphic serialization support.
 * Uses Jackson annotations for type discrimination during JSON serialization/deserialization.
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "messageType"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = AssistantMessageDTO.class, name = "assistant"),
		@JsonSubTypes.Type(value = UserMessageDTO.class, name = "user"),
		@JsonSubTypes.Type(value = ToolResponseMessageDTO.class, name = "tool"),
		@JsonSubTypes.Type(value = ToolRequestMessageDTO.class, name = "tool-request"),
		@JsonSubTypes.Type(value = ToolRequestConfirmMessageDTO.class, name = "tool-confirm")
})
public interface MessageDTO {

	/**
	 * Get the message type identifier.
	 */
	String getMessageType();

	/**
	 * Get the content of the message.
	 */
	String getContent();

	/**
	 * Factory class for converting between Spring AI Message and DTO types.
	 */
	class MessageDTOFactory {

		/**
		 * Convert Spring AI Message to appropriate DTO type.
		 */
		public static MessageDTO fromMessage(Message message) {
			if (message == null) {
				return null;
			}

			if (message instanceof AssistantMessage assistantMessage) {
				if (assistantMessage.hasToolCalls()) {
					return new ToolRequestMessageDTO(assistantMessage);
				}
				else {
					return new AssistantMessageDTO(assistantMessage);
				}
			}
			else if (message instanceof UserMessage) {
				return new UserMessageDTO((UserMessage) message);
			}
			else if (message instanceof ToolResponseMessage) {
				return new ToolResponseMessageDTO((ToolResponseMessage) message);
			}
			else {
				throw new IllegalArgumentException(
						"Unsupported message type: " + message.getClass().getName()
				);
			}
		}

		/**
		 * Convert InterruptionMetadata to ToolRequestMessageDTO.
		 * This is a specialized method for handling interruption metadata from agent execution.
		 */
		public static ToolRequestConfirmMessageDTO fromInterruptionMetadata(InterruptionMetadata interruptionMetadata) {
			if (interruptionMetadata == null) {
				return null;
			}
			return new ToolRequestConfirmMessageDTO(interruptionMetadata);
		}

		/**
		 * Convert DTO to Spring AI Message.
		 */
		public static Message toMessage(MessageDTO dto) {
			if (dto == null) {
				return null;
			}

			if (dto instanceof ToolRequestMessageDTO) {
				return ((ToolRequestMessageDTO) dto).toAssistantMessage();
			}
			else if (dto instanceof AssistantMessageDTO) {
				return ((AssistantMessageDTO) dto).toAssistantMessage();
			}
			else if (dto instanceof UserMessageDTO) {
				return ((UserMessageDTO) dto).toUserMessage();
			}
			else if (dto instanceof ToolResponseMessageDTO) {
				return ((ToolResponseMessageDTO) dto).toToolResponseMessage();
			}
			else {
				throw new IllegalArgumentException(
						"Unsupported DTO type: " + dto.getClass().getName()
				);
			}
		}
	}
}

