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
package com.alibaba.cloud.ai.studio.core.structured;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StructuredOutputService {

    private final StructuredOutputManager manager = new StructuredOutputManager();

    public static class UserInfo {
        private String name;
        private Integer age;
        private String city;

        public UserInfo() {}

        public UserInfo(String name, Integer age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        @Override
        public String toString() {
            return "UserInfo{name='" + name + "', age=" + age + ", city='" + city + "'}";
        }
    }

    public UserInfo parseUserInfo(ChatModel chatModel, String query) {
        return parseStructured(chatModel, "openai", "gpt-4o", query, UserInfo.class);
    }

    public <T> T parseStructured(
            ChatModel chatModel,
            String provider,
            String modelId,
            String query,
            Class<T> targetClass) {
        
        return parseStructured(chatModel, provider, modelId, query, targetClass, null);
    }

    public <T> T parseStructured(
            ChatModel chatModel,
            String provider,
            String modelId,
            String query,
            Class<T> targetClass,
            List<String> modelTags) {

        List<Message> messages = List.of(new UserMessage(query));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(modelId)
            .temperature(0.1)
            .build();

        StructuredOutputManager.PreparedContext context = manager.prepare(
            provider, 
            modelId, 
            messages, 
            options, 
            targetClass,
            modelTags
        );

        ChatResponse response = chatModel.call(
            new Prompt(context.messages(), context.options())
        );

        String responseText = response.getResult().getOutput().getText();
        return manager.parseToObject(
            responseText, 
            context.strategy(), 
            targetClass
        );
    }

    public String getStrategyName(String provider, String modelId, List<String> modelTags) {
        List<Message> messages = List.of(new UserMessage("test"));
        OpenAiChatOptions options = OpenAiChatOptions.builder().model(modelId).build();
        
        StructuredOutputManager.PreparedContext context = manager.prepare(
            provider, modelId, messages, options, UserInfo.class, modelTags
        );
        
        return context.strategy().getName();
    }
}

