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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.MultiCommandAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routing node that makes LLM-based routing decisions.
 * This node is designed to work with hooks - hooks can execute before this node
 * to trim or modify the message history before the routing decision is made.
 * Supports both single and multiple agent routing for parallel execution.
 */
public class RoutingNode implements MultiCommandAction {
	private static final Logger logger = LoggerFactory.getLogger(RoutingNode.class);
	private static final int DEFAULT_MAX_RETRIES = 2;

	private final ChatClient chatClient;
	private final BeanOutputConverter<RoutingDecision> outputConverter;
	private final Agent rootAgent;
	private final List<Agent> subAgents;

	public RoutingNode(ChatModel chatModel, Agent rootAgent, List<Agent> subAgents) {
		this.rootAgent = rootAgent;
		this.subAgents = subAgents;

		StringBuilder sb = new StringBuilder();
		if (rootAgent instanceof LlmRoutingAgent llmRoutingAgent && StringUtils.hasLength(llmRoutingAgent.getSystemPrompt())) {
			sb.append("You are responsible for task routing in a graph-based AI system.\n");
			sb.append("The instruction that you should follow to finish this task is:\n\n ");
			sb.append(llmRoutingAgent.getSystemPrompt());
		}
		else {
			sb.append("You are responsible for task routing in a graph-based AI system.\n");
			sb.append("\n\n");
			sb.append("Analyze the query and determine which specialized agents to delegate to.\n");
			sb.append("For each relevant agent, generate a targeted sub-question optimized for that agent's capabilities.\n");
			sb.append("\n");
			sb.append("Available agents:\n");
			for (Agent agent : subAgents) {
				sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
			}
			sb.append("\n");
			sb.append("Return ONLY the agents that are relevant to the query. Each agent should have a targeted sub-question.\n");
			sb.append("You can return one or multiple agents. If multiple agents are returned, they will execute in parallel.\n");
			sb.append("Available names: ");
			sb.append(String.join(", ", subAgents.stream().map(Agent::name).toList()));
			sb.append("\n\n");
			sb.append("Respond with a JSON object containing an \"agents\" array. Each element has \"agent\" and \"query\" keys.\n");
			sb.append("Example for single agent: {\"agents\":[{\"agent\":\"prose_writer_agent\",\"query\":\"What prose style should be used?\"}]}\n");
			sb.append("Example for multiple agents: {\"agents\":[{\"agent\":\"prose_writer_agent\",\"query\":\"What prose style should be used?\"},{\"agent\":\"code_reviewer_agent\",\"query\":\"What code review criteria apply?\"}]}\n");
			sb.append("Do not include markdown or explanation, only the JSON object.");
		}

		// Create BeanOutputConverter for structured output
		this.outputConverter = new BeanOutputConverter<>(RoutingDecision.class);
		sb.append("\n\n");
		sb.append(this.outputConverter.getFormat());

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
	}

	@Override
	public MultiCommand apply(OverAllState state, RunnableConfig config) throws Exception {
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		
		// Prepare messages with instruction if available
		List<Message> messagesWithInstruction = prepareMessagesWithInstruction(messages);
		
		RoutingDecision decision = getDecisionWithRetry(messagesWithInstruction, DEFAULT_MAX_RETRIES);
		List<String> decisionValues = decision.getAgentNames();

		// Validate all agent names are valid
		List<String> invalidAgents = decisionValues.stream()
				.filter(agentName -> subAgents.stream().noneMatch(agent -> agent.name().equals(agentName)))
				.collect(Collectors.toList());
				
		if (invalidAgents.isEmpty() && !decisionValues.isEmpty()) {
			if (decisionValues.size() == 1) {
				logger.info("RoutingAgent {} routed to single sub-agent {}.", rootAgent.name(), decisionValues.get(0));
			} else {
				logger.info("RoutingAgent {} routed to {} sub-agents in parallel: {}.", 
						rootAgent.name(), decisionValues.size(), String.join(", ", decisionValues));
			}
			
			// Return MultiCommand with the routing decisions as gotoNodes and agent queries in state.
			// Each agent's query is stored as independent key: agentName_input
			Map<String, Object> stateUpdate = new HashMap<>();
			decision.getAgentQueries().forEach((agentName, query) ->
					stateUpdate.put(agentName + "_input", query));
			return new MultiCommand(decisionValues, stateUpdate);
		}
		else {
			logger.error("RoutingAgent {} failed to get valid decision after {} retries. Invalid agents: {}.",
					rootAgent.name(), DEFAULT_MAX_RETRIES, invalidAgents);
			throw new IllegalStateException(
					"RoutingAgent " + rootAgent.name() + " failed to get valid decision after retries. Invalid agents: " + invalidAgents + ".");
		}
	}

