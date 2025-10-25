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
package com.alibaba.cloud.ai.graph.agent.structured;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试 AgentLlmNode 的结构化输出集成
 */
class AgentLlmNodeStructuredOutputIntegrationTest {

    private ChatModel mockChatModel;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);

        String jsonResponse = "{\"name\":\"张三\",\"age\":25,\"email\":\"test@example.com\"}";
        AssistantMessage mockMessage = new AssistantMessage(jsonResponse);
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        chatClient = ChatClient.builder(mockChatModel).build();
    }

    @Test
    void testStructuredOutputWithJsonSchema() throws Exception {
        // 1. 定义 JSON Schema
        String outputSchema = """
        {
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "age": {"type": "integer"},
            "email": {"type": "string"}
          },
          "required": ["name", "age"]
        }
        """;

        // 2. 创建 AgentLlmNode
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClient)
            .outputSchema(outputSchema)  // ✅ 设置 JSON Schema
            .build();

        // 3. 创建初始状态
        OverAllState state = new OverAllState();
        List<UserMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("请解析用户信息：姓名张三，年龄25，邮箱test@example.com"));
        state.updateState(Map.of("messages", messages));

        // 4. 执行（设置 stream=false 才会执行结构化输出处理）
        RunnableConfig config = RunnableConfig.builder()
            .addMetadata("_stream_", false)
            .build();
        Map<String, Object> result = node.apply(state, config);

        // 5. 验证结果
        assertNotNull(result);
        
        // 调试：打印所有 keys
        System.out.println("Result keys: " + result.keySet());
        System.out.println("Messages: " + result.get("messages"));
        
        // 验证 messages 正常保存
        assertNotNull(result.get("messages"));
        
        // 验证 structured_output key 存在
        Object structuredOutput = result.get("structured_output");
        System.out.println("Structured output: " + structuredOutput);
        assertNotNull(structuredOutput, "structured_output key should exist");
        
        // 验证结构化数据
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) structuredOutput;
        assertEquals("张三", parsed.get("name"));
        assertEquals(25, parsed.get("age"));
        assertEquals("test@example.com", parsed.get("email"));
        
        System.out.println("✅ 测试通过：AgentLlmNode 成功集成结构化输出");
        System.out.println("结构化结果: " + parsed);
    }

    @Test
    void testWithoutStructuredOutput() throws Exception {
        // 不设置 outputSchema，应该正常工作
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClient)
            .build();

        OverAllState state = new OverAllState();
        List<UserMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello"));
        state.updateState(Map.of("messages", messages));

        RunnableConfig config = RunnableConfig.builder()
            .addMetadata("_stream_", false)
            .build();
        Map<String, Object> result = node.apply(state, config);

        assertNotNull(result);
        
        // structured_output key 不应该存在
        Object structuredOutput = result.get("structured_output");
        assertNull(structuredOutput, "structured_output key should not exist");
    }

    @Test
    void testWithTextOutputSchema() throws Exception {
        // 使用文本格式的 outputSchema（不是 JSON Schema）
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClient)
            .outputSchema("Please return JSON format")  // 文本格式，不是 JSON Schema
            .build();

        OverAllState state = new OverAllState();
        List<UserMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello"));
        state.updateState(Map.of("messages", messages));

        RunnableConfig config = RunnableConfig.builder()
            .addMetadata("_stream_", false)
            .build();
        Map<String, Object> result = node.apply(state, config);

        assertNotNull(result);
        
        // structured_output key 不应该存在（因为不是 JSON Schema）
        Object structuredOutput = result.get("structured_output");
        assertNull(structuredOutput, "structured_output key should not exist for text schema");
    }
}


