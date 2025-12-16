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
package com.alibaba.cloud.ai.graph.agent.hooks.summarization;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class SummarizationTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }

    @Test
    public void testSummarizationEffect() throws Exception {
        // mock
        List<Message> longConversation = createLongConversation(50);


        SummarizationHook hook = SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(200) // 设置较低的阈值以便触发总结
                .messagesToKeep(10) // 保留最近10条消息
                .build();

        ReactAgent agent = createAgent(hook, "test-summarization-agent", chatModel);

        System.out.println("=== 测试带有总结功能的对话 ===");
        System.out.println("初始消息数量: " + longConversation.size());
        
        // 调用 agent，应该触发总结
        Optional<OverAllState> result = agent.invoke(longConversation);

        // 验证结果
        assertTrue(result.isPresent(), "结果应该存在");
        Object messagesObj = result.get().value("messages").get();
        assertNotNull(messagesObj, "消息应该存在于结果中");

        if (messagesObj instanceof List) {
            List<Message> messages = (List<Message>) messagesObj;
            System.out.println("总结后消息数量: " + messages.size());

            if (!messages.isEmpty()) {
                Message firstMessage = messages.get(0);
                if (firstMessage.getText().contains("summary of the conversation")) {
                    System.out.println("总结功能");
                    System.out.println("总结消息预览: " + firstMessage.getText().substring(0, 
                        Math.min(100, firstMessage.getText().length())) + "...");
                }
            }
        }
    }

    @Test
    public void testWithoutSummarization() throws Exception {
        // mock
        List<Message> shortConversation = createShortConversation();

        ReactAgent agent = ReactAgent.builder()
                .name("test-no-summarization-agent")
                .model(chatModel)
                .saver(new MemorySaver())
                .build();

        System.out.println("\n=== 测试不带总结功能的对话 ===");
        System.out.println("初始消息数量: " + shortConversation.size());

        // 调用 agent
        Optional<OverAllState> result = agent.invoke(shortConversation);

        // 验证结果
        assertTrue(result.isPresent(), "结果应该存在");
        Object messagesObj = result.get().value("messages").get();
        assertNotNull(messagesObj, "消息应该存在于结果中");
        
        if (messagesObj instanceof List) {
            List<Message> messages = (List<Message>) messagesObj;
            System.out.println("处理后消息数量: " + messages.size());
            System.out.println("✓ 正常对话流程，未触发总结");
        }
    }

    private List<Message> createLongConversation(int messageCount) {
        List<Message> messages = new ArrayList<>();
        // 添加初始系统消息
        messages.add(new UserMessage("我们开始一个长对话来测试总结功能。"));
        messages.add(new AssistantMessage("好的，我明白了。我们来进行一个长对话测试。"));
        
        // 添加大量交替的用户和助手消息
        for (int i = 0; i < messageCount; i++) {
            if (i % 2 == 0) {
                messages.add(new UserMessage("用户消息 " + i + "：这是对话中的一条用户消息，包含一些内容用于增加token数量，我们需要足够多的文字来确保能够触发总结功能。"));
            } else {
                messages.add(new AssistantMessage("助手消息 " + i + "：这是对话中的一条助手回复，也包含一些内容用于增加token数量，我们需要足够多的文字来确保能够触发总结功能。"));
            }
        }
        
        // 添加最后几条消息
        messages.add(new UserMessage("这是倒数第二条消息。"));
        messages.add(new AssistantMessage("我收到了你的消息。"));
        messages.add(new UserMessage("这是最后一条消息，请总结以上对话。"));
        return messages;
    }

    private List<Message> createShortConversation() {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("你好"));
        messages.add(new AssistantMessage("你好！有什么我可以帮助你的吗？"));
        messages.add(new UserMessage("我想了解总结功能是如何工作的"));
        messages.add(new AssistantMessage("总结功能会在对话变得很长时自动总结早期内容，以避免超出token限制。"));
        messages.add(new UserMessage("谢谢你的解释"));
        return messages;
    }

    public ReactAgent createAgent(SummarizationHook hook, String name, ChatModel model) throws GraphStateException {
        return ReactAgent.builder()
                .name(name)
                .model(model)
                .hooks(List.of(hook))
                .saver(new MemorySaver())
                .build();
    }

    @Test
    public void testSystemMessagePreservation() throws Exception {
        List<Message> conversation = new ArrayList<>();
        
        String firstUserPrompt = "我需要你帮我分析一个复杂的技术问题。";
        conversation.add(new UserMessage(firstUserPrompt));
        conversation.add(new AssistantMessage("好的，我很乐意帮助你。请详细描述你的问题。"));
        for (int i = 0; i < 50; i++) {
            conversation.add(new UserMessage("用户消息 " + i + "：这是一条测试消息，内容足够长以便触发摘要功能。"));
            conversation.add(new AssistantMessage("助手消息 " + i + "：这是一条回复消息，同样包含足够的内容。"));
        }
        conversation.add(new UserMessage("最后一条消息：请告诉我第一条用户消息的内容。"));

        SummarizationHook hook = SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(200)
                .messagesToKeep(10)
                .build();

        ReactAgent agent = createAgent(hook, "test-first-user-message-preservation", chatModel);
        Optional<OverAllState> result = agent.invoke(conversation);


        assertTrue(result.isPresent(), "结果应该存在");
        Object messagesObj = result.get().value("messages").get();
        assertNotNull(messagesObj, "消息应该存在于结果中");

        @SuppressWarnings("unchecked")
        List<Message> resultMessages = (List<Message>) messagesObj;
        System.out.println("摘要后消息数量: " + resultMessages.size());

        assertFalse(resultMessages.isEmpty(), "结果消息不应为空");
        Message firstMessage = resultMessages.get(0);
        assertTrue(firstMessage instanceof UserMessage, "第一条消息应该是 UserMessage");

        UserMessage firstUserMessage = (UserMessage) firstMessage;
        
        assertTrue(resultMessages.size() >= 2, "至少应该有两条消息");
        Message secondMessage = resultMessages.get(1);
        assertTrue(secondMessage instanceof SystemMessage, "第二条消息应该是 SystemMessage（摘要消息）");
        
        SystemMessage summaryMessage = (SystemMessage) secondMessage;

        assertEquals(firstUserPrompt, firstUserMessage.getText(), 
            "第一条用户消息应该完全保留");
        assertTrue(summaryMessage.getText().contains("Previous conversation summary") || 
                   summaryMessage.getText().contains("summary"), 
            "第二条消息应该是包含摘要的系统消息");
    }

    
}
