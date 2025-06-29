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

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.BrowserRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetElementPositionByNameAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.MoveToAndClickAction;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {

		manusProperties.setBrowserHeadless(false);
		manusProperties.setBrowserDebug(true);
		chromeDriverService.setManusProperties(manusProperties);
		browserUseTool = new BrowserUseTool(chromeDriverService);
		DummyBaseAgent agent = new DummyBaseAgent(llmService, planExecutionRecorder, manusProperties);
		agent.setPlanId("plan_123123124124124");
		browserUseTool.setPlanId(agent.getPlanId());

	}

	private static class DummyBaseAgent extends BaseAgent {

		public DummyBaseAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
				ManusProperties manusProperties) {
			// TODO #1371 引入的bug，临时传入 null 处理 CI 报错
			super(llmService, planExecutionRecorder, manusProperties, new HashMap<>(), null);

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
		protected Message getThinkMessage() {
			return null;
		}

		@Override
		public void clearUp(String planId) {
			return;
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
			Page page = browserUseTool.getDriver().getCurrentPage();

			// 步骤2: 获取并验证可交互元素
			log.info("步骤2: 获取可交互元素并分析");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
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
			Assertions.assertTrue(inputResult.getOutput().contains("Hello World"), "在搜索框输入文本失败");

			// 步骤5: 重新获取状态并查找搜索按钮
			log.info("步骤5: 定位搜索按钮");

			state = browserUseTool.getCurrentState(page);
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
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "点击搜索按钮失败 : " + clickResult.getOutput());

			// 步骤7: 等待并验证搜索结果
			log.info("步骤7: 等待页面加载并获取搜索结果");
			Thread.sleep(2000); // 等待页面加载
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"), "搜索结果中未找到 'Hello World'");

			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			searchButtonIndex = -1;
			elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("hello world") && elementLines[i].contains("百度百科")) {
					searchButtonIndex = i;
					break;
				}
			}
			clickResult = executeAction("click", null, searchButtonIndex, null);
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "点击搜索按钮失败 : " + clickResult.getOutput());

			log.info("登录 success ");

		}
		catch (Exception e) {
			log.error("测试过程中发生错误", e);
			Assertions.fail("测试执行失败: " + e.getMessage());
		}
	}

	@Test
	@Order(2)
	@DisplayName("测试通过元素名定位并点击百度搜索按钮")
	void testGetElementPositionAndMoveToClick() {
		try {
			// 步骤1: 导航到百度
			log.info("步骤1: 导航到百度");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertEquals("Navigated to https://www.baidu.com", navigateResult.getOutput(), "导航到百度失败");
			Page page = browserUseTool.getDriver().getCurrentPage();

			// 步骤2: 获取并验证可交互元素
			log.info("步骤2: 获取可交互元素并分析");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
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
			Assertions.assertTrue(inputResult.getOutput().contains("Hello World"), "在搜索框输入文本失败");

			// 步骤5: 使用GetElementPositionByNameAction查找搜索按钮
			log.info("步骤5: 使用GetElementPositionByNameAction查找'百度一下'按钮");
			BrowserRequestVO positionRequest = new BrowserRequestVO();
			positionRequest.setElementName("百度一下");
			GetElementPositionByNameAction positionAction = new GetElementPositionByNameAction(browserUseTool);
			ToolExecuteResult positionResult = positionAction.execute(positionRequest);
			log.info("获取到'百度一下'按钮位置信息: {}", positionResult.getOutput());

			// 解析JSON结果获取坐标
			List<?> positionsList = objectMapper.readValue(positionResult.getOutput(), new TypeReference<List<?>>() {
			});
			Assertions.assertFalse(positionsList.isEmpty(), "未找到'百度一下'按钮");
			Map<?, ?> elementPosition = (Map<?, ?>) positionsList.get(0);
			Double xNumber = (Double) elementPosition.get("x");
			Double yNumber = (Double) elementPosition.get("y");
			Double x = xNumber.doubleValue();
			Double y = yNumber.doubleValue();
			log.info("'百度一下'按钮坐标: x={}, y={}", x, y);

			// 步骤6: 使用MoveToAndClickAction点击搜索按钮
			log.info("步骤6: 使用MoveToAndClickAction点击'百度一下'按钮");
			BrowserRequestVO clickRequest = new BrowserRequestVO();
			clickRequest.setPositionX(x);
			clickRequest.setPositionY(y);
			MoveToAndClickAction clickAction = new MoveToAndClickAction(browserUseTool);
			ToolExecuteResult clickResult = clickAction.execute(clickRequest);
			log.info("点击结果: {}", clickResult.getOutput());
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "点击'百度一下'按钮失败");

			// 步骤7: 等待并验证搜索结果
			log.info("步骤7: 等待页面加载并获取搜索结果");
			Thread.sleep(2000); // 等待页面加载
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"), "搜索结果中未找到 'Hello World'");

			// 步骤8: 使用GetElementPositionByNameAction查找并点击百度百科链接
			log.info("步骤8: 使用GetElementPositionByNameAction查找'百度百科'链接");
			BrowserRequestVO baikePositionRequest = new BrowserRequestVO();
			baikePositionRequest.setElementName("百度百科");
			GetElementPositionByNameAction baikePositionAction = new GetElementPositionByNameAction(browserUseTool);
			ToolExecuteResult baikePositionResult = baikePositionAction.execute(baikePositionRequest);
			log.info("获取到'百度百科'链接位置信息: {}", baikePositionResult.getOutput());

			// 解析JSON结果获取百度百科链接坐标
			List<?> baikePositionsList = objectMapper.readValue(baikePositionResult.getOutput(),
					new TypeReference<List<?>>() {
					});

			if (!baikePositionsList.isEmpty()) {
				// 查找包含"hello world"相关内容的百度百科链接
				Map<?, ?> targetPosition = null;
				for (Object positionObj : baikePositionsList) {
					Map<?, ?> position = (Map<?, ?>) positionObj;
					String elementText = (String) position.get("elementText");
					if (elementText != null && elementText.toLowerCase().contains("hello world(程序代码调试常用文本) - 百度百科")) {
						targetPosition = position;
						break;
					}
				}

				if (targetPosition != null) {
					Double baikeX = (Double) targetPosition.get("x");
					Double baikeY = (Double) targetPosition.get("y");
					log.info("'百度百科'链接坐标: x={}, y={}", baikeX, baikeY);

					// 使用MoveToAndClickAction点击百度百科链接
					log.info("使用MoveToAndClickAction点击'百度百科'链接");
					BrowserRequestVO baikeClickRequest = new BrowserRequestVO();
					baikeClickRequest.setPositionX(baikeX);
					baikeClickRequest.setPositionY(baikeY);
					MoveToAndClickAction baikeClickAction = new MoveToAndClickAction(browserUseTool);
					ToolExecuteResult baikeClickResult = baikeClickAction.execute(baikeClickRequest);
					log.info("百度百科链接点击结果: {}", baikeClickResult.getOutput());
					Assertions.assertTrue(baikeClickResult.getOutput().contains("Clicked"), "点击'百度百科'链接失败");
					log.info("成功点击百度百科链接");
				}
				else {
					log.warn("未找到包含'hello world'的百度百科链接");
				}
			}
			else {
				log.warn("未找到'百度百科'链接");
			}

			log.info("测试成功完成！");

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

		Page page = browserUseTool.getDriver().getCurrentPage();
		// 步骤2: 获取并验证可交互元素
		log.info("步骤2: 获取可交互元素");
		Map<String, Object> state = tool.getCurrentState(page);
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
			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);

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

			Page page = browserUseTool.getDriver().getCurrentPage();
			// 获取可交互元素
			log.info("步骤2: 获取并查找登录元素");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "获取可交互元素失败");
			log.info("获取到的可交互元素: {}", elements);

			// 步骤2: 查找"登录"按钮的索引
			String[] elementLines = elements.split("\n");
			int loginButtonIndex = -1;

			for (String line : elementLines) {
				// 从每行开头提取元素的实际索引号，格式如 [195] <input...
				if (line.matches("\\[\\d+\\].*")) {
					int indexEndPos = line.indexOf("]");
					String indexStr = line.substring(1, indexEndPos);
					int elementIndex = Integer.parseInt(indexStr);

					if (line.contains("登录")) {
						loginButtonIndex = elementIndex;
						log.info("找到登录按钮，索引: {}", loginButtonIndex);
						break;
					}
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
			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n"); // 步骤4:
													// 使用GetElementPositionByNameAction查找"APP登录"元素并通过坐标点击
			log.info("步骤4: 使用GetElementPositionByNameAction查找'APP登录'元素");

			// 创建请求对象并设置元素名称
			BrowserRequestVO positionRequest = new BrowserRequestVO();
			positionRequest.setElementName("验证码登录");

			// 执行GetElementPositionByNameAction获取元素位置
			GetElementPositionByNameAction positionAction = new GetElementPositionByNameAction(browserUseTool);
			ToolExecuteResult positionResult = positionAction.execute(positionRequest);
			log.info("获取到'验证码登录'元素位置信息: {}", positionResult.getOutput());

			// 解析JSON结果获取坐标
			List<?> positionsList = objectMapper.readValue(positionResult.getOutput(), new TypeReference<List<?>>() {
			});
			Assertions.assertFalse(positionsList.isEmpty(), "未找到'APP登录'元素");

			// 获取第一个匹配元素的位置信息
			Map<?, ?> elementPosition = (Map<?, ?>) positionsList.get(0);
			Double xNumber = (Double) elementPosition.get("x");
			Double yNumber = (Double) elementPosition.get("y");
			log.info("验证码登录元素坐标: x={}, y={}", xNumber, yNumber);

			// 使用MoveToAndClickAction通过坐标点击元素
			BrowserRequestVO clickRequest = new BrowserRequestVO();
			clickRequest.setPositionX(xNumber);
			clickRequest.setPositionY(yNumber);

			MoveToAndClickAction clickAction = new MoveToAndClickAction(browserUseTool);
			ToolExecuteResult clickResult = clickAction.execute(clickRequest);
			log.info("点击结果: {}", clickResult.getOutput());
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "点击验证码登录元素失败");

			// 等待密码登录表单加载
			log.info("等待密码登录表单加载...");
			Thread.sleep(1000);

			// 获取更新后的交互元素
			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n");

			// 步骤5: 查找手机号输入框
			log.info("步骤5: 查找手机号输入框");
			int phoneInputIndex = -1;
			int verifyCodeButtonIndex = -1;

			for (String line : elementLines) {
				// 从每行开头提取元素的实际索引号，格式如 [195] <input...
				if (line.matches("\\[\\d+\\].*")) {
					int indexEndPos = line.indexOf("]");
					String indexStr = line.substring(1, indexEndPos);
					int elementIndex = Integer.parseInt(indexStr);

					// 查找手机号输入框(可能包含phone、tel或mobile相关的input)
					if (line.contains("input") && line.contains("手机号")) {
						phoneInputIndex = elementIndex;
						log.info("找到手机号输入框，索引: {}", phoneInputIndex);
					}
					// 查找获取验证码按钮
					else if ((line.contains("button") || line.contains("a ")) && line.contains("获取验证码")) {
						verifyCodeButtonIndex = elementIndex;
						log.info("找到获取验证码按钮，索引: {}", verifyCodeButtonIndex);
					}
				}
			}

			Assertions.assertNotEquals(-1, phoneInputIndex, "未找到手机号输入框");

			// 步骤6: 在手机号输入框中输入"123456789"
			log.info("步骤6: 在手机号输入框中输入'123456789'");
			ToolExecuteResult phoneInputResult = executeAction("input_text", null, phoneInputIndex, "123456789");
			Assertions.assertTrue(phoneInputResult.getOutput().contains("成功输入:"), "在手机号输入框输入文本失败");

			// 步骤8: 验证手机号输入是否成功
			log.info("步骤8: 验证手机号输入是否成功");
			browserUseTool.getDriver().getInteractiveElementRegistry().refresh(page);
			state = browserUseTool.getCurrentState(page);
			String updatedElements = (String) state.get("interactive_elements");

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
		BrowserRequestVO request = new BrowserRequestVO();
		request.setAction(action);

		if (url != null) {
			request.setUrl(url);
		}

		if (index != null) {
			request.setIndex(index);
		}

		if (text != null) {
			request.setText(text);
		}

		return browserUseTool.run(request);
	}

}
