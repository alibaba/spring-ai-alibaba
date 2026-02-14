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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.agent.tool.StreamingToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolResult;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStreamingErrorHandler;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.ToolStreamingOutput;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式工具执行示例 (Streaming Tool Execution Example)
 *
 * <p>本示例展示 Issue #3912 引入的流式工具和多模态结果支持功能，包括：</p>
 * <ul>
 *   <li>StreamingToolCallback - 流式工具回调接口，支持增量结果返回</li>
 *   <li>ToolResult - 富结果模型，支持文本、图片、音频、视频等多模态内容</li>
 *   <li>ToolStreamingOutput - 流式输出包装器，用于管道集成</li>
 *   <li>ToolStreamingErrorHandler - 统一错误处理，确保流不中断</li>
 *   <li>AgentToolNode.executeToolCallsStreaming() - 流式工具执行</li>
 * </ul>
 *
 * <h2>运行方式 (How to Run)</h2>
 *
 * <h3>前置条件 (Prerequisites)</h3>
 * <ul>
 *   <li>JDK 17 或更高版本</li>
 *   <li>设置环境变量 AI_DASHSCOPE_API_KEY（示例 7 需要有效的 API Key）</li>
 * </ul>
 *
 * <h3>方式一：使用 Maven 运行 (Run with Maven)</h3>
 * <pre>{@code
 * # 1. 进入项目根目录，安装依赖到本地仓库
 * cd spring-ai-alibaba
 * mvn install -DskipTests
 *
 * # 2. 进入示例目录
 * cd examples/documentation
 *
 * # 3. 确保 pom.xml 中 spring-ai-alibaba.version 为 1.1.2.0-SNAPSHOT
 *
 * # 4. 运行示例
 * export AI_DASHSCOPE_API_KEY=your_api_key
 * mvn exec:java -Dexec.mainClass="com.alibaba.cloud.ai.examples.documentation.framework.advanced.StreamingToolExecutionExample"
 * }</pre>
 *
 * <h3>方式二：在 IDE 中运行 (Run in IDE)</h3>
 * <ol>
 *   <li>在 IDE 中打开项目</li>
 *   <li>设置环境变量 AI_DASHSCOPE_API_KEY</li>
 *   <li>右键运行 main() 方法</li>
 * </ol>
 *
 * <h3>预期输出 (Expected Output)</h3>
 * <pre>{@code
 * === 流式工具执行示例 (Streaming Tool Execution Examples) ===
 * === 示例 1：基础流式工具 (StreamingToolCallback) ===
 * === 示例 2：多模态工具结果 (ToolResult with Media) ===
 * === 示例 3：流式结果合并 (ToolResult.merge) ===
 * === 示例 4：流式错误处理 (ToolStreamingErrorHandler) ===
 * === 示例 5：AgentToolNode 流式执行配置 ===
 * === 示例 6：ToolStreamingOutput 用法 ===
 * === 示例 7：在 ReactAgent 中使用流式工具 ===
 * 所有示例执行完成！
 * }</pre>
 *
 * <p><b>注意：</b>示例 1-6 不依赖外部 API，可直接运行。示例 7 需要有效的 DashScope API Key。</p>
 *
 * <p>参考文档: Issue #3912 - Streaming &amp; Multi-modal Tool Result Support</p>
 *
 * @author disaster
 * @since 1.0.0
 */
public class StreamingToolExecutionExample {

	private final ChatModel chatModel;

	public StreamingToolExecutionExample(ChatModel chatModel) {
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
		StreamingToolExecutionExample example = new StreamingToolExecutionExample(chatModel);

		// 运行所有示例
		example.runAllExamples();
	}

	// ==================== 示例 1：基础流式工具 ====================

