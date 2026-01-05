/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.agent.tools.PoetTool.createPoetToolCallback;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class LlmRoutingAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testLlmRoutingAgent() throws Exception {
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("可以写散文文章。")
			.instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答。")
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗。")
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问，调用工具进行回复。")
			.outputKey("poem_article")
			.tools(List.of(createPoetToolCallback()))
			.build();

		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("可以根据用户给定的主题写文章或作诗。")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.build();

		try {

			GraphRepresentation representation = blogAgent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
			System.out.println(representation.content());

			Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的现代诗");
			blogAgent.invoke("帮我写一个100字左右的现代诗");
			Optional<OverAllState> result3 = blogAgent.invoke("帮我写一个100字左右的现代诗");

			// 验证结果不为空
			assertTrue(result.isPresent(), "Result should be present");
			assertTrue(result3.isPresent(), "Third result should be present");

			OverAllState state = result.get();
			OverAllState state3 = result3.get();

			assertTrue(state.value("input").isPresent(), "Input should be present in state");
			assertEquals("帮我写一个100字左右的现代诗", state.value("input").get(), "Input should match the request");

			assertTrue(state.value("poem_article").isPresent(), "Poem article should be present");
			AssistantMessage poemContent = (AssistantMessage) state.value("poem_article").get();
			assertNotNull(poemContent.getText(), "Poem content should not be null");

			assertTrue(state3.value("poem_article").isPresent(), "Poem article should be present");
			AssistantMessage poemContent3 = (AssistantMessage) state3.value("poem_article").get();
			assertNotNull(poemContent3.getText(), "Poem content should not be null");

			System.out.println(result.get());
			System.out.println("------------------");
			System.out.println(result3.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("LlmRoutingAgent execution failed: " + e.getMessage());
		}

		// Verify all hooks were executed
	}

	/**
	 * 测试 LlmRoutingAgent 的 sub-agents 使用 MessageTrimmingHook 来裁剪记忆，
	 * 从而减少 token 消耗并防止记忆无限增长
	 */
	@Test
	public void testLlmRoutingAgentWithMessageTrimmingHook() throws Exception {
		// 创建一个简单的 MessageTrimmingHook，只保留最近的5条消息
		MessagesModelHook messageTrimmingHook = new MessageTrimmingHook(5);

		// 创建带有 hook 的 sub-agents
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("可以写散文文章。")
			.instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答。")
			.hooks(messageTrimmingHook)  // 添加 hook 到 sub-agent
			.saver(new MemorySaver())  // 使用 MemorySaver
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗。")
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问，调用工具进行回复。")
			.hooks(messageTrimmingHook)  // 添加 hook 到 sub-agent
			.saver(new MemorySaver())  // 使用 MemorySaver
			.outputKey("poem_article")
			.tools(List.of(createPoetToolCallback()))
			.build();

		// 创建 LlmRoutingAgent，使用 MemorySaver 来持久化状态
		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("可以根据用户给定的主题写文章或作诗。")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.saver(new MemorySaver())  // LlmRoutingAgent 也使用 MemorySaver
			.build();

		try {
			// 使用同一个 threadId 进行多次调用
			RunnableConfig config = RunnableConfig.builder().threadId("test-thread-1").build();

			// 第一次调用
			Optional<OverAllState> result1 = blogAgent.invoke("帮我写一首关于春天的现代诗", config);
			assertTrue(result1.isPresent(), "第一次结果应该存在");
			System.out.println("第一次调用结果:");
			System.out.println(result1.get());
			System.out.println("------------------");

			// 第二次调用（相同的 threadId，记忆会被重用）
			Optional<OverAllState> result2 = blogAgent.invoke("现在写一首关于秋天的现代诗", config);
			assertTrue(result2.isPresent(), "第二次结果应该存在");
			System.out.println("第二次调用结果:");
			System.out.println(result2.get());
			System.out.println("------------------");

			// 第三次调用（相同的 threadId，记忆会被重用，但由于 hook 的作用，只保留最近5条消息）
			Optional<OverAllState> result3 = blogAgent.invoke("再写一首关于冬天的现代诗", config);
			assertTrue(result3.isPresent(), "第三次结果应该存在");
			System.out.println("第三次调用结果:");
			System.out.println(result3.get());
			System.out.println("------------------");

			// 验证结果包含预期的输出
			assertTrue(result3.get().value("poem_article").isPresent(), "应该有诗歌输出");
			AssistantMessage poemContent = (AssistantMessage) result3.get().value("poem_article").get();
			assertNotNull(poemContent.getText(), "诗歌内容不应为空");

			System.out.println("测试成功：MessageTrimmingHook 正常工作，记忆被裁剪");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("LlmRoutingAgent with MessageTrimmingHook execution failed: " + e.getMessage());
		}
	}

	/**
	 * 简单的 MessageTrimmingHook 实现
	 * 只保留最近的 maxMessages 条消息，以减少 token 消耗
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	static class MessageTrimmingHook extends MessagesModelHook {
		private final int maxMessages;

		public MessageTrimmingHook(int maxMessages) {
			this.maxMessages = maxMessages;
		}

		@Override
		public String getName() {
			return "message_trimming_hook";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			if (previousMessages.size() <= maxMessages) {
				// 如果消息数量未超过限制，无需裁剪
				return new AgentCommand(previousMessages);
			}

			// 只保留最近的 maxMessages 条消息
			List<Message> trimmedMessages = previousMessages.subList(
				previousMessages.size() - maxMessages,
				previousMessages.size()
			);

			System.out.println("MessageTrimmingHook: 裁剪消息从 " + previousMessages.size() 
				+ " 条到 " + trimmedMessages.size() + " 条");

			// 使用 REPLACE 策略替换所有消息
			return new AgentCommand(new ArrayList<>(trimmedMessages), UpdatePolicy.REPLACE);
		}
	}

}
