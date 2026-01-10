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


import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentRunResponse {

	@JsonProperty("node")
	protected String node;

	@JsonProperty("agent")
	protected String agent;

	@JsonProperty("tokenUsage")
	protected Usage tokenUsage;

	/**
	 * The last message in DTO format for better serialization.
	 */
	@JsonProperty("message")
	protected MessageDTO message;

	@JsonProperty("chunk")
	private String chunk;

	AgentRunResponse() {
	}

	/**
	 * Constructor for creating response with Spring AI Message (will be converted to DTO).
	 */
	public AgentRunResponse(String node, String agent, Message message, Usage tokenUsage, String chunk) {
		this.node = node;
		this.agent = agent;
		this.message = message != null ? MessageDTO.MessageDTOFactory.fromMessage(message) : null;
		this.tokenUsage = tokenUsage;
		this.chunk = chunk;
	}

	/**
	 * Constructor for creating response with MessageDTO directly.
	 */
	public AgentRunResponse(String node, String agent, MessageDTO message, Usage tokenUsage, String chunk) {
		this.node = node;
		this.agent = agent;
		this.message = message;
		this.tokenUsage = tokenUsage;
		this.chunk = chunk;
	}

	// Public getters for Jackson serialization

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public Usage getTokenUsage() {
		return tokenUsage;
	}

	public void setTokenUsage(Usage tokenUsage) {
		this.tokenUsage = tokenUsage;
	}

	public MessageDTO getMessage() {
		return message;
	}

	public void setMessage(MessageDTO message) {
		this.message = message;
	}

	/**
	 * Helper method to get Spring AI Message from DTO.
	 */
	@JsonIgnore
	public Message getMessageAsSpringAI() {
		return message != null ? MessageDTO.MessageDTOFactory.toMessage(message) : null;
	}

	public String getChunk() {
		return chunk;
	}

	public void setChunk(String chunk) {
		this.chunk = chunk;
	}
}