	/**
	 * 示例 1：基础流式工具 (StreamingToolCallback)
	 *
	 * <p>StreamingToolCallback 接口允许工具以流式方式返回结果，通过 Flux&lt;ToolResult&gt; 逐步发送。
	 * 这对于需要长时间执行并希望实时反馈进度的工具非常有用。</p>
	 *
	 * <p>关键特性：</p>
	 * <ul>
	 *   <li>callStream() 返回 Flux&lt;ToolResult&gt;</li>
	 *   <li>使用 ToolResult.chunk() 发送进度更新</li>
	 *   <li>使用 withFinal(true) 标记流结束</li>
	 *   <li>继承自 AsyncToolCallback，自动支持异步执行</li>
	 * </ul>
	 */
	public void example1_basicStreamingTool() {
		System.out.println("=== 示例 1：基础流式工具 (StreamingToolCallback) ===\n");

		// 创建流式搜索工具
		StreamingToolCallback streamingSearchTool = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("streaming_search")
					.description("流式搜索工具，逐步返回搜索进度和结果")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				// 返回 Flux 流，逐步发送结果
				return Flux.interval(Duration.ofMillis(200))
					.take(5)
					// 发送进度更新 (chunk)
					.map(i -> ToolResult.chunk("搜索进度: " + ((i + 1) * 20) + "%...\n"))
					// 添加最终结果
					.concatWith(Flux.just(
						ToolResult.text("搜索完成！找到 42 条相关结果。\n最佳匹配: Spring AI Alibaba 文档")
							.withFinal(true)
					));
			}

			@Override
			public String call(String toolInput) {
				// 同步调用时，阻塞等待流完成并合并所有结果
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 测试流式工具
		System.out.println("测试流式执行 (订阅 Flux):");
		System.out.println("  开始接收流式结果...");

		streamingSearchTool.callStream("{\"query\":\"Spring AI\"}", new ToolContext(Map.of()))
			.doOnNext(result -> {
				if (result.isChunk()) {
					System.out.print("  [进度] " + result.getTextContent());
				}
				else {
					System.out.println("  [最终结果] " + result.getTextContent());
				}
			})
			.blockLast();

		// 测试同步调用 (会合并所有 chunk)
		System.out.println("\n测试同步调用 (自动合并所有 chunk):");
		String syncResult = streamingSearchTool.call("{\"query\":\"Agent Framework\"}");
		System.out.println("  合并后的结果: " + syncResult);

		// 演示 isStreaming() 和 isAsync() 特性
		System.out.println("\n工具特性:");
		System.out.println("  isStreaming: " + streamingSearchTool.isStreaming()); // true
		System.out.println("  isAsync: " + streamingSearchTool.isAsync()); // true (继承自 AsyncToolCallback)

		System.out.println();
	}

	// ==================== 示例 2：多模态工具结果 ====================

	/**
	 * 示例 2：多模态工具结果 (ToolResult with Media)
	 *
	 * <p>ToolResult 支持多种内容类型：TEXT, IMAGE, AUDIO, VIDEO, FILE, MIXED。
	 * 可以返回纯文本、媒体内容或两者的组合。</p>
	 *
	 * <p>关键方法：</p>
	 * <ul>
	 *   <li>ToolResult.text(String) - 纯文本结果</li>
	 *   <li>ToolResult.media(List&lt;Media&gt;) - 纯媒体结果</li>
	 *   <li>ToolResult.mixed(String, List&lt;Media&gt;) - 文本 + 媒体混合结果</li>
	 * </ul>
	 */
	public void example2_multimodalToolResults() {
		System.out.println("=== 示例 2：多模态工具结果 (ToolResult with Media) ===\n");

		// 创建图片生成工具（流式进度 + 多模态结果）
		StreamingToolCallback imageGeneratorTool = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("image_generator")
					.description("图片生成工具，支持流式进度和多模态结果")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\"}}}")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.interval(Duration.ofMillis(300))
					.take(5)
					// 发送生成进度
					.map(i -> ToolResult.chunk("生成进度: " + ((i + 1) * 20) + "%...\n"))
					// 添加包含图片的最终结果
					.concatWith(Flux.defer(() -> {
						// 模拟生成图片数据 (实际应用中会调用真实的图片生成 API)
						byte[] fakeImageData = generateFakeImageData();

						// 创建 Media 对象
						Media imageMedia = new Media(
							MimeTypeUtils.IMAGE_PNG,
							new ByteArrayResource(fakeImageData)
						);

						// 返回混合结果：文本描述 + 图片
						return Flux.just(
							ToolResult.mixed("图片生成完成！这是根据提示词生成的图片：", List.of(imageMedia))
								.withFinal(true)
						);
					}));
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 演示多模态结果
		System.out.println("2.1 流式生成图片:");
		AtomicReference<ToolResult> finalResult = new AtomicReference<>();

