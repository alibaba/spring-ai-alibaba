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

import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
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
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.BrowserRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetElementPositionByNameAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.MoveToAndClickAction;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.playwright.Page;

/**
 * Spring integration test class for BrowserUseTool using real Spring context to test
 * BrowserUseTool functionality
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("For local testing only, skip in CI environment") // Add this line
class BrowserUseToolSpringTest {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseToolSpringTest.class);

	@Autowired
	private ChromeDriverService chromeDriverService;

	@Autowired
	private SmartContentSavingService innerStorageService;

	private BrowserUseTool browserUseTool;

	@Autowired
	private LlmService llmService;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private ManusProperties manusProperties;

	@Autowired
	private PromptService promptService;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {

		manusProperties.setBrowserHeadless(false);
		manusProperties.setDebugDetail(true);
		chromeDriverService.setManusProperties(manusProperties);
		browserUseTool = new BrowserUseTool(chromeDriverService, innerStorageService);
		DummyBaseAgent agent = new DummyBaseAgent(llmService, planExecutionRecorder, manusProperties, promptService);
		agent.setCurrentPlanId("plan_123123124124124");
		browserUseTool.setCurrentPlanId(agent.getCurrentPlanId());

	}

	private static class DummyBaseAgent extends BaseAgent {

		public DummyBaseAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
				ManusProperties manusProperties, PromptService promptService) {
			super(llmService, planExecutionRecorder, manusProperties, new HashMap<>(), promptService);

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
		public ToolCallBackContext getToolCallBackContext(String toolKey) {
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
	@DisplayName("Test browser search for 'Hello World'")
	void testHelloWorldSearch() {
		try {
			// Step 1: Navigate to Baidu
			log.info("Step 1: Navigate to Baidu");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertEquals("successfully navigated to https://www.baidu.com", navigateResult.getOutput(),
					"Failed to navigate to Baidu");
			Page page = browserUseTool.getDriver().getCurrentPage();

			// Step 2: Get and verify interactive elements
			log.info("Step 2: Get interactive elements and analyze");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to get interactive elements");
			log.info("Retrieved interactive elements: {}", elements);

			// Step 3: Find the search box
			log.info("Step 3: Locate search box");
			int searchInputIndex = -1;
			String[] elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("name=\"wd\"") || elementLines[i].contains("id=\"kw\"")) { // Baidu
																										// search
																										// box
																										// characteristics
					searchInputIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchInputIndex, "Search box not found");
			log.info("Found search box index: {}", searchInputIndex);

			// Step 4: Input text in the search box
			log.info("Step 4: Input 'Hello World' in the search box");
			ToolExecuteResult inputResult = executeAction("input_text", null, searchInputIndex, "Hello World");
			Assertions.assertTrue(inputResult.getOutput().contains("Hello World"),
					"Failed to input text in search box");

			// Step 5: Re-get state and find search button
			log.info("Step 5: Locate search button");

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
			Assertions.assertNotEquals(-1, searchButtonIndex, "Search button not found");
			log.info("Found search button index: {}", searchButtonIndex);

			// Step 6: Click search button
			log.info("Step 6: Click search button");
			ToolExecuteResult clickResult = executeAction("click", null, searchButtonIndex, null);
			Assertions.assertTrue(clickResult.getOutput().contains("clicked"),
					"Failed to click search button: " + clickResult.getOutput());

			// Step 7: Wait and verify search results
			log.info("Step 7: Wait for page load and get search results");
			Thread.sleep(2000); // Wait for page load
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"),
					"Did not find 'Hello World' in search results");

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
			Assertions.assertTrue(clickResult.getOutput().contains("clicked"),
					"Failed to click search button: " + clickResult.getOutput());

			log.info("Login success");

		}
		catch (Exception e) {
			log.error("Error occurred during test", e);
			Assertions.fail("Test execution failed: " + e.getMessage());
		}
	}

	@Test
	@Order(2)
	@DisplayName("Test element positioning by name and click Baidu search button")
	void testGetElementPositionAndMoveToClick() {
		try {
			// Step 1: Navigate to Baidu
			log.info("Step 1: Navigate to Baidu");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertEquals("successfully navigated to https://www.baidu.com", navigateResult.getOutput(),
					"Failed to navigate to Baidu");
			Page page = browserUseTool.getDriver().getCurrentPage();

			// Step 2: Get and verify interactive elements
			log.info("Step 2: Get interactive elements and analyze");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to get interactive elements");
			log.info("Retrieved interactive elements: {}", elements);

			// Step 3: Find the search box
			log.info("Step 3: Locate search box");
			int searchInputIndex = -1;
			String[] elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("name=\"wd\"") || elementLines[i].contains("id=\"kw\"")) { // Baidu
																										// search
																										// box
																										// characteristics
					searchInputIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchInputIndex, "Search box not found");
			log.info("Found search box index: {}", searchInputIndex);

			// Step 4: Input text in search box
			log.info("Step 4: Input 'Hello World' in search box");
			ToolExecuteResult inputResult = executeAction("input_text", null, searchInputIndex, "Hello World");
			Assertions.assertTrue(inputResult.getOutput().contains("Hello World"),
					"Failed to input text in search box");

			// Step 5: Use GetElementPositionByNameAction to find search button
			log.info("Step 5: Use GetElementPositionByNameAction to find '百度一下' button");
			BrowserRequestVO positionRequest = new BrowserRequestVO();
			positionRequest.setElementName("百度一下");
			GetElementPositionByNameAction positionAction = new GetElementPositionByNameAction(browserUseTool);
			ToolExecuteResult positionResult = positionAction.execute(positionRequest);
			log.info("Retrieved '百度一下' button position info: {}", positionResult.getOutput());

			// Parse JSON result to get coordinates
			List<?> positionsList = objectMapper.readValue(positionResult.getOutput(), new TypeReference<List<?>>() {
			});
			Assertions.assertFalse(positionsList.isEmpty(), "'百度一下' button not found");
			Map<?, ?> elementPosition = (Map<?, ?>) positionsList.get(0);
			Double xNumber = (Double) elementPosition.get("x");
			Double yNumber = (Double) elementPosition.get("y");
			Double x = xNumber.doubleValue();
			Double y = yNumber.doubleValue();
			log.info("'百度一下' button coordinates: x={}, y={}", x, y);

			// Step 6: Use MoveToAndClickAction to click search button
			log.info("Step 6: Use MoveToAndClickAction to click '百度一下' button");
			BrowserRequestVO clickRequest = new BrowserRequestVO();
			clickRequest.setPositionX(x);
			clickRequest.setPositionY(y);
			MoveToAndClickAction clickAction = new MoveToAndClickAction(browserUseTool);
			ToolExecuteResult clickResult = clickAction.execute(clickRequest);
			log.info("Click result: {}", clickResult.getOutput());
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "Failed to click '百度一下' button");

			// Step 7: Wait and verify search results
			log.info("Step 7: Wait for page load and get search results");
			Thread.sleep(2000); // Wait for page load
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"),
					"Did not find 'Hello World' in search results");

			// Step 8: Use GetElementPositionByNameAction to find and click Baidu Baike
			// link
			log.info("Step 8: Use GetElementPositionByNameAction to find '百度百科' link");
			BrowserRequestVO baikePositionRequest = new BrowserRequestVO();
			baikePositionRequest.setElementName("百度百科");
			GetElementPositionByNameAction baikePositionAction = new GetElementPositionByNameAction(browserUseTool);
			ToolExecuteResult baikePositionResult = baikePositionAction.execute(baikePositionRequest);
			log.info("Retrieved '百度百科' link position info: {}", baikePositionResult.getOutput());

			// Parse JSON result to get Baidu Baike link coordinates
			List<?> baikePositionsList = objectMapper.readValue(baikePositionResult.getOutput(),
					new TypeReference<List<?>>() {
					});

			if (!baikePositionsList.isEmpty()) {
				// Find Baidu Baike link containing "hello world" related content
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
					log.info("'百度百科' link coordinates: x={}, y={}", baikeX, baikeY);

					// Use MoveToAndClickAction to click Baidu Baike link
					log.info("Use MoveToAndClickAction to click '百度百科' link");
					BrowserRequestVO baikeClickRequest = new BrowserRequestVO();
					baikeClickRequest.setPositionX(baikeX);
					baikeClickRequest.setPositionY(baikeY);
					MoveToAndClickAction baikeClickAction = new MoveToAndClickAction(browserUseTool);
					ToolExecuteResult baikeClickResult = baikeClickAction.execute(baikeClickRequest);
					log.info("Baidu Baike link click result: {}", baikeClickResult.getOutput());
					Assertions.assertTrue(baikeClickResult.getOutput().contains("Clicked"),
							"Failed to click '百度百科' link");
					log.info("Successfully clicked Baidu Baike link");
				}
				else {
					log.warn("Did not find Baidu Baike link containing 'hello world'");
				}
			}
			else {
				log.warn("Did not find '百度百科' link");
			}

			log.info("Test completed successfully!");

		}
		catch (Exception e) {
			log.error("Error occurred during test", e);
			Assertions.fail("Test execution failed: " + e.getMessage());
		}
	}

	/**
	 * Generic method to navigate to specified URL and verify interactive elements
	 * @param tool BrowserUseTool instance
	 * @param url Target URL
	 * @param expectedElements List of expected element keywords to appear on the page
	 * @return Retrieved interactive elements string
	 */
	private String navigateAndVerifyElements(BrowserUseTool tool, String url, List<String> expectedElements) {
		// Step 1: Navigate to specified URL
		log.info("Step 1: Navigate to {}", url);
		ToolExecuteResult navigateResult = executeAction("navigate", url);
		Assertions.assertEquals("successfully navigated to " + url, navigateResult.getOutput(), "Navigation failed");

		Page page = browserUseTool.getDriver().getCurrentPage();
		// Step 2: Get and verify interactive elements
		log.info("Step 2: Get interactive elements");
		Map<String, Object> state = tool.getCurrentState(page);
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "Failed to get interactive elements");
		log.info("Retrieved interactive elements: {}", elements);

		// Step 3: Verify expected elements
		log.info("Step 3: Verify expected elements");
		String[] elementLines = elements.split("\n");
		for (String expectedElement : expectedElements) {
			boolean found = false;
			for (String line : elementLines) {
				if (line.contains(expectedElement)) {
					found = true;
					break;
				}
			}
			log.info("Expected element '{}' found: {}", expectedElement, found);
		}
		return elements;
	}

	@Test
	@Order(4)
	@DisplayName("Test navigate to specified URL and get interactive elements")
	void testNavigateAndGetElements() {
		// Test navigating to different pages and verify interactive elements
		List<String> baiduExpected = Arrays.asList("Baidu Search", "input");
		String baiduElements = navigateAndVerifyElements(browserUseTool, "https://www.baidu.com", baiduExpected);
		Assertions.assertNotNull(baiduElements, "Failed to get Baidu interactive elements");

		List<String> githubExpected = Arrays.asList("search", "Sign");
		String githubElements = navigateAndVerifyElements(browserUseTool, "https://github.com", githubExpected);
		Assertions.assertNotNull(githubElements, "Failed to get GitHub interactive elements");

		log.info("All navigation tests passed!");
	}

	@Test
	@Order(5)
	@DisplayName("Test GitHub search page elements")
	void testGitHubSearch() {
		// Test simple GitHub page loading
		ToolExecuteResult navigateResult = executeAction("navigate", "https://github.com");
		Assertions.assertEquals("successfully navigated to https://github.com", navigateResult.getOutput(),
				"Failed to navigate to GitHub");

		Page page = browserUseTool.getDriver().getCurrentPage();
		Map<String, Object> state = browserUseTool.getCurrentState(page);
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "Failed to get GitHub interactive elements");
		log.info("GitHub interactive elements: {}", elements);
	}

	@Test
	@Order(6)
	@DisplayName("Test Nacos page elements")
	void testNacosPageLink() {
		// Test simple Nacos page loading
		ToolExecuteResult navigateResult = executeAction("navigate", "https://nacos.io");
		Assertions.assertEquals("successfully navigated to https://nacos.io", navigateResult.getOutput(),
				"Failed to navigate to Nacos");

		Page page = browserUseTool.getDriver().getCurrentPage();
		Map<String, Object> state = browserUseTool.getCurrentState(page);
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "Failed to get Nacos interactive elements");
		log.info("Nacos interactive elements: {}", elements);
	}

	@Test
	@Order(6)
	@DisplayName("Test Baidu homepage elements")
	void testBaiduElements() {
		// Test simple Baidu page loading
		ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
		Assertions.assertEquals("successfully navigated to https://www.baidu.com", navigateResult.getOutput(),
				"Failed to navigate to Baidu");

		Page page = browserUseTool.getDriver().getCurrentPage();
		Map<String, Object> state = browserUseTool.getCurrentState(page);
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "Failed to get Baidu interactive elements");
		log.info("Baidu interactive elements: {}", elements);
	}

	@Test
	@Order(7)
	@DisplayName("Test CSDN website login functionality")
	void testCsdnLogin() {
		try {
			// Step 1: Navigate to CSDN
			log.info("Step 1: Navigate to CSDN");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://passport.csdn.net/login");
			Assertions.assertEquals("successfully navigated to https://passport.csdn.net/login",
					navigateResult.getOutput(), "Failed to navigate to CSDN login");
			Page page = browserUseTool.getDriver().getCurrentPage();

			// Step 2: Get and verify interactive elements
			log.info("Step 2: Get interactive elements and analyze");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to get interactive elements");
			log.info("Retrieved interactive elements: {}", elements);

			// Step 3: Find username input field (not currently used but defined for
			// potential future use)
			log.info("Step 3: Locate username input box");
			@SuppressWarnings("unused")
			int usernameInputIndex = -1;
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
			log.info("Updated elements: {}",
					updatedElements != null ? updatedElements.length() + " characters" : "null");

			log.info("CSDN登录测试完成");
		}
		catch (Exception e) {
			log.error("Error occurred during CSDN login test", e);
			Assertions.fail("CSDN login test failed: " + e.getMessage());
		}
	}

	// Helper methods: Execute browser actions
	private ToolExecuteResult executeAction(String action, String url) {
		return executeAction(action, url, null, null);
	}

	// Helper methods: Execute browser actions (with index and text)
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
