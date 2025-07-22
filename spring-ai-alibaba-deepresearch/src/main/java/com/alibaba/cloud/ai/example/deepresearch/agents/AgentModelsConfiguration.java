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

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentModelsConfiguration {

    private final DashScopeConnectionProperties commonProperties;
    private final ToolCallingManager toolCallingManager;

    public AgentModelsConfiguration(DashScopeConnectionProperties dashScopeConnectionProperties,
                                   ToolCallingManager toolCallingManager) {
        this.toolCallingManager = toolCallingManager;
        this.commonProperties = dashScopeConnectionProperties;
    }

    /**
     * 动态构建 ChatClient，供 AgentsConfiguration 等调用
     */
    public ChatClient builderChatClient(String modelName) {
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder().apiKey(commonProperties.getApiKey()).build())
                .toolCallingManager(toolCallingManager)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(modelName)
                        .withTemperature(DashScopeChatModel.DEFAULT_TEMPERATURE)
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }
}
