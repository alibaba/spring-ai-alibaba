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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 工作流（Workflow）示�?
 *
 * 演示如何使用StateGraph构建智能工作流，包括�?
 * 1. 定义自定义Node
 * 2. Agent作为Node
 * 3. 混合使用Agent Node和普通Node
 * 4. 执行工作�?
 *
 * 参考文�? advanced_doc/workflow.md
 */
public class WorkflowExample {

	private final ChatModel chatModel;

	public WorkflowExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示�?
	 *
	 * 注意：需要配置ChatModel实例才能运行
	 */
	public static void main(String[] args) {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		if (chatModel == null) {
			System.err.println("错误：请先配置ChatModel实例");
			System.err.println("请设�?AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		WorkflowExample example = new WorkflowExample(chatModel);

		// 运行所有示�?
		example.runAllExamples();
	}

	/**
	 * 示例1：基础Node定义
	 *
	 * 创建简单的文本处理Node
	 */
	public void example1_basicNode() {
		class TextProcessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				// 1. 从状态中获取输入
				String input = state.value("query", "").toString();

				// 2. 执行业务逻辑
				String processedText = input.toUpperCase().trim();

				// 3. 返回更新后的状�?
				Map<String, Object> result = new HashMap<>();
				result.put("processed_text", processedText);
				return result;
			}
		}

		TextProcessorNode processor = new TextProcessorNode();
		System.out.println("基础Node定义示例完成");
	}

	/**
	 * 示例2：带配置的AI Node
	 *
	 * 创建调用LLM的Node
	 */
	public void example2_aiNode() {
		class QueryExpanderNode implements NodeActionWithConfig {
			private final ChatClient chatClient;
			private final PromptTemplate promptTemplate;

			public QueryExpanderNode(ChatClient.Builder chatClientBuilder) {
				this.chatClient = chatClientBuilder.build();
				this.promptTemplate = new PromptTemplate(
						"你是一个搜索优化专家。请为以下查询生�?{number} 个不同的变体。\n" +
								"原始查询：{query}\n\n" +
								"查询变体：\n"
				);
			}

			@Override
			public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
				// 获取输入参数
				String query = state.value("query", "").toString();
				Integer number = (Integer) state.value("expanderNumber", 3);

				// 调用 LLM
				String result = chatClient.prompt()
						.user(user -> user
								.text(promptTemplate.getTemplate())
								.param("query", query)
								.param("number", number))
						.call()
						.content();

				// 处理结果
				String[] variants = result.split("\n");

				// 返回更新的状�?
				Map<String, Object> output = new HashMap<>();
				output.put("queryVariants", Arrays.asList(variants));
				return output;
			}
		}

