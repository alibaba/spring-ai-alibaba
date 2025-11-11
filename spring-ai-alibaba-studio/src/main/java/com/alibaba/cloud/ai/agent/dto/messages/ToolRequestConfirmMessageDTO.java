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
package com.alibaba.cloud.ai.agent.dto.messages;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for AssistantMessage.
 * Provides serialization-friendly representation with default constructor and getters/setters.
 */
public class ToolRequestConfirmMessageDTO implements MessageDTO {

	@JsonProperty("messageType")
	private String messageType = "tool-confirm";

	@JsonProperty("content")
	private String content;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("toolCalls")
	private List<ToolCallConfigDTO> toolCalls;

	/**
	 * Default constructor for deserialization.
	 */
	public ToolRequestConfirmMessageDTO() {
		this.metadata = new HashMap<>();
		this.toolCalls = new ArrayList<>();
	}

	/**
	 * Constructor with content.
	 */
	public ToolRequestConfirmMessageDTO(String content) {
		this();
		this.content = content;
	}

	/**
	 * Constructor from InterruptionMetadata.
	 */
	public ToolRequestConfirmMessageDTO(InterruptionMetadata interruptionMetadata) {
		this();
		this.content = "";
		this.metadata = new HashMap<>(interruptionMetadata.metadata().orElse(Map.of()));

		// Convert toolFeedbacks to toolCalls
		if (interruptionMetadata.toolFeedbacks() != null && !interruptionMetadata.toolFeedbacks().isEmpty()) {
			this.toolCalls = new ArrayList<>();
			for (InterruptionMetadata.ToolFeedback feedback : interruptionMetadata.toolFeedbacks()) {
				this.toolCalls.add(new ToolCallConfigDTO(feedback));
			}
		}
	}

	/**
	 * Convert to InterruptionMetadata.
	 * @param nodeId the node ID where the interruption occurred
	 * @param state the overall state at the time of interruption
	 */
	public InterruptionMetadata toInterruptionMetadata(String nodeId, OverAllState state) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(nodeId, state);

		// Set metadata
		if (this.metadata != null) {
			for (Map.Entry<String, Object> entry : this.metadata.entrySet()) {
				builder.addMetadata(entry.getKey(), entry.getValue());
			}
		}

		// Convert toolCalls to toolFeedbacks
		if (this.toolCalls != null && !this.toolCalls.isEmpty()) {
			for (ToolCallConfigDTO toolCall : this.toolCalls) {
				builder.addToolFeedback(toolCall.toToolFeedback());
			}
		}

		return builder.build();
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

	public List<ToolCallConfigDTO> getToolCalls() {
		return toolCalls;
	}

	public void setToolCalls(List<ToolCallConfigDTO> toolCalls) {
		this.toolCalls = toolCalls;
	}

	/**
	 * DTO for ToolCall within AssistantMessage.
	 */
	public static class ToolCallConfigDTO {
		@JsonProperty("id")
		private String id;

		@JsonProperty("type")
		private String type;

		@JsonProperty("name")
		private String name;

		@JsonProperty("arguments")
		private String arguments;

		@JsonProperty("description")
		private String description;

		public ToolCallConfigDTO() {
		}

		public ToolCallConfigDTO(InterruptionMetadata.ToolFeedback feedback){
			this.id = feedback.getId();
			this.type = "Function";
			this.name = feedback.getName();
			this.arguments = feedback.getArguments();
			this.description = feedback.getDescription();
		}

		public InterruptionMetadata.ToolFeedback toToolFeedback() {
			return new InterruptionMetadata.ToolFeedback(this.id, this.name, this.arguments, InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED, this.description);
		}

		// Getters and Setters

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getArguments() {
			return arguments;
		}

		public void setArguments(String arguments) {
			this.arguments = arguments;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}

