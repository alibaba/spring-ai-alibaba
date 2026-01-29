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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCancelledException;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStateCollector;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;

/**
 * 异步工具执行示例 (Async Tool Execution Example)
 *
 * <p>本示例展示 Issue #3988 引入的异步工具支持功能，包括：</p>
 * <ul>
 *   <li>AsyncToolCallback - 基础异步工具接口</li>
 *   <li>CancellableAsyncToolCallback - 支持取消的异步工具接口</li>
 *   <li>CancellationToken - 协作取消机制</li>
 *   <li>StateAwareToolCallback - 状态感知工具</li>
 *   <li>ToolStateCollector - 并行执行时的状态收集与合并</li>
 *   <li>AgentToolNode - 并行工具执行配置</li>
 * </ul>
 *
 * <p>参考文档: Issue #3988 - Async Tool Support</p>
 *
 * @author disaster
 * @since 1.0.0
 */
public class AsyncToolExecutionExample {

	private final ChatModel chatModel;

	public AsyncToolExecutionExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	/**
	 * Main 方法：运行所有示例
	 *
	 * <p>注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量</p>
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
			System.err.println("错误：请先配置 ChatModel 实例");
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量");
			return;
		}

		// 创建示例实例
		AsyncToolExecutionExample example = new AsyncToolExecutionExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	// ==================== 示例 1：基础异步工具 ====================

	/**
	 * 示例 1：基础异步工具 (AsyncToolCallback)
	 *
	 * <p>AsyncToolCallback 接口允许工具异步执行，返回 CompletableFuture。
	 * 这对于需要执行 I/O 操作或长时间运行的任务非常有用。</p>
	 *
	 * <p>关键特性：</p>
	 * <ul>
	 *   <li>callAsync() 返回 CompletableFuture&lt;String&gt;</li>
	 *   <li>可自定义超时时间 (默认 5 分钟)</li>
	 *   <li>自动处理同步调用的回退 (call 方法会阻塞等待结果)</li>
	 * </ul>
	 */
	public void example1_basicAsyncTool() {
		System.out.println("=== 示例 1：基础异步工具 (AsyncToolCallback) ===\n");

		// 创建简单的异步工具
		AsyncToolCallback asyncWeatherTool = new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("async_weather")
					.description("异步获取天气信息")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				// 使用 CompletableFuture.supplyAsync 异步执行
				return CompletableFuture.supplyAsync(() -> {
					System.out.println("  [异步执行] 开始获取天气数据...");

					// 模拟异步 I/O 操作（如 HTTP 请求）
					try {
						Thread.sleep(1000); // 模拟网络延迟
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("获取天气数据被中断", e);
					}

					System.out.println("  [异步执行] 天气数据获取完成");
					return "{\"temperature\": 25, \"condition\": \"sunny\", \"city\": \"北京\"}";
				});
			}

			@Override
			public Duration getTimeout() {
				// 自定义超时时间为 30 秒
				return Duration.ofSeconds(30);
			}

			@Override
			public String call(String toolInput) {
				// 同步调用时，阻塞等待异步结果
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 测试异步工具
		System.out.println("测试异步调用:");
		CompletableFuture<String> future = asyncWeatherTool.callAsync("{\"city\":\"北京\"}", new ToolContext(Map.of()));

		// 非阻塞：可以做其他事情
		System.out.println("  异步调用已提交，等待结果...");

		// 获取结果
		String result = future.join();
		System.out.println("  结果: " + result);

		System.out.println("\n测试同步调用 (自动阻塞等待):");
		String syncResult = asyncWeatherTool.call("{\"city\":\"上海\"}");
		System.out.println("  结果: " + syncResult);

		System.out.println();
	}

	// ==================== 示例 2：可取消的异步工具 ====================

