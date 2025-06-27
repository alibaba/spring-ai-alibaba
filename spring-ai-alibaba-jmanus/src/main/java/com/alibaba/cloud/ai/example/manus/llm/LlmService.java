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
package com.alibaba.cloud.ai.example.manus.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

	private static final Logger log = LoggerFactory.getLogger(LlmService.class);

	private final ChatClient agentExecutionClient;

	private final ChatClient planningChatClient;

	private final ChatClient finalizeChatClient;

	private final ChatMemory conversationMemory = MessageWindowChatMemory.builder().maxMessages(1000).build();

	private final ChatMemory agentMemory = MessageWindowChatMemory.builder().maxMessages(1000).build();

	private final ChatModel chatModel;

	public LlmService(ChatModel chatModel) {

		this.chatModel = chatModel;
		// 执行和总结规划，用相同的memory
		this.planningChatClient = ChatClient.builder(chatModel)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().temperature(0.1).build())
			.build();

		// // 每个agent执行过程中，用独立的memroy

		this.agentExecutionClient = ChatClient.builder(chatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.finalizeChatClient = ChatClient.builder(chatModel)
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();

	}

	public ChatClient getAgentChatClient() {
		return agentExecutionClient;
	}

	public ChatMemory getAgentMemory() {
		return agentMemory;
	}

	public void clearAgentMemory(String planId) {
		this.agentMemory.clear(planId);
	}

	public ChatClient getPlanningChatClient() {
		return planningChatClient;
	}

	public void clearConversationMemory(String planId) {
		this.conversationMemory.clear(planId);
	}

	public ChatClient getFinalizeChatClient() {
		return finalizeChatClient;
	}

	public ChatModel getChatModel() {
		return chatModel;
	}

	public ChatMemory getConversationMemory() {
		return conversationMemory;
	}

}
