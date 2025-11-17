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
package com.alibaba.cloud.ai.agent.studio.dto.messages;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

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

	@JsonProperty("toolFeedback")
	private List<ToolFeedback> toolFeedback;

	/**
	 * Default constructor for deserialization.
	 */
	public ToolRequestConfirmMessageDTO() {
		this.metadata = new HashMap<>();
		this.toolFeedback = new ArrayList<>();
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

		// Convert InterruptionMetadata.ToolFeedback to ToolFeedback
		if (interruptionMetadata.toolFeedbacks() != null && !interruptionMetadata.toolFeedbacks().isEmpty()) {
			this.toolFeedback = new ArrayList<>();
			for (InterruptionMetadata.ToolFeedback feedback : interruptionMetadata.toolFeedbacks()) {
				ToolFeedback toolFeedback = new ToolFeedback();
				toolFeedback.setId(feedback.getId());
				toolFeedback.setName(feedback.getName());
				toolFeedback.setArguments(feedback.getArguments());
				toolFeedback.setDescription(feedback.getDescription());

				// Convert FeedbackResult
				if (feedback.getResult() != null) {
					toolFeedback.setResult(ToolFeedback.FeedbackResult.valueOf(feedback.getResult().name()));
				}

				this.toolFeedback.add(toolFeedback);
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

		// Convert ToolFeedback to InterruptionMetadata.ToolFeedback
		if (this.toolFeedback != null && !this.toolFeedback.isEmpty()) {
			for (ToolFeedback feedback : this.toolFeedback) {
				InterruptionMetadata.ToolFeedback.FeedbackResult result =
						feedback.getResult() != null
								? InterruptionMetadata.ToolFeedback.FeedbackResult.valueOf(feedback.getResult().name())
								: InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED;

				InterruptionMetadata.ToolFeedback interruptionToolFeedback =
						new InterruptionMetadata.ToolFeedback(
								feedback.getId(),
								feedback.getName(),
								feedback.getArguments(),
								result,
								feedback.getDescription()
						);

				builder.addToolFeedback(interruptionToolFeedback);
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

	public List<ToolFeedback> getToolFeedback() {
		return toolFeedback;
	}

	public void setToolFeedback(List<ToolFeedback> toolFeedback) {
		this.toolFeedback = toolFeedback;
	}

	/**
	 * DTO for ToolFeedback.
	 */
	public static class ToolFeedback {
		@JsonProperty("id")
		private String id;

		@JsonProperty("name")
		private String name;

		@JsonProperty("arguments")
		private String arguments;

		@JsonProperty("result")
		private FeedbackResult result;

		@JsonProperty("description")
		private String description;

		public ToolFeedback() {
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

		public String getArguments() {
			return arguments;
		}

		public void setArguments(String arguments) {
			this.arguments = arguments;
		}

		public FeedbackResult getResult() {
			return result;
		}

		public void setResult(FeedbackResult result) {
			this.result = result;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		/**
		 * Enum for ToolFeedback result.
		 */
		public enum FeedbackResult {
			APPROVED,
			REJECTED,
			EDITED;
		}
	}
}

