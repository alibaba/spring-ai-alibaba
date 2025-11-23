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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

public class RoutingEdgeAction implements AsyncEdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(RoutingEdgeAction.class);

	private final ChatClient chatClient;

	private final List<Agent> subAgents;

	public RoutingEdgeAction(ChatModel chatModel, Agent current, List<Agent> subAgents) {
		this.subAgents = subAgents;
		StringBuilder sb = new StringBuilder();
		sb.append("You are responsible for task routing in a graph-based AI system.\n");

		if (current instanceof ReactAgent reactAgent) {
			sb.append("The instruction that you should follow to finish this task is: ");
			sb.append(StringUtils.isEmpty(reactAgent.instruction()) ? reactAgent.description()
					: reactAgent.instruction());
		}
		else {
			sb.append("Your role seen by the user is: ");
			sb.append(current.description());
		}

		sb.append("\n\n");
		sb.append(
				"There are a few specialized agents that can handle this task. You must delegate the task to ONE of the following agents.\n");
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

		this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			List<Message> messages = (List<Message>)state.value("messages").orElseThrow();
			String rawResponse = this.chatClient.prompt(getFormatedPrompt(messages)).call().content();

			logger.debug("Raw routing response from ChatModel: '{}'", rawResponse);

			String parsedAgentName = parseAgentName(rawResponse);

			logger.info("Routing decision: '{}' -> parsed as '{}'", rawResponse, parsedAgentName);

			result.complete(parsedAgentName);
		}
		catch (Exception e) {
			logger.error("Error during routing decision", e);
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Parses and validates the agent name from ChatModel response.
	 * @param rawResponse the raw response from ChatModel
	 * @return the validated agent name
	 * @throws IllegalArgumentException if no valid agent can be determined
	 */
	private String parseAgentName(String rawResponse) {
		if (rawResponse == null || rawResponse.isBlank()) {
			throw new IllegalArgumentException("ChatModel returned empty response for routing decision");
		}

		String cleaned = rawResponse.trim();

		// Exact match
		for (Agent agent : subAgents) {
			if (agent.name().equals(cleaned)) {
				return agent.name();
			}
		}

		// Case-insensitive match
		for (Agent agent : subAgents) {
			if (agent.name().equalsIgnoreCase(cleaned)) {
				logger.warn("Routing response had incorrect case: '{}', expected: '{}'", cleaned, agent.name());
				return agent.name();
			}
		}

		// Substring match - sort by length to avoid partial matches
		String cleanedLower = cleaned.toLowerCase();
		Agent matchedAgent = subAgents.stream()
			.sorted((a1, a2) -> Integer.compare(a2.name().length(), a1.name().length()))
			.filter(agent -> cleanedLower.contains(agent.name().toLowerCase()))
			.findFirst()
			.orElse(null);

		if (matchedAgent != null) {
			logger.warn("Routing response contained extra text: '{}', extracted agent: '{}'", cleaned,
					matchedAgent.name());
			return matchedAgent.name();
		}

		String availableAgents = subAgents.stream().map(Agent::name).reduce((a, b) -> a + ", " + b).orElse("");

		throw new IllegalArgumentException(String.format(
				"Cannot determine routing target from ChatModel response: '%s'. " + "Available agents: [%s]. "
						+ "Please ensure the ChatModel returns exactly one of the available agent names.",
				rawResponse, availableAgents));
	}

	private String getFormatedPrompt(List<Message> messages) {
		if (messages == null || messages.isEmpty()) {
			return "Query from user:\n \n, Conversation History: \n <history></history> \n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Query from user:\n ");

		// Find the first UserMessage
		String firstMessageContent = "";
		for (Message message : messages) {
			if (message instanceof org.springframework.ai.chat.messages.UserMessage) {
				firstMessageContent = getMessageContent(message);
				break;
			}
		}

		sb.append(firstMessageContent != null ? firstMessageContent : "");
		sb.append(" \n, Conversation History: \n");
		sb.append("content below between <history></history> tag are conversation histories. \n <history>");

		// Convert remaining messages as history
		if (messages.size() > 1) {
			for (int i = 1; i < messages.size(); i++) {
				String messageContent = getMessageContent(messages.get(i));
				if (messageContent != null) {
					sb.append(messageContent);
				}
				// Add newline between messages (except for the last one)
				if (i < messages.size() - 1) {
					sb.append("\n");
				}
			}
		}

		sb.append("</history> \n");
		return sb.toString();
	}

	private String getMessageContent(Message message) {
		if (message instanceof org.springframework.ai.chat.messages.ToolResponseMessage toolMessage) {
			// Special handling for ToolResponseMessage
			StringBuilder toolContent = new StringBuilder();

			for (org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
				if (!toolContent.isEmpty()) {
					toolContent.append("\n");
				}
				toolContent.append("Tool Response [").append(response.id()).append("]: ");
				toolContent.append(response.responseData());
			}

			return toolContent.toString();
		}

		// For other message types, use getText() method
		return message.getText();
	}

}
