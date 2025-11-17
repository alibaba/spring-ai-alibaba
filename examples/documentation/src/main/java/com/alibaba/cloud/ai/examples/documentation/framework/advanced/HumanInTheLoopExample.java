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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Optional;

/**
 * 人工介入（Human-in-the-Loop）示例
 *
 * 演示如何使用人工介入Hook为Agent工具调用添加人工监督，包括：
 * 1. 配置中断和审批
 * 2. 批准（approve）决策
 * 3. 编辑（edit）决策
 * 4. 拒绝（reject）决策
 * 5. 完整示例
 * 6. 实用工具方法
 *
 * 参考文档: advanced_doc/human-in-the-loop.md
 */
public class HumanInTheLoopExample {

	private final ChatModel chatModel;

	public HumanInTheLoopExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * 实用工具方法：批准所有工具调用
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
	 * 实用工具方法：拒绝所有工具调用
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
		HumanInTheLoopExample example = new HumanInTheLoopExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：配置中断和基本使用
	 *
	 * 为特定工具配置人工审批
	 */
	public void example1_basicConfiguration() {
		// 配置检查点保存器（人工介入需要检查点来处理中断）
		MemorySaver memorySaver = new MemorySaver();

		// 创建工具回调（示例）
		ToolCallback writeFileTool = FunctionToolCallback.builder("write_file", (args) -> "文件已写入")
				.description("写入文件")
				.build();

		ToolCallback executeSqlTool = FunctionToolCallback.builder("execute_sql", (args) -> "SQL已执行")
				.description("执行SQL语句")
				.build();

		ToolCallback readDataTool = FunctionToolCallback.builder("read_data", (args) -> "数据已读取")
				.description("读取数据")
				.build();

		// 创建人工介入Hook
		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("write_file", ToolConfig.builder()
						.description("文件写入操作需要审批")
						.build())
				.approvalOn("execute_sql", ToolConfig.builder()
						.description("SQL执行操作需要审批")
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
	 * 示例2：批准（approve）决策
	 *
	 * 人工批准工具调用并继续执行
	 */
	public void example2_approveDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback poetTool = FunctionToolCallback.builder("poem", (args) -> "春江潮水连海平，海上明月共潮生...")
				.description("写诗工具")
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("poem", ToolConfig.builder()
						.description("请确认诗歌创作操作")
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

		// 第一次调用 - 触发中断
		System.out.println("=== 第一次调用：期望中断 ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput(
				"帮我写一首100字左右的诗",
				config
		);

		// 检查中断并处理
		if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

			System.out.println("检测到中断，需要人工审批");
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

			// 对每个工具调用设置批准决策
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
	 * 示例3：编辑（edit）决策
	 *
	 * 人工编辑工具参数后继续执行
	 */
	public void example3_editDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback executeSqlTool = FunctionToolCallback.builder("execute_sql", (args) -> "SQL执行结果")
				.description("执行SQL语句")
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("execute_sql", ToolConfig.builder()
						.description("SQL执行操作需要审批")
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

		// 第一次调用 - 触发中断
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
	 * 示例4：拒绝（reject）决策
	 *
	 * 人工拒绝工具调用并终止当前流程
	 */
	public void example4_rejectDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback deleteTool = FunctionToolCallback.builder("delete_data", (args) -> "数据已删除")
				.description("删除数据")
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("delete_data", ToolConfig.builder()
						.description("删除操作需要审批")
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

		// 第一次调用 - 触发中断
		Optional<NodeOutput> result = agent.invokeAndGetOutput(
				"删除所有用户数据",
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
								.description("不允许删除操作，请使用归档功能代替。")
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
	 * 示例5：处理多个工具调用
	 *
	 * 一次性处理多个需要审批的工具调用
	 */
	public void example5_multipleTools() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback tool1 = FunctionToolCallback.builder("tool1", (args) -> "工具1结果")
				.description("工具1")
				.build();

		ToolCallback tool2 = FunctionToolCallback.builder("tool2", (args) -> "工具2结果")
				.description("工具2")
				.build();

		ToolCallback tool3 = FunctionToolCallback.builder("tool3", (args) -> "工具3结果")
				.description("工具3")
				.build();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("tool1", ToolConfig.builder().description("工具1需要审批").build())
				.approvalOn("tool2", ToolConfig.builder().description("工具2需要审批").build())
				.approvalOn("tool3", ToolConfig.builder().description("工具3需要审批").build())
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

		Optional<NodeOutput> result = agent.invokeAndGetOutput("执行所有工具", config);

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

			agent.invokeAndGetOutput("", resumeConfig);

			System.out.println("多个决策示例执行完成");
		}
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 人工介入（Human-in-the-Loop）示例 ===\n");

		try {
			System.out.println("示例1: 配置中断和基本使用");
			example1_basicConfiguration();
			System.out.println();

			System.out.println("示例2: 批准（approve）决策");
			example2_approveDecision();
			System.out.println();

			System.out.println("示例3: 编辑（edit）决策");
			example3_editDecision();
			System.out.println();

			System.out.println("示例4: 拒绝（reject）决策");
			example4_rejectDecision();
			System.out.println();

			System.out.println("示例5: 处理多个工具调用决策");
			example5_multipleTools();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

