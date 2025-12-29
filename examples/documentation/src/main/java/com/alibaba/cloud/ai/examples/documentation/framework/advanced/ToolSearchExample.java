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
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch.LuceneToolSearcher;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch.ToolSearchModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch.ToolSearchTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Tool Search Tool 示例
 */
public class ToolSearchExample {

	private final ChatModel chatModel;

	public ToolSearchExample(ChatModel chatModel) {
		this.chatModel = chatModel;
	}


	public void basicToolSearchUsage() throws Exception {

		// 步骤1: 创建工具搜索器
		LuceneToolSearcher toolSearcher = new LuceneToolSearcher();

		// 步骤2: 创建所有可用工具
		List<ToolCallback> availableTools = createAvailableTools();

		// 步骤3: 索引所有工具
		toolSearcher.indexTools(availableTools);
		System.out.println("✓ 已索引 " + availableTools.size() + " 个工具");

		// 步骤4: 创建拦截器
		ToolSearchModelInterceptor interceptor = ToolSearchModelInterceptor.builder()
			.toolSearcher(toolSearcher)
			.maxResults(5)
			.maxRecursionDepth(3)
			.build();

		// 步骤5: 创建 tool_search 工具
		ToolCallback toolSearchTool = ToolSearchTool.builder(toolSearcher)
			.withMaxResults(5)
			.build();

		// 步骤6: 创建 Agent
		ReactAgent agent = ReactAgent.builder()
			.name("tool_search_agent")
			.model(chatModel)
			.systemPrompt("You are a helpful assistant.")
			.tools(toolSearchTool)
			.interceptors(interceptor)
			.enableLogging(true)
			.build();


		AssistantMessage response = agent.call("北京今天天气怎么样？");

		System.out.println("\n最终回答：");
		if (response.getText() != null && !response.getText().isEmpty()) {
			System.out.println(response.getText());
		} else {
			System.out.println("（Agent 完成了工具调用但未生成文本回答）");
			System.out.println("工具调用数量: " + (response.getToolCalls() != null ? response.getToolCalls().size() : 0));
		}
	}


	public void multiStepToolDiscovery() throws Exception {

		// 创建工具搜索环境
		LuceneToolSearcher toolSearcher = new LuceneToolSearcher();
		toolSearcher.indexTools(createAvailableTools());

		// 创建拦截器
		ToolSearchModelInterceptor interceptor = ToolSearchModelInterceptor.builder()
			.toolSearcher(toolSearcher)
			.maxResults(5)
			.maxRecursionDepth(5)
			.build();

		// 创建 tool_search 工具
		ToolCallback toolSearchTool = ToolSearchTool.builder(toolSearcher)
			.withMaxResults(5)
			.build();

		// 创建 Agent
		ReactAgent agent = ReactAgent.builder()
			.name("multi_step_agent")
			.model(chatModel)
			.systemPrompt("你是一个智能助手，可以按步骤完成复杂任务。")
			.tools(toolSearchTool)
			.interceptors(interceptor)
			.enableLogging(true)
			.build();


		AssistantMessage response = agent.call(
			"查询上海天气并发邮件告诉我结果"
		);

		if (response.getText() != null && !response.getText().isEmpty()) {
			System.out.println(response.getText());
		} else {
			System.out.println("（Agent 完成了工具调用但未生成文本回答）");
		}
	}


	public void handleToolNotFound() throws Exception {

		LuceneToolSearcher toolSearcher = new LuceneToolSearcher();
		toolSearcher.indexTools(createAvailableTools());

		ToolSearchModelInterceptor interceptor = ToolSearchModelInterceptor.builder()
			.toolSearcher(toolSearcher)
			.maxResults(5)
			.maxRecursionDepth(3)
			.build();

		ToolCallback toolSearchTool = ToolSearchTool.builder(toolSearcher)
			.withMaxResults(5)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("resilient_agent")
			.model(chatModel)
			.systemPrompt("你是一个诚实的助手。如果找不到需要的工具，请礼貌地告知用户你当前无法完成该任务。")
			.tools(toolSearchTool)
			.interceptors(interceptor)
			.build();

		AssistantMessage response = agent.call("帮我订一张下周去东京的机票");

		System.out.println("\n最终回答：");
		if (response.getText() != null && !response.getText().isEmpty()) {
			System.out.println(response.getText());
		} else {
			System.out.println("（Agent 完成了工具搜索但未生成文本回答）");
		}
	}

