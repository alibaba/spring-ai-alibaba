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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
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
 * 工作流（Workflow）示例
 *
 * 演示如何使用StateGraph构建智能工作流，包括：
 * 1. 定义自定义Node
 * 2. Agent作为Node
 * 3. 混合使用Agent Node和普通Node
 * 4. 执行工作流
 *
 * 参考文档: advanced_doc/workflow.md
 */
public class WorkflowExample {

	private final ChatModel chatModel;

	public WorkflowExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main方法：运行所有示例
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
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		WorkflowExample example = new WorkflowExample(chatModel);

		// 运行所有示例
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

				// 3. 返回更新后的状态
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
						"你是一个搜索优化专家。请为以下查询生成 {number} 个不同的变体。\n" +
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

				// 返回更新的状态
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
	 * 用于工作流中的条件分支判断
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
	 * 用于收集和聚合并行执行的多个Node的结果
	 */
	public void example4_aggregatorNode() {
		class ParallelResultAggregatorNode implements NodeAction {
			private final String outputKey;

			public ParallelResultAggregatorNode(String outputKey) {
				this.outputKey = outputKey;
			}

			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				// 收集所有并行任务的结果
				List<String> results = new ArrayList<>();

				// 假设并行任务将结果存储在不同的键中
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

		ParallelResultAggregatorNode aggregator = new ParallelResultAggregatorNode("merged_results");
		System.out.println("聚合Node示例完成");
	}

	/**
	 * 示例5：集成自定义Node到StateGraph
	 *
	 * 构建包含自定义Node的工作流
	 */
	public void example5_buildWorkflowWithCustomNodes() throws Exception {
		// 定义状态管理策略
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

		// 添加自定义 Node
		graph.addNode("processor", node_async(new TextProcessorNode()));
		graph.addNode("condition", node_async(new ConditionNode()));

		// 定义边（流程连接）
		graph.addEdge(StateGraph.START, "processor");
		graph.addEdge("processor", "condition");

		// 条件边：根据 condition node 的结果路由
		graph.addConditionalEdges(
				"condition",
				edge_async(state -> state.value("_condition_result", "short").toString()),
				Map.of(
						"long", "processor",  // 长文本重新处理
						"short", StateGraph.END  // 短文本结束
				)
		);

		System.out.println("自定义Node工作流构建完成");
	}

	/**
	 * 示例6：Agent作为SubGraph Node
	 *
	 * 将ReactAgent嵌入到工作流中
	 */
	public void example6_agentAsNode() throws Exception {
		// 创建专门的数据分析 Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("data_analyzer")
				.model(chatModel)
				.instruction("你是一个数据分析专家，负责分析数据并提供洞察")
				.build();

		// 创建报告生成 Agent
		ReactAgent reportAgent = ReactAgent.builder()
				.name("report_generator")
				.model(chatModel)
				.instruction("你是一个报告生成专家，负责将分析结果转化为专业报告")
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("analysis_result", new ReplaceStrategy());
			strategies.put("final_report", new ReplaceStrategy());
			return strategies;
		};

		// 构建包含 Agent 的工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 将 Agent 作为 SubGraph Node 添加
		workflow.addNode("analysis", analysisAgent.asNode(
				true,                     // includeContents: 是否传递父图的消息历史
				false,                    // returnReasoningContents: 是否返回推理过程
				"analysis_result"         // outputKeyToParent: 输出键名
		));

		workflow.addNode("reporting", reportAgent.asNode(
				true,
				false,
				"final_report"
		));

		// 定义流程
		workflow.addEdge(StateGraph.START, "analysis");
		workflow.addEdge("analysis", "reporting");
		workflow.addEdge("reporting", StateGraph.END);

		System.out.println("Agent作为Node工作流构建完成");
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
				.instruction("你是一个问答专家")
				.build();

		// 创建自定义 Node
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
				String result = state.value("qa_result", "").toString();
				boolean isValid = result.length() > 50; // 简单验证
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

		// 构建混合工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加普通 Node
		workflow.addNode("preprocess", node_async(new PreprocessorNode()));
		workflow.addNode("validate", node_async(new ValidatorNode()));

		// 添加 Agent Node
		workflow.addNode("qa", qaAgent.asNode(
				true,
				false,
				"qa_result"
		));

		// 定义流程：预处理 -> Agent处理 -> 验证
		workflow.addEdge(StateGraph.START, "preprocess");
		workflow.addEdge("preprocess", "qa");
		workflow.addEdge("qa", "validate");

		// 条件边：验证通过则结束，否则重新处理
		workflow.addConditionalEdges(
				"validate",
				edge_async(state -> (Boolean) state.value("is_valid", false) ? "end" : "qa"),
				Map.of("end", StateGraph.END, "qa", "qa")
		);

		System.out.println("混合工作流构建完成");
	}

