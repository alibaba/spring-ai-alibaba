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

import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for ToolResponseMessage.
 * Provides serialization-friendly representation with default constructor and getters/setters.
 */
public class ToolResponseMessageDTO implements MessageDTO {

	@JsonProperty("messageType")
	private String messageType = "tool";

	@JsonProperty("content")
	private String content;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("responses")
	private List<ToolResponseDTO> responses;

	/**
	 * Default constructor for deserialization.
	 */
	public ToolResponseMessageDTO() {
		this.metadata = new HashMap<>();
		this.responses = new ArrayList<>();
	}

	/**
	 * Constructor with content.
	 */
	public ToolResponseMessageDTO(String content) {
		this();
		this.content = content;
	}

	/**
	 * Constructor from Spring AI ToolResponseMessage.
	 */
	public ToolResponseMessageDTO(ToolResponseMessage message) {
		this();
		this.content = message.getText();
		this.metadata = new HashMap<>(message.getMetadata());

		// Convert tool responses
		if (message.getResponses() != null && !message.getResponses().isEmpty()) {
			this.responses = new ArrayList<>();
			for (ToolResponseMessage.ToolResponse response : message.getResponses()) {
				this.responses.add(new ToolResponseDTO(response));
			}
		}
	}

	/**
	 * Convert to Spring AI ToolResponseMessage.
	 */
	public ToolResponseMessage toToolResponseMessage() {
		List<ToolResponseMessage.ToolResponse> springResponses = new ArrayList<>();
		if (this.responses != null) {
			for (ToolResponseDTO dto : this.responses) {
				springResponses.add(dto.toToolResponse());
			}
		}

		return ToolResponseMessage.builder()
			.responses(springResponses)
			.metadata(this.metadata)
			.build();
	}

	// Getters and Setters

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public List<ToolResponseDTO> getResponses() {
		return responses;
	}

	public void setResponses(List<ToolResponseDTO> responses) {
		this.responses = responses;
	}

	/**
	 * DTO for ToolResponse within ToolResponseMessage.
	 */
	public static class ToolResponseDTO {
		@JsonProperty("id")
		private String id;

		@JsonProperty("name")
		private String name;

		@JsonProperty("responseData")
		private String responseData;

		public ToolResponseDTO() {
		}

		public ToolResponseDTO(ToolResponseMessage.ToolResponse response) {
			this.id = response.id();
			this.name = response.name();
			this.responseData = response.responseData();
		}

		public ToolResponseMessage.ToolResponse toToolResponse() {
			return new ToolResponseMessage.ToolResponse(this.id, this.name, this.responseData);
		}

		// Getters and Setters

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getResponseData() {
			return responseData;
		}

		public void setResponseData(String responseData) {
			this.responseData = responseData;
		}
	}
}

