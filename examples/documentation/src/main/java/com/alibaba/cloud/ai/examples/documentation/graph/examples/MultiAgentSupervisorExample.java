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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Multi-Agent Supervisor 示例
 *
 * 演示如何使用 LLM 来协调不同的 agents�?
 * 创建一�?agent 组，其中包含一�?supervisor agent 来帮助委派任务�?
 *
 * 架构�?
 * - Supervisor Agent: 负责路由到不同的 worker agents
 * - Researcher Agent: 负责研究任务，使用搜索工�?
 * - Coder Agent: 负责代码执行任务，使用代码执行工�?
 */
public class MultiAgentSupervisorExample {

	private final ChatModel chatModel;
	private final ChatModel chatModelWithTool;

	public MultiAgentSupervisorExample(ChatModel chatModel, ChatModel chatModelWithTool) {
		this.chatModel = chatModel;
		this.chatModelWithTool = chatModelWithTool;
	}

	/**
	 * Main 方法
	 */
	public static void main(String[] args) {
		System.out.println("=== Multi-Agent Supervisor 示例 ===\n");

		// 检查环境变�?
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			System.err.println("错误：请先配�?AI_DASHSCOPE_API_KEY 环境变量");
			System.err.println("示例需�?DashScope API Key 才能运行");
			return;
		}

