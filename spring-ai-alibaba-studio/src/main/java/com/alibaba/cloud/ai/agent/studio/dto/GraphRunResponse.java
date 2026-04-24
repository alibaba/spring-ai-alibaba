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

package com.alibaba.cloud.ai.agent.studio.dto;

import com.alibaba.cloud.ai.agent.studio.dto.messages.MessageDTO;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Response DTO for Graph SSE stream. Extends AgentRunResponse semantics with
 * {@code state} for workflow observation and debugging. Each event contains the
 * node name, optional message/chunk, and the current overall state after that node.
 */
public class GraphRunResponse {

	@JsonProperty("node")
	private String node;

	@JsonProperty("agent")
	private String agent;

	@JsonProperty("tokenUsage")
	private Usage tokenUsage;

	@JsonProperty("message")
	private MessageDTO message;

	@JsonProperty("chunk")
	private String chunk;

	/**
	 * Current overall state after this node execution. Map of state keys to values.
	 * Enables workflow observation and debugging (e.g. LangGraph Studio-style UI).
	 */
	@JsonProperty("state")
	private Map<String, Object> state;

	public GraphRunResponse() {
	}

	public GraphRunResponse(String node, String agent, Message message, Usage tokenUsage, String chunk,
			Map<String, Object> state) {
		this.node = node;
		this.agent = agent;
		this.message = message != null ? MessageDTO.MessageDTOFactory.fromMessage(message) : null;
		this.tokenUsage = tokenUsage;
		this.chunk = chunk;
		this.state = state;
	}

	public GraphRunResponse(String node, String agent, MessageDTO message, Usage tokenUsage, String chunk,
			Map<String, Object> state) {
		this.node = node;
		this.agent = agent;
		this.message = message;
		this.tokenUsage = tokenUsage;
		this.chunk = chunk;
		this.state = state;
	}

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

	public String getChunk() {
		return chunk;
	}

	public void setChunk(String chunk) {
		this.chunk = chunk;
	}

	public Map<String, Object> getState() {
		return state;
	}

	public void setState(Map<String, Object> state) {
		this.state = state;
	}
}
