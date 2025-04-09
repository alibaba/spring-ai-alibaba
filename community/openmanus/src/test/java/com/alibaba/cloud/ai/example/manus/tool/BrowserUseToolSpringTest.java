/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

/**
 * BrowserUseTool的Spring集成测试类 使用真实的Spring上下文来测试BrowserUseTool的功能
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("仅用于本地测试，CI 环境跳过") // 添加这一行
class BrowserUseToolSpringTest {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseToolSpringTest.class);

	@Autowired
	private ChromeDriverService chromeDriverService;

	private BrowserUseTool browserUseTool;

	@Autowired
	private LlmService llmService;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private ManusProperties manusProperties;

	@BeforeEach
	void setUp() {
		browserUseTool = new BrowserUseTool(chromeDriverService);
		manusProperties.setBrowserHeadless(true);
		DummyBaseAgent agent = new DummyBaseAgent(llmService, planExecutionRecorder, manusProperties);
		agent.setPlanId("plan_123123124124124");
		browserUseTool.setAgent(agent);
	}

	private static class DummyBaseAgent extends BaseAgent {

		public DummyBaseAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
				ManusProperties manusProperties) {
			super(llmService, planExecutionRecorder, manusProperties);

		}

		@Override
		public String getName() {
			return "DummyAgent";
		}

		@Override
		public String getDescription() {
			return "A dummy agent for testing";
		}

		@Override
		protected Message getNextStepWithEnvMessage() {
			return null;
		}

		@Override
		public List<ToolCallback> getToolCallList() {
			return null;
		}

		@Override
		protected String step() {
			return "Dummy step";
		}

		@Override
		protected Message addThinkPrompt(List<Message> messages) {
			return null;
		}

	}

	@Test
	@Order(1)
	@DisplayName("测试浏览器搜索'Hello World'")
	void testHelloWorldSearch() {
		try {
			// 步骤1: 导航到百度
			log.info("步骤1: 导航到百度");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertEquals("Navigated to https://www.baidu.com", navigateResult.getOutput(), "导航到百度失败");

			// 步骤2: 获取并验证可交互元素
			log.info("步骤2: 获取可交互元素并分析");
			Map<String, Object> state = browserUseTool.getCurrentState();
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "获取可交互元素失败");
			log.info("获取到的可交互元素: {}", elements);

			// 步骤3: 找到搜索框
			log.info("步骤3: 定位搜索框");
			int searchInputIndex = -1;
			String[] elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("name=\"wd\"") || elementLines[i].contains("id=\"kw\"")) { // 百度搜索框的特征
					searchInputIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchInputIndex, "未找到搜索框");
			log.info("找到搜索框索引: {}", searchInputIndex);

			// 步骤4: 在搜索框中输入文本
			log.info("步骤4: 在搜索框中输入'Hello World'");
			ToolExecuteResult inputResult = executeAction("input_text", null, searchInputIndex, "Hello World");
			Assertions.assertTrue(inputResult.getOutput().contains("Successfully input 'Hello World'"), "在搜索框输入文本失败");

			// 步骤5: 重新获取状态并查找搜索按钮
			log.info("步骤5: 定位搜索按钮");
			state = browserUseTool.getCurrentState();
			elements = (String) state.get("interactive_elements");
			int searchButtonIndex = -1;
			elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("value=\"百度一下\"") || elementLines[i].contains(">百度一下<")) {
					searchButtonIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchButtonIndex, "未找到搜索按钮");
			log.info("找到搜索按钮索引: {}", searchButtonIndex);

			// 步骤6: 点击搜索按钮
			log.info("步骤6: 点击搜索按钮");
			ToolExecuteResult clickResult = executeAction("click", null, searchButtonIndex, null);
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "点击搜索按钮失败");

			// 步骤7: 等待并验证搜索结果
			log.info("步骤7: 等待页面加载并获取搜索结果");
			Thread.sleep(2000); // 等待页面加载
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"), "搜索结果中未找到 'Hello World'");

			// 步骤8: 获取截图作为证据（可选）
			log.info("步骤8: 获取页面截图");
			ToolExecuteResult screenshotResult = executeAction("screenshot", null);
			Assertions.assertTrue(screenshotResult.getOutput().contains("Screenshot captured"), "获取截图失败");

		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	/**
	 * 导航到指定URL并验证可交互元素的通用方法
	 * @param tool BrowserUseTool实例
	 * @param url 目标URL
	 * @param expectedElements 期望在页面中出现的元素关键词列表
	 * @return 获取到的可交互元素字符串
	 */
	private String navigateAndVerifyElements(BrowserUseTool tool, String url, List<String> expectedElements) {
		// 步骤1: 导航到指定URL
		log.info("步骤1: 导航到 {}", url);
		ToolExecuteResult navigateResult = executeAction("navigate", url);
		Assertions.assertEquals("Navigated to " + url, navigateResult.getOutput(), "导航失败");

		// 步骤2: 获取并验证可交互元素
		log.info("步骤2: 获取可交互元素");
		Map<String, Object> state = tool.getCurrentState();
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "获取可交互元素失败");
		log.info("获取到的可交互元素: {}", elements);

		// 步骤3: 验证期望的元素
		log.info("步骤3: 验证期望的元素");
		String[] elementLines = elements.split("\n");
		boolean foundMatchingElement = false;

		for (String elementLine : elementLines) {
			boolean allExpectedFound = true;
			for (String expectedElement : expectedElements) {
				if (!elementLine.contains(expectedElement)) {
					allExpectedFound = false;
					break;
				}
			}
			if (allExpectedFound) {
				foundMatchingElement = true;
				log.info("找到匹配所有特征的元素: {}", elementLine);
				break;
			}
		}

		Assertions.assertTrue(foundMatchingElement, String.format("未找到同时包含所有期望特征的元素。期望特征: %s", expectedElements));

		return elements;
	}

	@Test
	@Order(4)
	@DisplayName("测试导航到指定URL并获取交互元素")
	void testNavigateAndGetElements() {
		try {
			String testUrl = "https://www.bing.com";
			List<String> expectedElements = Arrays.asList("search", "textarea");

			// 使用通用方法进行测试
			String elements = navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);

			// 获取截图（可选）
			log.info("获取页面截图作为证据");
			ToolExecuteResult screenshotResult = executeAction("screenshot", null);
			Assertions.assertTrue(screenshotResult.getOutput().contains("Screenshot captured"), "获取截图失败");

			log.info("测试成功完成！");

		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	@Test
	@Order(5)
	@DisplayName("测试GitHub搜索页面元素")
	void testGitHubSearch() {
		try {
			String testUrl = "https://github.com/";
			List<String> expectedElements = Arrays.asList( // GitHub搜索框的name属性
					"search" // 搜索相关元素

			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("GitHub搜索页面测试成功完成！");
		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("测试Nacos页面元素")
	void testNacosPageLink() {
		try {
			String testUrl = "https://nacos.io/docs/latest/overview";
			List<String> expectedElements = Arrays.asList( // Nacos页面的特征
					"Java SDK 容灾" // 搜索相关元素
			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("Nacos页面测试成功完成！");
		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("测试百度首页元素")
	void testBaiduElements() {
		try {
			String testUrl = "https://www.baidu.com";
			List<String> expectedElements = Arrays.asList("name=\"wd\"", // 百度搜索框的特征
					"id=\"kw\"" // 搜索框的另一个特征
			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("百度首页测试成功完成！");
		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	// 辅助方法：执行浏览器操作
	private ToolExecuteResult executeAction(String action, String url) {
		return executeAction(action, url, null, null);
	}

	// 辅助方法：执行浏览器操作（带索引和文本）
	private ToolExecuteResult executeAction(String action, String url, Integer index, String text) {

		Map<String, Object> params = new HashMap<>();
		params.put("action", action);

		if (url != null) {
			params.put("url", url);
		}

		if (index != null) {
			params.put("index", index);
		}

		if (text != null) {
			params.put("text", text);
		}

		String toolInput = JSON.toJSONString(params);
		return browserUseTool.run(toolInput);
	}

}
