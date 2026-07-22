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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
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

	/**
	 * Indicates this response comes from a sub-graph (sub-agent) within a
	 * ParallelAgent / SequentialAgent. The frontend uses this flag to group
	 * streaming chunks by {@code agent} name into separate cards.
	 */
	@JsonProperty("subGraph")
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private boolean subGraph;

	/**
	 * Parallel sub-agent results. Present only when this response comes from a
	 * ParallelAgent node whose state contains multiple named AssistantMessage results.
	 * Key = sub-agent result name (e.g. "poem_result"), Value = sub-agent result.
	 */
	@JsonProperty("subAgents")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Map<String, SubAgentResult> subAgents;

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

	/**
	 * Constructor with parallel sub-agent results.
	 */
	public AgentRunResponse(String node, String agent, Usage tokenUsage, Map<String, SubAgentResult> subAgents) {
		this.node = node;
		this.agent = agent;
		this.tokenUsage = tokenUsage;
		this.subAgents = subAgents;
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

	public Map<String, SubAgentResult> getSubAgents() {
		return subAgents;
	}

	public void setSubAgents(Map<String, SubAgentResult> subAgents) {
		this.subAgents = subAgents;
	}

	public boolean isSubGraph() {
		return subGraph;
	}

	public void setSubGraph(boolean subGraph) {
		this.subGraph = subGraph;
	}

	/**
	 * Represents a single parallel sub-agent's result.
	 */
	public static class SubAgentResult {

		@JsonProperty("name")
		private String name;

		@JsonProperty("message")
		private MessageDTO message;

		@JsonProperty("chunk")
		private String chunk;

		public SubAgentResult() {
		}

		public SubAgentResult(String name, MessageDTO message, String chunk) {
			this.name = name;
			this.message = message;
			this.chunk = chunk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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
	}
}
