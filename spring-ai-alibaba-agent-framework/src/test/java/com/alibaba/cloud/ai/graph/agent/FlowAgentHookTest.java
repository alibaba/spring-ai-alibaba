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
import com.alibaba.cloud.ai.graph.agent.flow.agent.*;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.CountLoopStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.utils.HookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FlowAgent subclasses with Hook functionality.
 *
 * @author haojun.phj (Jackie)
 * @since 2025/01/13
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class FlowAgentHookTest {

	private ChatModel chatModel;

	private AgentHook logAgentHook;

	private ModelHook logModelHook;

	@BeforeEach
	public void setUp() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Create hooks using HookFactory
		logAgentHook = HookFactory.createLogAgentHook();
		logModelHook = HookFactory.createLogModelHook();
	}

	@Test
	public void testSupervisorAgentWithHook() throws Exception {
		System.out.println("\n========== Testing SupervisorAgent with Hook ==========\n");

		// Create sub-agents
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长创作各类文章")
				.instruction("你是一个知名的作家，擅长写诗，20字以内。")
				.outputKey("writer_output")
				.enableLogging(false)
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长翻译")
				.instruction("你是一个专业的翻译家，20字以内。")
				.outputKey("translator_output")
				.enableLogging(false)
				.build();

		// Create SupervisorAgent with hook
		SupervisorAgent supervisorAgent = SupervisorAgent.builder()
				.name("content_supervisor")
				.description("内容管理监督者")
				.model(chatModel)
				.subAgents(List.of(writerAgent, translatorAgent))
				.hooks(List.of(logAgentHook, logModelHook))
				.systemPrompt("""
					你是一个智能的内容处理监督者。
					可用的子Agent：writer_agent（写作）、translator_agent（翻译）
					只返回Agent名称或FINISH，不要包含其他解释。
					""")
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = supervisorAgent.invoke("帮我写一首关于春天的诗");

			assertTrue(result.isPresent(), "Result should be present");

			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = supervisorAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== SupervisorAgent Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("=====================================\n");

		} catch (Exception e) {
			e.printStackTrace();
			fail("SupervisorAgent with hook execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testSequentialAgentWithHook() throws Exception {
		System.out.println("\n========== Testing SequentialAgent with Hook ==========\n");

		// Create sub-agents
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("专业写作Agent")
				.instruction("你是一个知名的作家，擅长写诗。请根据用户的提问进行回答：{input}，20字以内。")
				.outputKey("article")
				.enableLogging(false)
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("专业评审Agent")
				.instruction("你是一个知名的评论家。待评论诗歌：{article}，20字以内。")
				.outputKey("reviewed_article")
				.enableLogging(false)
				.build();

		// Create SequentialAgent with hook
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("writing_workflow")
				.description("写作工作流：先写诗，然后评审")
				.subAgents(List.of(writerAgent, reviewerAgent))
				.hooks(List.of(logAgentHook, logModelHook))
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = sequentialAgent.invoke("写一篇关于春天的诗歌");

			assertTrue(result.isPresent(), "Result should be present");

			System.out.println("result=" + result.get());

			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = sequentialAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== SequentialAgent Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("======================================\n");

		} catch (Exception e) {
			e.printStackTrace();
			fail("SequentialAgent with hook execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testLoopAgentWithHook() throws Exception {
		System.out.println("\n========== Testing LoopAgent with Hook ==========\n");

		// Create a simple agent for loop
		ReactAgent processorAgent = ReactAgent.builder()
				.name("processor_agent")
				.model(chatModel)
				.description("数据处理Agent")
				.instruction("你是一个数据处理专家。请处理：{input}，20字以内。")
				.outputKey("processed_data")
				.enableLogging(false)
				.build();

		// Create LoopAgent with hook
		LoopAgent loopAgent = LoopAgent.builder()
				.name("loop_processor")
				.description("循环处理工作流")
				.subAgent(processorAgent)
				.hooks(List.of(logAgentHook, logModelHook))
				.loopStrategy(new CountLoopStrategy(2))
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = loopAgent.invoke("处理数据");

			assertTrue(result.isPresent(), "Result should be present");

			System.out.println("result=" + result.get());
			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = loopAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== LoopAgent Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("================================\n");

		} catch (Exception e) {
			e.printStackTrace();
			fail("LoopAgent with hook execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testParallelAgentWithHook() throws Exception {
		System.out.println("\n========== Testing ParallelAgent with Hook ==========\n");

		// Create multiple agents for parallel execution
		ReactAgent agent1 = ReactAgent.builder()
				.name("agent_1")
				.model(chatModel)
				.description("处理Agent 1")
				.instruction("你是处理器1。请处理：{input}，20字以内。")
				.outputKey("output_1")
				.enableLogging(false)
				.build();

		ReactAgent agent2 = ReactAgent.builder()
				.name("agent_2")
				.model(chatModel)
				.description("处理Agent 2")
				.instruction("你是处理器2。请处理：{input}，20字以内。")
				.outputKey("output_2")
				.enableLogging(false)
				.build();

		ReactAgent agent3 = ReactAgent.builder()
				.name("agent_3")
				.model(chatModel)
				.description("处理Agent 3")
				.instruction("你是处理器3。请处理：{input}，20字以内。")
				.outputKey("output_3")
				.enableLogging(false)
				.build();

		// Create ParallelAgent with hook
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_processor")
				.description("并行处理工作流")
				.subAgents(List.of(agent1, agent2, agent3))
				.hooks(List.of(logAgentHook, logModelHook))
				.maxConcurrency(3)
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = parallelAgent.invoke("并行处理任务");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("result=" + result.get());

			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = parallelAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== ParallelAgent Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("====================================\n");

		} catch (Exception e) {
			e.printStackTrace();
			fail("ParallelAgent with hook execution failed: " + e.getMessage());
		}
	}

	@Test
	public void testLlmRoutingAgentWithHook() throws Exception {
		System.out.println("\n========== Testing LlmRoutingAgent with Hook ==========\n");

		// Create sub-agents for routing
		ReactAgent writerAgent = ReactAgent.builder()
				.name("writer_agent")
				.model(chatModel)
				.description("擅长写作")
				.instruction("你是一个知名的作家，20字以内。")
				.outputKey("writer_output")
				.enableLogging(false)
				.build();

		ReactAgent translatorAgent = ReactAgent.builder()
				.name("translator_agent")
				.model(chatModel)
				.description("擅长翻译")
				.instruction("你是一个专业的翻译家，20字以内。")
				.outputKey("translator_output")
				.enableLogging(false)
				.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
				.name("reviewer_agent")
				.model(chatModel)
				.description("擅长评审")
				.instruction("你是一个知名的评论家，20字以内。")
				.outputKey("reviewer_output")
				.enableLogging(false)
				.build();

		// Create LlmRoutingAgent with hook
		LlmRoutingAgent llmRoutingAgent = LlmRoutingAgent.builder()
				.name("llm_router")
				.description("智能路由Agent")
				.model(chatModel)
				.subAgents(List.of(writerAgent, translatorAgent, reviewerAgent))
				.hooks(List.of(logAgentHook, logModelHook))
				.fallbackAgent("writer_agent")
				.systemPrompt("""
					你是一个智能路由器。
					可用的Agent：writer_agent（写作）、translator_agent（翻译）、reviewer_agent（评审）
					只返回Agent名称或FINISH，不要包含其他解释。
					""")
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = llmRoutingAgent.invoke("帮我写一首诗");

			assertTrue(result.isPresent(), "Result should be present");
			System.out.println("result=" + result.get());

			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = llmRoutingAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== LlmRoutingAgent Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("======================================\n");

		} catch (Exception e) {
			e.printStackTrace();
			fail("LlmRoutingAgent with hook execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test case for ParallelAgent with both beforeAgent and beforeModel hooks.
	 */
	@Test
	public void testParallelAgentWithBeforeAgentAndBeforeModelHooks() throws Exception {
		System.out.println("\n========== Testing ParallelAgent with BeforeAgent AND BeforeModel Hooks ==========\n");

		// Create multiple agents for parallel execution
		ReactAgent agent1 = ReactAgent.builder()
				.name("agent_1")
				.model(chatModel)
				.description("处理Agent 1")
				.instruction("你是处理器1。请处理：{input}，20字以内。")
				.outputKey("output_1")
				.enableLogging(false)
				.build();

		ReactAgent agent2 = ReactAgent.builder()
				.name("agent_2")
				.model(chatModel)
				.description("处理Agent 2")
				.instruction("你是处理器2。请处理：{input}，20字以内。")
				.outputKey("output_2")
				.enableLogging(false)
				.build();

		// Create ParallelAgent with BOTH beforeAgent and beforeModel hooks
		ParallelAgent parallelAgent = ParallelAgent.builder()
				.name("parallel_processor")
				.description("并行处理工作流")
				.subAgents(List.of(agent1, agent2))
				.hooks(List.of(logAgentHook, logModelHook))  // Both hooks present!
				.maxConcurrency(2)
				.build();

		try {
			// Execute the agent
			Optional<OverAllState> result = parallelAgent.invoke("并行处理任务");

			assertTrue(result.isPresent(), "Result should be present");

			// Print Mermaid graph representation
			GraphRepresentation mermaidGraph = parallelAgent.getGraph()
					.getGraph(GraphRepresentation.Type.MERMAID);
			assertNotNull(mermaidGraph, "Mermaid graph should not be null");
			System.out.println("\n=== ParallelAgent with Both Hooks Mermaid Graph ===");
			System.out.println(mermaidGraph.content());
			System.out.println("====================================================\n");

			System.out.println("✓ Bug fix verified: beforeAgent and beforeModel hooks work correctly together in ParallelAgent!");

		} catch (Exception e) {
			e.printStackTrace();
			fail("ParallelAgent with both beforeAgent and beforeModel hooks execution failed: " + e.getMessage());
		}
	}
}
