package com.alibaba.cloud.ai.memory.jdbc;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

/**
 * @author future0923
 *
 */
class MysqlChatMemoryTest {

    @Test
    public void mysql() {

        MysqlChatMemory chatMemory = new MysqlChatMemory("root", "123456", "jdbc:mysql://127.0.0.1:3306/spring_ai_alibaba_chat_memory");
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