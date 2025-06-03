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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

/**
 * ThinkNode is a specialized node that handles the thinking/reasoning process
 * with pre and post LLM hooks for state management and processing.
 * It uses LlmNode for the core LLM operations and adds hook capabilities.
 */
public class ThinkNode implements NodeAction {

    private final NodeAction preLlmHook;
    private final NodeAction postLlmHook;
    private final LlmNode llmNode;

    private ThinkNode(LlmNode llmNode, NodeAction preLlmHook, NodeAction postLlmHook) {
        this.llmNode = llmNode;
        this.preLlmHook = preLlmHook;
        this.postLlmHook = postLlmHook;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // Execute pre-LLM hook if exists
        if (preLlmHook != null) {
            preLlmHook.apply(state);
        }

        // Execute LLM call
        Map<String, Object> result = llmNode.apply(state);

        // Execute post-LLM hook if exists
        if (postLlmHook != null) {
            postLlmHook.apply(state);
        }

        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LlmNode.Builder llmNodeBuilder = LlmNode.builder()
            .chatOptions(ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build());
        private NodeAction preLlmHook;
        private NodeAction postLlmHook;

        public Builder systemPrompt(String systemPrompt) {
            llmNodeBuilder.systemPromptTemplate(systemPrompt);
            return this;
        }

        public Builder userPrompt(String userPrompt) {
            llmNodeBuilder.userPromptTemplate(userPrompt);
            return this;
        }

        public Builder params(Map<String, String> params) {
            llmNodeBuilder.params(params);
            return this;
        }

        public Builder messages(List<Message> messages) {
            llmNodeBuilder.messages(messages);
            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            llmNodeBuilder.toolCallbacks(toolCallbacks);
            return this;
        }

        public Builder chatClient(ChatClient chatClient) {
            llmNodeBuilder.chatClient(chatClient);
            return this;
        }

        public Builder preLlmHook(NodeAction preLlmHook) {
            this.preLlmHook = preLlmHook;
            return this;
        }

        public Builder postLlmHook(NodeAction postLlmHook) {
            this.postLlmHook = postLlmHook;
            return this;
        }

        public Builder messagesKey(String messagesKey) {
            llmNodeBuilder.messagesKey(messagesKey);
            return this;
        }

        public Builder chatOptions(ChatOptions chatOptions) {
            llmNodeBuilder.chatOptions(chatOptions);
            return this;
        }

        public ThinkNode build() {
            LlmNode llmNode = llmNodeBuilder.build();
            return new ThinkNode(llmNode, preLlmHook, postLlmHook);
        }
    }
}