	/**
	 * 示例 2：可取消的异步工具 (CancellableAsyncToolCallback)
	 *
	 * <p>CancellableAsyncToolCallback 扩展了 AsyncToolCallback，支持协作取消。
	 * 当工具执行超时或需要提前终止时，可以通过 CancellationToken 通知工具优雅停止。</p>
	 *
	 * <p>关键特性：</p>
	 * <ul>
	 *   <li>接收 CancellationToken 参数</li>
	 *   <li>支持 isCancelled() 检查</li>
	 *   <li>支持 throwIfCancelled() 抛出异常</li>
	 *   <li>支持 onCancel() 注册清理回调</li>
	 * </ul>
	 */
	public void example2_cancellableAsyncTool() {
		System.out.println("=== 示例 2：可取消的异步工具 (CancellableAsyncToolCallback) ===\n");

		// 模拟的资源管理器（用于演示取消回调）
		AtomicInteger resourcesAllocated = new AtomicInteger(0);

		// 创建支持取消的异步工具
		CancellableAsyncToolCallback cancellableSearchTool = new CancellableAsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("cancellable_search")
					.description("可取消的搜索工具，支持优雅停止")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context,
					CancellationToken cancellationToken) {
				return CompletableFuture.supplyAsync(() -> {
					System.out.println("  [可取消工具] 开始搜索...");

					// 注册取消回调 - 用于资源清理
					cancellationToken.onCancel(() -> {
						System.out.println("  [取消回调] 清理已分配的资源...");
						resourcesAllocated.set(0);
					});

					StringBuilder results = new StringBuilder();

					// 模拟分页搜索，每页检查取消状态
					for (int page = 1; page <= 10; page++) {
						// 方式1：使用 isCancelled() 检查并提前返回
						if (cancellationToken.isCancelled()) {
							System.out.println("  [可取消工具] 检测到取消请求，优雅退出 (第 " + page + " 页)");
							return results + "\n[搜索在第 " + page + " 页被取消]";
						}

						// 模拟资源分配和页面处理
						resourcesAllocated.incrementAndGet();
						System.out.println("  [可取消工具] 正在处理第 " + page + " 页...");

						try {
							Thread.sleep(200); // 模拟处理时间
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException("搜索被中断", e);
						}

						results.append("Page ").append(page).append(" results; ");

						// 方式2：使用 throwIfCancelled() 抛出异常
						// cancellationToken.throwIfCancelled();
					}

					System.out.println("  [可取消工具] 搜索完成");
					return results.toString();
				});
			}

