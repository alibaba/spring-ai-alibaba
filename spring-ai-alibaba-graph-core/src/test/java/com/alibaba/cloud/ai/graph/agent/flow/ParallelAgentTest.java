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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ParallelAgent to verify parallel execution and result merging functionality.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ParallelAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testParallelAgentBasicFunctionality() throws Exception {
		// Configure KeyStrategyFactory for state management
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("prose_result", new ReplaceStrategy());
			keyStrategyHashMap.put("poem_result", new ReplaceStrategy());
			keyStrategyHashMap.put("summary_result", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		// Create specialized sub-agents with unique output keys and specific instructions
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("专门写散文的AI助手")
			.instruction("你是一个知名的散文作家，擅长写优美的散文。用户会给你一个主题，你只需要创作一篇100字左右的散文，不要写诗或做总结。请专注于散文创作，确保内容优美、意境深远。")
			.outputKey("prose_result")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("专门写现代诗的AI助手")
			.instruction("你是一个知名的现代诗人，擅长写现代诗。用户会给你一个主题，你只需要创作一首现代诗，不要写散文或做总结。请专注于诗歌创作，确保语言精炼、意象丰富。")
			.outputKey("poem_result")
			.build();

		ReactAgent summaryAgent = ReactAgent.builder()
			.name("summary_agent")
			.model(chatModel)
			.description("专门做内容总结的AI助手")
			.instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。用户会给你一个主题，你只需要对这个主题进行简要总结，不要写散文或诗歌。请专注于总结分析，确保观点清晰、概括准确。")
			.outputKey("summary_result")
			.build();

		// Create ParallelAgent that will execute all sub-agents in parallel
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallel_creative_agent")
			.state(stateFactory)
			.description("并行执行多个创作任务，包括写散文、写诗和做总结")
			.inputKey("input")
			.outputKey("topic")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
			.build();

		// Execute the parallel workflow
		try {
			String userRequest = "以'西湖'为主题";
			
			Optional<OverAllState> result = parallelAgent.invoke(Map.of("input", userRequest));
			
			// Verify the results
			assertTrue(result.isPresent(), "Result should be present");
			OverAllState finalState = result.get();

			// Verify input was preserved
			assertTrue(finalState.value("input").isPresent(), "Input should be preserved");
			assertEquals(userRequest, finalState.value("input").get());

			// Verify topic was set (from TransparentNode)
			assertTrue(finalState.value("topic").isPresent(), "Topic should be set");
			assertEquals(userRequest, finalState.value("topic").get());

			// Verify all sub-agents produced results
			assertTrue(finalState.value("prose_result").isPresent(), "Prose result should be present");
			assertTrue(finalState.value("poem_result").isPresent(), "Poem result should be present");
			assertTrue(finalState.value("summary_result").isPresent(), "Summary result should be present");

			// Verify results are not empty
			String proseResult = (String) finalState.value("prose_result").get();
			String poemResult = (String) finalState.value("poem_result").get();
			String summaryResult = (String) finalState.value("summary_result").get();

			assertNotNull(proseResult, "Prose result should not be null");
			assertNotNull(poemResult, "Poem result should not be null");
			assertNotNull(summaryResult, "Summary result should not be null");

			assertFalse(proseResult.trim().isEmpty(), "Prose result should not be empty");
			assertFalse(poemResult.trim().isEmpty(), "Poem result should not be empty");
			assertFalse(summaryResult.trim().isEmpty(), "Summary result should not be empty");

			// Verify that each agent produced different types of content
			// Prose should not contain poem-like content
			assertFalse(proseResult.contains("《") && proseResult.contains("》"),
				"Prose result should not contain poem-like formatting");

			// Poem should not contain prose-like content
			assertFalse(poemResult.contains("散文：") || poemResult.contains("总结："),
				"Poem result should not contain prose or summary content");

			// Summary should not contain creative content
			assertFalse(summaryResult.contains("《") && summaryResult.contains("》"),
				"Summary result should not contain poem-like formatting");
			
			// Print results for manual verification
			System.out.println("=== ParallelAgent Test Results ===");
			System.out.println("Input: " + userRequest);
//			System.out.println("result: " + result);
			System.out.println("Prose Result: " + proseResult);
			System.out.println("Poem Result: " + poemResult);
			System.out.println("Summary Result: " + summaryResult);
			System.out.println("==================================");
			
		} catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ParallelAgent execution failed: " + e.getMessage());
		}
	}

    // todo: has error
	@Test
	public void testAdkStyleWorkflow() throws Exception {
		// 创建数据获取Agent - 模拟API调用
		ReactAgent fetchWeatherAgent = ReactAgent.builder()
			.name("WeatherFetcher")
			.model(chatModel)
			.instruction("你是一个天气数据获取助手。请模拟获取杭州今天的天气信息，包括温度、湿度、风力等。直接返回模拟数据，不需要真实API调用。")
			.outputKey("weather_data")
			.build();

		ReactAgent fetchNewsAgent = ReactAgent.builder()
			.name("NewsFetcher")
			.model(chatModel)
			.instruction("你是一个新闻数据获取助手。请模拟获取今天杭州的主要新闻，重点关注科技和民生。直接返回模拟数据，不需要真实API调用。")
			.outputKey("news_data")
			.build();

		// 创建并行数据收集Agent - 实现Fan-Out模式
		ParallelAgent dataCollector = ParallelAgent.builder()
			.name("DataCollector")
			.inputKey("messages")  // 改为messages，因为ReactAgent.SubGraphNodeAdapter传递的是messages
			.outputKey("output")
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("input", new ReplaceStrategy());
				keyStrategyHashMap.put("output", new ReplaceStrategy());
				keyStrategyHashMap.put("weather_data", new ReplaceStrategy());
				keyStrategyHashMap.put("news_data", new ReplaceStrategy());
				keyStrategyHashMap.put("messages", new AppendStrategy());  // ReactAgent需要messages键
				return keyStrategyHashMap;
			})
			.subAgents(List.of(fetchWeatherAgent, fetchNewsAgent))
			.build();

		// 创建结果合成Agent - 实现Gather模式
		ReactAgent synthesizer = ReactAgent.builder()
			.name("DailyReportSynthesizer")
			.model(chatModel)
			.instruction("""
				你是一个日报生成器。请基于以下信息生成一份杭州今日综合报告：
				
				天气信息: {weather_data}
				新闻动态: {news_data}
				
				请生成一份包含以下内容的报告：
				1. 今日天气概况
				2. 重要新闻摘要
				3. 天气对生活的影响分析
				4. 今日城市生活建议
				
				要求：内容要真实、具体，基于提供的数据进行分析和总结。
				""")
			.outputKey("daily_report")
			.build();

		// 创建完整工作流 - 组合并行和顺序执行
		SequentialAgent dailyWorkflow = SequentialAgent.builder()
			.name("DailyWorkflow")
			.inputKey("messages")  // 改为messages，因为ReactAgent.SubGraphNodeAdapter传递的是messages
			.outputKey("output")
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("input", new ReplaceStrategy());
				keyStrategyHashMap.put("output", new ReplaceStrategy());
				keyStrategyHashMap.put("weather_data", new ReplaceStrategy());
				keyStrategyHashMap.put("news_data", new ReplaceStrategy());
				keyStrategyHashMap.put("daily_report", new ReplaceStrategy());
				keyStrategyHashMap.put("messages", new AppendStrategy());  // ReactAgent需要messages键
				return keyStrategyHashMap;
			})
			.subAgents(List.of(dataCollector, synthesizer))
			.build();

		// 执行工作流
		System.out.println("=== 开始执行ADK风格工作流 ===");
		System.out.println("步骤1: 并行数据收集 (Fan-Out)");
		System.out.println("步骤2: 结果合成 (Gather)");
		System.out.println("================================");
		
		Optional<OverAllState> result = dailyWorkflow.invoke(Map.of("input", "生成杭州今日综合报告"));

		// 验证结果
		assertTrue(result.isPresent(), "工作流执行结果应该存在");
		OverAllState finalState = result.get();

		// 验证并行收集的数据
		assertTrue(finalState.value("weather_data").isPresent(), "天气数据应该存在");
		assertTrue(finalState.value("news_data").isPresent(), "新闻数据应该存在");

		// 验证合成报告
		assertTrue(finalState.value("daily_report").isPresent(), "综合报告应该存在");

		// 输出结果
		System.out.println("=== ADK风格工作流执行结果 ===");
		System.out.println("并行收集的天气数据: " + finalState.value("weather_data").get());
		System.out.println("并行收集的新闻数据: " + finalState.value("news_data").get());
		System.out.println("合成的综合报告: " + finalState.value("daily_report").get());
		System.out.println("================================");
		
		// 验证数据质量
		String weatherData = (String) finalState.value("weather_data").get();
		String newsData = (String) finalState.value("news_data").get();
		String dailyReport = (String) finalState.value("daily_report").get();
		
		assertFalse(weatherData.trim().isEmpty(), "天气数据不应为空");
		assertFalse(newsData.trim().isEmpty(), "新闻数据不应为空");
		assertFalse(dailyReport.trim().isEmpty(), "综合报告不应为空");
		
		// 验证报告内容包含天气和新闻信息
		assertTrue(dailyReport.contains("天气") || dailyReport.contains("温度") || dailyReport.contains("湿度"), 
			"综合报告应包含天气相关信息");
		assertTrue(dailyReport.contains("新闻") || dailyReport.contains("科技") || dailyReport.contains("民生"), 
			"综合报告应包含新闻相关信息");
		
		System.out.println("✅ ADK风格工作流测试通过！");
		System.out.println("✅ 并行数据收集 (Fan-Out) 成功");
		System.out.println("✅ 结果合成 (Gather) 成功");
		System.out.println("✅ 完整工作流执行成功");
	}


	@Test
	public void testParallelAgentWithSingleSubAgent() throws Exception {
		// Test edge case: ParallelAgent with only one sub-agent
		// This should still work, though it's not truly "parallel"
		
		KeyStrategyFactory stateFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("input", new ReplaceStrategy());
			keyStrategyHashMap.put("topic", new ReplaceStrategy());
			keyStrategyHashMap.put("single_result", new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		ReactAgent singleAgent = ReactAgent.builder()
			.name("single_agent")
			.model(chatModel)
			.description("单个测试Agent")
			.instruction("你是一个测试助手，请简单回复用户的请求。")
			.outputKey("single_result")
			.build();

		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("single_parallel_agent")
			.state(stateFactory)
			.description("只有一个子Agent的并行Agent")
			.inputKey("input")
			.outputKey("topic")
			.subAgents(List.of(singleAgent))
			.build();

		try {
			Optional<OverAllState> result = parallelAgent.invoke(Map.of("input", "你好"));
			
			assertTrue(result.isPresent(), "Result should be present");
			OverAllState finalState = result.get();
			
			assertTrue(finalState.value("single_result").isPresent(), "Single result should be present");
			String singleResult = (String) finalState.value("single_result").get();
			assertNotNull(singleResult, "Single result should not be null");

            System.out.println(singleResult);
		} catch (Exception e) {
			fail("Single sub-agent ParallelAgent should work: " + e.getMessage());
		}
	}

	@Test
	public void testParallelAgentDuplicateOutputKeyValidation() throws GraphStateException {
		// Test that ParallelAgent correctly validates unique output keys
		
		// Create two agents with the same output key
		ReactAgent agent1 = ReactAgent.builder()
			.name("agent1")
			.model(chatModel)
			.description("第一个测试Agent")
			.instruction("测试助手1")
			.outputKey("duplicate_key")  // Same output key as agent2
			.build();

		ReactAgent agent2 = ReactAgent.builder()
			.name("agent2")
			.model(chatModel)
			.description("第二个测试Agent")
			.instruction("测试助手2")
			.outputKey("duplicate_key")  // Same output key as agent1
			.build();

		// Test that building ParallelAgent with duplicate output keys throws exception
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder()
				.name("duplicate_key_test")
				.description("测试重复outputKey的验证")
				.inputKey("input")
				.outputKey("output")
				.subAgents(List.of(agent1, agent2))
				.build();
		}, "Should throw exception when sub-agents have duplicate output keys");

		// Verify the error message contains the duplicate key information
		String errorMessage = exception.getMessage();
		assertTrue(errorMessage.contains("Duplicate output keys found among sub-agents: [duplicate_key]"), 
			"Error message should contain the duplicate key information");
		assertTrue(errorMessage.contains("Each sub-agent must have a unique output key"), 
			"Error message should explain the requirement for unique output keys");
	}
}