	public void fuzzySearchCapability() throws Exception {

		LuceneToolSearcher toolSearcher = new LuceneToolSearcher();
		toolSearcher.indexTools(createAvailableTools());

		ToolSearchModelInterceptor interceptor = ToolSearchModelInterceptor.builder()
			.toolSearcher(toolSearcher)
			.maxResults(5)
			.maxRecursionDepth(3)
			.build();

		ToolCallback toolSearchTool = ToolSearchTool.builder(toolSearcher)
			.withMaxResults(5)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("fuzzy_search_agent")
			.model(chatModel)
			.systemPrompt("你是一个智能助手，善于理解用户的意图。")
			.tools(toolSearchTool)
			.interceptors(interceptor)
			.enableLogging(true)
			.build();

		// 测试用不同的描述方式
		String[] testQueries = {
			"外面温度怎么样？",           // 模糊描述天气
			"给我同事发个消息",           // 模糊描述邮件
			"帮我算一下数学题"            // 模糊描述计算
		};

		for (String query : testQueries) {

			try {
				AssistantMessage response = agent.call(query);
				System.out.println("成功找到并使用了合适的工具");
				System.out.println("回答: " + response.getText().substring(0, Math.min(100, response.getText().length())) + "...");
			} catch (Exception e) {
				System.out.println("未能找到合适的工具: " + e.getMessage());
			}
		}
	}