		imageGeneratorTool.callStream("{\"prompt\":\"一只可爱的猫咪\"}", new ToolContext(Map.of()))
			.doOnNext(result -> {
				if (result.isChunk()) {
					System.out.print("  " + result.getTextContent());
				}
				else {
					finalResult.set(result);
				}
			})
			.blockLast();

		// 检查最终结果的属性
		ToolResult result = finalResult.get();
		System.out.println("\n2.2 多模态结果属性:");
		System.out.println("  类型: " + result.getType()); // MIXED
		System.out.println("  文本内容: " + result.getTextContent());
		System.out.println("  包含媒体: " + result.hasMedia()); // true
		System.out.println("  媒体数量: " + result.getMediaContent().size()); // 1
		System.out.println("  是否仅文本: " + result.isTextOnly()); // false
		System.out.println("  是否最终结果: " + result.isFinal()); // true

		// 演示不同类型的 ToolResult 创建
		System.out.println("\n2.3 不同类型的 ToolResult:");

		// 纯文本
		ToolResult textOnly = ToolResult.text("这是纯文本结果");
		System.out.println("  纯文本 - 类型: " + textOnly.getType() + ", isTextOnly: " + textOnly.isTextOnly());

		// 纯媒体
		byte[] audioData = new byte[]{0x00, 0x01, 0x02}; // 模拟音频数据
		Media audioMedia = new Media(MimeTypeUtils.parseMimeType("audio/mp3"), new ByteArrayResource(audioData));
		ToolResult mediaOnly = ToolResult.media(List.of(audioMedia));
		System.out.println("  纯媒体 - 类型: " + mediaOnly.getType() + ", hasMedia: " + mediaOnly.hasMedia());

		// 混合
		ToolResult mixed = ToolResult.mixed("这是混合结果", List.of(audioMedia));
		System.out.println("  混合 - 类型: " + mixed.getType());

		// 演示序列化/反序列化
		System.out.println("\n2.4 多模态结果序列化:");
		String serialized = result.toStringResult();
		System.out.println("  序列化格式检测: " + ToolResult.isToolResultFormat(serialized)); // true
		System.out.println("  序列化前缀: " + serialized.substring(0, Math.min(50, serialized.length())) + "...");

		// 反序列化
		ToolResult deserialized = ToolResult.fromString(serialized);
		System.out.println("  反序列化后类型: " + deserialized.getType());
		System.out.println("  反序列化后媒体数量: " + deserialized.getMediaContent().size());