		try {
			// 创建 DashScope API 实例
			DashScopeApi dashScopeApi = DashScopeApi.builder()
					.apiKey(apiKey)
					.build();

			// 创建 ChatModel（用�?Supervisor�?
			ChatModel chatModel = DashScopeChatModel.builder()
					.dashScopeApi(dashScopeApi)
					.build();

			// 创建 ChatModel（用�?Worker Agents，可以使用相同的模型�?
			ChatModel chatModelWithTool = DashScopeChatModel.builder()
					.dashScopeApi(dashScopeApi)
					.build();

			// 创建示例实例
			MultiAgentSupervisorExample example = new MultiAgentSupervisorExample(
					chatModel, chatModelWithTool);

			// 创建 Graph
			System.out.println("创建 Multi-Agent Supervisor Graph...");
			CompiledGraph graph = example.createSupervisorGraph();
			System.out.println("Graph 创建完成\n");

			// 执行示例 1: Supervisor -> Coder
			example.executeGraphWithCoder(graph);

			// 执行示例 2: Supervisor -> Researcher
			example.executeGraphWithResearcher(graph);

			System.out.println("\n所有示例执行完�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 创建 Multi-Agent Supervisor Graph
	 */
	public CompiledGraph createSupervisorGraph() throws GraphStateException {
		// 定义状态管理策�?
		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			strategies.put("next", new ReplaceStrategy());
			return strategies;
		};

		// 创建 agents
		String[] members = {"researcher", "coder"};
		SupervisorNode supervisor = new SupervisorNode(chatModel, members);
		ResearcherNode researcher = new ResearcherNode(chatModelWithTool);
		CoderNode coder = new CoderNode(chatModelWithTool);

		// 构建 StateGraph
		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("supervisor", node_async(supervisor))
				.addNode("researcher", node_async(researcher))
				.addNode("coder", node_async(coder))
				.addEdge(START, "supervisor")
				.addConditionalEdges(
						"supervisor",
						edge_async(state -> {
							String next = (String) state.value("next").orElse("FINISH");
							return next;
						}),
						Map.of(
								"FINISH", END,
								"researcher", "researcher",
								"coder", "coder"
						)
				)
				.addEdge("researcher", "supervisor")
				.addEdge("coder", "supervisor");

		return workflow.compile();
	}

	/**
	 * 执行 Graph（Supervisor -> Coder�?
	 */
	public void executeGraphWithCoder(CompiledGraph graph) {
		System.out.println("\n=== 执行 Graph (Supervisor -> Coder) ===");

		Map<String, Object> input = Map.of(
				"messages", List.of(
						Map.of("role", "user", "content", "1 + 1 的结果是多少�?)
				)
		);

		RunnableConfig config = RunnableConfig.builder()
				.threadId("supervisor-coder-thread")
				.build();

		graph.stream(input, config)
				.doOnNext(event -> System.out.println("节点: " + event.node() + ", 状�? " + event.state()))
				.doOnError(error -> System.err.println("流错�? " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完�?))
				.blockLast();
	}

	/**
	 * 执行 Graph（Supervisor -> Researcher�?
	 */
	public void executeGraphWithResearcher(CompiledGraph graph) {
		System.out.println("\n=== 执行 Graph (Supervisor -> Researcher) ===");

		Map<String, Object> input = Map.of(
				"messages", List.of(
						Map.of("role", "user", "content", "下一届冬奥会在哪里举行？")
				)
		);

		RunnableConfig config = RunnableConfig.builder()
				.threadId("supervisor-researcher-thread")
				.build();

		graph.stream(input, config)
				.doOnNext(event -> System.out.println("节点: " + event.node() + ", 状�? " + event.state()))
				.doOnError(error -> System.err.println("流错�? " + error.getMessage()))
				.doOnComplete(() -> System.out.println("流完�?))
				.blockLast();
	}

	/**
	 * 搜索工具（模拟实现）
	 */
	public static class SearchTool implements BiFunction<SearchTool.SearchRequest, ToolContext, String> {

		public static final String DESCRIPTION = """
				使用此工具在互联网上执行搜索�?
				
				Usage:
				- query 参数是要搜索的查询字符串
				- 工具会执行搜索并返回搜索结果
				- 这是一个模拟实现，返回固定的搜索结�?
				""";

		@Override
		public String apply(SearchRequest request, ToolContext toolContext) {
			System.out.println("搜索查询: '" + request.query + "'");
			// 模拟搜索结果
			return "下一届冬奥会将在意大利的科尔蒂纳举行，时间是2026�?;
		}

		/**
		 * 搜索请求结构
		 */
		public static class SearchRequest {
			@JsonProperty(required = true)
			@JsonPropertyDescription("要搜索的查询字符�?)
			public String query;

			public SearchRequest() {
			}

			public SearchRequest(String query) {
				this.query = query;
			}
		}
	}

	/**
	 * 代码执行工具（模拟实现）
	 */
	public static class CoderTool implements BiFunction<CoderTool.CodeRequest, ToolContext, String> {

		public static final String DESCRIPTION = """
				使用此工具执�?Java 代码并进行数学计算�?
				
				Usage:
				- request 参数是要执行的代码请�?
				- 如果你想查看某个值的输出，应该使�?`System.out.println(...);` 打印出来
				- 这对用户可见
				- 这是一个模拟实现，返回固定的执行结�?
				""";

		@Override
		public String apply(CodeRequest request, ToolContext toolContext) {
			System.out.println("代码执行请求: '" + request.request + "'");
			// 模拟代码执行结果
			return "2";
		}

		/**
		 * 代码执行请求结构
		 */
		public static class CodeRequest {
			@JsonProperty(required = true)
			@JsonPropertyDescription("要执行的代码请求")
			public String request;

			public CodeRequest() {
			}

			public CodeRequest(String request) {
				this.request = request;
			}
		}
	}

	/**
	 * Supervisor Agent Node
	 * 负责决定将任务路由到哪个 worker
	 */
	public static class SupervisorNode implements NodeAction {
		private final ChatClient chatClient;
		private final String[] members;

		public SupervisorNode(ChatModel model, String[] members) {
			this.chatClient = ChatClient.builder(model).build();
			this.members = members;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 获取最后一条消�?
			List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
			if (messages.isEmpty()) {
				throw new IllegalStateException("No messages in state");
			}

			// 获取最后一条消息的文本内容
			String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

			// 构建系统提示
			String membersList = String.join(", ", members);
			String systemPrompt = String.format(
					"你是一�?supervisor，负责管理以�?workers 之间的对话：%s。\n" +
							"根据以下用户请求，响应应该由哪个 worker 来处理。\n" +
							"每个 worker 将执行任务并返回结果和状态。\n" +
							"当任务完成时，响�?FINISH。\n" +
							"只返�?worker 名称�?s）或 FINISH，不要返回其他内容�?,
					membersList, membersList
			);

			// 调用 LLM 决定路由
			String result = chatClient.prompt()
					.system(systemPrompt)
					.user("用户消息: " + lastMessageText)
					.call()
					.content();

			// 清理结果，确保只返回 worker 名称�?FINISH
			String next = normalizeRoute(result, members);

			return Map.of("next", next);
		}

		/**
		 * 规范化路由结�?
		 */
		private String normalizeRoute(String result, String[] members) {
			if (result == null || result.trim().isEmpty()) {
				return "FINISH";
			}

			String normalized = result.trim().toLowerCase();

			// 检查是否是 FINISH
			if (normalized.equals("finish") || normalized.contains("finish")) {
				return "FINISH";
			}

			// 检查是否匹配任何成�?
			for (String member : members) {
				if (normalized.equals(member.toLowerCase()) ||
						normalized.contains(member.toLowerCase())) {
					return member;
				}
			}

			// 如果无法确定，根据消息内容推�?
			// 这里可以根据实际需求添加更智能的路由逻辑
			// 默认返回第一�?worker
			return members.length > 0 ? members[0] : "FINISH";
		}

		private String extractTextFromMessage(Object message) {
			if (message instanceof Map) {
				Map<?, ?> msgMap = (Map<?, ?>) message;
				Object content = msgMap.get("content");
				if (content != null) {
					return content.toString();
				}
			}
			return message.toString();
		}
	}

	/**
	 * Researcher Agent Node
	 * 负责执行研究任务
	 */
	public static class ResearcherNode implements NodeAction {
		private final ChatClient chatClient;

		public ResearcherNode(ChatModel model) {
			ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
					.description(SearchTool.DESCRIPTION)
					.inputType(SearchTool.SearchRequest.class)
					.build();

			this.chatClient = ChatClient.builder(model)
					.defaultTools(searchTool)
					.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 获取最后一条消�?
			List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
			if (messages.isEmpty()) {
				throw new IllegalStateException("No messages in state");
			}

			String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

			// 使用 ChatClient 调用 LLM，LLM 可能会调用搜索工�?
			String result = chatClient.prompt()
					.user(lastMessageText)
					.call()
					.content();

			// 返回结果消息
			return Map.of("messages", List.of(
					Map.of("role", "assistant", "content", result)
			));
		}

		private String extractTextFromMessage(Object message) {
			if (message instanceof Map) {
				Map<?, ?> msgMap = (Map<?, ?>) message;
				Object content = msgMap.get("content");
				if (content != null) {
					return content.toString();
				}
			}
			return message.toString();
		}
	}

	/**
	 * Coder Agent Node
	 * 负责执行代码任务
	 */
	public static class CoderNode implements NodeAction {
		private final ChatClient chatClient;

		public CoderNode(ChatModel model) {
			ToolCallback coderTool = FunctionToolCallback.builder("executeCode", new CoderTool())
					.description(CoderTool.DESCRIPTION)
					.inputType(CoderTool.CodeRequest.class)
					.build();

			this.chatClient = ChatClient.builder(model)
					.defaultTools(coderTool)
					.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 获取最后一条消�?
			List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
			if (messages.isEmpty()) {
				throw new IllegalStateException("No messages in state");
			}

			String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

			// 使用 ChatClient 调用 LLM，LLM 可能会调用代码执行工�?
			String result = chatClient.prompt()
					.user(lastMessageText)
					.call()
					.content();

			// 返回结果消息
			return Map.of("messages", List.of(
					Map.of("role", "assistant", "content", result)
			));
		}

		private String extractTextFromMessage(Object message) {
			if (message instanceof Map) {
				Map<?, ?> msgMap = (Map<?, ?>) message;
				Object content = msgMap.get("content");
				if (content != null) {
					return content.toString();
				}
			}
			return message.toString();
		}
	}
}

