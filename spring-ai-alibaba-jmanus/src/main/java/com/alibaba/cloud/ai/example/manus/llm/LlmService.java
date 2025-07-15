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
import com.alibaba.cloud.ai.example.manus.event.JmanusListener;
import com.alibaba.cloud.ai.example.manus.event.ModelChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmService implements JmanusListener<ModelChangeEvent> {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private ChatClient agentExecutionClient;

    private ChatClient planningChatClient;

    private ChatClient finalizeChatClient;

    private ChatMemory conversationMemory;

    private ChatMemory agentMemory;

    private final ChatModel chatModel;

    private Map<ChatClient, Long> clients = new ConcurrentHashMap<>();

    public LlmService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public ChatClient getAgentChatClient() {
        return agentExecutionClient;
    }

    public ChatClient getDynamicChatClient(DynamicModelEntity model, Boolean internalToolExecutionEnabled) {
        return getDynamicChatClient(model.getBaseUrl(), model.getApiKey(), model.getModelName(), model.getHeaders(), internalToolExecutionEnabled);
    }

    public ChatClient getDynamicChatClient(String host, String apiKey, String modelName, Map<String, String> headers) {
        return getDynamicChatClient(host, apiKey, modelName, headers, false);
    }

    public ChatClient getDynamicChatClient(String host, String apiKey, String modelName, Map<String, String> headers, boolean internalToolExecutionEnabled) {
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(host).apiKey(apiKey).build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().model(modelName).build();
        if (headers != null) {
            chatOptions.setHttpHeaders(headers);
        }

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
        return ChatClient.builder(openAiChatModel)
                // .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(internalToolExecutionEnabled).build())
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

    @Override
    public void onEvent(ModelChangeEvent event) {

        ChatOptions defaultOptions = chatModel.getDefaultOptions();
        Boolean internalToolExecutionEnabled = false;
        // If internalToolExecutionEnabled is not configured, set
        // internalToolExecutionEnabled of agentExecutionClient to false
        if (defaultOptions instanceof OpenAiChatOptions) {
            OpenAiChatOptions options = (OpenAiChatOptions) defaultOptions;
            internalToolExecutionEnabled = options.getInternalToolExecutionEnabled();
            if (internalToolExecutionEnabled == null) {
                internalToolExecutionEnabled = false;
            }
            options.setInternalToolExecutionEnabled(internalToolExecutionEnabled);
            defaultOptions = options;
        }
        DynamicModelEntity dynamicModelEntity = event.getDynamicModelEntity();
        Long moduleId = dynamicModelEntity.getId();

        if (this.planningChatClient == null) {
            // Execute and summarize planning, use the same memory
            this.planningChatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultOptions(defaultOptions)
                    .build();
            clients.put(this.planningChatClient, moduleId);
        } else {
            Long planningModuleId = clients.get(this.planningChatClient);
            if (moduleId.equals(planningModuleId)) {
                this.planningChatClient = getDynamicChatClient(dynamicModelEntity, internalToolExecutionEnabled);
                clients.put(this.planningChatClient, moduleId);
            }
        }

        if (this.agentExecutionClient == null) {
            // Each agent execution process uses independent memory
            this.agentExecutionClient = ChatClient.builder(chatModel)
                    // .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentMemory).build())
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultOptions(defaultOptions)
                    .build();
            clients.put(this.agentExecutionClient, moduleId);
        } else {
            Long agentModuleId = clients.get(this.agentExecutionClient);
            if (moduleId.equals(agentModuleId)) {
                this.agentExecutionClient = getDynamicChatClient(dynamicModelEntity, false);
                clients.put(this.agentExecutionClient, moduleId);
            }
        }

        if (this.finalizeChatClient == null) {
            this.finalizeChatClient = ChatClient.builder(chatModel)
                    // .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory).build())
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .build();
            clients.put(this.finalizeChatClient, moduleId);
        } else {
            Long finalizeModuleId = clients.get(this.finalizeChatClient);
            if (moduleId.equals(finalizeModuleId)) {
                this.finalizeChatClient = getDynamicChatClient(dynamicModelEntity, internalToolExecutionEnabled);
                clients.put(this.finalizeChatClient, moduleId);
            }
        }
    }
}
