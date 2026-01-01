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
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 人工介入（Human-in-the-Loop）示�?
 *
 * 演示如何使用人工介入Hook为Agent工具调用添加人工监督，包括：
 * 1. 配置中断和审�?
 * 2. 批准（approve）决�?
 * 3. 编辑（edit）决�?
 * 4. 拒绝（reject）决�?
 * 5. 处理多个工具调用
 * 6. Workflow中嵌套ReactAgent的人工中�?
 * 7. 实用工具方法
 *
 * 参考文�? advanced_doc/human-in-the-loop.md
 */
public class HumanInTheLoopExample {

	private final ChatModel chatModel;

	public HumanInTheLoopExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * 实用工具方法：批准所有工具调�?
	 */
	public static InterruptionMetadata approveAll(InterruptionMetadata interruptionMetadata) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
				.nodeId(interruptionMetadata.node())
				.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			builder.addToolFeedback(
					InterruptionMetadata.ToolFeedback.builder(toolFeedback)
							.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
							.build()
			);
		});

		return builder.build();
	}

	/**
	 * 实用工具方法：拒绝所有工具调�?
	 */
	public static InterruptionMetadata rejectAll(InterruptionMetadata interruptionMetadata, String reason) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
				.nodeId(interruptionMetadata.node())
				.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			builder.addToolFeedback(
					InterruptionMetadata.ToolFeedback.builder(toolFeedback)
							.result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
							.description(reason)
							.build()
			);
		});

		return builder.build();
	}

	/**
	 * 实用工具方法：编辑特定工具的参数
	 */
	public static InterruptionMetadata editTool(
			InterruptionMetadata interruptionMetadata,
			String toolName,
			String newArguments) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
				.nodeId(interruptionMetadata.node())
				.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			if (toolFeedback.getName().equals(toolName)) {
				builder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.arguments(newArguments)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
								.build()
				);
			}
			else {
				builder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
								.build()
				);
			}
		});

		return builder.build();
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
		HumanInTheLoopExample example = new HumanInTheLoopExample(chatModel);

		// 运行所有示�?
		example.runAllExamples();
	}

	/**
	 * 示例1：配置中断和基本使用
	 *
	 * 为特定工具配置人工审�?
	 */
	public void example1_basicConfiguration() {
		// 配置检查点保存器（人工介入需要检查点来处理中断）
		MemorySaver memorySaver = new MemorySaver();

		// 创建工具回调（示例）
		ToolCallback writeFileTool = FunctionToolCallback.builder("write_file", (args) -> "文件已写�?)
				.description("写入文件")
				.inputType(String.class)
				.build();

		ToolCallback executeSqlTool = FunctionToolCallback.builder("execute_sql", (args) -> "SQL已执�?)
				.description("执行SQL语句")
				.inputType(String.class)
				.build();

		ToolCallback readDataTool = FunctionToolCallback.builder("read_data", (args) -> "数据已读�?)
				.description("读取数据")
				.inputType(String.class)
				.build();

		// 创建人工介入Hook
		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("write_file", ToolConfig.builder()
						.description("文件写入操作需要审�?)
						.build())
				.approvalOn("execute_sql", ToolConfig.builder()
						.description("SQL执行操作需要审�?)
						.build())
				.build();

		// 创建Agent
		ReactAgent agent = ReactAgent.builder()
				.name("approval_agent")
				.model(chatModel)
				.tools(writeFileTool, executeSqlTool, readDataTool)
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		System.out.println("人工介入Hook配置示例完成");
	}

	/**
	 * 示例2：批准（approve）决�?
	 *
	 * 人工批准工具调用并继续执�?
	 */
	public void example2_approveDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback poetTool = FunctionToolCallback.builder("poem", (args) -> "春江潮水连海平，海上明月共潮�?..")
				.description("写诗工具")
				.inputType(String.class)
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("poem", ToolConfig.builder()
						.description("请确认诗歌创作操�?)
						.build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("poet_agent")
				.model(chatModel)
				.tools(List.of(poetTool))
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		String threadId = "user-session-001";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// 第一次调�?- 触发中断
		System.out.println("=== 第一次调用：期望中断 ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput(
				"帮我写一�?00字左右的�?,
				config
		);

		// 检查中断并处理
		if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

			System.out.println("检测到中断，需要人工审�?);
			List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
					interruptionMetadata.toolFeedbacks();

			for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
				System.out.println("工具: " + feedback.getName());
				System.out.println("参数: " + feedback.getArguments());
				System.out.println("描述: " + feedback.getDescription());
			}

			// 构建批准反馈
			InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
					.nodeId(interruptionMetadata.node())
					.state(interruptionMetadata.state());

			// 对每个工具调用设置批准决�?
			interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
				InterruptionMetadata.ToolFeedback approvedFeedback =
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
								.build();
				feedbackBuilder.addToolFeedback(approvedFeedback);
			});

			InterruptionMetadata approvalMetadata = feedbackBuilder.build();

			// 使用批准决策恢复执行
			RunnableConfig resumeConfig = RunnableConfig.builder()
					.threadId(threadId) // 相同的线程ID以恢复暂停的对话
					.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
					.build();

			// 第二次调用以恢复执行
			System.out.println("\n=== 第二次调用：使用批准决策恢复 ===");
			Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

			if (finalResult.isPresent()) {
				System.out.println("执行完成");
			}
		}

		System.out.println("批准决策示例执行完成");
	}

	/**
	 * 示例3：编辑（edit）决�?
	 *
	 * 人工编辑工具参数后继续执�?
	 */
	public void example3_editDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback executeSqlTool = FunctionToolCallback.builder("execute_sql", (args) -> "SQL执行结果")
				.description("执行SQL语句")
				.inputType(String.class)
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("execute_sql", ToolConfig.builder()
						.description("SQL执行操作需要审�?)
						.build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("sql_agent")
				.model(chatModel)
				.tools(executeSqlTool)
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		String threadId = "sql-session-001";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// 第一次调�?- 触发中断
		Optional<NodeOutput> result = agent.invokeAndGetOutput(
				"删除数据库中的旧记录",
				config
		);

		if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

			// 构建编辑反馈
			InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
					.nodeId(interruptionMetadata.node())
					.state(interruptionMetadata.state());

			interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
				// 修改工具参数
				String editedArguments = toolFeedback.getArguments()
						.replace("DELETE FROM records", "DELETE FROM old_records");

				InterruptionMetadata.ToolFeedback editedFeedback =
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.arguments(editedArguments)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
								.build();
				feedbackBuilder.addToolFeedback(editedFeedback);
			});

			InterruptionMetadata editMetadata = feedbackBuilder.build();

			// 使用编辑决策恢复执行
			RunnableConfig resumeConfig = RunnableConfig.builder()
					.threadId(threadId)
					.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, editMetadata)
					.build();

			Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

			System.out.println("编辑决策示例执行完成");
		}
	}

	/**
	 * 示例4：拒绝（reject）决�?
	 *
	 * 人工拒绝工具调用并终止当前流�?
	 */
	public void example4_rejectDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback deleteTool = FunctionToolCallback.builder("delete_data", (args) -> "数据已删�?)
				.description("删除数据")
				.inputType(String.class)
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("delete_data", ToolConfig.builder()
						.description("删除操作需要审�?)
						.build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("delete_agent")
				.model(chatModel)
				.tools(deleteTool)
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		String threadId = "delete-session-001";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// 第一次调�?- 触发中断
		Optional<NodeOutput> result = agent.invokeAndGetOutput(
				"删除所有用户数�?,
				config
		);

		if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

			// 构建拒绝反馈
			InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
					.nodeId(interruptionMetadata.node())
					.state(interruptionMetadata.state());

			interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
				InterruptionMetadata.ToolFeedback rejectedFeedback =
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
								.description("不允许删除操作，请使用归档功能代替�?)
								.build();
				feedbackBuilder.addToolFeedback(rejectedFeedback);
			});

			InterruptionMetadata rejectMetadata = feedbackBuilder.build();

			// 使用拒绝决策恢复执行
			RunnableConfig resumeConfig = RunnableConfig.builder()
					.threadId(threadId)
					.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, rejectMetadata)
					.build();

			Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

			System.out.println("拒绝决策示例执行完成");
		}
	}

	/**
	 * 示例5：处理多个工具调�?
	 *
	 * 一次性处理多个需要审批的工具调用
	 */
	public void example5_multipleTools() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback tool1 = FunctionToolCallback.builder("tool1", (args) -> "工具1结果")
				.description("工具1")
				.inputType(String.class)
				.build();

		ToolCallback tool2 = FunctionToolCallback.builder("tool2", (args) -> "工具2结果")
				.description("工具2")
				.inputType(String.class)
				.build();

		ToolCallback tool3 = FunctionToolCallback.builder("tool3", (args) -> "工具3结果")
				.description("工具3")
				.inputType(String.class)
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("tool1", ToolConfig.builder().description("工具1需要审�?).build())
				.approvalOn("tool2", ToolConfig.builder().description("工具2需要审�?).build())
				.approvalOn("tool3", ToolConfig.builder().description("工具3需要审�?).build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.tools(tool1, tool2, tool3)
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		String threadId = "multi-session-001";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		Optional<NodeOutput> result = agent.invokeAndGetOutput("执行所有工�?, config);

		if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

			InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
					.nodeId(interruptionMetadata.node())
					.state(interruptionMetadata.state());

			List<InterruptionMetadata.ToolFeedback> feedbacks = interruptionMetadata.toolFeedbacks();

			// 第一个工具：批准
			if (feedbacks.size() > 0) {
				feedbackBuilder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(feedbacks.get(0))
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
								.build()
				);
			}

			// 第二个工具：编辑
			if (feedbacks.size() > 1) {
				feedbackBuilder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(feedbacks.get(1))
								.arguments("{\"param\": \"new_value\"}")
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
								.build()
				);
			}

			// 第三个工具：拒绝
			if (feedbacks.size() > 2) {
				feedbackBuilder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(feedbacks.get(2))
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
								.description("不允许此操作")
								.build()
				);
			}

			InterruptionMetadata decisionsMetadata = feedbackBuilder.build();

			RunnableConfig resumeConfig = RunnableConfig.builder()
					.threadId(threadId)
					.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, decisionsMetadata)
					.build();

			Optional<NodeOutput> outputOptional = agent.invokeAndGetOutput("", resumeConfig);

			System.out.println("多个决策示例执行完成，最终状态：\n\n" + outputOptional.get().state());
		}
	}

	/**
	 * 示例6：Workflow中嵌套ReactAgent的人工中�?
	 *
	 * 演示如何在StateGraph工作流中嵌套带有HumanInTheLoopHook的ReactAgent�?
	 * 并处理工作流执行过程中的中断和恢�?
	 */
	public void example6_workflowWithHumanInTheLoop() throws Exception {
		// 创建工具回调
		ToolCallback searchTool = FunctionToolCallback
				.builder("search", (args) -> "搜索结果：AI Agent是能够感知环境、自主决策并采取行动的智能系统�?)
				.description("搜索工具，用于查找相关信�?)
				.inputType(String.class)
				.build();

		// 配置检查点保存器（人工介入需要检查点来处理中断）
		MemorySaver saver = new MemorySaver();

		// 创建带有人工介入Hook的ReactAgent
		ReactAgent qaAgent = ReactAgent.builder()
				.name("qa_agent")
				.model(chatModel)
				.instruction("你是一个问答专家，负责回答用户的问题。如果需要搜索信息，请使用search工具。\n用户问题：{cleaned_input}")
				.outputKey("qa_result")
				.saver(saver)
				.hooks(HumanInTheLoopHook.builder()
						.approvalOn("search", ToolConfig.builder()
								.description("搜索操作需要人工审批，请确认是否执行搜�?)
								.build())
						.build())
				.tools(searchTool)
				.enableLogging(true)
				.build();

		// 创建预处理Node：清理输�?
		class PreprocessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				String cleaned = input.trim();
				System.out.println("预处理节点：清理输入 -> " + cleaned);
				return Map.of("cleaned_input", cleaned);
			}
		}

		// 创建验证Node：验证结果质�?
		class ValidatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				Optional<Object> qaResultOpt = state.value("qa_result");
				if (qaResultOpt.isPresent() && qaResultOpt.get() instanceof Message message) {
					boolean isValid = message.getText().length() > 30; // 简单验证：答案长度需大于30
					System.out.println("验证节点：结果验�?-> " + (isValid ? "通过" : "不通过"));
					return Map.of("is_valid", isValid);
				}
				return Map.of("is_valid", false);
			}
		}

		// 定义状态管理策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("cleaned_input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			strategies.put("is_valid", new ReplaceStrategy());
			return strategies;
		};

		// 构建工作�?
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加普通Node
		workflow.addNode("preprocess", node_async(new PreprocessorNode()));
		workflow.addNode("validate", node_async(new ValidatorNode()));

		// 添加Agent Node（嵌套的ReactAgent�?
		workflow.addNode(qaAgent.name(), qaAgent.asNode(
				true,   // includeContents: 传递父图的消息历史
				false   // includeReasoning: 不返回推理过�?
		));

		// 定义流程：预处理 -> Agent处理 -> 验证
		workflow.addEdge(StateGraph.START, "preprocess");
		workflow.addEdge("preprocess", qaAgent.name());
		workflow.addEdge(qaAgent.name(), "validate");

		// 条件边：验证通过则结束，否则重新处理
		workflow.addConditionalEdges(
				"validate",
				edge_async(state -> {
					Boolean isValid = (Boolean) state.value("is_valid", false);
					return isValid ? "end" : qaAgent.name();
				}),
				Map.of(
						"end", StateGraph.END,
						qaAgent.name(), qaAgent.name()
				)
		);

		// 编译工作�?
		CompiledGraph compiledGraph = workflow.compile(
				CompileConfig.builder()
						.saverConfig(SaverConfig.builder().register(saver).build())
						.build()
		);

		String threadId = "workflow-hilt-001";
		Map<String, Object> input = Map.of("input", "请解释量子计算的基本原理");

		// 第一次调�?- 可能触发中断
		System.out.println("=== 第一次调用工作流：可能触发中�?===");
		Optional<NodeOutput> nodeOutputOptional = compiledGraph.invokeAndGetOutput(
				input,
				RunnableConfig.builder().threadId(threadId).build()
		);

		// 检查是否发生中�?
		if (nodeOutputOptional.isPresent() && nodeOutputOptional.get() instanceof InterruptionMetadata interruptionMetadata) {
			System.out.println("\n工作流被中断，等待人工审核�?);
			System.out.println("中断节点: " + interruptionMetadata.node());
			System.out.println("中断状�? " + interruptionMetadata.state());

			List<InterruptionMetadata.ToolFeedback> feedbacks = interruptionMetadata.toolFeedbacks();
			System.out.println("需要审批的工具调用数量: " + feedbacks.size());

			// 显示所有需要审批的工具调用
			for (InterruptionMetadata.ToolFeedback feedback : feedbacks) {
				System.out.println("\n工具名称: " + feedback.getName());
				System.out.println("工具参数: " + feedback.getArguments());
				System.out.println("工具描述: " + feedback.getDescription());
			}

			// 构建人工反馈（批准所有工具调用）
			InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
					.nodeId(interruptionMetadata.node())
					.state(interruptionMetadata.state());

			// 对每个工具调用设置批准决�?
			feedbacks.forEach(toolFeedback -> {
				feedbackBuilder.addToolFeedback(
						InterruptionMetadata.ToolFeedback.builder(toolFeedback)
								.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
								.build()
				);
			});

			InterruptionMetadata approvalMetadata = feedbackBuilder.build();

			// 使用批准决策恢复执行
			System.out.println("\n=== 第二次调用：使用批准决策恢复工作�?===");
			RunnableConfig resumableConfig = RunnableConfig.builder()
					.threadId(threadId)
					.addHumanFeedback(approvalMetadata)
					.build();

			nodeOutputOptional = compiledGraph.invokeAndGetOutput(Map.of(), resumableConfig);
			System.out.println("\n工作流中嵌套ReactAgent的人工中断示例执行完�?);

		}

	}

	/**
	 * 运行所有示�?
	 */
	public void runAllExamples() {
		System.out.println("=== 人工介入（Human-in-the-Loop）示�?===\n");

		try {
//			System.out.println("示例1: 配置中断和基本使�?);
//			example1_basicConfiguration();
//			System.out.println();
//
//			System.out.println("示例2: 批准（approve）决�?);
//			example2_approveDecision();
//			System.out.println();
//
//			System.out.println("示例3: 编辑（edit）决�?);
//			example3_editDecision();
//			System.out.println();
//
//			System.out.println("示例4: 拒绝（reject）决�?);
//			example4_rejectDecision();
//			System.out.println();
//
//			System.out.println("示例5: 处理多个工具调用决策");
//			example5_multipleTools();
//			System.out.println();

			System.out.println("示例6: Workflow中嵌套ReactAgent的人工中�?);
			example6_workflowWithHumanInTheLoop();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}
}

