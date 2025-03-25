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
class SqlServerChatMemoryTest {

	@Test
	public void sqlServer() {
		ChatMemory chatMemory = new SqlServerChatMemory("sa", "qWeR124563",
				"jdbc:sqlserver://localhost:1433;database=spring_ai_alibaba_chat_memory;encrypt=true;trustServerCertificate=true");
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