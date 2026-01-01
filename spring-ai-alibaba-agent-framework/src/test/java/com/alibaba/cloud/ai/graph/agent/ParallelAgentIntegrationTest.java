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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test class for ParallelAgent to verify parallel execution and result
 * merging functionality. Tests the actual execution flow with real LLM agents.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ParallelAgentIntegrationTest {

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
		// Create specialized sub-agents with unique output keys and specific instructions
		ReactAgent proseWriterAgent = ReactAgent.builder()
			.name("prose_writer_agent")
			.model(chatModel)
			.description("专门写散文的AI助手")
			.instruction("你是一个知名的散文作家，擅长写优美的散文。用户会给你一个主题，你只需要创作一�?00字左右的散文，不要写诗或做总结。请专注于散文创作，确保内容优美、意境深远�?)
			.outputKey("prose_result")
			.build();

		ReactAgent poemWriterAgent = ReactAgent.builder()
			.name("poem_writer_agent")
			.model(chatModel)
			.description("专门写现代诗的AI助手")
			.instruction("你是一个知名的现代诗人，擅长写现代诗。用户会给你一个主题，你只需要创作一首现代诗，不要写散文或做总结。请专注于诗歌创作，确保语言精炼、意象丰富�?)
			.outputKey("poem_result")
			.outputKeyStrategy(KeyStrategy.REPLACE)
			.build();

		ReactAgent summaryAgent = ReactAgent.builder()
			.name("summary_agent")
			.model(chatModel)
			.description("专门做内容总结的AI助手")
			.instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。用户会给你一个主题，你只需要对这个主题进行简要总结，不要写散文或诗歌。请专注于总结分析，确保观点清晰、概括准确�?)
			.outputKey("summary_result")
			.build();

		// Create ParallelAgent that will execute all sub-agents in parallel
		ParallelAgent parallelAgent = ParallelAgent.builder()
			.name("parallel_creative_agent")
			.description("并行执行多个创作任务，包括写散文、写诗和做总结")
			.mergeOutputKey("merged_results")
			.subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
			.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
			.build();

		// Execute the parallel workflow
		try {
			String userRequest = "�?西湖'为主�?;

			Optional<OverAllState> result = parallelAgent.invoke(userRequest);

			// Verify the results
			assertTrue(result.isPresent(), "Result should be present");
			OverAllState finalState = result.get();

			// Verify input was preserved
			assertTrue(finalState.value("input").isPresent(), "Input should be preserved");
			assertEquals(userRequest, finalState.value("input").get());

			// Verify topic was set (from TransparentNode)
			assertTrue(finalState.value("merged_results").isPresent(), "Topic should be set");

			// Verify all sub-agents produced results
			assertTrue(finalState.value("prose_result").isPresent(), "Prose result should be present");
			assertTrue(finalState.value("poem_result").isPresent(), "Poem result should be present");
			assertTrue(finalState.value("summary_result").isPresent(), "Summary result should be present");

			
			// Verify the merged results contain all individual results
			Map mergedResults = (Map) finalState.value("merged_results").get();
			assertTrue(mergedResults.containsKey("prose_result"),
					"Merged results should contain prose result");
			assertTrue(mergedResults.containsKey("poem_result"),
					"Merged results should contain poem result");
			assertTrue(mergedResults.containsKey("summary_result"),
					"Merged results should contain summary result");

			assertEquals(mergedResults.get("prose_result"),finalState.value("prose_result").get());
			assertEquals(mergedResults.get("poem_result"),finalState.value("poem_result").get());
			assertEquals(mergedResults.get("summary_result"),finalState.value("summary_result").get());

			System.out.println("Final state: " + finalState);
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ParallelAgent execution failed: " + e.getMessage());
		}
	}

	// @Test
	// public void testAdkStyleWorkflow() throws Exception {
	// // 创建共用的KeyStrategyFactory
	// KeyStrategyFactory sharedStateFactory = () -> {
	// HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
	// keyStrategyHashMap.put("input", new ReplaceStrategy());
	// keyStrategyHashMap.put("output", new ReplaceStrategy());
	// keyStrategyHashMap.put("weather_data", new ReplaceStrategy());
	// keyStrategyHashMap.put("news_data", new ReplaceStrategy());
	// keyStrategyHashMap.put("raw_data", new ReplaceStrategy());
	// keyStrategyHashMap.put("daily_report", new ReplaceStrategy());
	// keyStrategyHashMap.put("workflow_output", new ReplaceStrategy());
	// keyStrategyHashMap.put("messages", new AppendStrategy()); // ReactAgent需要messages�?
	// return keyStrategyHashMap;
	// };
	//
	// // 创建数据获取Agent - 模拟API调用
	// ReactAgent fetchWeatherAgent = ReactAgent.builder()
	// .name("WeatherFetcher")
	// .model(chatModel)
	// .instruction("你是一个天气数据获取助手。请模拟获取杭州今天的天气信息，包括温度、湿度、风力等。直接返回模拟数据，不需要真实API调用�?)
	// .outputKey("weather_data")
	// .build();
	//
	// ReactAgent fetchNewsAgent = ReactAgent.builder()
	// .name("NewsFetcher")
	// .model(chatModel)
	// .instruction("你是一个新闻数据获取助手。请模拟获取今天杭州的主要新闻，重点关注科技和民生。直接返回模拟数据，不需要真实API调用�?)
	// .outputKey("news_data")
	// .build();
	//
	// // 创建并行数据收集Agent - 实现Fan-Out模式
	// ParallelAgent dataCollector = ParallelAgent.builder()
	// .name("DataCollector")
	// .description("并行收集天气和新闻数�?)
	// .inputKeys("input") // 改为input，避免与ReactAgent的messages冲突
	// .outputKey("raw_data")
	// .state(sharedStateFactory)
	// .subAgents(List.of(fetchWeatherAgent, fetchNewsAgent))
	// .build();
	//
	// // 创建结果合成Agent - 实现Gather模式
	// ReactAgent synthesizer =
	// ReactAgent.builder().name("DailyReportSynthesizer").model(chatModel).instruction("""
	// 你是一个日报生成器。请基于以下信息生成一份杭州今日综合报告：
	//
	// 天气信息: {weather_data}
	// 新闻动�? {news_data}
	//
	// 请生成一份包含以下内容的报告�?
	// 1. 今日天气概况
	// 2. 重要新闻摘要
	// 3. 天气对生活的影响分析
	// 4. 今日城市生活建议
	//
	// 要求：内容要真实、具体，基于提供的数据进行分析和总结�?
	// """).outputKey("daily_report").build();
	//
	// // 创建完整工作�?- 组合并行和顺序执�?
	// SequentialAgent dailyWorkflow = SequentialAgent.builder()
	// .name("DailyWorkflow")
	// .description("收集数据并行执行，然后合成结�?)
	// .inputKeys("input") // 改为input，与dataCollector保持一�?
	// .outputKey("workflow_output")
	// .state(sharedStateFactory)
	// .subAgents(List.of(dataCollector, synthesizer))
	// .build();
	//
	// Optional<OverAllState> result = dailyWorkflow.invoke(Map.of("input",
	// "生成杭州今日综合报告"));
	//
	// // 验证结果
	// assertTrue(result.isPresent(), "工作流执行结果应该存�?);
	// OverAllState finalState = result.get();
	//
	// // 验证并行收集的数�?
	// assertTrue(finalState.value("weather_data").isPresent(), "天气数据应该存在");
	// assertTrue(finalState.value("news_data").isPresent(), "新闻数据应该存在");
	//
	// // 验证合成报告
	// assertTrue(finalState.value("daily_report").isPresent(), "综合报告应该存在");
	//
	// // 输出结果
	// System.out.println("并行收集的天气数�? " + finalState.value("weather_data").get());
	// System.out.println("并行收集的新闻数�? " + finalState.value("news_data").get());
	// System.out.println("合成的综合报�? " + finalState.value("daily_report").get());
	// System.out.println("================================");
	//
	// // 验证数据质量
	// String weatherData = (String) finalState.value("weather_data").get();
	// String newsData = (String) finalState.value("news_data").get();
	// String dailyReport = (String) finalState.value("daily_report").get();
	//
	// assertFalse(weatherData.trim().isEmpty(), "天气数据不应为空");
	// assertFalse(newsData.trim().isEmpty(), "新闻数据不应为空");
	// assertFalse(dailyReport.trim().isEmpty(), "综合报告不应为空");
	// }

	@Test
	public void testParallelAgentDuplicateOutputKeyValidation() throws GraphStateException {
		// Test that ParallelAgent correctly validates unique output keys

		// Create two agents with the same output key
		ReactAgent agent1 = ReactAgent.builder()
			.name("agent1")
			.model(chatModel)
			.description("第一个测试Agent")
			.instruction("测试助手1")
			.outputKey("duplicate_key") // Same output key as agent2
			.build();

		ReactAgent agent2 = ReactAgent.builder()
			.name("agent2")
			.model(chatModel)
			.description("第二个测试Agent")
			.instruction("测试助手2")
			.outputKey("duplicate_key") // Same output key as agent1
			.build();

		// Test that building ParallelAgent with duplicate output keys throws exception
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			ParallelAgent.builder()
				.name("duplicate_key_test")
				.description("测试重复outputKey的验�?)
				.mergeOutputKey("output")
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

	@Test
	public void testParallelAgentWithCustomMergeStrategy() throws Exception {
		// Test ParallelAgent with different merge strategies

		ReactAgent agent1 = ReactAgent.builder()
			.name("agent1")
			.model(chatModel)
			.description("第一个测试Agent")
			.instruction("请返回数�?")
			.outputKey("result1")
			.build();

		ReactAgent agent2 = ReactAgent.builder()
			.name("agent2")
			.model(chatModel)
			.description("第二个测试Agent")
			.instruction("请返回数�?")
			.outputKey("result2")
			.build();

		// Test with ListMergeStrategy
		ParallelAgent listMergeAgent = ParallelAgent.builder()
			.name("list_merge_test")
			.description("测试列表合并策略")
			.mergeOutputKey("merged_list")
			.mergeStrategy(new ParallelAgent.ListMergeStrategy())
			.subAgents(List.of(agent1, agent2))
			.build();

		Optional<OverAllState> result = listMergeAgent.invoke("test");
		assertTrue(result.isPresent());

		OverAllState state = result.get();
		assertTrue(state.value("merged_list").isPresent(), "Merged list result should be present");

		System.out.println("List merge result: " + state.value("merged_list").get());
	}

	@Test
	public void testParallelAgentWithMaxConcurrency() throws Exception {
		// Test ParallelAgent with concurrency control

		// Create multiple agents
		List<Agent> agents = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			agents.add(ReactAgent.builder()
				.name("worker_" + i)
				.model(chatModel)
				.description("Worker agent " + i)
				.instruction("请返回工作结�?" + i)
				.outputKey("result_" + i)
				.build());
		}

		ParallelAgent concurrencyAgent = ParallelAgent.builder()
			.name("concurrency_test")
			.description("测试并发控制")
			.mergeOutputKey("concurrency_results")
			.maxConcurrency(3) // 限制最大并发数�?
			.subAgents(agents)
			.build();

		Optional<OverAllState> result = concurrencyAgent.invoke("test concurrency");
		assertTrue(result.isPresent());

		OverAllState state = result.get();
		assertTrue(state.value("concurrency_results").isPresent(), "Concurrency results should be present");

		System.out.println("Concurrency test completed with maxConcurrency=3");
		System.out.println("Results: " + state.value("concurrency_results").get());
	}

}
