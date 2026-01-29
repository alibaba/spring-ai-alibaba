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
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class SupervisorEdgeAction implements AsyncEdgeAction {
	private static final Logger logger = LoggerFactory.getLogger(SupervisorEdgeAction.class);

	private static final int DEFAULT_MAX_RETRIES = 2;

	private final ChatClient chatClient;
	private final BeanOutputConverter<SupervisorDecision> outputConverter;
	private final Agent rootAgent;
	private final List<Agent> subAgents;

	public SupervisorEdgeAction(ChatModel chatModel, Agent rootAgent, List<Agent> subAgents) {
		this.rootAgent = rootAgent;
		this.subAgents = subAgents;

		StringBuilder sb = new StringBuilder();

		if (rootAgent instanceof SupervisorAgent supervisorAgent && StringUtils.hasLength(supervisorAgent.getSystemPrompt())) {
			sb.append("You are a supervisor agent responsible for task routing and completion decisions.\n Break down user requests into appropriate subtasks and delegate them to specialized agents when necessary.\n");
			sb.append("The instruction that you should follow to finish this task is:\n\n ");
			sb.append(supervisorAgent.getSystemPrompt());
		}
		else {
			sb.append("You are a supervisor agent responsible for task routing and completion decisions.\n Break down user requests into appropriate subtasks and delegate them to specialized agents when necessary.\n");

			sb.append("\n\n");
			sb.append("You have access to some specialized agents that can handle this task. You must decide:\n");
			sb.append("1. Delegate each subtask to ONE of the following agents, OR\n");
			sb.append("2. Mark the task as complete (FINISH) if no further action is needed.\n");
			sb.append("\n");
			sb.append("The available agents and their capabilities are listed below:\n");
			for (Agent agent : subAgents) {
				sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
			}
		}

		sb.append("\n");
		sb.append("You can also return \"FINISH\" to indicate that the task is complete and no further agent action is needed.\n");
		sb.append("\n");
		sb.append("Available options: ");
		sb.append(String.join(", ", subAgents.stream().map(Agent::name).toList()));
		sb.append(", FINISH");
		sb.append("\n\n");
		sb.append("Example: prose_writer_agent");
		sb.append("\n");
		sb.append("Example: FINISH");

		// Create BeanOutputConverter for structured output
		this.outputConverter = new BeanOutputConverter<>(SupervisorDecision.class);
		sb.append("\n\n");
		sb.append(this.outputConverter.getFormat());

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			
			// Prepare messages with instruction if available
			List<Message> messagesWithInstruction = prepareMessagesWithInstruction(messages);
			
			String decisionValue = getDecisionWithRetry(messagesWithInstruction, DEFAULT_MAX_RETRIES);

			// Validate the decision
			if ("FINISH".equalsIgnoreCase(decisionValue)) {
				logger.info("Supervisor {} decided to finish the task.", rootAgent.name());
				result.complete(END);
			}
			else {
				// Check if it's a valid sub-agent name
				boolean isValidAgent = subAgents.stream()
						.anyMatch(agent -> agent.name().equals(decisionValue));
				if (isValidAgent) {
					logger.info("Supervisor {} delegated to sub-agent {}.", rootAgent.name(), decisionValue);
					result.complete(decisionValue);
				}
				else {
					logger.error("Supervisor {} failed to get valid decision after {} retries. Last invalid decision: {}.",
							rootAgent.name(), DEFAULT_MAX_RETRIES, decisionValue);
					result.completeExceptionally(new IllegalArgumentException(
							"Invalid routing decision after retries: " + decisionValue + ". Must be one of the sub-agents or FINISH."));
				}
			}
		}
		catch (Exception e) {
			logger.error("Error during supervisor decision: ", e);
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Prepares messages with instruction. If rootAgent has instruction, adds it as UserMessage.
	 * Otherwise, adds a default instruction message.
	 * @param messages the original conversation messages
	 * @return messages list with instruction added
	 */
	private List<Message> prepareMessagesWithInstruction(List<Message> messages) {
		java.util.ArrayList<Message> messagesWithInstruction = new java.util.ArrayList<>(messages);
		
		// Check if rootAgent is SupervisorAgent and has instruction
		if (rootAgent instanceof SupervisorAgent supervisorAgent) {
			String instruction = supervisorAgent.getInstruction();
			if (StringUtils.hasLength(instruction)) {
				// If instruction is set, add it as UserMessage
				messagesWithInstruction.add(new UserMessage(instruction));
			}
			else {
				// If no instruction, add default message
				messagesWithInstruction.add(new UserMessage(
						"Based on the chat history and current task progress, please decide the next agent to delegate the task to, or return FINISH if you think the task is complete."));
			}
		}
		else {
			// If rootAgent is not SupervisorAgent, add default message
			messagesWithInstruction.add(new UserMessage(
					"Based on the chat history and current task progress, please decide the next agent to delegate the task to, or return FINISH if you think the task is complete."));
		}
		
		return messagesWithInstruction;
	}

	/**
	 * Gets a valid routing decision with retry logic. If the model returns an invalid agent name,
	 * it will retry up to maxRetries times before giving up.
	 * @param messages the conversation messages
	 * @param maxRetries maximum number of retries (default: 2)
	 * @return a valid decision (agent name or "FINISH")
	 * @throws Exception if all retries fail or other errors occur
	 */
	private String getDecisionWithRetry(List<Message> messages, int maxRetries) throws Exception {
		String lastInvalidDecision = null;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				SupervisorDecision decision;

				if (attempt == 0) {
					// First attempt: use original messages
					decision = this.chatClient.prompt().messages(messages).call().entity(this.outputConverter);
				}
				else {
					// Retry attempts: add error feedback to help the model correct its decision
					String errorFeedback = String.format(
							"Previous attempt returned an invalid agent name '%s'. " +
									"Please choose from the available agents: %s, or return 'FINISH' if the task is complete.",
							lastInvalidDecision,
							String.join(", ", subAgents.stream().map(Agent::name).toList()));

					logger.warn("Supervisor {} retry attempt {}/{}. Previous invalid decision: {}",
							rootAgent.name(), attempt, maxRetries, lastInvalidDecision);

					// Create a new message list with error feedback
					// Try to append to existing SystemMessage, otherwise use UserMessage
					java.util.ArrayList<Message> messagesWithFeedback = new java.util.ArrayList<>();
					boolean systemMessageFound = false;

					for (Message msg : messages) {
						if (msg instanceof SystemMessage && !systemMessageFound) {
							// Append error feedback to the first SystemMessage found
							String enhancedContent = msg.getText() + "\n\n" + errorFeedback;
							messagesWithFeedback.add(new SystemMessage(enhancedContent));
							systemMessageFound = true;
						}
						else {
							messagesWithFeedback.add(msg);
						}
					}

					// If no SystemMessage was found, add error feedback as UserMessage
					if (!systemMessageFound) {
						messagesWithFeedback.add(new UserMessage(errorFeedback));
					}

					decision = this.chatClient.prompt().messages(messagesWithFeedback).call()
							.entity(this.outputConverter);
				}

				String decisionValue = decision.agent();

				// Validate the decision
				if ("FINISH".equalsIgnoreCase(decisionValue)) {
					if (attempt > 0) {
						logger.info("Supervisor {} succeeded on retry attempt {}. Decision: FINISH",
								rootAgent.name(), attempt);
					}
					return decisionValue;
				}

				// Check if it's a valid sub-agent name
				boolean isValidAgent = subAgents.stream()
						.anyMatch(agent -> agent.name().equals(decisionValue));

				if (isValidAgent) {
					if (attempt > 0) {
						logger.info("Supervisor {} succeeded on retry attempt {}. Delegated to sub-agent: {}",
								rootAgent.name(), attempt, decisionValue);
					}
					return decisionValue;
				}
				else {
					// Invalid agent name, store for next retry
					lastInvalidDecision = decisionValue;
					logger.warn("Supervisor {} attempt {}/{} returned invalid agent name: {}",
							rootAgent.name(), attempt, maxRetries, decisionValue);
				}
			}
			catch (Exception e) {
				if (attempt == maxRetries) {
					// Last attempt failed, rethrow the exception
					logger.error("Supervisor {} failed on final attempt {}/{}", rootAgent.name(), attempt, maxRetries, e);
					throw e;
				}
				logger.warn("Supervisor {} attempt {}/{} encountered an error, will retry", rootAgent.name(), attempt, maxRetries, e);
			}
		}

		// All retries exhausted
		throw new IllegalStateException(
				String.format("Failed to get valid decision after %d retries. Last invalid decision: %s",
						maxRetries, lastInvalidDecision));
	}

	/**
	 * Response record for structured supervisor decision output.
	 * The agent field can be either a sub-agent name or "FINISH" to indicate completion.
	 */
	public record SupervisorDecision(String agent) { }

}