		System.out.println();
	}

	// ==================== 示例 3：流式结果合并 ====================

	/**
	 * 示例 3：流式结果合并 (ToolResult.merge)
	 *
	 * <p>当需要将多个流式 chunk 合并为单个结果时，使用 merge() 方法。
	 * 这在同步调用或需要最终完整结果时非常有用。</p>
	 *
	 * <p>合并规则：</p>
	 * <ul>
	 *   <li>文本内容会拼接</li>
	 *   <li>媒体列表会合并</li>
	 *   <li>isFinal 标记取最新值</li>
	 * </ul>
	 */
	public void example3_chunkMergingAndAccumulation() {
		System.out.println("=== 示例 3：流式结果合并 (ToolResult.merge) ===\n");

		// 创建几个模拟的 chunk
		ToolResult chunk1 = ToolResult.chunk("第一部分: Hello ");
		ToolResult chunk2 = ToolResult.chunk("第二部分: World ");
		ToolResult chunk3 = ToolResult.text("第三部分: !").withFinal(true);

		System.out.println("3.1 原始 chunk:");
		System.out.println("  chunk1: \"" + chunk1.getTextContent() + "\" (isChunk=" + chunk1.isChunk() + ")");
		System.out.println("  chunk2: \"" + chunk2.getTextContent() + "\" (isChunk=" + chunk2.isChunk() + ")");
		System.out.println("  chunk3: \"" + chunk3.getTextContent() + "\" (isFinal=" + chunk3.isFinal() + ")");

		// 手动合并
		System.out.println("\n3.2 手动合并 (merge 方法):");
		ToolResult merged = chunk1.merge(chunk2).merge(chunk3);
		System.out.println("  合并后文本: \"" + merged.getTextContent() + "\"");
		System.out.println("  合并后 isFinal: " + merged.isFinal()); // true (来自 chunk3)
		System.out.println("  合并后 isChunk: " + merged.isChunk()); // false (合并后不再是 chunk)

		// 使用 Flux.reduce 合并
		System.out.println("\n3.3 使用 Flux.reduce 合并流:");
		StreamingToolCallback demoTool = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("demo")
					.description("demo")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.just(
					ToolResult.chunk("A"),
					ToolResult.chunk("B"),
					ToolResult.chunk("C"),
					ToolResult.text("D").withFinal(true)
				);
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 使用 reduce 合并
		ToolResult reducedResult = demoTool.callStream("{}", new ToolContext(Map.of()))
			.reduce(ToolResult::merge)
			.block();

		System.out.println("  reduce 合并结果: \"" + reducedResult.getTextContent() + "\""); // "ABCD"
		System.out.println("  isFinal: " + reducedResult.isFinal()); // true

		// 合并包含媒体的结果
		System.out.println("\n3.4 合并包含媒体的结果:");
		byte[] img1 = new byte[]{1, 2, 3};
		byte[] img2 = new byte[]{4, 5, 6};
		Media media1 = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(img1));
		Media media2 = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(img2));

		ToolResult textChunk = ToolResult.chunk("图片说明: ");
		ToolResult mediaResult1 = ToolResult.mixed("第一张图", List.of(media1));
		ToolResult mediaResult2 = ToolResult.mixed("第二张图", List.of(media2)).withFinal(true);

		ToolResult mergedWithMedia = textChunk.merge(mediaResult1).merge(mediaResult2);
		System.out.println("  合并后文本: \"" + mergedWithMedia.getTextContent() + "\"");
		System.out.println("  合并后媒体数量: " + mergedWithMedia.getMediaContent().size()); // 2
		System.out.println("  合并后类型: " + mergedWithMedia.getType()); // MIXED

		System.out.println();
	}

	// ==================== 示例 4：流式错误处理 ====================

	/**
	 * 示例 4：流式错误处理 (ToolStreamingErrorHandler)
	 *
	 * <p><b>关键设计原则：</b> 永远不要在流式工具中使用 Flux.error() 抛出异常。
	 * 这会中断整个流，导致 doneMap 无法发送，破坏 GraphResponse 契约。</p>
	 *
	 * <p>正确做法：使用 ToolStreamingErrorHandler 将错误转换为错误结果，继续通过流发送。</p>
	 */
	public void example4_streamingErrorHandling() {
		System.out.println("=== 示例 4：流式错误处理 (ToolStreamingErrorHandler) ===\n");

		// 4.1 演示错误消息提取
		System.out.println("4.1 错误消息提取:");

		// 普通异常
		RuntimeException normalError = new RuntimeException("Something went wrong");
		System.out.println("  普通异常: " + ToolStreamingErrorHandler.extractErrorMessage(normalError));

		// 超时异常
		TimeoutException timeoutError = new TimeoutException("Operation timed out");
		System.out.println("  超时异常: " + ToolStreamingErrorHandler.extractErrorMessage(timeoutError));
		System.out.println("  是否超时: " + ToolStreamingErrorHandler.isTimeout(timeoutError)); // true

		// 4.2 演示错误类型检测
		System.out.println("\n4.2 错误类型检测:");
		System.out.println("  TimeoutException isTimeout: " + ToolStreamingErrorHandler.isTimeout(timeoutError));
		System.out.println("  RuntimeException isTimeout: " + ToolStreamingErrorHandler.isTimeout(normalError));

		// 4.3 演示在流中处理错误的正确方式
		System.out.println("\n4.3 流中错误处理的正确方式:");

		StreamingToolCallback errorProneTools = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("error_prone_tool")
					.description("可能出错的工具")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.just(
					ToolResult.chunk("开始处理...\n"),
					ToolResult.chunk("处理中...\n")
				)
				.concatWith(Flux.defer(() -> {
					// 模拟错误发生
					if (arguments.contains("error")) {
						// 错误做法：throw new RuntimeException("xxx")
						// 正确做法：返回错误结果
						return Flux.just(
							ToolResult.text("Error: 处理过程中发生错误").withFinal(true)
						);
					}
					return Flux.just(ToolResult.text("处理完成！").withFinal(true));
				}));
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 测试正常执行
		System.out.println("  正常执行:");
		errorProneTools.callStream("{}", new ToolContext(Map.of()))
			.doOnNext(r -> System.out.println("    " + r.getTextContent().trim()))
			.blockLast();

		// 测试错误情况
		System.out.println("  错误情况:");
		errorProneTools.callStream("{\"trigger\": \"error\"}", new ToolContext(Map.of()))
			.doOnNext(r -> System.out.println("    " + r.getTextContent().trim()))
			.blockLast();

		// 4.4 使用 onErrorResume 处理意外错误
		System.out.println("\n4.4 使用 onErrorResume 处理意外错误:");
		Flux.just(ToolResult.chunk("Processing..."))
			.concatWith(Flux.error(new RuntimeException("Unexpected error")))
			.onErrorResume(ex -> {
				// 在实际的 AgentToolNode 中会使用 ToolStreamingErrorHandler.handleError()
				String errorMsg = ToolStreamingErrorHandler.extractErrorMessage(ex);
				return Flux.just(ToolResult.text("Error: " + errorMsg).withFinal(true));
			})
			.doOnNext(r -> System.out.println("  结果: " + r.getTextContent()))
			.blockLast();

		System.out.println();
	}

	// ==================== 示例 5：AgentToolNode 流式执行配置 ====================

	/**
	 * 示例 5：AgentToolNode 流式执行配置
	 *
	 * <p>AgentToolNode 支持混合使用不同类型的工具：</p>
	 * <ul>
	 *   <li>StreamingToolCallback - 流式工具，优先使用流式执行</li>
	 *   <li>AsyncToolCallback - 异步工具</li>
	 *   <li>ToolCallback - 同步工具</li>
	 * </ul>
	 *
	 * <p>使用 executeToolCallsStreaming() 方法进行流式执行。</p>
	 */
	public void example5_agentToolNodeStreamingExecution() {
		System.out.println("=== 示例 5：AgentToolNode 流式执行配置 ===\n");

		// 创建流式工具
		StreamingToolCallback streamingTool = createStreamingTool("streaming_weather",
			"流式天气查询", 3, "天气查询中...\n", "北京今日天气: 晴, 25°C");

		// 创建普通异步工具（用于对比）
		StreamingToolCallback asyncStyleTool = createStreamingTool("async_stock",
			"股票查询", 2, "查询股票...\n", "BABA: $85.50 (+2.3%)");

		// 配置 AgentToolNode 支持流式执行
		AgentToolNode toolNode = AgentToolNode.builder()
			.agentName("streaming_agent")
			// 启用并行执行
			.parallelToolExecution(true)
			// 最多同时执行 5 个工具
			.maxParallelTools(5)
			// 每个工具最长执行 2 分钟
			.toolExecutionTimeout(Duration.ofMinutes(2))
			// 注册工具（包括流式和非流式）
			.toolCallbacks(List.of(streamingTool, asyncStyleTool))
			// 异常处理器 - 不抛出异常，返回错误结果
			.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
				.alwaysThrow(false)
				.build())
			.build();

		System.out.println("AgentToolNode 配置:");
		System.out.println("  - Agent 名称: streaming_agent");
		System.out.println("  - 并行执行: 已启用");
		System.out.println("  - 最大并行数: 5");
		System.out.println("  - 执行超时: 2 分钟");
		System.out.println("  - 注册工具数: " + toolNode.getToolCallbacks().size());
		System.out.println("  - 工具列表:");
		toolNode.getToolCallbacks().forEach(tool -> {
			String streamingFlag = tool instanceof StreamingToolCallback ? " [流式]" : "";
			System.out.println("    * " + tool.getToolDefinition().name() + streamingFlag);
		});

		System.out.println("\n说明: executeToolCallsStreaming() 方法会:");
		System.out.println("  1. 自动检测工具类型 (Streaming > Async > Sync)");
		System.out.println("  2. 为每个 chunk 发送 ToolStreamingOutput");
		System.out.println("  3. 支持并行执行多个工具");
		System.out.println("  4. 最后发送 GraphResponse.done() 包含完整结果");

		System.out.println();
	}

	// ==================== 示例 6：ToolStreamingOutput 用法 ====================

	/**
	 * 示例 6：ToolStreamingOutput 用法
	 *
	 * <p>ToolStreamingOutput 是流式输出的包装类，继承自 StreamingOutput，
	 * 添加了工具标识 (toolCallId, toolName) 用于客户端区分不同工具的输出。</p>
	 */
	public void example6_toolStreamingOutputUsage() {
		System.out.println("=== 示例 6：ToolStreamingOutput 用法 ===\n");

		// 创建模拟状态
		OverAllState mockState = OverAllStateBuilder.builder()
			.putData("example", "streaming_tool_output")
			.build();

		// 创建流式输出
		ToolResult chunkData = ToolResult.chunk("Processing step 1...");
		ToolStreamingOutput<ToolResult> streamingOutput = new ToolStreamingOutput<>(
			chunkData,                          // 数据
			"agent_tool",                       // 节点 ID
			"myAgent",                          // Agent 名称
			mockState,                          // 状态快照
			OutputType.AGENT_TOOL_STREAMING,    // 输出类型 (流式中)
			"call_12345",                       // 工具调用 ID
			"searchTool"                        // 工具名称
		);

		System.out.println("6.1 ToolStreamingOutput 属性:");
		System.out.println("  工具调用 ID: " + streamingOutput.getToolCallId());
		System.out.println("  工具名称: " + streamingOutput.getToolName());
		System.out.println("  输出类型: " + streamingOutput.getOutputType());
		System.out.println("  是否最终 chunk: " + streamingOutput.isFinalChunk()); // false

		// 获取数据
		System.out.println("\n6.2 获取数据:");
		System.out.println("  getChunkData(): " + streamingOutput.getChunkData());
		System.out.println("  getOriginData(): " + streamingOutput.getOriginData()); // 同上

		// 序列化为 JSON (用于传输)
		System.out.println("\n6.3 序列化为 JSON:");
		String jsonChunk = streamingOutput.chunk();
		System.out.println("  chunk(): " + jsonChunk);

		// 创建最终输出
		System.out.println("\n6.4 最终输出 (FINISHED):");
		ToolResult finalData = ToolResult.text("Search completed!").withFinal(true);
		ToolStreamingOutput<ToolResult> finalOutput = new ToolStreamingOutput<>(
			finalData,
			"agent_tool",
			"myAgent",
			mockState,
			OutputType.AGENT_TOOL_FINISHED,     // 输出类型 (已完成)
			"call_12345",
			"searchTool"
		);

		System.out.println("  输出类型: " + finalOutput.getOutputType());
		System.out.println("  是否最终 chunk: " + finalOutput.isFinalChunk()); // true

		// OutputType 枚举说明
		System.out.println("\n6.5 OutputType 枚举:");
		System.out.println("  AGENT_TOOL_STREAMING - 工具正在执行中，输出中间结果");
		System.out.println("  AGENT_TOOL_FINISHED - 工具执行完成，输出最终结果");

		System.out.println();
	}

	// ==================== 示例 7：在 ReactAgent 中使用流式工具 ====================

	/**
	 * 示例 7：在 ReactAgent 中使用流式工具
	 *
	 * <p>ReactAgent 完全支持流式工具。当 Agent 调用流式工具时，
	 * 会通过流逐步返回工具执行进度。</p>
	 */
	public void example7_streamingToolsInReactAgent() {
		System.out.println("=== 示例 7：在 ReactAgent 中使用流式工具 ===\n");

		// 创建流式天气查询工具
		StreamingToolCallback streamingWeatherTool = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name("streaming_get_weather")
					.description("流式获取天气信息，实时返回查询进度")
					.inputSchema("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名称\"}},\"required\":[\"city\"]}")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.concat(
					Flux.just(ToolResult.chunk("正在查询天气数据...\n")),
					Flux.just(ToolResult.chunk("获取实时气温...\n")).delaySubscription(Duration.ofMillis(200)),
					Flux.just(ToolResult.chunk("分析天气趋势...\n")).delaySubscription(Duration.ofMillis(200)),
					Flux.just(ToolResult.text("{\"city\": \"北京\", \"temperature\": 25, \"condition\": \"晴朗\", \"humidity\": 45}")
						.withFinal(true)).delaySubscription(Duration.ofMillis(200))
				);
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};

		// 创建 ReactAgent 并配置流式工具
		ReactAgent agent = ReactAgent.builder()
			.name("streaming_tools_agent")
			.model(chatModel)
			.description("一个配置了流式工具的智能助手")
			.instruction("你是一个智能助手，可以流式查询天气信息。当用户询问天气时，使用 streaming_get_weather 工具。")
			// 添加流式工具
			.tools(streamingWeatherTool)
			// 配置记忆
			.saver(new MemorySaver())
			.build();

		System.out.println("ReactAgent 配置完成:");
		System.out.println("  - Agent 名称: streaming_tools_agent");
		System.out.println("  - 流式工具数: 1");
		System.out.println("  - 说明: ReactAgent 自动处理流式工具的执行");

		// 调用 Agent
		RunnableConfig config = RunnableConfig.builder()
			.threadId("streaming_tools_session")
			.build();

		System.out.println("\n调用 Agent (查询天气)...");
		try {
			Optional<OverAllState> result = agent.invoke("北京今天天气怎么样？", config);

			if (result.isPresent()) {
				System.out.println("  Agent 执行成功");
			}
		}
		catch (Exception e) {
			System.out.println("  Agent 执行出错: " + e.getMessage());
			System.out.println("  (这在没有配置 API Key 的情况下是正常的)");
		}

		System.out.println();
	}

	// ==================== 辅助方法 ====================

	/**
	 * 创建简单的流式工具（用于示例）
	 */
	private StreamingToolCallback createStreamingTool(String name, String description,
			int progressSteps, String progressMessage, String finalMessage) {
		return new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return DefaultToolDefinition.builder()
					.name(name)
					.description(description)
					.inputSchema("{\"type\":\"object\"}")
					.build();
			}

			@Override
			public Flux<ToolResult> callStream(String arguments, ToolContext context) {
				return Flux.interval(Duration.ofMillis(100))
					.take(progressSteps)
					.map(i -> ToolResult.chunk(progressMessage))
					.concatWith(Flux.just(ToolResult.text(finalMessage).withFinal(true)));
			}

			@Override
			public String call(String toolInput) {
				return callAsync(toolInput, new ToolContext(Map.of())).join();
			}
		};
	}

	/**
	 * 生成模拟的图片数据（用于示例）
	 *
	 * <p>实际应用中，应该调用真实的图片生成 API</p>
	 */
	private byte[] generateFakeImageData() {
		// 创建一个简单的 1x1 PNG 图片（红色像素）
		// PNG 文件格式: 签名 + IHDR + IDAT + IEND
		return new byte[] {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG 签名
			0x00, 0x00, 0x00, 0x0D, // IHDR 长度
			0x49, 0x48, 0x44, 0x52, // "IHDR"
			0x00, 0x00, 0x00, 0x01, // 宽度 1
			0x00, 0x00, 0x00, 0x01, // 高度 1
			0x08, 0x02, // 位深度 8, 彩色
			0x00, 0x00, 0x00, // 压缩、过滤、隔行
			(byte) 0x90, 0x77, 0x53, (byte) 0xDE, // CRC
			0x00, 0x00, 0x00, 0x0C, // IDAT 长度
			0x49, 0x44, 0x41, 0x54, // "IDAT"
			0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00, // 压缩数据
			0x00, 0x03, 0x00, 0x01, // CRC 前半
			0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB4, // CRC 后半
			0x00, 0x00, 0x00, 0x00, // IEND 长度
			0x49, 0x45, 0x4E, 0x44, // "IEND"
			(byte) 0xAE, 0x42, 0x60, (byte) 0x82  // CRC
		};
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 流式工具执行示例 (Streaming Tool Execution Examples) ===\n");
		System.out.println("Issue #3912 - 流式工具和多模态结果支持功能演示\n");
		System.out.println("================================================================\n");

		try {
			// 示例 1：基础流式工具
			example1_basicStreamingTool();

			// 示例 2：多模态工具结果
			example2_multimodalToolResults();

			// 示例 3：流式结果合并
			example3_chunkMergingAndAccumulation();

			// 示例 4：流式错误处理
			example4_streamingErrorHandling();

			// 示例 5：AgentToolNode 流式执行配置
			example5_agentToolNodeStreamingExecution();

			// 示例 6：ToolStreamingOutput 用法
			example6_toolStreamingOutputUsage();

			// 示例 7：在 ReactAgent 中使用流式工具
			example7_streamingToolsInReactAgent();

			System.out.println("================================================================");
			System.out.println("所有示例执行完成！");
			System.out.println("================================================================");

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			// 清理 Reactor 调度器线程，避免 Maven exec 插件警告
			// Clean up Reactor scheduler threads to avoid Maven exec plugin warnings
			Schedulers.shutdownNow();
		}
	}

}
