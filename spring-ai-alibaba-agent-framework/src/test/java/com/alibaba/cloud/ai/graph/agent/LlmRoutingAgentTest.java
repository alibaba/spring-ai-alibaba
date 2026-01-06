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
	 * 测试在 LlmRoutingAgent 级别直接使用 MessageTrimmingHook
	 * 这样可以在路由决策之前就裁剪记忆，减少 LlmRoutingAgent 本身的 token 消耗
	 */
	@Test
	public void testLlmRoutingAgentWithHooksAtRoutingLevel() throws Exception {
		// 创建可追踪的 MessageTrimmingHook，只保留最近的3条消息（用于路由决策）
		TrackableMessageTrimmingHook routingLevelHook = new TrackableMessageTrimmingHook(3);

		// 创建普通的 sub-agents（不带 hook）
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("可以写散文文章。")
			.instruction("你是一个知名的作家，擅长写散文。请根据用户的提问进行回答。")
			.saver(new MemorySaver())
			.outputKey("prose_article")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("可以写现代诗。")
			.instruction("你是一个知名的诗人，擅长写现代诗。请根据用户的提问进行回复。")
			.saver(new MemorySaver())
			.outputKey("poem_article")
			.build();

		// 在 LlmRoutingAgent 级别配置 hook
		LlmRoutingAgent blogAgent = LlmRoutingAgent.builder()
			.name("blog_agent")
			.model(chatModel)
			.description("可以根据用户给定的主题写文章或作诗。")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent))
			.hooks(routingLevelHook)  //  在 LlmRoutingAgent 级别配置 hook
			.saver(new MemorySaver())
			.build();

		try {
			// 使用同一个 threadId 进行多次调用
			RunnableConfig config = RunnableConfig.builder().threadId("test-routing-hook").build();

			// 第一次调用
			routingLevelHook.reset();
			Optional<OverAllState> result1 = blogAgent.invoke("帮我写一篇关于春天的散文", config);
			assertTrue(result1.isPresent(), "第一次结果应该存在");
			System.out.println("第一次调用完成");
			
			// 验证第一次调用：hook 应该被调用1次，输入消息数=1，不需要裁剪
			assertEquals(1, routingLevelHook.getCallCount(), "第一次调用，hook应该被调用1次");
			assertEquals(1, routingLevelHook.getLastInputMessageCount(), "第一次调用只有1条用户消息");
			assertEquals(1, routingLevelHook.getLastOutputMessageCount(), "第一次调用消息数<=3，不需要裁剪");
			System.out.println(" 验证通过：第一次调用消息未被裁剪（1条消息）");
			System.out.println("------------------");

			// 第二次调用（相同的 threadId，此时历史中有：user1, assistant1, user2 = 3条）
			routingLevelHook.reset();
			Optional<OverAllState> result2 = blogAgent.invoke("现在写一首关于夏天的诗", config);
			assertTrue(result2.isPresent(), "第二次结果应该存在");
			System.out.println("第二次调用完成");
			
			// 验证第二次调用：hook应该被调用1次，输入约3条消息（user1+assistant1+user2）
			assertEquals(1, routingLevelHook.getCallCount(), "第二次调用，hook应该被调用1次");
			assertTrue(routingLevelHook.getLastInputMessageCount() >= 3, 
				"第二次调用应该有至少3条消息（包含历史）, 实际: " + routingLevelHook.getLastInputMessageCount());
			assertEquals(3, routingLevelHook.getLastOutputMessageCount(), 
				"第二次调用应该裁剪到3条消息, 实际: " + routingLevelHook.getLastOutputMessageCount());
			System.out.println(" 验证通过：第二次调用消息被裁剪（从 " + routingLevelHook.getLastInputMessageCount() + " 条裁剪到3条）");
			System.out.println("------------------");

			// 第三次调用（验证记忆被持续裁剪，此时历史更多）
			routingLevelHook.reset();
			Optional<OverAllState> result3 = blogAgent.invoke("再写一首关于秋天的诗", config);
			assertTrue(result3.isPresent(), "第三次结果应该存在");
			System.out.println("第三次调用完成");
			
			// 验证第三次调用：应该继续裁剪到3条
			assertEquals(1, routingLevelHook.getCallCount(), "第三次调用，hook应该被调用1次");
			assertTrue(routingLevelHook.getLastInputMessageCount() > 3, 
				"第三次调用应该有超过3条历史消息, 实际: " + routingLevelHook.getLastInputMessageCount());
			assertEquals(3, routingLevelHook.getLastOutputMessageCount(), 
				"第三次调用应该裁剪到3条消息, 实际: " + routingLevelHook.getLastOutputMessageCount());
			System.out.println(" 验证通过：第三次调用消息被裁剪（从 " + routingLevelHook.getLastInputMessageCount() + " 条裁剪到3条）");
			System.out.println("------------------");

			// 验证结果
			System.out.println(" 测试成功：LlmRoutingAgent 级别的 hooks 正常工作！");
			System.out.println("- Hook 在路由决策前执行，裁剪了记忆");
			System.out.println("- 减少了 LlmRoutingAgent 的 token 消耗");
			System.out.println("- 防止了记忆无限增长的问题");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("LlmRoutingAgent with hooks at routing level failed: " + e.getMessage());
		}
	}

	/**
	 * 可追踪的 MessageTrimmingHook，用于测试验证
	 * 记录每次调用的输入输出消息数量
	 */
	@HookPositions({HookPosition.BEFORE_MODEL})
	static class TrackableMessageTrimmingHook extends MessagesModelHook {
		private final int maxMessages;
		private int callCount = 0;
		private int lastInputMessageCount = 0;
		private int lastOutputMessageCount = 0;

		public TrackableMessageTrimmingHook(int maxMessages) {
			this.maxMessages = maxMessages;
		}

		@Override
		public String getName() {
			return "trackable_message_trimming_hook";
		}

		@Override
		public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
			callCount++;
			lastInputMessageCount = previousMessages.size();

			if (previousMessages.size() <= maxMessages) {
				// 如果消息数量未超过限制，无需裁剪
				lastOutputMessageCount = previousMessages.size();
				System.out.println("TrackableMessageTrimmingHook [调用#" + callCount + "]: 消息数=" 
					+ lastInputMessageCount + "，无需裁剪");
				return new AgentCommand(previousMessages);
			}

			// 只保留最近的 maxMessages 条消息
			List<Message> trimmedMessages = previousMessages.subList(
				previousMessages.size() - maxMessages,
				previousMessages.size()
			);
			lastOutputMessageCount = trimmedMessages.size();

			System.out.println("TrackableMessageTrimmingHook [调用#" + callCount + "]: 裁剪消息从 " 
				+ lastInputMessageCount + " 条到 " + lastOutputMessageCount + " 条");

			// 使用 REPLACE 策略替换所有消息
			return new AgentCommand(new ArrayList<>(trimmedMessages), UpdatePolicy.REPLACE);
		}

		// 测试辅助方法
		public int getCallCount() {
			return callCount;
		}

		public int getLastInputMessageCount() {
			return lastInputMessageCount;
		}

		public int getLastOutputMessageCount() {
			return lastOutputMessageCount;
		}

		public void reset() {
			callCount = 0;
			lastInputMessageCount = 0;
			lastOutputMessageCount = 0;
		}
	}

}
