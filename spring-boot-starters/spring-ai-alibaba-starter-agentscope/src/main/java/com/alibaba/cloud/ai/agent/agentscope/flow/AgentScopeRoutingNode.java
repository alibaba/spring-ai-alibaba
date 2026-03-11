/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.agent.agentscope.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeMessageUtils;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.MultiCommandAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingNode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

/**
 * Routing node that uses an AgentScope {@link ReActAgent} (assembled with a Model and
 * system prompt) to make LLM-based routing decisions. Equivalent to the framework's
 * {@link RoutingNode} which uses {@link org.springframework.ai.chat.client.ChatClient}
 * and {@link org.springframework.ai.converter.BeanOutputConverter}; here we use
 * ReActAgent's {@link ReActAgent#call(java.util.List, Class)} to obtain structured
 * {@link RoutingNode.RoutingDecision} output. No direct Model calls—all invocation
 * goes through the ReActAgent.
 */
public class AgentScopeRoutingNode implements MultiCommandAction {

	private static final Logger logger = LoggerFactory.getLogger(AgentScopeRoutingNode.class);
	private static final int DEFAULT_MAX_RETRIES = 2;

	private final ReActAgent.Builder reactAgentBuilder;
	private final Agent rootAgent;
	private final List<Agent> subAgents;
	private final String systemPrompt;

	public AgentScopeRoutingNode(io.agentscope.core.model.Model model, Agent rootAgent, List<Agent> subAgents,
			String systemPrompt) {
		this.rootAgent = rootAgent;
		this.subAgents = subAgents;
		this.systemPrompt = systemPrompt != null ? systemPrompt : buildDefaultSystemPrompt(subAgents);
		this.reactAgentBuilder = ReActAgent.builder()
				.name("routing")
				.model(model)
				.sysPrompt(this.systemPrompt)
				.memory(new InMemoryMemory());
	}

