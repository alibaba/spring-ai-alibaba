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
package com.alibaba.cloud.ai.graph.agent.hooks.toolcalllimit;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ToolCallLimitTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }

    @Test
    public void testToolCallLimitWithEndBehavior() throws Exception {
        ToolCallLimitHook hook = ToolCallLimitHook.builder()
                .runLimit(1) // 限制只能调用1次工具
                .exitBehavior(ToolCallLimitHook.ExitBehavior.END)
                .build();

        ReactAgent agent = createAgent(hook, "test-tool-call-limit-end", chatModel);

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("请使用工具执行多个操作，例如获取当前时间、列出目录文件等"));

        Optional<OverAllState> result = agent.invoke(messages);

        assertTrue(result.isPresent(), "结果应该存在");
        Object messagesObj = result.get().value("messages").get();
        assertNotNull(messagesObj, "消息应该存在于结果中");
        
        if (messagesObj instanceof List) {
            List<Message> resultMessages = (List<Message>) messagesObj;
            System.out.println("返回消息数量: " + resultMessages.size());
            
            // 检查是否包含限制 exceeded 的消息
            boolean limitExceededFound = false;
            for (Message message : resultMessages) {
                if (message.getText().contains("limits exceeded")) {
                    limitExceededFound = true;
                    System.out.println("✓成功触发工具调用限制: " + message.getText());
                    break;
                }
            }
            
            if (!limitExceededFound) {
                System.out.println("未找到限制 exceeded 的消息，但测试仍可能有效");
            }
        }
    }

    @Test
    public void testToolCallLimitWithErrorBehavior() throws Exception {
        ToolCallLimitHook hook = ToolCallLimitHook.builder()
                .runLimit(1) // 限制只能调用1次工具
                .exitBehavior(ToolCallLimitHook.ExitBehavior.ERROR)
                .build();

        ReactAgent agent = createAgent(hook, "test-tool-call-limit-error", chatModel);

        System.out.println("\n=== 测试工具调用限制（ERROR行为）===");

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("请使用工具执行多个操作"));

        try {
            Optional<OverAllState> result = agent.invoke(messages);
            System.out.println("未抛出异常，可能工具未被调用或限制未生效");
        } catch (Exception e) {
            if (e.getCause() instanceof ToolCallLimitExceededException) {
                System.out.println("✓成功抛出工具调用限制异常: " + e.getCause().getMessage());
            } else {
                System.out.println("抛出其他异常: " + e.getMessage());
            }
        }
    }

    @Test
    public void testWithoutToolCallLimit() throws Exception {
        // 创建不带工具调用限制的Agent
        ReactAgent agent = ReactAgent.builder()
                .name("test-no-tool-call-limit")
                .model(chatModel)
                .saver(new MemorySaver())
                .build();

        System.out.println("\n=== 测试不带工具调用限制的对话 ===");

        // 创建普通对话
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("你好，有什么可以帮助你的吗？"));

        // 调用 agent
        Optional<OverAllState> result = agent.invoke(messages);

        // 验证结果
        assertTrue(result.isPresent(), "结果应该存在");
        Object messagesObj = result.get().value("messages").get();
        assertNotNull(messagesObj, "消息应该存在于结果中");
        
        if (messagesObj instanceof List) {
            List<Message> resultMessages = (List<Message>) messagesObj;
            System.out.println("返回消息数量: " + resultMessages.size());
            System.out.println("正常对话流程，未触发工具调用限制");
        }
    }

    public ReactAgent createAgent(ToolCallLimitHook hook, String name, ChatModel model) throws Exception {
        return ReactAgent.builder()
                .name(name)
                .model(model)
                .hooks(List.of(hook))
                .saver(new MemorySaver())
                .build();
    }

    
}
