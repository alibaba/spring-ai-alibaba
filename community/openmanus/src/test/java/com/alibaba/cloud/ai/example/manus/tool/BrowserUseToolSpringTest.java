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
import com.alibaba.cloud.ai.example.manus.agent.AgentState;
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
// @Disabled("仅用于本地测试，CI 环境跳过") // 添加这一行
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
		browserUseTool.setPlanId(agent.getPlanId());
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
		protected AgentExecResult step() {
			return new AgentExecResult("Dummy step executed", AgentState.COMPLETED);
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
	
	@Test
	@Order(7)
	@DisplayName("测试CSDN网站登录功能")
	void testCsdnLogin() {
		try {
			// 步骤1: 导航到CSDN网站
			String testUrl = "https://www.csdn.net/";
			log.info("步骤1: 导航到CSDN网站");
			ToolExecuteResult navigateResult = executeAction("navigate", testUrl);
			Assertions.assertEquals("Navigated to " + testUrl, navigateResult.getOutput(), "导航到CSDN网站失败");
			
			// 获取可交互元素
			log.info("步骤2: 获取并查找登录元素");
			Map<String, Object> state = browserUseTool.getCurrentState();
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "获取可交互元素失败");
			log.info("获取到的可交互元素: {}", elements);
			
			// 步骤2: 查找"登录"按钮的索引
			String[] elementLines = elements.split("\n");
			int loginButtonIndex = -1;
			
			for (int i = 0; i < elementLines.length; i++) {
				String line = elementLines[i];
				if (line.contains("登录")) {
					loginButtonIndex = i;
					log.info("找到登录按钮，索引: {}", loginButtonIndex);
					break;
				}
			}
			
			Assertions.assertNotEquals(-1, loginButtonIndex, "未找到登录按钮");
			
			// 步骤3: 点击"登录"按钮
			log.info("步骤3: 点击登录按钮");
			ToolExecuteResult clickLoginResult = executeAction("click", null, loginButtonIndex, null);
			Assertions.assertTrue(clickLoginResult.getOutput().contains("Clicked"), "点击登录按钮失败");
			
			// 等待登录对话框加载
			log.info("等待登录对话框加载...");
			Thread.sleep(2000);
			
			// 获取更新后的交互元素
			state = browserUseTool.getCurrentState();
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n");
			
			// 步骤4: 查找并点击"密码登录"选项
			log.info("步骤4: 查找并点击'密码登录'选项");
			int passwordLoginTabIndex = -1;
			
			for (int i = 0; i < elementLines.length; i++) {
				String line = elementLines[i];
				if (line.contains("密码登录")) {
					passwordLoginTabIndex = i;
					log.info("找到密码登录选项，索引: {}", passwordLoginTabIndex);
					break;
				}
			}
			
			Assertions.assertNotEquals(-1, passwordLoginTabIndex, "未找到密码登录选项");
			
			// 点击"密码登录"选项
			ToolExecuteResult clickPasswordLoginResult = executeAction("click", null, passwordLoginTabIndex, null);
			Assertions.assertTrue(clickPasswordLoginResult.getOutput().contains("Clicked"), "点击密码登录选项失败");
			
			// 等待密码登录表单加载
			log.info("等待密码登录表单加载...");
			Thread.sleep(1000);
			
			// 获取更新后的交互元素
			state = browserUseTool.getCurrentState();
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n");
			
			// 步骤5: 查找用户名和密码输入框
			log.info("步骤5: 查找用户名和密码输入框");
			int usernameInputIndex = -1;
			int passwordInputIndex = -1;
			
			for (int i = 0; i < elementLines.length; i++) {
				String line = elementLines[i];
				// 查找用户名输入框(可能是email、phone或username相关的input)
				if (line.contains("input") && (line.contains("email") || line.contains("phone") || 
					line.contains("user") || line.contains("account") || line.contains("name"))) {
					usernameInputIndex = i;
					log.info("找到用户名输入框，索引: {}", usernameInputIndex);
				}
				// 查找密码输入框
				else if (line.contains("input") && (line.contains("password") || line.contains("pwd"))) {
					passwordInputIndex = i;
					log.info("找到密码输入框，索引: {}", passwordInputIndex);
				}
			}
			
			Assertions.assertNotEquals(-1, usernameInputIndex, "未找到用户名输入框");
			Assertions.assertNotEquals(-1, passwordInputIndex, "未找到密码输入框");
			
			// 步骤6: 在用户名输入框中输入"123"
			log.info("步骤6: 在用户名输入框中输入'123'");
			ToolExecuteResult usernameInputResult = executeAction("input_text", null, usernameInputIndex, "123");
			Assertions.assertTrue(usernameInputResult.getOutput().contains("Successfully input '123'"), "在用户名输入框输入文本失败");
			
			// 步骤7: 在密码输入框中输入"546"
			log.info("步骤7: 在密码输入框中输入'546'");
			ToolExecuteResult passwordInputResult = executeAction("input_text", null, passwordInputIndex, "546");
			Assertions.assertTrue(passwordInputResult.getOutput().contains("Successfully input '546'"), "在密码输入框输入文本失败");
			
			// 步骤8: 验证输入是否成功
			log.info("步骤8: 验证输入是否成功");
			state = browserUseTool.getCurrentState();
			String updatedElements = (String) state.get("interactive_elements");
			String[] updatedElementLines = updatedElements.split("\n");
			
			// 验证用户名和密码输入框的值
			boolean usernameVerified = false;
			boolean passwordVerified = false;
			
			for (String line : updatedElementLines) {
				if (line.contains("value=\"123\"") && (line.contains("email") || line.contains("phone") || 
					line.contains("user") || line.contains("account") || line.contains("name"))) {
					usernameVerified = true;
					log.info("验证用户名输入成功: {}", line);
				}
				if (line.contains("value=\"546\"") && (line.contains("password") || line.contains("pwd"))) {
					passwordVerified = true;
					log.info("验证密码输入成功: {}", line);
				}
			}
			
			Assertions.assertTrue(usernameVerified, "用户名未成功输入");
			Assertions.assertTrue(passwordVerified, "密码未成功输入");
			log.info("用户名和密码输入验证成功！");
			
			// 获取截图作为证据
			log.info("获取页面截图作为证据");
			ToolExecuteResult screenshotResult = executeAction("screenshot", null);
			Assertions.assertTrue(screenshotResult.getOutput().contains("Screenshot captured"), "获取截图失败");
			
			log.info("CSDN登录测试完成");
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
