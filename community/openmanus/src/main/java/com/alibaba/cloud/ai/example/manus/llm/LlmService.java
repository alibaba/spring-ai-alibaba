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

import com.alibaba.cloud.ai.example.manus.tool.support.PromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

	private final ChatClient chatClient;

	private ChatMemory memory = new InMemoryChatMemory();

	private final ChatClient planningChatClient;

	private ChatMemory planningMemory = new InMemoryChatMemory();

	private final ChatClient finalizeChatClient;

	private ChatMemory finalizeMemory = new InMemoryChatMemory();

	private final ChatModel chatModel;

	public LlmService(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
		this.chatModel = chatModel;

		this.planningChatClient = ChatClient.builder(chatModel)
			.defaultSystem(PromptLoader.loadPromptFromClasspath("prompts/planning_system_prompt.md"))
			.defaultAdvisors(new MessageChatMemoryAdvisor(planningMemory))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultTools(toolCallbackProvider)
			.build();

		this.chatClient = ChatClient.builder(chatModel)
			.defaultSystem(PromptLoader.loadPromptFromClasspath("prompts/manus_system_prompt.md"))
			.defaultAdvisors(new MessageChatMemoryAdvisor(memory))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultTools(toolCallbackProvider)
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		this.finalizeChatClient = ChatClient.builder(chatModel)
			.defaultSystem(PromptLoader.loadPromptFromClasspath("prompts/finalize_system_prompt.md"))
			.defaultAdvisors(new MessageChatMemoryAdvisor(finalizeMemory))
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();

	}

	public ChatClient getChatClient() {
		return chatClient;
	}

	public ChatClient getPlanningChatClient() {
		return planningChatClient;
	}

	public ChatClient getFinalizeChatClient() {
		return finalizeChatClient;
	}

	public ChatMemory getMemory() {
		return memory;
	}

	public ChatMemory getPlanningMemory() {
		return planningMemory;
	}

	public ChatModel getChatModel() {
		return chatModel;
	}

}
