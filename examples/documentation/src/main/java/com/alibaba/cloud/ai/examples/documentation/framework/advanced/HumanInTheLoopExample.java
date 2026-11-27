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
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 演示 ReactAgent 的条件审批、批次决策、人工回答和嵌套工作流恢复。
 */
public class HumanInTheLoopExample {

	private static final ObjectReader ARGUMENTS =
			new ObjectMapper().reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	private final ChatModel chatModel;

	public HumanInTheLoopExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * 为允许批准的批次生成逐项批准决策。
	 */
	public static InterruptionMetadata approveAll(InterruptionMetadata interruptionMetadata) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(interruptionMetadata);

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			builder.addToolFeedback(
					ToolFeedback.builder(toolFeedback)
							.result(FeedbackResult.APPROVED)
							.build()
			);
		});

		return builder.build();
	}

	/**
	 * 为允许拒绝的批次生成逐项拒绝决策。
	 */
	public static InterruptionMetadata rejectAll(InterruptionMetadata interruptionMetadata, String reason) {
		InterruptionMetadata.Builder builder = InterruptionMetadata.builder(interruptionMetadata);

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			builder.addToolFeedback(
					ToolFeedback.builder(toolFeedback)
							.result(FeedbackResult.REJECTED)
							.description(reason)
							.build()
			);
		});

		return builder.build();
	}

	/**
	 * 按调用 ID 编辑一项，并为批次中的其余调用生成批准决策。
	 */
	public static InterruptionMetadata editCall(
			InterruptionMetadata interruptionMetadata,
			String callId,
			String newArguments) {
		if (interruptionMetadata.toolFeedbacks().stream().noneMatch(request -> request.getId().equals(callId))) {
			throw new IllegalArgumentException("Unknown tool call ID " + callId);
		}
		var decisions = interruptionMetadata.toolFeedbacks().stream().map(request -> {
			var decision = ToolFeedback.builder(request);
			return request.getId().equals(callId)
					? decision.result(FeedbackResult.EDITED).arguments(newArguments).build()
					: decision.result(FeedbackResult.APPROVED).build();
		}).toList();
		return InterruptionMetadata.builder(interruptionMetadata).toolFeedbacks(decisions).build();
	}

	/**
	 * 创建仅接受人工回答的提问工具。
	 */
	public ReactAgent createQuestionAgent() {
		var askUser = FunctionToolCallback.builder("ask_user", (Question question) -> {
			throw new IllegalStateException("The answer must come from the user");
		})
				.description("需要用户做选择时，提交问题和选项")
				.inputType(Question.class)
				.build();
		var review = HumanInTheLoopHook.builder().approvalOn("ask_user", ToolConfig.builder()
				.description("请回答 Agent 的问题")
				.allowedDecisions(FeedbackResult.RESPONDED)
				.build()).build();
		return ReactAgent.builder().name("question_agent").model(chatModel).tools(askUser).hooks(review)
				.saver(new MemorySaver()).build();
	}

	/**
	 * 对中断中的每个问题提交回答，供 Agent 作为对应的工具结果继续处理。
	 */
	public static RunnableConfig answerQuestions(RunnableConfig thread, InterruptionMetadata interruption,
			Map<String, String> answersByCallId) {
		var answers = interruption.toolFeedbacks().stream()
				.map(request -> ToolFeedback.builder(request).result(FeedbackResult.RESPONDED)
						.response(answersByCallId.get(request.getId())).build())
				.toList();
		return resumeWith(thread, InterruptionMetadata.builder(interruption).toolFeedbacks(answers).build());
	}

	record Question(String question, List<String> options) {
	}

	private static RunnableConfig resumeWith(RunnableConfig thread, InterruptionMetadata feedback) {
		return RunnableConfig.builder(thread).addHumanFeedback(feedback).build();
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
	 * 预览公告直接执行，模拟发布须经批准或拒绝。
	 */
	public ReactAgent createPublishingAgent() {
		var publishNotice = FunctionToolCallback.builder("publish_notice",
				(PublishNotice request) -> (request.preview() ? "预览 " : "模拟发布 ") + request.content())
				.description("将公告生成预览或模拟发布，preview 字段指定是否仅预览")
				.inputType(PublishNotice.class)
				.build();
		var review = HumanInTheLoopHook.builder()
				.approvalOn("publish_notice", ToolConfig.builder()
						.description("请审核要发布的公告")
						.allowedDecisions(FeedbackResult.APPROVED, FeedbackResult.REJECTED)
						.when(request -> requiresPublishReview(request.getArguments()))
						.build())
				.build();
		return ReactAgent.builder()
				.name("publication_agent")
				.model(chatModel)
				.tools(publishNotice)
				.hooks(review)
				.saver(new MemorySaver())
				.build();
	}

	private static boolean requiresPublishReview(String arguments) {
		if (arguments == null || arguments.isBlank()) {
			return true;
		}
		try {
			var preview = ARGUMENTS.readTree(arguments).path("preview");
			return !preview.isBoolean() || !preview.booleanValue();
		}
		catch (JsonProcessingException ex) {
			return true;
		}
	}

	record PublishNotice(String content, boolean preview) {
	}

	public void example1_basicConfiguration() throws Exception {
		var agent = createPublishingAgent();
		var config = RunnableConfig.builder().threadId("publication-session").build();
		var preview = agent.invokeAndGetOutput("用 publish_notice 预览公告「周五发布新版本」，设置 preview 为 true。", config);
		if (preview.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			return;
		}
		var output = agent.invokeAndGetOutput("用 publish_notice 模拟发布这条公告，设置 preview 为 false。", config);
		if (output.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			// 模拟审查者批准发布。
			agent.invokeAndGetOutput("", resumeWith(config, approveAll(interruption)));
		}
	}

	private static void printReview(InterruptionMetadata interruption) {
		System.out.printf("中断 ID %s%n", interruption.interruptionId().orElseThrow());
		for (var request : interruption.toolFeedbacks()) {
			System.out.printf("%s %s %s %s %s%n", request.getId(), request.getName(),
					request.getArguments(), request.getDescription(), request.getAllowedDecisions());
		}
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
				.inputType(String.class)
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

		if (result.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			agent.invokeAndGetOutput("", resumeWith(config, approveAll(interruption)));
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

		var executeSqlTool = FunctionToolCallback.builder("execute_sql", (SqlQuery request) -> "模拟执行 " + request.query())
				.description("模拟执行给定的 SQL")
				.inputType(SqlQuery.class)
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

		if (result.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			var callId = interruption.toolFeedbacks().get(0).getId();
			var review = editCall(interruption, callId, "{\"query\":\"SELECT * FROM records LIMIT 10\"}");
			agent.invokeAndGetOutput("", resumeWith(config, review));
			System.out.println("编辑决策示例执行完成");
		}
	}

	record SqlQuery(String query) {
	}

	/**
	 * 示例4：拒绝（reject）决策
	 *
	 * 跳过被拒绝的工具调用，将拒绝理由返回给模型。
	 */
	public void example4_rejectDecision() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		ToolCallback deleteTool = FunctionToolCallback.builder("delete_data", (args) -> "模拟删除")
				.description("模拟删除数据")
				.inputType(String.class)
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

		if (result.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			var feedback = rejectAll(interruption, "请先归档数据。");
			agent.invokeAndGetOutput("", resumeWith(config, feedback));
			System.out.println("拒绝决策示例执行完成");
		}
	}

	/**
	 * 示例5：处理多个工具调用
	 *
	 * 一次性处理多个需要审批的工具调用
	 * 使用 {@code @Tool} 注解方式定义工具
	 */
	public void example5_multipleTools() throws Exception {
		MemorySaver memorySaver = new MemorySaver();

		// 使用 @Tool 注解方式定义多个工具
		MultiTools multiTools = new MultiTools();

		HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
				.approvalOn("tool1", ToolConfig.builder().description("工具1需要审批").build())
				.approvalOn("tool2", ToolConfig.builder().description("工具2需要审批").build())
				.approvalOn("tool3", ToolConfig.builder().description("工具3需要审批").build())
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.methodTools(multiTools)
				.hooks(List.of(humanInTheLoopHook))
				.saver(memorySaver)
				.build();

		String threadId = "multi-session-001";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		var output = agent.invokeAndGetOutput("在一轮中调用 tool1、tool2 和 tool3。", config);
		if (output.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			var decisions = interruption.toolFeedbacks().stream().map(request -> {
				var decision = ToolFeedback.builder(request);
				return switch (request.getName()) {
					case "tool1" -> decision.result(FeedbackResult.APPROVED).build();
					case "tool2" -> decision.result(FeedbackResult.EDITED).arguments("{\"content\":\"修订内容\"}").build();
					case "tool3" -> decision.result(FeedbackResult.REJECTED).description("请跳过此操作。").build();
					default -> throw new IllegalArgumentException("Unexpected tool " + request.getName());
				};
			}).toList();
			var feedback = InterruptionMetadata.builder(interruption).toolFeedbacks(decisions).build();
			agent.invokeAndGetOutput("", resumeWith(config, feedback));
		}
	}

	/**
	 * 示例6：Workflow中嵌套ReactAgent的人工中断
	 *
	 * 演示如何在StateGraph工作流中嵌套带有HumanInTheLoopHook的ReactAgent，
	 * 并处理工作流执行过程中的中断和恢复
	 */
	public void example6_workflowWithHumanInTheLoop() throws Exception {
		// 创建工具回调
		ToolCallback searchTool = FunctionToolCallback
				.builder("search", (args) -> "搜索结果：AI Agent是能够感知环境、自主决策并采取行动的智能系统。")
				.description("搜索工具，用于查找相关信息")
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
								.description("搜索操作需要人工审批，请确认是否执行搜索")
								.build())
						.build())
				.tools(searchTool)
				.enableLogging(true)
				.build();

		// 创建预处理Node：清理输入
		class PreprocessorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				String input = state.value("input", "").toString();
				String cleaned = input.trim();
				System.out.println("预处理节点：清理输入 -> " + cleaned);
				return Map.of("cleaned_input", cleaned);
			}
		}

		// 创建验证Node：验证结果质量
		class ValidatorNode implements NodeAction {
			@Override
			public Map<String, Object> apply(OverAllState state) throws Exception {
				Optional<Object> qaResultOpt = state.value("qa_result");
				if (qaResultOpt.isPresent() && qaResultOpt.get() instanceof Message message) {
					boolean isValid = message.getText().length() > 30; // 简单验证：答案长度需大于30
					System.out.println("验证节点：结果验证 -> " + (isValid ? "通过" : "不通过"));
					return Map.of("is_valid", isValid);
				}
				return Map.of("is_valid", false);
			}
		}

		// 定义状态管理策略
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("cleaned_input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			strategies.put("is_valid", new ReplaceStrategy());
			return strategies;
		};

		// 构建工作流
		StateGraph workflow = new StateGraph(keyStrategyFactory);

		// 添加普通Node
		workflow.addNode("preprocess", node_async(new PreprocessorNode()));
		workflow.addNode("validate", node_async(new ValidatorNode()));

		// 添加Agent Node（嵌套的ReactAgent）
		workflow.addNode(qaAgent.name(), qaAgent.asNode(
				true,   // includeContents: 传递父图的消息历史
				false   // includeReasoning: 不返回推理过程
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

		// 编译工作流
		CompiledGraph compiledGraph = workflow.compile(
				CompileConfig.builder()
						.saverConfig(SaverConfig.builder().register(saver).build())
						.build()
		);

		var config = RunnableConfig.builder().threadId("workflow-hitl-001").build();
		Map<String, Object> input = Map.of("input", "请解释量子计算的基本原理");

		var output = compiledGraph.invokeAndGetOutput(input, config);
		if (output.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			compiledGraph.invokeAndGetOutput(Map.of(), resumeWith(config, approveAll(interruption)));
		}
	}

	/**
	 * 用示例答案恢复提问工具。实际应用由界面按调用 ID 收集答案。
	 */
	public Optional<NodeOutput> example7_respondDecision(String answer) throws Exception {
		var agent = createQuestionAgent();
		var config = RunnableConfig.builder().threadId("question-session").build();
		var output = agent.invokeAndGetOutput("调用 ask_user，询问用户希望用简体中文还是英语接收摘要。", config);
		if (output.orElse(null) instanceof InterruptionMetadata interruption) {
			printReview(interruption);
			var answers = interruption.toolFeedbacks().stream()
					.collect(Collectors.toMap(ToolFeedback::getId, request -> answer));
			return agent.invokeAndGetOutput("", answerQuestions(config, interruption, answers));
		}
		return output;
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

			System.out.println("示例6: Workflow中嵌套ReactAgent的人工中断");
			example6_workflowWithHumanInTheLoop();
			System.out.println();

			System.out.println("示例7 人工回答");
			example7_respondDecision("简体中文");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 使用 @Tool 注解定义的多工具类，用于示例5
	 */
	static class MultiTools {

		@Tool(name = "tool1", description = "工具1")
		public String tool1(@ToolParam(description = "输入工具1模拟审批内容") String content) {
			return "工具1结果";
		}

		@Tool(name = "tool2", description = "工具2")
		public String tool2(@ToolParam(description = "输入工具2模拟审批内容") String content) {
			return "工具2收到 " + content;
		}

		@Tool(name = "tool3", description = "工具3")
		public String tool3(@ToolParam(description = "输入工具3模拟审批内容") String content) {
			return "工具3结果";
		}
	}
}

