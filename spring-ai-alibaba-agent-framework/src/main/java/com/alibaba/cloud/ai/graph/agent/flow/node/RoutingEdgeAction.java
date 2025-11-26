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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;

public class RoutingEdgeAction implements AsyncEdgeAction {

	private final ChatClient chatClient;
    private final BeanOutputConverter<RoutingDecision> outputConverter;

	public RoutingEdgeAction(ChatModel chatModel, Agent current, List<Agent> subAgents) {
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
				"There're a few agents that can handle this task, you can delegate the task to one of the following.");
		sb.append("The agents ability are listed in a 'name:description' format as below:\n");
		for (Agent agent : subAgents) {
			sb.append("- ").append(agent.name()).append(": ").append(agent.description()).append("\n");
		}
		sb.append("\n\n");
		sb.append("Return the agent name to delegate the task to.");
		sb.append("\n\n");
		sb.append(
				"It should be emphasized that the returned result only requires the agent name and no other content.");
		sb.append("\n\n");
		sb.append(
				"For example, if you want to delegate the task to the agent named 'agent1', you should return 'agent1'.");

        // Create BeanOutputConverter for structured output
        this.outputConverter = new BeanOutputConverter<>(RoutingDecision.class);
        sb.append("\n\n");
        sb.append(this.outputConverter.getFormat());

        this.chatClient = ChatClient.builder(chatModel).defaultSystem(sb.toString()).build();
	}

	@Override
	public CompletableFuture<String> apply(OverAllState state) {
		CompletableFuture<String> result = new CompletableFuture<>();
		try {
			List<Message> messages = (List<Message>)state.value("messages").orElseThrow();
            RoutingDecision routingDecision = this.chatClient.prompt(getFormatedPrompt(messages)).call().entity(this.outputConverter);
			result.complete(routingDecision.agent());
		}
		catch (Exception e) {
			result.completeExceptionally(e);
		}
		return result;
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

    /**
     * Response record for structured routing decision output
     */
    public record RoutingDecision(String agent) {}
}
