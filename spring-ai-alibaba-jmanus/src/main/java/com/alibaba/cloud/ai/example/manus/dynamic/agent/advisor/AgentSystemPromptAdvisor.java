/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author azir
 * @since 2025/06/10
 */
public class AgentSystemPromptAdvisor implements BaseChatMemoryAdvisor {

	private final SystemMessage systemMessage;

	private final ChatMemory chatMemory;

	private final String defaultConversationId = ChatMemory.DEFAULT_CONVERSATION_ID;

	public AgentSystemPromptAdvisor(List<Message> systemMessageList, ChatMemory chatMemory) {
		String systemPromptText = systemMessageList.stream()
			.filter(message -> message instanceof SystemMessage)
			.map(Message::getText)
			.collect(Collectors.joining(System.lineSeparator()));
		this.systemMessage = new SystemMessage(systemPromptText);
		this.chatMemory = chatMemory;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		// Add system message to the first position of the message.
		List<Message> processedMessages = new ArrayList<>();
		processedMessages.add(systemMessage);
		// Retrieve the chat memory for the current conversation.
		String conversationId = getConversationId(chatClientRequest.context(), this.defaultConversationId);
		List<Message> memoryMessages = this.chatMemory.get(conversationId);
		processedMessages.addAll(memoryMessages);
		processedMessages.addAll(chatClientRequest.prompt().getInstructions());
		// Do not add current user messages to the conversation memory(env data message).
		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
			.build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		// @see MessageChatMemoryAdvisor#after()
		List<Message> assistantMessages = new ArrayList<>();
		if (chatClientResponse.chatResponse() != null) {
			assistantMessages = chatClientResponse.chatResponse()
				.getResults()
				.stream()
				.map(g -> (Message) g.getOutput())
				.toList();
		}
		this.chatMemory.add(this.getConversationId(chatClientResponse.context(), this.defaultConversationId),
				assistantMessages);
		return chatClientResponse;
	}

	@Override
	public int getOrder() {
		return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;
	}

}
