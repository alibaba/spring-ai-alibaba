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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced.toolselection;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Optional;

/**
 * ToolSelectionInterceptor 示例
 *
 * 本示例演示如何使用 ToolSelectionInterceptor 进行智能工具选择。
 *
 * 核心功能：
 * 1. 当 Agent 有多个工具时，使用 LLM 智能选择最相关的工具
 * 2. 工具描述会自动传递给选择模型，提高选择准确性
 * 3. 可配置 maxTools 限制每次选择的工具数量
 * 4. 支持 alwaysInclude 确保关键工具始终可用
 *
 * 使用场景：
 * - Agent 拥有大量工具（>5个），需要减少 token 消耗
 * - 需要提高工具选择的准确性
 * - 不同查询需要不同的工具子集
 */
public class ToolSelectionExample {

	// ==================== 示例1：基础用法 ====================

	/**
	 * 基础用法：创建带有工具选择的 Agent
	 */
	public static void basicToolSelection() throws Exception {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建工具类实例
		TravelTools travelTools = new TravelTools();

		// 创建 ToolSelectionInterceptor
		// 当工具数量超过 maxTools 时，会使用 LLM 选择最相关的工具
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(chatModel)  // 用于选择工具的模型
			.maxTools(3)                // 最多选择3个工具
			.build();

		// 创建 Agent
		ReactAgent agent = ReactAgent.builder()
			.name("travel_assistant")
			.model(chatModel)
			.methodTools(travelTools)   // 自动扫描 @Tool 注解的方法
			.interceptors(interceptor)
			.saver(new MemorySaver())
			.build();

		// 调用 Agent - 会自动选择最相关的工具
		Optional<OverAllState> result = agent.invoke("北京今天天气怎么样？");
		printResult(result, "基础用法");
	}

	// ==================== 示例2：使用 alwaysInclude ====================

	/**
	 * 高级用法：使用 alwaysInclude 确保关键工具始终可用
	 */
	public static void toolSelectionWithAlwaysInclude() throws Exception {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		TravelTools travelTools = new TravelTools();

		// 使用 alwaysInclude 确保某些工具始终可用
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(chatModel)
			.maxTools(2)
			.alwaysInclude("get_weather")  // 天气工具始终包含
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("travel_assistant")
			.model(chatModel)
			.methodTools(travelTools)
			.interceptors(interceptor)
			.saver(new MemorySaver())
			.build();

		// 即使查询与天气无关，weather 工具也会被包含
		Optional<OverAllState> result = agent.invoke("帮我预订一张去上海的机票");
		printResult(result, "alwaysInclude 示例");
	}

	// ==================== 示例3：自定义系统提示词 ====================

	/**
	 * 高级用法：自定义工具选择的系统提示词
	 */
	public static void toolSelectionWithCustomPrompt() throws Exception {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		TravelTools travelTools = new TravelTools();

		// 自定义选择逻辑的系统提示词
		String customPrompt = """
			你是一个旅行助手的工具选择器。
			根据用户的查询，选择最相关的工具来帮助回答问题。

			选择原则：
			1. 优先选择能直接解决用户问题的工具
			2. 如果用户询问多个方面，选择覆盖所有方面的工具
			3. 避免选择明显不相关的工具
			""";

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(chatModel)
			.maxTools(3)
			.systemPrompt(customPrompt)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("travel_assistant")
			.model(chatModel)
			.methodTools(travelTools)
			.interceptors(interceptor)
			.saver(new MemorySaver())
			.build();

		Optional<OverAllState> result = agent.invoke("我下周要去杭州旅游，帮我看看天气和景点");
		printResult(result, "自定义提示词示例");
	}

	// ==================== 示例4：多工具场景 ====================

	/**
	 * 复杂场景：拥有多个工具的 Agent
	 */
	public static void multiToolScenario() throws Exception {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		// 创建多个工具类
		TravelTools travelTools = new TravelTools();
		UtilityTools utilityTools = new UtilityTools();

		// 配置工具选择
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
			.selectionModel(chatModel)
			.maxTools(3)  // 从8+个工具中选择3个
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("smart_assistant")
			.model(chatModel)
			.methodTools(travelTools, utilityTools)  // 注册多个工具类
			.interceptors(interceptor)
			.saver(new MemorySaver())
			.build();

		// 测试不同的查询
		System.out.println("\n--- 测试1：天气查询 ---");
		Optional<OverAllState> result1 = agent.invoke("北京今天天气如何？");
		printResult(result1, "天气查询");

		System.out.println("\n--- 测试2：机票查询 ---");
		Optional<OverAllState> result2 = agent.invoke("查一下明天从上海到北京的机票");
		printResult(result2, "机票查询");

		System.out.println("\n--- 测试3：货币转换 ---");
		Optional<OverAllState> result3 = agent.invoke("100美元能换多少人民币？");
		printResult(result3, "货币转换");

		System.out.println("\n--- 测试4：复合查询 ---");
		Optional<OverAllState> result4 = agent.invoke("我要去杭州旅游，帮我看看天气、推荐景点、再找个酒店");
		printResult(result4, "复合查询");
	}

	// ==================== 工具类定义 ====================

	/**
	 * 旅行相关工具
	 *
	 * 注意：工具描述要详细、准确，这样 ToolSelectionInterceptor 才能做出正确选择
	 */
	public static class TravelTools {

		@Tool(name = "get_weather",
			  description = "获取指定城市的实时天气信息，包括温度、湿度、天气状况和空气质量。" +
						   "当用户询问某个城市的天气时使用此工具。")
		public String getWeather(
				@ToolParam(description = "城市名称，如：北京、上海、广州") String city) {
			return String.format("%s今日天气：晴，温度 18-25°C，湿度 45%%，空气质量良好。", city);
		}

