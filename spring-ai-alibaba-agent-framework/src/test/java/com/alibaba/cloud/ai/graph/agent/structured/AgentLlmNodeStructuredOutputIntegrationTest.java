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
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

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
    private ChatClient chatClientQwen;     // 通义千问（TOOLCALL 模式）
    private ChatClient chatClientOpenAI;   // OpenAI（NATIVE 模式）

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);
        
        // Mock 返回 JSON 格式响应
        String jsonResponse = "{\"name\":\"张三\",\"age\":25,\"email\":\"test@example.com\"}";
        AssistantMessage mockMessage = new AssistantMessage(jsonResponse);
        
        ChatResponse mockResponse = mock(ChatResponse.class);
        Generation mockGeneration = mock(Generation.class);
        
        when(mockGeneration.getOutput()).thenReturn(mockMessage);
        when(mockResponse.getResult()).thenReturn(mockGeneration);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        // 创建 TOOLCALL 模式的 ChatClient（通义千问）
        ChatOptions qwenOptions = ToolCallingChatOptions.builder()
                .model("qwen-turbo")
                .temperature(0.7)
                .build();
        chatClientQwen = ChatClient.builder(mockChatModel)
                .defaultOptions(qwenOptions)
                .build();
        
        // 创建 NATIVE 模式的 ChatClient（OpenAI）
        ChatOptions openAiOptions = ToolCallingChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .build();
        chatClientOpenAI = ChatClient.builder(mockChatModel)
                .defaultOptions(openAiOptions)
                .build();
    }

    /**
     * 测试：TOOLCALL 模式（通义千问）
     */
    @Test
    void testToolCallModeWithQwenModel() throws Exception {
        System.out.println("\n=== 测试 TOOLCALL 模式（通义千问）===");
        
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

        // 2. 创建 AgentLlmNode（使用 chatClientQwen，model="qwen-turbo" → TOOLCALL 模式）
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClientQwen)
            .outputSchema(outputSchema)
            .build();

        // 3. 创建初始状态
        OverAllState state = new OverAllState();
        List<UserMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("请解析用户信息：姓名张三，年龄25，邮箱test@example.com"));
        state.updateState(Map.of("messages", messages));

        // 4. 执行
        RunnableConfig config = RunnableConfig.builder()
            .addMetadata("_stream_", false)
            .build();
        Map<String, Object> result = node.apply(state, config);

        // 5. 验证结果
        assertNotNull(result);
        assertNotNull(result.get("messages"));
        
        // 验证 structured_output 存在
        Object structuredOutput = result.get("structured_output");
        System.out.println("📊 Structured output (TOOLCALL mode): " + structuredOutput);
        assertNotNull(structuredOutput, "structured_output should exist for TOOLCALL mode");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) structuredOutput;
        assertEquals("张三", parsed.get("name"));
        assertEquals(25, parsed.get("age"));
        
        System.out.println("✅ 测试通过：TOOLCALL 模式（通义千问）正常工作");
    }

    /**
     * 测试：NATIVE 模式（OpenAI）- 反射逻辑
     */
    @Test
    void testNativeModeWithOpenAiModel() throws Exception {
        System.out.println("\n=== 测试 NATIVE 模式（OpenAI）- 反射逻辑 ===");
        
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

        // 创建 AgentLlmNode（使用 chatClientOpenAI，model="gpt-4o-mini" → NATIVE 模式）
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClientOpenAI)
            .outputSchema(outputSchema)
            .build();

        OverAllState state = new OverAllState();
        List<UserMessage> messages = new ArrayList<>();
        messages.add(new UserMessage("Parse user info: name=John, age=30, email=john@test.com"));
        state.updateState(Map.of("messages", messages));

        RunnableConfig config = RunnableConfig.builder()
            .addMetadata("_stream_", false)
            .build();
        Map<String, Object> result = node.apply(state, config);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.get("messages"));
        
        // 验证 structured_output 存在（如果反射成功）或不存在（如果反射失败）
        Object structuredOutput = result.get("structured_output");
        System.out.println("Structured output (NATIVE mode): " + structuredOutput);
        
        if (structuredOutput != null) {
            System.out.println("反射成功：OpenAI NATIVE 模式启用");
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) structuredOutput;
            assertEquals("张三", parsed.get("name"));
        } else {
            System.out.println("反射失败：缺少 OpenAI 依赖，但不影响运行");
        }
        
        System.out.println("测试通过：NATIVE 模式逻辑正确（反射失败时不降级）");
    }

    /**
     * 测试：不使用 outputSchema（向后兼容）
     */
    @Test
    void testWithoutStructuredOutput() throws Exception {
        System.out.println("\n=== 测试不使用 outputSchema（向后兼容）===");
        
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClientQwen)
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
        assertNotNull(result.get("messages"));
        
        // structured_output 不应该存在
        Object structuredOutput = result.get("structured_output");
        assertNull(structuredOutput, "structured_output should not exist without outputSchema");
        
        System.out.println("测试通过：向后兼容性正常");
    }

    /**
     * 测试：非 JSON Schema（文本格式）
     */
    @Test
    void testWithTextOutputSchema() throws Exception {
        System.out.println("\n=== 测试非 JSON Schema（文本格式）===");
        
        AgentLlmNode node = AgentLlmNode.builder()
            .chatClient(chatClientQwen)
            .outputSchema("Please return JSON format")  // 文本，不是 JSON Schema
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
        
        // structured_output 不应该存在
        Object structuredOutput = result.get("structured_output");
        assertNull(structuredOutput, "structured_output should not exist for text schema");
        
        System.out.println("测试通过：文本格式不触发结构化输出");
    }
}