	/**
	 * 示例8：执行工作流
	 *
	 * 编译并执行StateGraph工作流
	 */
	public void example8_executeWorkflow() throws Exception {
		// 创建简单的工作流
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

		// 编译工作流
		CompileConfig compileConfig = CompileConfig.builder().build();
		CompiledGraph compiledGraph = workflow.compile(compileConfig);

		// 准备输入
		Map<String, Object> input = Map.of(
				"input", "请分析2024年AI行业发展趋势"
		);

		// 配置运行参数
		RunnableConfig runnableConfig = RunnableConfig.builder()
				.threadId("workflow-001")
				.build();

		// 执行工作流
		Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

		// 处理结果
		result.ifPresent(state -> {
			System.out.println("输入: " + state.value("input").orElse("无"));
			System.out.println("输出: " + state.value("output").orElse("无"));
		});

		System.out.println("工作流执行完成");
	}

	/**
	 * 示例9：多Agent协作工作流
	 *
	 * 构建完整的研究工作流
	 */
	public void example9_multiAgentResearchWorkflow() throws Exception {
		// 创建工具（示例）
		ToolCallback searchTool = FunctionToolCallback.builder("search", (args) -> "搜索结果")
				.description("搜索工具")
				.build();

		ToolCallback analysisTool = FunctionToolCallback.builder("analysis", (args) -> "分析结果")
				.description("分析工具")
				.build();

		ToolCallback summaryTool = FunctionToolCallback.builder("summary", (args) -> "总结结果")
				.description("总结工具")
				.build();

		// 1. 创建信息收集 Agent
		ReactAgent researchAgent = ReactAgent.builder()
				.name("researcher")
				.model(chatModel)
				.instruction("你是一个研究专家，负责收集和整理相关信息")
				.tools(searchTool)
				.build();

		// 2. 创建数据分析 Agent
		ReactAgent analysisAgent = ReactAgent.builder()
				.name("analyst")
				.model(chatModel)
				.instruction("你是一个分析专家，负责深入分析研究数据")
				.tools(analysisTool)
				.build();

		// 3. 创建总结 Agent
		ReactAgent summaryAgent = ReactAgent.builder()
				.name("summarizer")
				.model(chatModel)
				.instruction("你是一个总结专家，负责将分析结果提炼为简洁的结论")
				.tools(summaryTool)
				.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("research_data", new ReplaceStrategy());
			strategies.put("analysis_result", new ReplaceStrategy());
			strategies.put("final_summary", new ReplaceStrategy());
			return strategies;
		};

		// 4. 构建工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加 Agent 节点
		workflow.addNode("research", researchAgent.asNode(
				true,    // 包含历史消息
				false,   // 不返回推理过程
				"research_data"
		));

		workflow.addNode("analysis", analysisAgent.asNode(
				true,
				false,
				"analysis_result"
		));

		workflow.addNode("summary", summaryAgent.asNode(
				true,
				true,    // 返回完整推理过程
				"final_summary"
		));

		// 定义顺序执行流程
		workflow.addEdge(StateGraph.START, "research");
		workflow.addEdge("research", "analysis");
		workflow.addEdge("analysis", "summary");
		workflow.addEdge("summary", StateGraph.END);

		System.out.println("多Agent研究工作流构建完成");
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 工作流（Workflow）示例 ===\n");

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

			System.out.println("示例7: 混合使用Agent Node和普通Node");
			example7_hybridWorkflow();
			System.out.println();

			System.out.println("示例8: 执行工作流");
			example8_executeWorkflow();
			System.out.println();

			System.out.println("示例9: 多Agent协作工作流");
			example9_multiAgentResearchWorkflow();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