		@Tool(name = "search_flights",
			  description = "搜索两个城市之间的航班信息，返回航班号、出发时间、到达时间和票价。" +
						   "当用户想要查询或预订机票时使用此工具。")
		public String searchFlights(
				@ToolParam(description = "出发城市") String from,
				@ToolParam(description = "到达城市") String to,
				@ToolParam(description = "出发日期，格式：YYYY-MM-DD") String date) {
			return String.format("找到 %s 到 %s 的航班（%s）：\n" +
				"1. CA1234 08:00-10:30 ¥680\n" +
				"2. MU5678 12:00-14:30 ¥720\n" +
				"3. CZ9012 18:00-20:30 ¥650", from, to, date);
		}

		@Tool(name = "search_hotels",
			  description = "搜索指定城市的酒店，可按入住日期和价格范围筛选。" +
						   "当用户想要预订住宿时使用此工具。")
		public String searchHotels(
				@ToolParam(description = "城市名称") String city,
				@ToolParam(description = "入住日期，格式：YYYY-MM-DD") String arrivalDate) {
			return String.format("%s 酒店推荐（%s 入住）：\n" +
				"1. 希尔顿酒店 ★★★★★ ¥800/晚\n" +
				"2. 如家酒店 ★★★ ¥280/晚\n" +
				"3. 民宿小院 ★★★★ ¥450/晚", city, arrivalDate);
		}

		@Tool(name = "get_attractions",
			  description = "获取指定城市的热门旅游景点列表，包括景点介绍、门票价格和推荐游览时间。" +
						   "当用户想要了解旅游目的地的景点时使用此工具。")
		public String getAttractions(
				@ToolParam(description = "城市名称") String city) {
			return String.format("%s 热门景点：\n" +
				"1. 西湖 - 免费，建议游览半天\n" +
				"2. 灵隐寺 - 门票¥45，上香另付\n" +
				"3. 宋城 - 门票¥300，含演出", city);
		}

		@Tool(name = "search_restaurants",
			  description = "搜索指定城市的餐厅，可按菜系和价格范围筛选。" +
						   "当用户想要找地方吃饭或了解当地美食时使用此工具。")
		public String searchRestaurants(
				@ToolParam(description = "城市名称") String city,
				@ToolParam(description = "菜系类型，如：火锅、川菜、粤菜等") String cuisine) {
			return String.format("%s %s 餐厅推荐：\n" +
				"1. 老字号餐厅 - 人均¥80 评分4.8\n" +
				"2. 网红打卡店 - 人均¥120 评分4.5\n" +
				"3. 本地特色馆 - 人均¥60 评分4.7", city, cuisine);
		}
	}

	/**
	 * 实用工具类
	 */
	public static class UtilityTools {

		@Tool(name = "convert_currency",
			  description = "货币汇率转换，支持多种货币之间的转换（如 USD、EUR、CNY、JPY）。" +
						   "当用户需要了解汇率或进行货币换算时使用此工具。")
		public String convertCurrency(
				@ToolParam(description = "金额") double amount,
				@ToolParam(description = "源货币代码，如 USD, EUR, CNY") String from,
				@ToolParam(description = "目标货币代码") String to) {
			double rate = 7.2; // 简化的汇率
			if ("USD".equals(from) && "CNY".equals(to)) {
				return String.format("%.2f 美元 = %.2f 人民币（汇率: 1 USD = %.2f CNY）",
					amount, amount * rate, rate);
			}
			return String.format("%.2f %s = %.2f %s", amount, from, amount, to);
		}

		@Tool(name = "translate_text",
			  description = "文本翻译服务，支持中英日韩等多种语言互译。" +
						   "当用户需要翻译文字或了解外语含义时使用此工具。")
		public String translateText(
				@ToolParam(description = "要翻译的文本") String text,
				@ToolParam(description = "目标语言：中文、英文、日文、韩文") String targetLang) {
			return String.format("翻译结果（%s）：[翻译后的内容]", targetLang);
		}

		@Tool(name = "calculate",
			  description = "数学计算器，支持加减乘除、幂运算、百分比等计算。" +
						   "当用户需要进行数学计算时使用此工具。")
		public String calculate(
				@ToolParam(description = "数学表达式，如：100*1.1、50+30") String expression) {
			return "计算结果：" + expression + " = [结果]";
		}
	}

	// ==================== 辅助方法 ====================

	private static void printResult(Optional<OverAllState> result, String testName) {
		System.out.println("[" + testName + "] 执行结果：");
		result.ifPresent(state -> {
			List<Message> messages = state.value("messages", List.of());
			for (Message msg : messages) {
				if (msg instanceof AssistantMessage) {
					System.out.println("助手: " + msg.getText());
				}
			}
		});
	}

	// ==================== Main 方法 ====================

	public static void main(String[] args) {
		System.out.println("=== ToolSelectionInterceptor 示例 ===");
		System.out.println("注意：需要设置 AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：基础用法 ---");
			basicToolSelection();

			System.out.println("\n--- 示例2：使用 alwaysInclude ---");
			toolSelectionWithAlwaysInclude();

			System.out.println("\n--- 示例3：自定义系统提示词 ---");
			toolSelectionWithCustomPrompt();

			System.out.println("\n--- 示例4：多工具场景 ---");
			multiToolScenario();

			System.out.println("\n=== 所有示例执行完成 ===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错误: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
