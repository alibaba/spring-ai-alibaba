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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data transfer object for a tool approval request.
 */
public class ToolRequestConfirmMessageDTO implements MessageDTO {

	@JsonProperty("messageType")
	private String messageType = "tool-confirm";

	@JsonProperty("content")
	private String content;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("interruptionId")
	private String interruptionId;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonProperty("pendingInterruptions")
	private List<ToolRequestConfirmMessageDTO> pendingInterruptions = List.of();

	@JsonProperty("toolFeedback")
	private List<ToolFeedback> toolFeedback;

	@JsonProperty("toolsAutomaticallyApproved")
	private List<ToolCallDTO> toolsAutomaticallyApproved;

	/**
	 * Default constructor for deserialization.
	 */
	public ToolRequestConfirmMessageDTO() {
		this.metadata = new HashMap<>();
		this.toolFeedback = new ArrayList<>();
		this.toolsAutomaticallyApproved = new ArrayList<>();
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
		this.interruptionId = interruptionMetadata.interruptionId().orElse(null);
		this.pendingInterruptions = interruptionMetadata.pendingInterruptions().stream()
			.map(ToolRequestConfirmMessageDTO::new)
			.toList();

		// Convert InterruptionMetadata.ToolFeedback to ToolFeedback
		if (interruptionMetadata.toolFeedbacks() != null && !interruptionMetadata.toolFeedbacks().isEmpty()) {
			this.toolFeedback = new ArrayList<>();
			for (InterruptionMetadata.ToolFeedback feedback : interruptionMetadata.toolFeedbacks()) {
				ToolFeedback toolFeedback = new ToolFeedback();
				toolFeedback.setId(feedback.getId());
				toolFeedback.setName(feedback.getName());
				toolFeedback.setArguments(feedback.getArguments());
				toolFeedback.setDescription(feedback.getDescription());
				toolFeedback.setAllowedDecisions(feedback.getAllowedDecisions().stream()
						.map(decision -> ToolFeedback.FeedbackResult.valueOf(decision.name())).toList());
				toolFeedback.setResponse(feedback.getResponse());

				// Convert FeedbackResult
				if (feedback.getResult() != null) {
					toolFeedback.setResult(ToolFeedback.FeedbackResult.valueOf(feedback.getResult().name()));
				}

				this.toolFeedback.add(toolFeedback);
			}
		}

		// Convert toolsAutomaticallyApproved
		if (interruptionMetadata.getToolsAutomaticallyApproved() != null
				&& !interruptionMetadata.getToolsAutomaticallyApproved().isEmpty()) {
			this.toolsAutomaticallyApproved = new ArrayList<>();
			for (AssistantMessage.ToolCall toolCall : interruptionMetadata.getToolsAutomaticallyApproved()) {
				this.toolsAutomaticallyApproved.add(new ToolCallDTO(toolCall));
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
		if (this.interruptionId != null) {
			builder.interruptionId(this.interruptionId);
		}
		if (this.pendingInterruptions != null) {
			builder.pendingInterruptions(this.pendingInterruptions.stream()
				.map(interruption -> interruption.toInterruptionMetadata(nodeId, state))
				.toList());
		}

		// Convert ToolFeedback to InterruptionMetadata.ToolFeedback
		if (this.toolFeedback != null && !this.toolFeedback.isEmpty()) {
			for (ToolFeedback feedback : this.toolFeedback) {
				builder.addToolFeedback(feedback.toGraphFeedback());
			}
		}

		// Convert toolsAutomaticallyApproved
		if (this.toolsAutomaticallyApproved != null && !this.toolsAutomaticallyApproved.isEmpty()) {
			List<AssistantMessage.ToolCall> springToolCalls = new ArrayList<>();
			for (ToolCallDTO toolCallDTO : this.toolsAutomaticallyApproved) {
				springToolCalls.add(toolCallDTO.toToolCall());
			}
			builder.toolsAutomaticallyApproved(springToolCalls);
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

	public String getInterruptionId() {
		return interruptionId;
	}

	public List<ToolRequestConfirmMessageDTO> getPendingInterruptions() {
		return pendingInterruptions;
	}

	public List<ToolFeedback> getToolFeedback() {
		return toolFeedback;
	}

	public void setToolFeedback(List<ToolFeedback> toolFeedback) {
		this.toolFeedback = toolFeedback;
	}

	public List<ToolCallDTO> getToolsAutomaticallyApproved() {
		return toolsAutomaticallyApproved;
	}

	public void setToolsAutomaticallyApproved(List<ToolCallDTO> toolsAutomaticallyApproved) {
		this.toolsAutomaticallyApproved = toolsAutomaticallyApproved;
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

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty("allowedDecisions")
		private List<FeedbackResult> allowedDecisions;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty("response")
		private String response;

		public ToolFeedback() {
		}

		public InterruptionMetadata.ToolFeedback toGraphFeedback() {
			var builder = InterruptionMetadata.ToolFeedback.builder().id(id).name(name)
					.arguments(arguments).description(description).response(response);
			if (result != null) {
				builder.type(result.name());
			}
			if (allowedDecisions != null) {
				builder.allowedDecisions(allowedDecisions.stream()
						.map(value -> InterruptionMetadata.ToolFeedback.FeedbackResult.valueOf(value.name())).toList());
			}
			return builder.build();
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

		public List<FeedbackResult> getAllowedDecisions() {
			return allowedDecisions;
		}

		public void setAllowedDecisions(List<FeedbackResult> allowedDecisions) {
			this.allowedDecisions = allowedDecisions;
		}

		public String getResponse() {
			return response;
		}

		public void setResponse(String response) {
			this.response = response;
		}

		/**
		 * Enum for ToolFeedback result.
		 */
		public enum FeedbackResult {
			APPROVED,
			REJECTED,
			EDITED,
			RESPONDED
		}
	}

	/**
	 * DTO for ToolCall within AssistantMessage.
	 */
	public static class ToolCallDTO {
		@JsonProperty("id")
		private String id;

		@JsonProperty("type")
		private String type;

		@JsonProperty("name")
		private String name;

		@JsonProperty("arguments")
		private String arguments;

		public ToolCallDTO() {
		}

		public ToolCallDTO(AssistantMessage.ToolCall toolCall) {
			this.id = toolCall.id();
			this.type = toolCall.type();
			this.name = toolCall.name();
			this.arguments = toolCall.arguments();
		}

		public AssistantMessage.ToolCall toToolCall() {
			return new AssistantMessage.ToolCall(this.id, this.type, this.name, this.arguments);
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
	}
}

