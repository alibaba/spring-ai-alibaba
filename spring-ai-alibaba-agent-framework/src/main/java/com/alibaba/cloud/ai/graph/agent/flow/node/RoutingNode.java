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
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Routing node that makes LLM-based routing decisions.
 * This node is designed to work with hooks - hooks can execute before this node
 * to trim or modify the message history before the routing decision is made.
 */
public class RoutingNode implements AsyncNodeActionWithConfig {
	private static final Logger logger = LoggerFactory.getLogger(RoutingNode.class);
	private static final int DEFAULT_MAX_RETRIES = 2;
	private static final String ROUTING_DECISION_KEY = "__routing_decision__";

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
			sb.append(
					"You have access to some specialized agents that can handle this task. You must delegate the task to ONE of the following agents.\n");
			sb.append("The available agents and their capabilities are listed below:\n");
			for (Agent agent : subAgents) {
				sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
			}
			sb.append("\n");
			sb.append("Return ONLY the exact agent name from the list above, without any explanation or additional text.\n");
			sb.append("Available names: ");
			sb.append(String.join(", ", subAgents.stream().map(Agent::name).toList()));
			sb.append("\n\n");
			sb.append("Example: prose_writer_agent");
		}

		// Create BeanOutputConverter for structured output
		this.outputConverter = new BeanOutputConverter<>(RoutingDecision.class);
		sb.append("\n\n");
		sb.append(this.outputConverter.getFormat());

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
	}

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
		
		try {
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			
			// Prepare messages with instruction if available
			List<Message> messagesWithInstruction = prepareMessagesWithInstruction(messages);
			
			String decisionValue = getDecisionWithRetry(messagesWithInstruction, DEFAULT_MAX_RETRIES);

			// Check if it's a valid sub-agent name
			boolean isValidAgent = subAgents.stream()
					.anyMatch(agent -> agent.name().equals(decisionValue));
					
			if (isValidAgent) {
				logger.info("RoutingAgent {} routed to sub-agent {}.", rootAgent.name(), decisionValue);
				
				// Store the routing decision in state
				Map<String, Object> updates = new HashMap<>();
				updates.put(ROUTING_DECISION_KEY, decisionValue);
				result.complete(updates);
			}
			else {
				logger.error("RoutingAgent {} failed to get valid decision after {} retries. Last invalid decision: {}.",
						rootAgent.name(), DEFAULT_MAX_RETRIES, decisionValue);
				result.completeExceptionally(new IllegalStateException(
						"RoutingAgent " + rootAgent.name() + " failed to get valid decision after retries. Last invalid decision: " + decisionValue + "."));
			}
		}
		catch (Exception e) {
			logger.error("Error during routing decision: ", e);
			result.completeExceptionally(e);
		}
		
		return result;
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
	 */
	private String getDecisionWithRetry(List<Message> messages, int maxRetries) throws Exception {
		String lastInvalidDecision = null;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				RoutingDecision decision;

				if (attempt == 0) {
					decision = this.chatClient.prompt().messages(messages).call().entity(this.outputConverter);
				}
				else {
					String errorFeedback = String.format(
							"Previous attempt returned an invalid agent name '%s'. " +
									"Please choose from the available agents: %s.",
							lastInvalidDecision,
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

				String decisionValue = decision.agent();
				boolean isValidAgent = subAgents.stream()
						.anyMatch(agent -> agent.name().equals(decisionValue));

				if (isValidAgent) {
					if (attempt > 0) {
						logger.info("RoutingAgent {} succeeded on retry attempt {}. Routed to sub-agent: {}",
								rootAgent.name(), attempt, decisionValue);
					}
					return decisionValue;
				}
				else {
					lastInvalidDecision = decisionValue;
					logger.warn("RoutingAgent {} attempt {}/{} returned invalid agent name: {}",
							rootAgent.name(), attempt, maxRetries, decisionValue);
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
	 * Get the key used to store routing decision in state
	 */
	public static String getRoutingDecisionKey() {
		return ROUTING_DECISION_KEY;
	}

	/**
	 * Response record for structured routing decision output
	 */
	public record RoutingDecision(String agent) { }
}