	private static String buildDefaultSystemPrompt(List<Agent> subAgents) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system.\n\n");
		sb.append("Analyze the query and determine which specialized agents to delegate to.\n");
		sb.append("For each relevant agent, generate a targeted sub-question optimized for that agent's capabilities.\n\n");
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
		sb.append("Example for multiple agents: {\"agents\":[{\"agent\":\"prose_writer_agent\",\"query\":\"...\"},{\"agent\":\"code_reviewer_agent\",\"query\":\"...\"}]}\n");
		sb.append("Do not include markdown or explanation, only the JSON object.");
		return sb.toString();
	}

	@Override
	public MultiCommand apply(OverAllState state, RunnableConfig config) throws Exception {
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		List<Message> messagesWithInstruction = prepareMessagesWithInstruction(messages);
		RoutingNode.RoutingDecision decision = getDecisionWithRetry(messagesWithInstruction, DEFAULT_MAX_RETRIES);
		List<String> decisionValues = decision.getAgentNames();

		List<String> invalidAgents = decisionValues.stream()
				.filter(agentName -> subAgents.stream().noneMatch(a -> a.name().equals(agentName)))
				.collect(Collectors.toList());

		if (invalidAgents.isEmpty() && !decisionValues.isEmpty()) {
			if (decisionValues.size() == 1) {
				logger.info("AgentScopeRoutingAgent {} routed to single sub-agent {}.", rootAgent.name(), decisionValues.get(0));
			} else {
				logger.info("AgentScopeRoutingAgent {} routed to {} sub-agents in parallel: {}.",
						rootAgent.name(), decisionValues.size(), String.join(", ", decisionValues));
			}
			Map<String, Object> stateUpdate = new HashMap<>();
			decision.getAgentQueries().forEach((agentName, query) ->
					stateUpdate.put(agentName + "_input", query));
			return new MultiCommand(decisionValues, stateUpdate);
		}
		throw new IllegalStateException(
				"AgentScopeRoutingAgent " + rootAgent.name() + " failed to get valid decision after retries. Invalid agents: " + invalidAgents + ".");
	}

	private List<Message> prepareMessagesWithInstruction(List<Message> messages) {
		List<Message> out = new ArrayList<>(messages);
		String instruction = getInstruction();
		if (StringUtils.hasLength(instruction)) {
			out.add(new UserMessage(instruction));
		} else {
			out.add(new UserMessage(
					"Based on the chat history and current task progress, please decide the next agent to delegate the task to."));
		}
		return out;
	}

	private String getInstruction() {
		if (rootAgent instanceof AgentScopeRoutingAgent scopeAgent) {
			return scopeAgent.getInstruction();
		}
		return null;
	}

	private RoutingNode.RoutingDecision getDecisionWithRetry(List<Message> messages, int maxRetries) throws Exception {
		List<String> lastInvalidDecision = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				List<Message> messagesToSend = messages;
				if (attempt > 0) {
					String errorFeedback = String.format(
							"Previous attempt returned invalid agent names: %s. Please choose from the available agents: %s.",
							lastInvalidDecision != null ? String.join(", ", lastInvalidDecision) : "[]",
							String.join(", ", subAgents.stream().map(Agent::name).toList()));
					logger.warn("AgentScopeRoutingAgent {} retry attempt {}/{}. Previous invalid decision: {}",
							rootAgent.name(), attempt, maxRetries, lastInvalidDecision);
					messagesToSend = new ArrayList<>(messages);
					messagesToSend.add(new UserMessage(errorFeedback));
				}

				List<Msg> agentScopeMsgs = AgentScopeMessageUtils.toAgentScopeMessages(messagesToSend);
				ReActAgent reactAgent = reactAgentBuilder.build();
				Msg response = reactAgent.call(agentScopeMsgs, RoutingDecisionSchema.class).block();
				if (response == null) {
					lastInvalidDecision = Collections.emptyList();
					continue;
				}
				RoutingDecisionSchema schema = response.getStructuredData(RoutingDecisionSchema.class);
				if (schema == null || schema.agents == null) {
					lastInvalidDecision = Collections.emptyList();
					continue;
				}
				RoutingNode.RoutingDecision decision = toRoutingDecision(schema);

				List<String> decisionValues = decision.getAgentNames();
				if (decisionValues != null && !decisionValues.isEmpty()) {
					List<String> invalidAgents = decisionValues.stream()
							.filter(agentName -> subAgents.stream().noneMatch(a -> a.name().equals(agentName)))
							.collect(Collectors.toList());
					if (invalidAgents.isEmpty()) {
						if (attempt > 0) {
							logger.info("AgentScopeRoutingAgent {} succeeded on retry attempt {}. Routed to: {}",
									rootAgent.name(), attempt, String.join(", ", decisionValues));
						}
						return decision;
					}
					lastInvalidDecision = decisionValues;
				} else {
					lastInvalidDecision = Collections.emptyList();
				}
			} catch (Exception e) {
				if (attempt == maxRetries) {
					logger.error("AgentScopeRoutingAgent {} failed on final attempt {}/{}", rootAgent.name(), attempt, maxRetries, e);
					throw e;
				}
				logger.warn("AgentScopeRoutingAgent {} attempt {}/{} encountered an error, will retry", rootAgent.name(), attempt, maxRetries, e);
			}
		}
		throw new IllegalStateException(
				String.format("Failed to get valid decision after %d retries. Last invalid decision: %s", maxRetries, lastInvalidDecision));
	}

	private static RoutingNode.RoutingDecision toRoutingDecision(RoutingDecisionSchema schema) {
		List<RoutingNode.AgentRouting> list = new ArrayList<>();
		for (RoutingDecisionSchema.AgentRoutingSchema e : schema.agents) {
			if (e != null && StringUtils.hasText(e.agent)) {
				list.add(new RoutingNode.AgentRouting(e.agent, e.query != null ? e.query : ""));
			}
		}
		return new RoutingNode.RoutingDecision(list);
	}
}