	/**
	 * Prepares messages with instruction. If rootAgent has instruction, adds it as UserMessage.
	 * Otherwise, adds a default instruction message.
	 */
	private List<Message> prepareMessagesWithInstruction(List<Message> messages) {
		List<Message> messagesWithInstruction = new ArrayList<>(messages);
		
		// Check if rootAgent is LlmRoutingAgent and has instruction
		if (rootAgent instanceof LlmRoutingAgent llmRoutingAgent) {
			String instruction = llmRoutingAgent.getInstruction();
			if (StringUtils.hasLength(instruction)) {
				messagesWithInstruction.add(new UserMessage(instruction));
			}
			else {
				messagesWithInstruction.add(new UserMessage(
						"Based on the chat history and current task progress, please decide the next agent to delegate the task to."));
			}
		}
		else {
			messagesWithInstruction.add(new UserMessage(
					"Based on the chat history and current task progress, please decide the next agent to delegate the task to."));
		}
		
		return messagesWithInstruction;
	}

	/**
	 * Gets a valid routing decision with retry logic.
	 * Returns RoutingDecision containing agent names and their targeted sub-queries.
	 */
	private RoutingDecision getDecisionWithRetry(List<Message> messages, int maxRetries) throws Exception {
		List<String> lastInvalidDecision = null;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				RoutingDecision decision;

				if (attempt == 0) {
					decision = this.chatClient.prompt().messages(messages).call().entity(this.outputConverter);
				}
				else {
					String errorFeedback = String.format(
							"Previous attempt returned invalid agent names: %s. " +
									"Please choose from the available agents: %s.",
							lastInvalidDecision != null ? String.join(", ", lastInvalidDecision) : "[]",
							String.join(", ", subAgents.stream().map(Agent::name).toList()));

					logger.warn("RoutingAgent {} retry attempt {}/{}. Previous invalid decision: {}",
							rootAgent.name(), attempt, maxRetries, lastInvalidDecision);

					List<Message> messagesWithFeedback = new ArrayList<>();
					boolean systemMessageFound = false;

					for (Message msg : messages) {
						if (msg instanceof SystemMessage && !systemMessageFound) {
							String enhancedContent = msg.getText() + "\n\n" + errorFeedback;
							messagesWithFeedback.add(new SystemMessage(enhancedContent));
							systemMessageFound = true;
						}
						else {
							messagesWithFeedback.add(msg);
						}
					}

					if (!systemMessageFound) {
						messagesWithFeedback.add(new UserMessage(errorFeedback));
					}

					decision = this.chatClient.prompt().messages(messagesWithFeedback).call()
							.entity(this.outputConverter);
				}

				List<String> decisionValues = decision.getAgentNames();

				if (decisionValues != null && !decisionValues.isEmpty()) {
					// Validate all agent names
					List<String> invalidAgents = decisionValues.stream()
							.filter(agentName -> subAgents.stream().noneMatch(agent -> agent.name().equals(agentName)))
							.collect(Collectors.toList());

					if (invalidAgents.isEmpty()) {
						if (attempt > 0) {
							logger.info("RoutingAgent {} succeeded on retry attempt {}. Routed to sub-agents: {}",
									rootAgent.name(), attempt, String.join(", ", decisionValues));
						}
						return decision;
					}
					else {
						lastInvalidDecision = decisionValues;
						logger.warn("RoutingAgent {} attempt {}/{} returned invalid agent names: {}",
								rootAgent.name(), attempt, maxRetries, invalidAgents);
					}
				}
				else {
					lastInvalidDecision = Collections.emptyList();
					logger.warn("RoutingAgent {} attempt {}/{} returned empty agent list",
							rootAgent.name(), attempt, maxRetries);
				}
			}
			catch (Exception e) {
				if (attempt == maxRetries) {
					logger.error("RoutingAgent {} failed on final attempt {}/{}", rootAgent.name(), attempt, maxRetries, e);
					throw e;
				}
				logger.warn("RoutingAgent {} attempt {}/{} encountered an error, will retry", rootAgent.name(), attempt, maxRetries, e);
			}
		}

		throw new IllegalStateException(
				String.format("Failed to get valid decision after %d retries. Last invalid decision: %s",
						maxRetries, lastInvalidDecision));
	}

	/**
	 * Response record for structured routing decision output.
	 * Each agent has a targeted sub-question optimized for that agent's capabilities.
	 */
	public record RoutingDecision(List<AgentRouting> agents) {

		public RoutingDecision {
			agents = agents != null ? agents : Collections.emptyList();
		}

		/**
		 * Gets the list of agent names for routing.
		 */
		public List<String> getAgentNames() {
			return agents.stream()
					.filter(a -> a != null && StringUtils.hasText(a.agent()))
					.map(AgentRouting::agent)
					.toList();
		}

		/**
		 * Gets the map of agent name to targeted sub-query for downstream use.
		 */
		public Map<String, String> getAgentQueries() {
			return agents.stream()
					.filter(a -> a != null && StringUtils.hasText(a.agent()))
					.collect(Collectors.toMap(AgentRouting::agent, a -> a.query() != null ? a.query() : "", (a, b) -> a));
		}
	}

	/**
	 * Single agent routing entry with targeted sub-question.
	 */
	public record AgentRouting(String agent, String query) {
	}
}