		QueryExpanderNode expander = new QueryExpanderNode(ChatClient.builder(chatModel));
		System.out.println("AI Node示例完成");
	}

	/**
	 * 示例3：条件评估Node
	 *
	 * 用于工作流中的条件分支判�?
	 */
	public void example3_conditionNode() {
		class ConditionEvaluatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString().toLowerCase();

				// 根据输入内容决定路由
				String route;
				if (input.contains("错误") || input.contains("异常")) {
					route = "error_handling";
				}
				else if (input.contains("数据") || input.contains("分析")) {
					route = "data_processing";
				}
				else if (input.contains("报告") || input.contains("总结")) {
					route = "report_generation";
				}
				else {
					route = "default";
				}

				Map<String, Object> result = new HashMap<>();
				result.put("_condition_result", route);
				return result;
			}
		}

		ConditionEvaluatorNode evaluator = new ConditionEvaluatorNode();
		System.out.println("条件评估Node示例完成");
	}

	/**
	 * 示例4：并行结果聚合Node
	 *
	 * 用于收集和聚合并行执行的多个Node的结�?
	 */
	public void example4_aggregatorNode() {
		ParallelResultAggregatorNode aggregator = new ParallelResultAggregatorNode("merged_results");
		System.out.println("聚合Node示例完成");
	}

	public static class ParallelResultAggregatorNode implements NodeAction {
		private final String outputKey;

		public ParallelResultAggregatorNode(String outputKey) {
			this.outputKey = outputKey;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 收集所有并行任务的结果
			List<String> results = new ArrayList<>();

			// 假设并行任务将结果存储在不同的键�?
			state.value("result_1").ifPresent(r -> results.add(r.toString()));
			state.value("result_2").ifPresent(r -> results.add(r.toString()));
			state.value("result_3").ifPresent(r -> results.add(r.toString()));

			// 聚合结果
			String aggregatedResult = String.join("\n---\n", results);

			Map<String, Object> output = new HashMap<>();
			output.put(outputKey, aggregatedResult);
			return output;
		}
	}


	/**
	 * 示例5：集成自定义Node到StateGraph
	 *
	 * 构建包含自定义Node的工作流
	 */
	public void example5_buildWorkflowWithCustomNodes() throws Exception {
		// 定义状态管理策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("query", new ReplaceStrategy());
			strategies.put("processed_text", new ReplaceStrategy());
			strategies.put("queryVariants", new ReplaceStrategy());
			strategies.put("final_result", new ReplaceStrategy());
			return strategies;
		};

		// 创建Node实例
		class TextProcessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("query", "").toString();
				String processed = input.toUpperCase().trim();
				return Map.of("processed_text", processed);
			}
		}

		class ConditionNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("processed_text", "").toString();
				String route = input.length() > 10 ? "long" : "short";
				return Map.of("_condition_result", route);
			}
		}

		// 构建 StateGraph
		StateGraph graph = new StateGraph(keyStrategyFactory);

		// 添加自定�?Node
		graph.addNode("processor", node_async(new TextProcessorNode()));
		graph.addNode("condition", node_async(new ConditionNode()));

		// 定义边（流程连接�?
		graph.addEdge(StateGraph.START, "processor");
		graph.addEdge("processor", "condition");

		// 条件边：根据 condition node 的结果路�?
		graph.addConditionalEdges(
				"condition",
				edge_async(state -> state.value("_condition_result", "short").toString()),
				Map.of(
						"long", "processor",  // 长文本重新处�?
						"short", StateGraph.END  // 短文本结�?
				)
		);

		System.out.println("自定义Node工作流构建完�?);
	}

	/**
	 * 示例6：Agent作为SubGraph Node
	 *
	 * 将ReactAgent嵌入到工作流�?
	 */
	public void example6_agentAsNode() throws Exception {
		// 创建专门的数据分�?Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("data_analyzer")
				.model(chatModel)
				.instruction("你是一个数据分析专家，负责分析数据并提供洞察，请分析以下输入数据：\n {input}")
				.outputKey("analysis_result")
				.build();

		// 创建报告生成 Agent
		ReactAgent reportAgent = ReactAgent.builder()
				.name("report_generator")
				.model(chatModel)
				.instruction("你是一个报告生成专家，负责将分析结�?“{analysis_result}�?转化为专业报�?)
				.outputKey("final_report")
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			return strategies;
		};

		// 构建包含 Agent 的工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// �?Agent 作为 SubGraph Node 添加
		workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
				true,                     // includeContents: 是否传递父图的消息历史
				false));

		workflow.addNode(reportAgent.name(), reportAgent.asNode(
				true,
				false));

		// 定义流程
		workflow.addEdge(StateGraph.START, analysisAgent.name());
		workflow.addEdge(analysisAgent.name(), reportAgent.name());
		workflow.addEdge(reportAgent.name(), StateGraph.END);

		CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
		NodeOutput lastOutput = compiledGraph.stream(Map.of("input", "2025年全年销�?00亿，毛利�?23%，净利率 13%�?024年全年销�?0亿，毛利�?20%，净利率 8%�?)).doOnNext(output -> {
			if (output instanceof StreamingOutput<?> streamingOutput) {
				System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
			}
		}).blockLast();

		System.out.println("\n\n最终结果，包含所有节点状态：\n" + lastOutput.state().data());
	}

	/**
	 * 示例7：混合使用Agent Node和普通Node
	 *
	 * 在工作流中结合Agent和自定义Node
	 */
	public void example7_hybridWorkflow() throws Exception {
		// 创建 Agent
		ReactAgent qaAgent = ReactAgent.builder()
				.name("qa_agent")
				.model(chatModel)
				.instruction("你是一个问答专家，负责回答用户的问题：\n {cleaned_input}")
				.outputKey("qa_result")
				.enableLogging(true)
				.build();

		// 创建自定�?Node
		class PreprocessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				String cleaned = input.trim().toLowerCase();
				return Map.of("cleaned_input", cleaned);
			}
		}

		class ValidatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				Message message = (Message)state.value("qa_result").get();
				boolean isValid = message.getText().length() > 50; // 简单验�?
				return Map.of("is_valid", isValid);
			}
		}

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("cleaned_input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			strategies.put("is_valid", new ReplaceStrategy());
			return strategies;
		};

		// 构建混合工作�?
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加普�?Node
		workflow.addNode("preprocess", node_async(new PreprocessorNode()));
		workflow.addNode("validate", node_async(new ValidatorNode()));

		// 添加 Agent Node
		workflow.addNode(qaAgent.name(), qaAgent.asNode(
				true,
				false));

		// 定义流程：预处理 -> Agent处理 -> 验证
		workflow.addEdge(StateGraph.START, "preprocess");
		workflow.addEdge("preprocess", qaAgent.name());
		workflow.addEdge(qaAgent.name(), "validate");

		// 条件边：验证通过则结束，否则重新处理
		workflow.addConditionalEdges(
				"validate",
				edge_async(state -> (Boolean) state.value("is_valid", false) ? "end" : qaAgent.name()),
				Map.of("end", StateGraph.END, qaAgent.name(), qaAgent.name())
		);

		CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
		NodeOutput lastOutput = compiledGraph.stream(Map.of("input", "请解释量子计算的基本原理")).doOnNext(output -> {
			if (output instanceof StreamingOutput<?> streamingOutput) {
				if (streamingOutput.message() != null) {
					// steaming output from streaming llm node
					System.out.println("Streaming output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
				} else {
					// output from normal node, investigate the state to get the node data
					System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.state().data());
				}
			}
		}).blockLast();

		System.out.println("\n\n\n最终结果，包含所有节点状态：\n" + lastOutput.state().data());
	}

	/**
	 * 示例8：执行工作流
	 *
	 * 编译并执行StateGraph工作�?
	 */
	public void example8_executeWorkflow() throws Exception {
		// 创建简单的工作�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("output", new ReplaceStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory);

		class SimpleNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				return Map.of("output", "Processed: " + input);
			}
		}

		workflow.addNode("process", node_async(new SimpleNode()));
		workflow.addEdge(StateGraph.START, "process");
		workflow.addEdge("process", StateGraph.END);

		// 编译工作�?
		CompileConfig compileConfig = CompileConfig.builder().build();
		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		// 准备输入
		Map<String, Object> input = Map.of(
				"input", "请分�?024年AI行业发展趋势"
		);

		// 配置运行参数
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("workflow-001")
				.build();

		// 执行工作�?
		Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

		// 处理结果
		result.ifPresent(state -> {
			System.out.println("输入: " + state.value("input").orElse("�?));
			System.out.println("输出: " + state.value("output").orElse("�?));
		});

		System.out.println("工作流执行完�?);
	}

	/**
	 * 示例9：多Agent协作工作�?
	 *
	 * 构建完整的研究工作流
	 */
	private static final String RESEARCH_RESULT = """
			#### 1. 引言
			AI Agent（人工智能代理）是近年来人工智能领域的重要研究方向之一。它指的是一种能够感知环境、自主决策并采取行动以实现特定目标的智能系统。随着深度学习、强化学习和自然语言处理等技术的发展，AI Agent 在多个领域展现出巨大的潜力�?
			
			本报告旨在全面梳�?AI Agent 的技术发展、应用场景、典型案例以及未来趋势，为相关研究和应用提供参考�?
			
			---
			
			#### 2. 技术发�?
			
			##### 2.1 核心技�?
			- **感知能力**：通过计算机视觉、语音识别和传感器数据处理，AI Agent 能够理解外部环境�?
			- **决策能力**：基于强化学习、规则引擎或大模型推理，AI Agent 可以在复杂环境中做出最优决策�?
			- **执行能力**：通过与物理设备（如机器人）或软件系统（如自动化工具）集成，AI Agent 实现任务执行�?
			- **学习与适应**：利用在线学习和迁移学习技术，AI Agent 能够不断优化自身行为�?
			
			##### 2.2 关键进展
			- **大模型驱动的 Agent**：以 LLM（大语言模型）为基础�?AI Agent 成为研究热点，例�?AutoGPT、BabyAGI 等项目展示了自主任务分解与执行的能力�?
			- **多模态融�?*：结合文本、图像、音频等多种输入形式，提�?Agent 的环境理解能力�?
			- **人机协作**：设计更自然的人机交互机制，�?AI Agent 更好地融入人类工作流程�?
		
			""";

	public void example9_multiAgentResearchWorkflow() throws Exception {
		// 创建工具（示例）
		ToolCallback searchTool = FunctionToolCallback
				.builder("search", (args) -> RESEARCH_RESULT)
				.description("搜索工具")
				.inputType(String.class)
				.build();

		ToolCallback analysisTool = FunctionToolCallback
				.builder("analysis", (args) -> "分析结果")
				.description("分析工具")
				.inputType(String.class)
				.build();

		ToolCallback summaryTool = FunctionToolCallback
				.builder("summary", (args) -> "总结结果")
				.description("总结结果�?)
				.inputType(String.class)
				.build();

		// 1. 创建信息收集 Agent
		ReactAgent researchAgent = ReactAgent.builder()
				.name("researcher")
				.model(chatModel)
				.instruction("你是一个研究专家，负责收集和整理相关信息，请研究主题： {input}")
				.tools(searchTool)
				.outputKey("research_data")
				.enableLogging(true)
				.build();

		// 2. 创建数据分析 Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("analyst")
				.model(chatModel)
				.instruction("你是一个分析专家，负责深入分析关于主题 “{input}�?的研究数据。数据如下： \n\n {research_data}")
				.tools(analysisTool)
				.outputKey("analysis_result")
				.enableLogging(true)
				.build();

		// 3. 创建总结 Agent
		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summarizer")
				.model(chatModel)
				.instruction("你是一个总结专家，负责将分析结果提炼为简洁的结论，结果：\n\n {analysis_result}")
				.tools(summaryTool)
				.outputKey("final_summary")
				.enableLogging(true)
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			return strategies;
		};

		// 4. 构建工作�?
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加 Agent 节点
		workflow.addNode(researchAgent.name(), researchAgent.asNode(
				true,    // 包含历史消息
				false   // 不返回推理过�?
		));

		workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
				true,
				false));

		workflow.addNode(summaryAgent.name(), summaryAgent.asNode(
				true,
				true    // 返回完整推理过程
		));

		// 定义顺序执行流程
		workflow.addEdge(StateGraph.START, researchAgent.name());
		workflow.addEdge(researchAgent.name(), analysisAgent.name());
		workflow.addEdge(analysisAgent.name(), summaryAgent.name());
		workflow.addEdge(summaryAgent.name(), StateGraph.END);


		CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
		NodeOutput finaOutput = compiledGraph.stream(Map.of("input", "帮我做一份关于AI Agent的研究报�?)).doOnNext(output -> {
			if (output instanceof StreamingOutput<?> streamingOutput) {
				System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
			}
		}).blockLast();

		System.out.println("多Agent研究工作流构建完�?);
		System.out.println("最终输�? " + finaOutput.state().value("final_summary").orElse("�?));
	}

	/**
	 * 运行所有示�?
	 */
	public void runAllExamples() {
		System.out.println("=== 工作流（Workflow）示�?===\n");

		try {
			System.out.println("示例1: 基础Node定义");
			example1_basicNode();
			System.out.println();

			System.out.println("示例2: 带配置的AI Node");
			example2_aiNode();
			System.out.println();

			System.out.println("示例3: 条件评估Node");
			example3_conditionNode();
			System.out.println();

			System.out.println("示例4: 并行结果聚合Node");
			example4_aggregatorNode();
			System.out.println();

			System.out.println("示例5: 集成自定义Node到StateGraph");
			example5_buildWorkflowWithCustomNodes();
			System.out.println();

			System.out.println("示例6: Agent作为SubGraph Node");
			example6_agentAsNode();
			System.out.println();
//
			System.out.println("示例7: 混合使用Agent Node和普通Node");
			example7_hybridWorkflow();
			System.out.println();

			System.out.println("示例8: 执行工作�?);
			example8_executeWorkflow();
			System.out.println();

			System.out.println("示例9: 多Agent协作工作�?);
			example9_multiAgentResearchWorkflow();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