	/**
	 * 创建模拟工具列表
	 */
	private List<ToolCallback> createAvailableTools() {
		List<ToolCallback> tools = new ArrayList<>();

		// 天气查询工具
		tools.add(FunctionToolCallback.builder("get_weather", 
			(BiFunction<WeatherRequest, ToolContext, String>) (request, context) -> {
				return String.format("%s 的天气：晴天，温度 25°C，湿度 60%%", request.city);
			})
			.description("获取指定城市的当前天气信息 Get current weather information for a specific city. 包括温度 temperature、天气状况 conditions 和湿度 humidity")
			.inputType(WeatherRequest.class)
			.build());

		// 数据库查询工具
		tools.add(FunctionToolCallback.builder("query_database",
			(BiFunction<DatabaseRequest, ToolContext, String>) (request, context) -> {
				return String.format("查询已执行：%s\n影响行数：5\n结果：[{id: 1, name: '示例数据'}, ...]", request.sql);
			})
			.description("对应用数据库执行 SQL 查询 Execute SQL queries against the database. 支持 SELECT、INSERT、UPDATE、DELETE 操作")
			.inputType(DatabaseRequest.class)
			.build());

		// 文件操作工具
		tools.add(FunctionToolCallback.builder("read_file",
			(BiFunction<FileRequest, ToolContext, String>) (request, context) -> {
				return String.format("文件：%s\n大小：1024 字节\n内容：示例文件内容...", request.path);
			})
			.description("从文件系统读取文件内容 Read file content from filesystem. 支持文本文件 text files、JSON、CSV 和 XML 格式")
			.inputType(FileRequest.class)
			.build());

		// 计算工具
		tools.add(FunctionToolCallback.builder("calculate",
			(BiFunction<CalculateRequest, ToolContext, String>) (request, context) -> {
				int result = request.a + request.b;
				return String.format("计算：%d + %d = %d", request.a, request.b, result);
			})
			.description("执行算术计算 Perform arithmetic calculations. 包括加法 addition、减法 subtraction、乘法 multiplication 和除法 division")
			.inputType(CalculateRequest.class)
			.build());

		// 邮件发送工具
		tools.add(FunctionToolCallback.builder("send_email",
			(BiFunction<EmailRequest, ToolContext, String>) (request, context) -> {
				return String.format("邮件发送成功！\n收件人：%s\n主题：%s\n消息ID：msg_%d", 
					request.to, request.subject, System.currentTimeMillis());
			})
			.description("向指定收件人发送电子邮件 Send email to recipient. 支持纯文本和 HTML 内容 plain text and HTML content")
			.inputType(EmailRequest.class)
			.build());

		// HTTP 请求工具
		tools.add(FunctionToolCallback.builder("http_request",
			(BiFunction<HttpRequest, ToolContext, String>) (request, context) -> {
				return String.format("HTTP %s 请求到：%s\n状态：200 OK\n响应：{\"success\": true}", 
					request.method, request.url);
			})
			.description("向外部 API 发起 HTTP 请求 Make HTTP request to external API. 支持 GET、POST、PUT、DELETE 方法")
			.inputType(HttpRequest.class)
			.build());

		// 数据转换工具
		tools.add(FunctionToolCallback.builder("convert_data",
			(BiFunction<ConvertRequest, ToolContext, String>) (request, context) -> {
				return String.format("数据已从 %s 转换为 %s 格式\n结果：转换后的内容...", 
					request.fromFormat, request.toFormat);
			})
			.description("在不同格式之间转换数据 Convert data between different formats. 如 JSON、XML、CSV、YAML")
			.inputType(ConvertRequest.class)
			.build());

		// 时间工具
		tools.add(FunctionToolCallback.builder("get_time",
			(BiFunction<TimeRequest, ToolContext, String>) (request, context) -> {
				return String.format("%s 时区的当前时间：2025-12-29 15:30:00 %s", 
					request.timezone, request.timezone);
			})
			.description("获取指定时区的当前时间 Get current time in timezone. 支持所有标准时区标识符 all standard timezone identifiers")
			.inputType(TimeRequest.class)
			.build());

		// 图片生成工具
		tools.add(FunctionToolCallback.builder("generate_image",
			(BiFunction<ImageRequest, ToolContext, String>) (request, context) -> {
				return String.format("图片已生成：%s\n尺寸：1024x1024\n格式：PNG\n链接：https://example.com/image_%d.png", 
					request.prompt, System.currentTimeMillis());
			})
			.description("使用先进的图像生成模型根据文本描述生成 AI 图片 Generate AI image from text description using advanced image generation model")
			.inputType(ImageRequest.class)
			.build());

		// 翻译工具
		tools.add(FunctionToolCallback.builder("translate",
			(BiFunction<TranslateRequest, ToolContext, String>) (request, context) -> {
				return String.format("从 %s 翻译到 %s：\n原文：%s\n译文：[已翻译的文本]", 
					request.fromLang, request.toLang, request.text);
			})
			.description("在不同语言之间翻译文本 Translate text between languages. 支持 50+ 种语言，包括中文 Chinese、英文 English、日文 Japanese 等")
			.inputType(TranslateRequest.class)
			.build());

		return tools;
	}

	// ==================== 工具请求类 ====================

	public record WeatherRequest(String city) {}

	public record DatabaseRequest(String sql) {}

	public record FileRequest(String path) {}

	public record CalculateRequest(int a, int b) {}

	public record EmailRequest(String to, String subject, String body) {}

	public record HttpRequest(String method, String url) {}

	public record ConvertRequest(String fromFormat, String toFormat, String data) {}

	public record TimeRequest(String timezone) {}

	public record ImageRequest(String prompt) {}

	public record TranslateRequest(String text, String fromLang, String toLang) {}

	/**
	 * Main 方法：运行所有示例
	 * 
	 * 使用前请设置环境变量：
	 * export AI_DASHSCOPE_API_KEY=your_api_key
	 */
	public static void main(String[] args) {
		// 检查 API Key
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			System.err.println("错误：请先设置环境变量 AI_DASHSCOPE_API_KEY");
			System.err.println("示例：export AI_DASHSCOPE_API_KEY=your_api_key");
			return;
		}

		// 创建 ChatModel
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(apiKey)
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建示例实例
		ToolSearchExample example = new ToolSearchExample(chatModel);

		try {

			example.basicToolSearchUsage();

			example.multiStepToolDiscovery();

			example.handleToolNotFound();

			example.fuzzySearchCapability();

		} catch (Exception e) {
			System.err.println("示例执行出错：" + e.getMessage());
			e.printStackTrace();
		}
	}

}