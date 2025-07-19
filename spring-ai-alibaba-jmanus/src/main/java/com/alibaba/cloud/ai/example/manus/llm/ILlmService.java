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

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

/**
 * LLM service interface, providing chat client and memory management functionality
 */
public interface ILlmService {

	/**
	 * Get Agent chat client
	 * @return ChatClient
	 */
	ChatClient getAgentChatClient();

	/**
	 * Get dynamic chat client
	 * @param model
	 * @return ChatClient
	 */
	ChatClient getDynamicChatClient(DynamicModelEntity model);

	/**
	 * Get Agent memory
	 * @param maxMessages maximum number of messages
	 * @return ChatMemory
	 */
	ChatMemory getAgentMemory(Integer maxMessages);

	/**
	 * Clear Agent memory
	 * @param planId plan ID
	 */
	void clearAgentMemory(String planId);

	/**
	 * Get planning chat client
	 * @return ChatClient
	 */
	ChatClient getPlanningChatClient();

	/**
	 * Clear conversation memory
	 * @param planId plan ID
	 */
	void clearConversationMemory(String planId);

	/**
	 * Get finalize chat client
	 * @return ChatClient
	 */
	ChatClient getFinalizeChatClient();

	/**
	 * Get chat model
	 * @return ChatModel
	 */
	ChatModel getChatModel();

	/**
	 * Get conversation memory
	 * @param maxMessages maximum number of messages
	 * @return ChatMemory
	 */
	ChatMemory getConversationMemory(Integer maxMessages);

}
