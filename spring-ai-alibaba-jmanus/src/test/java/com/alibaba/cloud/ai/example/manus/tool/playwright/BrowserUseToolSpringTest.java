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
package com.alibaba.cloud.ai.example.manus.tool.playwright;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.BrowserRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetElementPositionByNameAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.MoveToAndClickAction;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Spring integration test class for BrowserUseTool utilizes a real Spring context to
 * test the functionality of BrowserUseTool
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("For local testing only, skipped in CI environment")
class BrowserUseToolSpringTest {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseToolSpringTest.class);

	@Autowired
	private ChromeDriverService chromeDriverService;

	private BrowserUseTool browserUseTool;

	@Autowired
	private ManusProperties manusProperties;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private SmartContentSavingService innerStorageService;

	@BeforeEach
	void setUp() {
		manusProperties.setBrowserHeadless(false);
		manusProperties.setDebugDetail(true);
		chromeDriverService.setManusProperties(manusProperties);
		browserUseTool = new BrowserUseTool(chromeDriverService, innerStorageService, objectMapper);
		browserUseTool.setCurrentPlanId("test");
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

			// Step 2: Obtain and validate interactive elements
			log.info("Step 2: Obtain interactive elements and analyze them");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to obtain interactive elements");
			log.info("Obtained interactive elements: {}", elements);

			// Step 3: Locate the search box
			log.info("Step 3: Locate the search box");
			int searchInputIndex = -1;
			String[] elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("id=\"chat-textarea\"")) { // Features of
																		// Baidu search
																		// box
					searchInputIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchInputIndex, "Search box not found");
			log.info("Find the search box index: {}", searchInputIndex);

			// Step 4: Enter text in the search box
			log.info("Step 4: Enter 'Hello World' in the search box");
			ToolExecuteResult inputResult = executeAction("input_text", null, searchInputIndex, "java");
			// Assertions.assertTrue(inputResult.getOutput().contains("Hello World"),
			// "Failed to enter text into the search box");

			// Step 5: Reacquire the state and locate the search button
			log.info("Step 5: Locate the search button");

			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			int searchButtonIndex = -1;
			elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("button: 百度一下")) {
					searchButtonIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchButtonIndex, "Search button not found");
			log.info("Find the index of the search button: {}", searchButtonIndex);

			// Step 6: Click the search button
			log.info("Step 6: Click the search button");
			ToolExecuteResult clickResult = executeAction("click", null, searchButtonIndex, null);
			Assertions.assertTrue(clickResult.getOutput().contains("Successfully clicked"),
					"Clicking the search button failed : " + clickResult.getOutput());

			// Step 7: Wait for the page to load and obtain the search results
			log.info("Step 7: Wait for the page to load and obtain the search results");
			Thread.sleep(2000); // Waiting for page to load
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			// Assertions.assertTrue(searchResults.contains("Hello World"), "'Hello World'
			// not found in search results");

			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			searchButtonIndex = -1;
			elementLines = elements.split("\n");

			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("Java") && elementLines[i].contains("百度百科")) {
					searchButtonIndex = i;
					break;
				}
			}
			clickResult = executeAction("click", null, searchButtonIndex, null);
			Assertions.assertTrue(clickResult.getOutput().contains("Successfully clicked"),
					"Clicking the search button failed : " + clickResult.getOutput());

			log.info("login success ");

		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	@Test
	@Order(2)
	@DisplayName("Test by locating and clicking the Baidu search button through element name")
	void testGetElementPositionAndMoveToClick() {
		try {
			// Step 1: Navigate to Baidu
			log.info("Step 1: Navigate to Baidu");
			ToolExecuteResult navigateResult = executeAction("navigate", "https://www.baidu.com");
			Assertions.assertEquals("successfully navigated to https://www.baidu.com", navigateResult.getOutput(),
					"Navigating to Baidu failed");
			Page page = browserUseTool.getDriver().getCurrentPage();

			// Step 2: Obtain interactive elements and analyze them
			log.info("Step 2: Obtain interactive elements and analyze them");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to obtain interactive elements");
			log.info("The obtained interactive elements: {}", elements);

			// Step 3: Locate the search box
			log.info("Step 3: Locate the search box");
			int searchInputIndex = -1;
			String[] elementLines = elements.split("\n");
			for (int i = 0; i < elementLines.length; i++) {
				if (elementLines[i].contains("id=\"chat-textarea\"")) { // Features of
																		// Baidu search
																		// box
					searchInputIndex = i;
					break;
				}
			}
			Assertions.assertNotEquals(-1, searchInputIndex, "Search box not found");
			log.info("Find the search box index: {}", searchInputIndex);

			// Step 4: Enter text into the search box
			log.info("Step 4: Enter 'Hello World' in the search box");
			ToolExecuteResult inputResult = executeAction("input_text", null, searchInputIndex, "Hello World");
			Assertions.assertTrue(inputResult.getOutput().contains("Hello World"),
					"Failed to enter text into the search box");

			// Step 5: Use GetElementPositionByNameAction to find the search button
			log.info("Step 5: Use GetElementPositionByNameAction to find the '百度一下' button");
			BrowserRequestVO positionRequest = new BrowserRequestVO();
			positionRequest.setElementName("百度一下");
			GetElementPositionByNameAction positionAction = new GetElementPositionByNameAction(browserUseTool,
					objectMapper);
			ToolExecuteResult positionResult = positionAction.execute(positionRequest);
			log.info("Obtain the position information of the '百度一下' button: {}", positionResult.getOutput());

			// 解析JSON结果获取坐标
			List<?> positionsList = objectMapper.readValue(positionResult.getOutput(), new TypeReference<List<?>>() {
			});
			Assertions.assertFalse(positionsList.isEmpty(), "'百度一下' button not found");
			Map<?, ?> elementPosition = (Map<?, ?>) positionsList.get(0);
			Double xNumber = (Double) elementPosition.get("x");
			Double yNumber = (Double) elementPosition.get("y");
			Double x = xNumber.doubleValue();
			Double y = yNumber.doubleValue();
			log.info("'百度一下' button coordinates: x={}, y={}", x, y);

			// Step 6: Use the MoveToAndClickAction to click the search button
			log.info("Step 6: Use MoveToAndClickAction to click the '百度一下' button");
			BrowserRequestVO clickRequest = new BrowserRequestVO();
			clickRequest.setPositionX(x);
			clickRequest.setPositionY(y);
			MoveToAndClickAction clickAction = new MoveToAndClickAction(browserUseTool);
			ToolExecuteResult clickResult = clickAction.execute(clickRequest);
			log.info("result: {}", clickResult.getOutput());
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"), "click '百度一下' eoor");

			// Step 7: Wait and verify search results
			log.info("Step 7: Wait and verify search results");
			Thread.sleep(2000); // Waiting for the page to load
			ToolExecuteResult textResult = executeAction("get_text", null);
			String searchResults = textResult.getOutput();
			Assertions.assertTrue(searchResults.contains("Hello World"), "'Hello World' not found in search results");

			// Step 8: Find and click on the Baidu Baike link using the FHIR
			// lementPositionByNameAction
			log.info("Step 8: Find and click on the 百度百科 link using the FHIR lementPositionByNameAction");
			BrowserRequestVO baikePositionRequest = new BrowserRequestVO();
			baikePositionRequest.setElementName("百度百科");
			GetElementPositionByNameAction baikePositionAction = new GetElementPositionByNameAction(browserUseTool,
					objectMapper);
			ToolExecuteResult baikePositionResult = baikePositionAction.execute(baikePositionRequest);
			log.info("get '百度百科' link location information: {}", baikePositionResult.getOutput());

			// Parse JSON results to obtain Baidu Baike link coordinates
			List<?> baikePositionsList = objectMapper.readValue(baikePositionResult.getOutput(),
					new TypeReference<List<?>>() {
					});

			if (!baikePositionsList.isEmpty()) {
				// Search for Baidu Baike links containing content related to "hello
				// world"
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

					// Click on the Baidu Baike link using MoveToAndClickAction
					log.info("Click on the 百度百科 link using MoveToAndClickAction");
					BrowserRequestVO baikeClickRequest = new BrowserRequestVO();
					baikeClickRequest.setPositionX(baikeX);
					baikeClickRequest.setPositionY(baikeY);
					MoveToAndClickAction baikeClickAction = new MoveToAndClickAction(browserUseTool);
					ToolExecuteResult baikeClickResult = baikeClickAction.execute(baikeClickRequest);
					log.info("Baidu Baike link click results: {}", baikeClickResult.getOutput());
					Assertions.assertTrue(baikeClickResult.getOutput().contains("Clicked"),
							"Clicking on the 百度百科 link failed");
					log.info("Successfully clicked on the 百度百科 link");
				}
				else {
					log.warn("No 百度百科 link containing 'hello world' found");
				}
			}
			else {
				log.warn("No 百度百科 link found");
			}

			log.info("test success!");

		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	/**
	 * A general method for navigating to a specified URL and validating interactive
	 * elements
	 * @param tool BrowserUseTool instance
	 * @param url Target URL
	 * @param expectedElements List of elements and keywords expected to appear on the
	 * page
	 * @return Obtained interactive element string
	 */
	private String navigateAndVerifyElements(BrowserUseTool tool, String url, List<String> expectedElements) {
		// Step 1: Navigate to the specified URL
		log.info("Step 1: Navigate to the {}", url);
		ToolExecuteResult navigateResult = executeAction("navigate", url);
		Assertions.assertEquals("successfully navigated to " + url, navigateResult.getOutput(), "Navigation failed");

		Page page = browserUseTool.getDriver().getCurrentPage();
		// Step 2: Obtain and validate interactive elements
		log.info("Step 2: Obtain and validate interactive elements");
		Map<String, Object> state = tool.getCurrentState(page);
		String elements = (String) state.get("interactive_elements");
		Assertions.assertNotNull(elements, "Failed to obtain interactive elements");
		log.info("Obtained interactive elements: {}", elements);

		// Step 3: Verify the expected elements
		log.info("Step 3: Verify the expected elements");
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
				log.info("Find elements that match all features: {}", elementLine);
				break;
			}
		}

		Assertions.assertTrue(foundMatchingElement, String.format(
				"No elements were found that contain all expected features simultaneously. Expected features: %s",
				expectedElements));

		return elements;
	}

	@Test
	@Order(4)
	@DisplayName("Test navigating to the specified URL and obtaining interactive elements")
	void testNavigateAndGetElements() {
		try {
			String testUrl = "https://www.bing.com";
			List<String> expectedElements = Arrays.asList("search", "input");

			// Use universal methods for testing
			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);

			// Get screenshot (optional)
			log.info("Obtain a screenshot of the page as evidence");
			ToolExecuteResult screenshotResult = executeAction("screenshot", null);
			Assertions.assertTrue(screenshotResult.getOutput().contains("Screenshot captured"),
					"Failed to obtain screenshot");

			log.info("Test successfully completed!");

		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	@Test
	@Order(5)
	@DisplayName("Test GitHub search page elements")
	void testGitHubSearch() {
		try {
			String testUrl = "https://github.com/";
			List<String> expectedElements = Arrays.asList( // The name attribute of GitHub
															// search box
					"search" // Search for related elements

			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("GitHub search page test successfully completed!");
		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("Test Nacos page elements")
	void testNacosPageLink() {
		try {
			String testUrl = "https://nacos.io/docs/latest/overview";
			List<String> expectedElements = Arrays.asList( // Characteristics of Nacos
															// pages
					"Java SDK 容灾" // Search for related elements
			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("Nacos page testing successfully completed!");
		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("Test Baidu homepage elements")
	void testBaiduElements() {
		try {
			String testUrl = "https://www.baidu.com";
			List<String> expectedElements = Arrays.asList("id=\"chat-textarea\"", // The
																					// characteristics
																					// of
																					// Baidu
																					// search
																					// box
					"textarea" // Another feature of the search box
			);

			navigateAndVerifyElements(browserUseTool, testUrl, expectedElements);
			log.info("Baidu homepage test successfully completed!");
		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	@Test
	@Order(7)
	@DisplayName("Test CSDN website login function")
	void testCsdnLogin() {
		try {
			// Step 1: Navigate to the CSDN website
			String testUrl = "https://www.csdn.net/";
			log.info("Step 1: Navigate to the CSDN website");
			ToolExecuteResult navigateResult = executeAction("navigate", testUrl);
			Assertions.assertEquals("successfully navigated to " + testUrl, navigateResult.getOutput(), "导航到CSDN网站失败");

			Page page = browserUseTool.getDriver().getCurrentPage();
			// Step 2: Retrieve and search for login elements
			log.info("Step 2: Retrieve and search for login elements");
			Map<String, Object> state = browserUseTool.getCurrentState(page);
			String elements = (String) state.get("interactive_elements");
			Assertions.assertNotNull(elements, "Failed to obtain interactive elements");
			log.info("Obtained interactive elements: {}", elements);

			// Step 2: Search for the index of the "Login" button
			String[] elementLines = elements.split("\n");
			int loginButtonIndex = -1;

			for (String line : elementLines) {
				// Extract the actual index number of the element from the beginning of
				// each line, in the format of [195] <input
				if (line.matches("\\[\\d+\\].*")) {
					int indexEndPos = line.indexOf("]");
					String indexStr = line.substring(1, indexEndPos);
					int elementIndex = Integer.parseInt(indexStr);

					if (line.contains("登录")) {
						loginButtonIndex = elementIndex;
						log.info("Find login button, index: {}", loginButtonIndex);
						break;
					}
				}
			}

			Assertions.assertNotEquals(-1, loginButtonIndex, "Login button not found");

			// Step 3: Click on the "Login" button
			log.info("Step 3: Click on the Login button");
			ToolExecuteResult clickLoginResult = executeAction("click", null, loginButtonIndex, null);
			Assertions.assertTrue(clickLoginResult.getOutput().contains("Successfully clicked"),
					"Click login button failed");

			// Waiting for the login dialog box to load
			log.info("Waiting for the login dialog box to load...");
			Thread.sleep(2000);

			// Obtain updated interaction elements
			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n");
			// Step 4: Find the 'APP login' element using the 'FHIR
			// lementPositionByNameAction' and click on it through the coordinates
			log.info(
					"Step 4: Find the 'APP login' element using the 'FHIR lementPositionByNameAction' and click on it through the coordinates");

			// Create request object and set element name
			BrowserRequestVO positionRequest = new BrowserRequestVO();
			positionRequest.setElementName("验证码登录");

			// Execute the Get ElementPositionByNameAction to retrieve the position of the
			// element
			GetElementPositionByNameAction positionAction = new GetElementPositionByNameAction(browserUseTool,
					objectMapper);
			ToolExecuteResult positionResult = positionAction.execute(positionRequest);
			log.info("Obtain the location information of the '验证码登录' element: {}", positionResult.getOutput());

			// Parse JSON results to obtain coordinates
			List<?> positionsList = objectMapper.readValue(positionResult.getOutput(), new TypeReference<List<?>>() {
			});
			Assertions.assertFalse(positionsList.isEmpty(), "The 'APP 登录' element was not found");

			// Get the position information of the first matching element
			Map<?, ?> elementPosition = (Map<?, ?>) positionsList.get(0);
			Double xNumber = (Double) elementPosition.get("x");
			Double yNumber = (Double) elementPosition.get("y");
			log.info("Verification code login element coordinates: x={}, y={}", xNumber, yNumber);

			// Use MoveToAndClickAction to click on elements through coordinates
			BrowserRequestVO clickRequest = new BrowserRequestVO();
			clickRequest.setPositionX(xNumber);
			clickRequest.setPositionY(yNumber);

			MoveToAndClickAction clickAction = new MoveToAndClickAction(browserUseTool);
			ToolExecuteResult clickResult = clickAction.execute(clickRequest);
			log.info("result: {}", clickResult.getOutput());
			Assertions.assertTrue(clickResult.getOutput().contains("Clicked"),
					"Clicking on the verification code login element failed");

			// Waiting for password login form to load
			log.info("Waiting for password login form to load...");
			Thread.sleep(1000);

			// Obtain updated interaction elements
			state = browserUseTool.getCurrentState(page);
			elements = (String) state.get("interactive_elements");
			elementLines = elements.split("\n");

			// Step 5: Search for the phone number input box
			log.info("Step 5: Search for the phone number input box");
			int phoneInputIndex = -1;
			int verifyCodeButtonIndex = -1;

			for (String line : elementLines) {
				// Extract the actual index number of the element from the beginning of
				// each line, in the format of [195] <input...
				if (line.matches("\\[\\d+\\].*")) {
					int indexEndPos = line.indexOf("]");
					String indexStr = line.substring(1, indexEndPos);
					int elementIndex = Integer.parseInt(indexStr);

					// Search for the phone number input box (which may contain phone,
					// tel, or mobile related inputs)
					if (line.contains("input") && line.contains("手机号")) {
						phoneInputIndex = elementIndex;
						log.info("Find the phone number input box, index: {}", phoneInputIndex);
					}
					// 查找获取验证码按钮
					else if ((line.contains("button") || line.contains("a ")) && line.contains("获取验证码")) {
						verifyCodeButtonIndex = elementIndex;
						log.info("Find the button to obtain the verification code, index: {}", verifyCodeButtonIndex);
					}
				}
			}

			Assertions.assertNotEquals(-1, phoneInputIndex, "Phone number input box not found");

			// Step 6: Enter "123456789" in the phone number input box
			log.info("Step 6: Enter \"123456789\" in the phone number input box");
			ToolExecuteResult phoneInputResult = executeAction("input_text", null, phoneInputIndex, "123456789");
			Assertions.assertTrue(phoneInputResult.getOutput().contains("Successfully input"),
					"Failed to input text in the phone number input box");

			// Step 7: Verify if the phone number input is successful
			log.info("Step 7: Verify if the phone number input is successful");
			browserUseTool.getDriver().getInteractiveElementRegistry().refresh(page);
			state = browserUseTool.getCurrentState(page);
			String updatedElements = (String) state.get("interactive_elements");

			log.info("CSDN login test completed");
		}
		catch (Exception e) {
			log.error("test error", e);
			Assertions.fail("test error: " + e.getMessage());
		}
	}

	// Auxiliary method: Perform browser operations
	private ToolExecuteResult executeAction(String action, String url) {
		return executeAction(action, url, null, null);
	}

	// Auxiliary method: Perform browser operations (with index and text)
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
