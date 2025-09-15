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

package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.model.ChatModel;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class LoopAgentTest {

	private static final Logger logger = LoggerFactory.getLogger(LoopAgentTest.class);

	private ChatModel chatModel;

	private KeyStrategyFactory stateFactory;

	private ReactAgent writerAgent;

	private ReactAgent reviewerAgent;

	@BeforeEach
	void setUp() throws GraphStateException {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

		this.stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("article", new ReplaceStrategy());
			keyStrategyHashMap.put("reviewed_article", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		this.writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("可以写文章。")
			.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
			.outputKey("article")
			.build();

		this.reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("可以对文章进行评论和修改。")
			.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
			.outputKey("reviewed_article")
			.build();
	}

	@Test
	public void testCountLoopAgent() throws Exception {
		LoopAgent loopAgent = LoopAgent.builder()
			.name("loop_agent")
			.description("循环执行3次")
			.inputKey("loop_input")
			.outputKey("loop_output")
			.state(() -> Map.of("loop_output", new AppendStrategy(), "loop_input", new ReplaceStrategy()))
			.loopMode(LoopAgent.LoopMode.COUNT)
			.loopCount(3)
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		Map<String, Object> data = loopAgent.invoke(Map.of("loop_input", "帮我写一个散文，题目是：如何进行垃圾分类")).get().data();

		List<?> loopOutput = (List<?>) data.get("loop_output");
		logger.info("loopOutput: {}", loopOutput);

		assertEquals(3, loopOutput.size());
	}

	@Test
	public void testConditionalLoopAgent() throws Exception {
		LoopAgent loopAgent = LoopAgent.builder()
			.name("loop_agent")
			.description("迭代执行")
			.inputKey("loop_input")
			.outputKey("loop_output")
			.loopCondition(result -> result instanceof String && StringUtils.hasText((String) result))
			.state(() -> Map.of("loop_output", new AppendStrategy(), "loop_input", new ReplaceStrategy()))
			.loopMode(LoopAgent.LoopMode.CONDITION)
			.subAgents(List.of(writerAgent))
			.build();

		Map<String, Object> data = loopAgent.invoke(Map.of("loop_input", "帮我写一个散文，题目是：如何进行垃圾分类")).get().data();

		List<?> loopOutput = (List<?>) data.get("loop_output");
		logger.info("loopOutput: {}", loopOutput);

		assertEquals(1, loopOutput.size());
	}

	@Test
	public void testIterableLoopAgent() throws Exception {
		LoopAgent loopAgent = LoopAgent.builder()
			.name("loop_agent")
			.description("迭代执行")
			.inputKey("loop_input")
			.outputKey("loop_output")
			.state(() -> Map.of("loop_output", new AppendStrategy(), "loop_input", new ReplaceStrategy()))
			.loopMode(LoopAgent.LoopMode.ITERABLE)
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		Map<String, Object> data = loopAgent
			.invoke(Map.of("loop_input", List.of("帮我写一个散文，题目是：如何进行垃圾分类", "帮我写一个散文，题目是：如何节约资源", "帮我写一个散文，题目是：大自然的风景")))
			.get()
			.data();

		List<?> loopOutput = (List<?>) data.get("loop_output");
		logger.info("loopOutput: {}", loopOutput);

		assertEquals(3, loopOutput.size());
	}

	@Test
	public void testArrayLoopAgent() throws Exception {
		LoopAgent loopAgent = LoopAgent.builder()
			.name("loop_agent")
			.description("迭代执行")
			.inputKey("loop_input")
			.outputKey("loop_output")
			.state(() -> Map.of("loop_output", new AppendStrategy(), "loop_input", new ReplaceStrategy()))
			.loopMode(LoopAgent.LoopMode.ARRAY)
			.subAgents(List.of(writerAgent))
			.build();

		Map<String, Object> data = loopAgent
			.invoke(Map.of("loop_input",
					new String[] { "帮我写一个散文，题目是：如何进行垃圾分类", "帮我写一个散文，题目是：如何节约资源", "帮我写一个散文，题目是：大自然的风景" }))
			.get()
			.data();

		List<?> loopOutput = (List<?>) data.get("loop_output");
		logger.info("loopOutput: {}", loopOutput);

		assertEquals(3, loopOutput.size());
	}

	@Test
	public void testJsonArrayLoopAgent() throws Exception {
		LoopAgent loopAgent = LoopAgent.builder()
			.name("loop_agent")
			.description("迭代执行")
			.inputKey("loop_input")
			.outputKey("loop_output")
			.state(() -> Map.of("loop_output", new AppendStrategy(), "loop_input", new ReplaceStrategy()))
			.loopMode(LoopAgent.LoopMode.JSON_ARRAY)
			.subAgents(List.of(writerAgent))
			.build();

		Map<String, Object> data = loopAgent
			.invoke(Map.of("loop_input", "[\"帮我写一个散文，题目是：如何进行垃圾分类\", \"帮我写一个散文，题目是：如何节约资源\", \"帮我写一个散文，题目是：大自然的风景\"]"))
			.get()
			.data();

		List<?> loopOutput = (List<?>) data.get("loop_output");
		logger.info("loopOutput: {}", loopOutput);

		assertEquals(3, loopOutput.size());
	}

}
