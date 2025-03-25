package com.alibaba.cloud.ai.memory.jdbc;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * @author future0923
 *
 */
class PostgresChatMemoryTest {

    @Test
    public void postgresSQL() {
        ChatMemory chatMemory = new PostgresChatMemory("root", "123456", "jdbc:postgresql://127.0.0.1:5432/spring_ai_alibaba_chat_memory");
        ChatClient chatClient = ChatClient.create(new DashScopeChatModel(new DashScopeApi("")));
        String content1 = chatClient.prompt()
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .user("我是张三😄")
                .call()
                .content();
        System.out.println(content1);
        String content2 = chatClient.prompt()
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .user("我是谁")
                .call()
                .content();
        System.out.println(content2);
        Assertions.assertTrue(content2.contains("张三"));
    }
}