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
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class LlmService implements ILlmService {

	private static final Logger log = LoggerFactory.getLogger(LlmService.class);

	private final ChatClient agentExecutionClient;

	private final ChatClient planningChatClient;

	private final ChatClient finalizeChatClient;

	private ChatMemory conversationMemory;

	private ChatMemory agentMemory;

	private final ChatModel chatModel;

	public LlmService(ChatModel chatModel) {

		this.chatModel = chatModel;
		// Execute and summarize planning, use the same memory
		this.planningChatClient = ChatClient.builder(chatModel)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().temperature(0.1).build())
			.build();

		// Each agent execution process uses independent memory

		this.agentExecutionClient = ChatClient.builder(chatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.finalizeChatClient = ChatClient.builder(chatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();

	}

	public ChatClient getAgentChatClient() {
		return agentExecutionClient;
	}

	public ChatClient getDynamicChatClient(String host, String apiKey, String modelName) {
		OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(host).apiKey(apiKey).build();

		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().model(modelName).build();

		OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
			.openAiApi(openAiApi)
			.defaultOptions(chatOptions)
			.build();
		return ChatClient.builder(openAiChatModel)
			// .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();
	}

	public ChatMemory getAgentMemory(Integer maxMessages) {
		if (agentMemory == null) {
			agentMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
		}
		return agentMemory;
	}

	public void clearAgentMemory(String planId) {
		this.agentMemory.clear(planId);
	}

	public ChatClient getPlanningChatClient() {
		return planningChatClient;
	}

	public void clearConversationMemory(String planId) {
		if (this.conversationMemory == null) {
			// Default to 100 messages if not specified elsewhere
			this.conversationMemory = MessageWindowChatMemory.builder().maxMessages(100).build();
		}
		this.conversationMemory.clear(planId);
	}

	public ChatClient getFinalizeChatClient() {
		return finalizeChatClient;
	}

	public ChatModel getChatModel() {
		return chatModel;
	}

	public ChatMemory getConversationMemory(Integer maxMessages) {
		if (conversationMemory == null) {
			conversationMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
		}
		return conversationMemory;
	}

}