			@Override
			public Duration getTimeout() {
				return Duration.ofSeconds(5); // 短超时用于演示
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of()), CancellationToken.NONE).join();
			}
		};

		// 测试1：正常完成
		System.out.println("测试1：正常执行（不取消）");
		CompletableFuture<String> future1 = cancellableSearchTool.callAsync("{\"query\":\"AI\"}", new ToolContext(Map.of()),
				CancellationToken.NONE);
		System.out.println("  结果: " + future1.join().substring(0, Math.min(50, future1.join().length())) + "...");

		// 测试2：主动取消
		System.out.println("\n测试2：主动取消执行");
		DefaultCancellationToken cancelToken = new DefaultCancellationToken();

		CompletableFuture<String> future2 = cancellableSearchTool.callAsync("{\"query\":\"Spring\"}", new ToolContext(Map.of()),
				cancelToken);

		// 延迟 500ms 后取消
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			System.out.println("  [主线程] 请求取消...");
			cancelToken.cancel();
		});

		String cancelledResult = future2.join();
		System.out.println("  结果: " + cancelledResult);

		System.out.println();
	}

	// ==================== 示例 3：状态感知异步工具 ====================

	/**
	 * 示例 3：状态感知工具 (StateAwareToolCallback)
	 *
	 * <p>StateAwareToolCallback 是一个标记接口，实现此接口的工具将自动接收
	 * Agent 状态注入。工具可以读取当前状态并写入更新。</p>
	 *
	 * <p>注入的上下文键：</p>
	 * <ul>
	 *   <li>AGENT_STATE_CONTEXT_KEY - 当前 OverAllState（只读）</li>
	 *   <li>AGENT_CONFIG_CONTEXT_KEY - RunnableConfig 配置</li>
	 *   <li>AGENT_STATE_FOR_UPDATE_CONTEXT_KEY - 状态更新 Map（可写）</li>
	 * </ul>
	 */
	public void example3_stateAwareAsyncTool() {
		System.out.println("=== 示例 3：状态感知工具 (StateAwareToolCallback) ===\n");

		// 创建状态感知的异步工具
		// 注意：AsyncToolCallback 继承自 StateAwareToolCallback
		AsyncToolCallback stateAwareCalculator = new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("state_aware_calculator")
					.description("状态感知计算器，可以读取和更新 Agent 状态")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"operation\":{\"type\":\"string\"},\"value\":{\"type\":\"number\"}}}")
					.build();
			}

			@Override
			@SuppressWarnings("unchecked")
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.supplyAsync(() -> {
					// 从 context 中获取注入的状态
					Map<String, Object> contextMap = context.getContext();

					// 1. 读取当前 Agent 状态
					OverAllState state = (OverAllState) contextMap.get(AGENT_STATE_CONTEXT_KEY);
					if (state != null) {
						System.out.println("  [状态读取] 当前状态: " + state.data());
					}

					// 2. 读取 RunnableConfig
					RunnableConfig config = (RunnableConfig) contextMap.get(AGENT_CONFIG_CONTEXT_KEY);
					if (config != null) {
						System.out.println("  [配置读取] ThreadId: " + config.threadId().orElse("default"));
					}

					// 3. 获取状态更新 Map（用于写入更新）
					Map<String, Object> updateMap = (Map<String, Object>) contextMap.get(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);

					// 模拟计算
					int currentTotal = 0;
					if (state != null) {
						Object totalObj = state.value("runningTotal").orElse(0);
						currentTotal = totalObj instanceof Integer ? (Integer) totalObj : 0;
					}

					// 假设输入是 {"operation": "add", "value": 10}
					int newValue = 10; // 简化示例
					int newTotal = currentTotal + newValue;

					// 4. 写入状态更新
					if (updateMap != null) {
						updateMap.put("runningTotal", newTotal);
						updateMap.put("lastOperation", "add");
						updateMap.put("lastValue", newValue);
						System.out.println("  [状态更新] 写入 runningTotal=" + newTotal);
					}

					return "计算完成: " + currentTotal + " + " + newValue + " = " + newTotal;
				});
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 演示状态感知工具的使用
		System.out.println("演示状态感知工具（模拟 Agent 上下文）:");

		// 模拟 Agent 注入的上下文
		Map<String, Object> simulatedContext = new ConcurrentHashMap<>();
		// 模拟状态（实际由 AgentToolNode 注入）
		// simulatedContext.put(AGENT_STATE_CONTEXT_KEY, state);
		// simulatedContext.put(AGENT_CONFIG_CONTEXT_KEY, config);
		Map<String, Object> updateMap = new ConcurrentHashMap<>();
		simulatedContext.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, updateMap);

		String result = stateAwareCalculator.callAsync("{\"operation\":\"add\",\"value\":10}", new ToolContext(simulatedContext))
			.join();

		System.out.println("  工具返回: " + result);
		System.out.println("  状态更新 Map: " + updateMap);

		System.out.println();
	}

	// ==================== 示例 4：并行工具执行配置 ====================

	/**
	 * 示例 4：并行工具执行配置 (AgentToolNode)
	 *
	 * <p>AgentToolNode 支持并行执行多个工具调用。通过 Builder 可以配置：</p>
	 * <ul>
	 *   <li>parallelToolExecution(true) - 启用并行执行</li>
	 *   <li>maxParallelTools(n) - 最大并行数量</li>
	 *   <li>toolExecutionTimeout(duration) - 执行超时时间</li>
	 * </ul>
	 */
	public void example4_parallelExecutionConfiguration() {
		System.out.println("=== 示例 4：并行工具执行配置 (AgentToolNode) ===\n");

		// 创建多个异步工具
		AsyncToolCallback tool1 = createSimpleAsyncTool("async_tool_1", "异步工具 1", 500);
		AsyncToolCallback tool2 = createSimpleAsyncTool("async_tool_2", "异步工具 2", 800);
		AsyncToolCallback tool3 = createSimpleAsyncTool("async_tool_3", "异步工具 3", 300);

		// 配置 AgentToolNode 支持并行执行
		AgentToolNode parallelNode = AgentToolNode.builder()
			.agentName("parallel_agent")
			// 启用并行执行
			.parallelToolExecution(true)
			// 最多同时执行 5 个工具
			.maxParallelTools(5)
			// 每个工具最长执行 2 分钟
			.toolExecutionTimeout(Duration.ofMinutes(2))
			// 注册工具
			.toolCallbacks(List.of(tool1, tool2, tool3))
			// 异常处理器
			.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
				.alwaysThrow(false)
				.build())
			.build();

		System.out.println("AgentToolNode 配置:");
		System.out.println("  - 并行执行: 已启用");
		System.out.println("  - 最大并行数: 5");
		System.out.println("  - 执行超时: 2 分钟");
		System.out.println("  - 注册工具数: 3");
		System.out.println("  - 工具列表: " + parallelNode.getToolCallbacks().stream()
			.map(t -> t.getToolDefinition().name())
			.toList());

		// 顺序执行配置示例
		AgentToolNode sequentialNode = AgentToolNode.builder()
			.agentName("sequential_agent")
			// 禁用并行执行（默认行为）
			.parallelToolExecution(false)
			.toolCallbacks(List.of(tool1, tool2, tool3))
			.toolExecutionTimeout(Duration.ofMinutes(5))
			.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
				.alwaysThrow(false)
				.build())
			.build();

		System.out.println("\n顺序执行 AgentToolNode 配置:");
		System.out.println("  - 并行执行: 已禁用");
		System.out.println("  - 工具将按顺序执行");

		System.out.println();
	}

	// ==================== 示例 5：在 ReactAgent 中使用异步工具 ====================

	/**
	 * 示例 5：在 ReactAgent 中使用异步工具
	 *
	 * <p>ReactAgent 支持异步工具。当 Agent 需要调用多个工具时，
	 * 可以配置并行执行以提高效率。</p>
	 */
	public void example5_asyncToolsInReactAgent() throws GraphRunnerException {
		System.out.println("=== 示例 5：在 ReactAgent 中使用异步工具 ===\n");

		// 创建异步天气查询工具
		AsyncToolCallback asyncWeatherTool = new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("async_get_weather")
					.description("异步获取指定城市的天气信息")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名称\"}},\"required\":[\"city\"]}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.supplyAsync(() -> {
					System.out.println("    [异步工具] 查询天气中...");
					try {
						Thread.sleep(500); // 模拟 API 调用
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return "{\"city\": \"北京\", \"temperature\": 25, \"condition\": \"晴朗\"}";
				});
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 创建异步股票查询工具
		AsyncToolCallback asyncStockTool = new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("async_get_stock")
					.description("异步获取股票价格")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"symbol\":{\"type\":\"string\",\"description\":\"股票代码\"}},\"required\":[\"symbol\"]}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.supplyAsync(() -> {
					System.out.println("    [异步工具] 查询股票中...");
					try {
						Thread.sleep(700); // 模拟 API 调用
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return "{\"symbol\": \"BABA\", \"price\": 85.50, \"change\": \"+2.3%\"}";
				});
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 创建 ReactAgent 并配置异步工具
		ReactAgent agent = ReactAgent.builder()
			.name("async_tools_agent")
			.model(chatModel)
			.description("一个配置了异步工具的智能助手")
			.instruction("你是一个智能助手，可以异步查询天气和股票信息。当用户询问时，使用相应的工具。")
			// 添加异步工具
			.tools(asyncWeatherTool, asyncStockTool)
			// 配置记忆
			.saver(new MemorySaver())
			.build();

		System.out.println("ReactAgent 配置完成:");
		System.out.println("  - 异步工具数: 2");
		System.out.println("  - 说明: ReactAgent 自动处理异步工具的执行");

		// 调用 Agent
		RunnableConfig config = RunnableConfig.builder()
			.threadId("async_tools_session")
			.build();

		System.out.println("\n调用 Agent (查询天气)...");
		Optional<OverAllState> result = agent.invoke("北京今天天气怎么样？", config);

		if (result.isPresent()) {
			System.out.println("  Agent 执行成功");
		}

		System.out.println();
	}

	// ==================== 示例 6：取消令牌高级用法 ====================

	/**
	 * 示例 6：取消令牌高级用法 (CancellationToken)
	 *
	 * <p>CancellationToken 提供了灵活的取消机制：</p>
	 * <ul>
	 *   <li>DefaultCancellationToken.linkedTo() - 链接到 CompletableFuture</li>
	 *   <li>onCancel() - 注册多个取消回调</li>
	 *   <li>回调的幂等性 - 每个回调只执行一次</li>
	 * </ul>
	 */
	public void example6_cancellationTokenAdvanced() {
		System.out.println("=== 示例 6：取消令牌高级用法 (CancellationToken) ===\n");

		// 6.1 基本用法
		System.out.println("6.1 基本取消令牌用法:");
		DefaultCancellationToken basicToken = new DefaultCancellationToken();

		System.out.println("  初始状态 - isCancelled: " + basicToken.isCancelled());

		// 注册回调
		basicToken.onCancel(() -> System.out.println("  [回调1] 取消回调被触发"));
		basicToken.onCancel(() -> System.out.println("  [回调2] 清理资源"));

		// 触发取消
		basicToken.cancel();
		System.out.println("  取消后 - isCancelled: " + basicToken.isCancelled());

		// 重复取消是幂等的
		basicToken.cancel(); // 不会重复触发回调

		// 6.2 链接到 CompletableFuture
		System.out.println("\n6.2 链接到 CompletableFuture:");

		CompletableFuture<String> longRunningTask = new CompletableFuture<>();

		// 创建链接到 Future 的取消令牌
		DefaultCancellationToken linkedToken = DefaultCancellationToken.linkedTo(longRunningTask);

		linkedToken.onCancel(() -> System.out.println("  [链接令牌] Future 被取消，触发清理"));

		System.out.println("  Future 取消前 - token.isCancelled: " + linkedToken.isCancelled());

		// 取消 Future 会自动触发令牌
		longRunningTask.cancel(true);

		// 需要短暂等待异步回调执行
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		System.out.println("  Future 取消后 - token.isCancelled: " + linkedToken.isCancelled());

		// 6.3 在取消后注册回调
		System.out.println("\n6.3 在取消后注册回调:");
		DefaultCancellationToken alreadyCancelled = new DefaultCancellationToken();
		alreadyCancelled.cancel();

		// 取消后注册的回调会立即执行
		alreadyCancelled.onCancel(() -> System.out.println("  [延迟注册] 回调立即执行（因为已取消）"));

		// 6.4 CancellationToken.NONE - 永不取消的令牌
		System.out.println("\n6.4 CancellationToken.NONE:");
		CancellationToken noneToken = CancellationToken.NONE;
		System.out.println("  NONE.isCancelled: " + noneToken.isCancelled()); // 永远是 false
		noneToken.onCancel(() -> System.out.println("  这个回调永远不会执行")); // 无操作

		// 6.5 throwIfCancelled 用法
		System.out.println("\n6.5 throwIfCancelled 用法:");
		DefaultCancellationToken throwToken = new DefaultCancellationToken();
		throwToken.cancel();

		try {
			throwToken.throwIfCancelled();
		}
		catch (ToolCancelledException e) {
			System.out.println("  捕获到 ToolCancelledException: " + e.getMessage());
		}

		System.out.println();
	}

	// ==================== 示例 7：ToolStateCollector 高级用法 ====================

	/**
	 * 示例 7：ToolStateCollector 高级用法
	 *
	 * <p>ToolStateCollector 用于并行工具执行时收集和合并状态更新。
	 * 每个工具获得隔离的更新 Map，避免并发冲突。</p>
	 *
	 * <p>关键特性：</p>
	 * <ul>
	 *   <li>createToolUpdateMap() - 为工具创建隔离的更新 Map</li>
	 *   <li>discardToolUpdateMap() - 丢弃超时工具的更新</li>
	 *   <li>mergeAll() - 按索引顺序合并所有更新</li>
	 * </ul>
	 */
	public void example7_toolStateCollector() {
		System.out.println("=== 示例 7：ToolStateCollector 高级用法 ===\n");

		// 定义 KeyStrategy（合并策略）
		Map<String, KeyStrategy> keyStrategies = Map.of(
			"counter", KeyStrategy.APPEND,  // 追加策略
			"lastUpdated", KeyStrategy.REPLACE  // 替换策略（默认）
		);

		// 创建 ToolStateCollector（3 个工具）
		ToolStateCollector collector = new ToolStateCollector(3, keyStrategies);

		System.out.println("7.1 创建隔离的更新 Map:");

		// 为每个工具创建隔离的更新 Map
		Map<String, Object> tool0Updates = collector.createToolUpdateMap(0);
		Map<String, Object> tool1Updates = collector.createToolUpdateMap(1);
		Map<String, Object> tool2Updates = collector.createToolUpdateMap(2);

		// 模拟并行工具写入更新（每个工具写入自己的 Map）
		System.out.println("  工具 0 写入: counter=A, result=tool0_done");
		tool0Updates.put("counter", "A");
		tool0Updates.put("result", "tool0_done");
		tool0Updates.put("lastUpdated", "tool0");

		System.out.println("  工具 1 写入: counter=B, data=from_tool1");
		tool1Updates.put("counter", "B");
		tool1Updates.put("data", "from_tool1");
		tool1Updates.put("lastUpdated", "tool1");

		System.out.println("  工具 2 写入: counter=C, extra=info");
		tool2Updates.put("counter", "C");
		tool2Updates.put("extra", "info");
		tool2Updates.put("lastUpdated", "tool2");

		System.out.println("\n7.2 检查状态:");
		System.out.println("  已完成工具数: " + collector.getCompletedCount());
		System.out.println("  是否已合并: " + collector.isMerged());

		// 合并所有更新
		System.out.println("\n7.3 合并所有更新 (mergeAll):");
		Map<String, Object> merged = collector.mergeAll();
		System.out.println("  合并结果: " + merged);
		System.out.println("  - counter (APPEND): " + merged.get("counter")); // [A, B, C]
		System.out.println("  - lastUpdated (REPLACE): " + merged.get("lastUpdated")); // tool2（最后）

		System.out.println("\n7.4 丢弃超时工具的更新:");

		// 创建新的 Collector 演示丢弃
		ToolStateCollector collector2 = new ToolStateCollector(2, null);
		Map<String, Object> goodTool = collector2.createToolUpdateMap(0);
		Map<String, Object> timeoutTool = collector2.createToolUpdateMap(1);

		goodTool.put("status", "success");
		timeoutTool.put("status", "partial"); // 假设这个工具超时了

		// 丢弃超时工具的更新
		collector2.discardToolUpdateMap(1);
		System.out.println("  丢弃工具 1 的更新");

		Map<String, Object> merged2 = collector2.mergeAll();
		System.out.println("  合并结果（排除超时工具）: " + merged2);

		System.out.println("\n7.5 mergeAll 只能调用一次:");
		try {
			collector.mergeAll(); // 再次调用会抛异常
		}
		catch (IllegalStateException e) {
			System.out.println("  捕获到异常: " + e.getMessage());
		}

		System.out.println();
	}

	// ==================== 辅助方法 ====================

	/**
	 * 创建简单的异步工具（用于示例）
	 */
	private AsyncToolCallback createSimpleAsyncTool(String name, String description, int delayMs) {
		return new AsyncToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name(name)
					.description(description)
					.inputSchema("{\"type\":\"object\"}")
					.build();
			}

			@Override
			public CompletableFuture<String> callAsync(String arguments, ToolContext context) {
				return CompletableFuture.supplyAsync(() -> {
					try {
						Thread.sleep(delayMs);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return name + " 执行完成 (耗时 " + delayMs + "ms)";
				});
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 异步工具执行示例 (Async Tool Execution Examples) ===\n");
		System.out.println("Issue #3988 - 异步工具支持功能演示\n");
		System.out.println("================================================\n");

		try {
			// 示例 1：基础异步工具
			example1_basicAsyncTool();

			// 示例 2：可取消的异步工具
			example2_cancellableAsyncTool();

			// 示例 3：状态感知异步工具
			example3_stateAwareAsyncTool();

			// 示例 4：并行工具执行配置
			example4_parallelExecutionConfiguration();

			// 示例 5：在 ReactAgent 中使用异步工具
			example5_asyncToolsInReactAgent();

			// 示例 6：取消令牌高级用法
			example6_cancellationTokenAdvanced();

			// 示例 7：ToolStateCollector 高级用法
			example7_toolStateCollector();

			System.out.println("================================================");
			System.out.println("所有示例执行完成！");
			System.out.println("================================================");

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
