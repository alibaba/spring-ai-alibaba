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
package com.alibaba.cloud.ai.example.manus.tool.browser;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class BrowserUseTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private final ChromeDriverService chromeDriverService;

	// 添加标签页缓存字段
	private List<Map<String, Object>> cachedTabs;

	private BaseAgent agent;

	public BrowserUseTool(ChromeDriverService chromeDriverService) {
		this.chromeDriverService = chromeDriverService;
	}

	private WebDriver getDriver() {
		return chromeDriverService.getDriver(agent.getPlanId());
	}

	private final int MAX_LENGTH = 20000;

	private final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "enum": [
			                "navigate",
			                "click",
			                "input_text",
			                "key_enter",
			                "screenshot",
			                "get_html",
			                "get_text",
			                "execute_js",
			                "scroll",
			                "switch_tab",
			                "new_tab",
			                "close_tab",
			                "refresh"
			            ],
			            "description": "The browser action to perform"
			        },
			        "url": {
			            "type": "string",
			            "description": "URL for 'navigate' or 'new_tab' actions"
			        },
			        "index": {
			            "type": "integer",
			            "description": "Element index for 'click' or 'input_text' actions"
			        },
			        "text": {
			            "type": "string",
			            "description": "Text for 'input_text' action"
			        },
			        "script": {
			            "type": "string",
			            "description": "JavaScript code for 'execute_js' action"
			        },
			        "scroll_amount": {
			            "type": "integer",
			            "description": "Pixels to scroll (positive for down, negative for up) for 'scroll' action"
			        },
			        "tab_id": {
			            "type": "integer",
			            "description": "Tab ID for 'switch_tab' action"
			        }
			    },
			    "required": [
			        "action"
			    ],
			    "dependencies": {
			        "navigate": [
			            "url"
			        ],
			        "click": [
			            "index"
			        ],
			        "input_text": [
			            "index",
			            "text"
			        ],
			        "key_enter": [
			            "index"
			        ],
			        "execute_js": [
			            "script"
			        ],
			        "switch_tab": [
			            "tab_id"
			        ],
			        "new_tab": [
			            "url"
			        ],
			        "scroll": [
			            "scroll_amount"
			        ]
			    }
			}
			""";

	private final String name = "browser_use";

	private final String description = """
			Interact with a web browser to perform various actions such as navigation, element interaction,搜索类优先考虑此工具
			content extraction, and tab management. Supported actions include:
			- 'navigate': Go to a specific URL, use https://baidu.com by default
			- 'click': Click an element by index
			- 'input_text': Input text into an element, for 百度(Baidu), the index of the input button is
			- 'key_enter': Hit the Enter key
			- 'screenshot': Capture a screenshot
			- 'get_html': Get page HTML content
			- 'get_text': Get text content of the page
			- 'execute_js': Execute JavaScript code
			- 'scroll': Scroll the page
			- 'switch_tab': Switch to a specific tab
			- 'new_tab': Open a new tab
			- 'close_tab': Close the current tab
			- 'refresh': Refresh the current page
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static synchronized BrowserUseTool getInstance(ChromeDriverService chromeDriverService) {
		BrowserUseTool instance = new BrowserUseTool(chromeDriverService);
		return instance;
	}

	@SuppressWarnings("rawtypes")
	public FunctionToolCallback getFunctionToolCallback(ChromeDriverService chromeDriverService) {
		return FunctionToolCallback.builder(name, getInstance(chromeDriverService))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	private void simulateHumanBehavior(WebElement element) {
		try {

			// 添加随机延迟
			Thread.sleep(new Random().nextInt(500) + 200);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void typeWithHumanDelay(WebElement element, String text) {
		simulateHumanBehavior(element);

		// 模拟人类输入速度
		Random random = new Random();
		for (char c : text.toCharArray()) {
			element.sendKeys(String.valueOf(c));
			try {
				Thread.sleep(random.nextInt(100) + 50);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("BrowserUseTool toolInput:" + toolInput);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});

		String action = null;
		if (toolInputMap.get("action") != null) {
			action = (String) toolInputMap.get("action");
		}
		String url = null;
		if (toolInputMap.get("url") != null) {
			url = (String) toolInputMap.get("url");
		}
		Integer index = null;
		if (toolInputMap.get("index") != null) {
			index = (Integer) toolInputMap.get("index");
		}
		String text = null;
		if (toolInputMap.get("text") != null) {
			text = (String) toolInputMap.get("text");
		}
		String script = null;
		if (toolInputMap.get("script") != null) {
			script = (String) toolInputMap.get("script");
		}
		Integer scrollAmount = null;
		if (toolInputMap.get("scroll_amount") != null) {
			scrollAmount = (Integer) toolInputMap.get("scroll_amount");
		}
		Integer tabId = null;
		if (toolInputMap.get("tab_id") != null) {
			tabId = (Integer) toolInputMap.get("tab_id");
		}
		try {
			if (action == null) {
				return new ToolExecuteResult("Action parameter is required");
			}
			WebDriver driver = getDriver();
			List<WebElement> interactiveElements = getInteractiveElements(driver);

			switch (action) {
				case "navigate":
					if (url == null) {
						return new ToolExecuteResult("URL is required for 'navigate' action");
					}
					driver.get(url);
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Navigated to " + url);

				case "click":
					if (index == null) {
						return new ToolExecuteResult("Index is required for 'click' action");
					}
					if (index < 0 || index >= interactiveElements.size()) {
						return new ToolExecuteResult("Element with index " + index + " not found");
					}

					WebElement element = interactiveElements.get(index);
					log.info("Clicking element: {}", (element.getText() != null ? element.getText() : "No text"));

					// 记录点击前的窗口状态
					Set<String> beforeWindowHandles = driver.getWindowHandles();
					String currentUrl = driver.getCurrentUrl();

					// 执行点击操作
					simulateHumanBehavior(element);
					try {
						element.click();
					}
					catch (ElementClickInterceptedException e) {
						// 如果普通点击失败，尝试使用 JavaScript 点击
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("arguments[0].click();", element);
					}

					// 等待页面变化（最多等待10秒）
					WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
					try {
						// 检查是否有新窗口打开
						Set<String> afterWindowHandles = driver.getWindowHandles();
						if (afterWindowHandles.size() > beforeWindowHandles.size()) {
							// 找出新打开的窗口
							afterWindowHandles.removeAll(beforeWindowHandles);
							String newHandle = afterWindowHandles.iterator().next();

							// 切换到新窗口
							driver.switchTo().window(newHandle);
							log.info("New tab detected, switched to: {}", driver.getCurrentUrl());
							refreshTabsInfo(driver); // 刷新标签页信息
							return new ToolExecuteResult(
									"Clicked element and opened in new tab: " + driver.getCurrentUrl());
						}

						// 检查URL是否发生变化
						boolean urlChanged = wait.until(d -> !d.getCurrentUrl().equals(currentUrl));
						if (urlChanged) {
							log.info("Page navigated to: {}", driver.getCurrentUrl());
							refreshTabsInfo(driver); // 刷新标签页信息
							return new ToolExecuteResult("Clicked element and navigated to: " + driver.getCurrentUrl());
						}
						refreshTabsInfo(driver); // 刷新标签页信息
						// 如果没有明显变化，返回普通点击成功消息
						return new ToolExecuteResult("Clicked element at index " + index);

					}
					catch (TimeoutException e) {
						// 如果超时，检查是否仍在原页面
						if (!driver.getCurrentUrl().equals(currentUrl)) {
							return new ToolExecuteResult("Clicked and page changed to: " + driver.getCurrentUrl());
						}
						return new ToolExecuteResult(
								"Clicked element at index " + index + " (no visible navigation occurred)");
					}

				case "input_text":
					if (index == null || text == null) {
						return new ToolExecuteResult("Index and text are required for 'input_text' action");
					}
					if (index < 0 || index >= interactiveElements.size()) {
						return new ToolExecuteResult("Element with index " + index + " not found");
					}
					WebElement inputElement = interactiveElements.get(index);
					if (!inputElement.getTagName().equals("input") && !inputElement.getTagName().equals("textarea")) {
						return new ToolExecuteResult("Element at index " + index + " is not an input element");
					}
					typeWithHumanDelay(inputElement, text);
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Successfully input '" + text + "' into element at index " + index);

				case "key_enter":
					if (index == null) {
						return new ToolExecuteResult("Index is required for 'key_enter' action");
					}
					if (index < 0 || index >= interactiveElements.size()) {
						return new ToolExecuteResult("Element with index " + index + " not found");
					}
					WebElement enterElement = interactiveElements.get(index);
					enterElement.sendKeys(Keys.RETURN);
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Hit the enter key at index " + index);

				case "screenshot":
					TakesScreenshot screenshot = (TakesScreenshot) driver;
					String base64Screenshot = screenshot.getScreenshotAs(OutputType.BASE64);
					return new ToolExecuteResult(
							"Screenshot captured (base64 length: " + base64Screenshot.length() + ")");

				case "get_html":
					String html = driver.getPageSource();
					return new ToolExecuteResult(
							html.length() > MAX_LENGTH ? html.substring(0, MAX_LENGTH) + "..." : html);

				case "get_text":
					String body = driver.findElement(By.tagName("body")).getText();
					log.info("get_text body is {}", body);

					return new ToolExecuteResult(body);

				case "execute_js":
					if (script == null) {
						return new ToolExecuteResult("Script is required for 'execute_js' action");
					}
					JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
					Object result = jsExecutor.executeScript(script);
					refreshTabsInfo(driver); // 刷新标签页信息
					if (result == null) {

						return new ToolExecuteResult("Successfully executed JavaScript code.");
					}
					else {
						return new ToolExecuteResult(result.toString());
					}
				case "scroll":
					if (scrollAmount == null) {
						return new ToolExecuteResult("Scroll amount is required for 'scroll' action");
					}
					((JavascriptExecutor) driver).executeScript("window.scrollBy(0," + scrollAmount + ");");
					String direction = scrollAmount > 0 ? "down" : "up";
					return new ToolExecuteResult("Scrolled " + direction + " by " + Math.abs(scrollAmount) + " pixels");

				case "new_tab":
					if (url == null) {
						return new ToolExecuteResult("URL is required for 'new_tab' action");
					}
					((JavascriptExecutor) driver).executeScript("window.open('" + url + "', '_blank');");
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Opened new tab with URL " + url);

				case "close_tab":
					driver.close();
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Closed current tab");

				case "switch_tab":
					if (tabId == null) {
						return new ToolExecuteResult("Tab ID is out of range for 'switch_tab' action");
					}
					Object[] windowHandles = driver.getWindowHandles().toArray();
					driver.switchTo().window(windowHandles[tabId].toString());
					refreshTabsInfo(driver); // 刷新标签页信息
					return new ToolExecuteResult("Switched to tab " + tabId);

				case "refresh":
					driver.navigate().refresh();
					return new ToolExecuteResult("Refreshed current page");

				default:
					return new ToolExecuteResult("Unknown action: " + action);
			}
		}
		catch (Exception e) {
			if (e instanceof ElementNotInteractableException) {
				String errorMessage = String.format(
						"""
								Browser action '%s' failed, mostly like to have used the wrong index argument.
								You can try to use 'get_html' to get and analyze the page HTML content first and then use other actions to find the right input element.

								Tips for :
								1. ignore all the hidden input or textarea elements.
								2. for baidu engine, you can use js script to do the operation

								detailed exception message:
								%s
								""",
						action, e.getMessage());
				return new ToolExecuteResult(errorMessage);
			}
			return new ToolExecuteResult("Browser action '" + action + "' failed: " + e.getMessage());
		}
	}

	private static final String INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea[type='search'], textarea, [role='button'], [role='link'], [role='textbox'], [role='search'], [role='searchbox']";

	private String formatElementInfo(int index, WebElement element) {
		try {
			if (!isElementVisible(element)) {
				return ""; // 如果元素不可见，直接返回空字符串
			}

			JavascriptExecutor js = (JavascriptExecutor) getDriver();
			@SuppressWarnings("unchecked")
			Map<String, Object> props = (Map<String, Object>) js.executeScript("""
					function getElementInfo(el) {
					    try {
					        const style = window.getComputedStyle(el);
					        return {
					            tagName: el.tagName.toLowerCase(),
					            type: el.getAttribute('type'),
					            role: el.getAttribute('role'),
					            text: el.textContent.trim(),
					            value: el.value,
					            placeholder: el.getAttribute('placeholder'),
					            name: el.getAttribute('name'),
					            id: el.getAttribute('id'),
					            'aria-label': el.getAttribute('aria-label'),
					            isVisible: (
					                el.offsetWidth > 0 &&
					                el.offsetHeight > 0 &&
					                style.visibility !== 'hidden' &&
					                style.display !== 'none'
					            )
					        };
					    } catch(e) {
					        return null; // 如果获取元素信息失败，返回null
					    }
					}
					return getElementInfo(arguments[0]);
					""", element);

			if (props == null || !(Boolean) props.get("isVisible")) {
				return "";
			}

			// 构建HTML属性字符串
			StringBuilder attributes = new StringBuilder();

			// 添加基本属性
			if (props.get("type") != null) {
				attributes.append(" type=\"").append(props.get("type")).append("\"");
			}
			if (props.get("role") != null) {
				attributes.append(" role=\"").append(props.get("role")).append("\"");
			}
			if (props.get("placeholder") != null) {
				attributes.append(" placeholder=\"").append(props.get("placeholder")).append("\"");
			}
			if (props.get("name") != null) {
				attributes.append(" name=\"").append(props.get("name")).append("\"");
			}
			if (props.get("id") != null) {
				attributes.append(" id=\"").append(props.get("id")).append("\"");
			}
			if (props.get("aria-label") != null) {
				attributes.append(" aria-label=\"").append(props.get("aria-label")).append("\"");
			}
			if (props.get("value") != null) {
				attributes.append(" value=\"").append(props.get("value")).append("\"");
			}

			String tagName = (String) props.get("tagName");
			String text = (String) props.get("text");

			// 生成标准HTML格式输出
			return String.format("[%d] <%s%s>%s</%s>\n", index, tagName, attributes.toString(), text, tagName);

		}
		catch (StaleElementReferenceException | NoSuchElementException e) {
			log.debug("忽略过期或不存在的元素: {}", e.getMessage());
			return "";
		}
		catch (Exception e) {
			log.warn("格式化元素信息失败，跳过当前元素: {}", e.getMessage());
			return "";
		}
	}

	// 添加新的方法获取可交互元素
	private boolean isElementVisible(WebElement element) {
		try {
			return element.isDisplayed() && element.isEnabled();
		}
		catch (StaleElementReferenceException | NoSuchElementException e) {
			// 忽略过期或不存在的元素
			log.debug("忽略过期或不存在的元素: {}", e.getMessage());
			return false;
		}
	}

	private List<WebElement> getInteractiveElements(WebDriver driver) {
		try {
			return driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR))
				.stream()
				.filter(this::isElementVisible)
				.collect(Collectors.toList());
		}
		catch (StaleElementReferenceException e) {
			log.warn("元素在获取过程中过期，重试一次: {}", e.getMessage());
			// 如果发生异常，等待一下然后重试
			try {
				Thread.sleep(500);
				return driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR))
					.stream()
					.filter(this::isElementVisible)
					.collect(Collectors.toList());
			}
			catch (Exception retryEx) {
				log.error("重试获取元素失败: {}", retryEx.getMessage());
				return new ArrayList<>(); // 返回空列表而不是抛出异常
			}
		}
	}

	private String getInteractiveElementsInfo(WebDriver driver) {
		StringBuilder resultInfo = new StringBuilder();
		List<WebElement> interactiveElements = getInteractiveElements(driver);

		for (int i = 0; i < interactiveElements.size(); i++) {
			String formattedInfo = formatElementInfo(i, interactiveElements.get(i));
			if (!formattedInfo.isEmpty()) {
				resultInfo.append(formattedInfo);
			}
		}

		return resultInfo.toString();
	}

	private List<Map<String, Object>> getTabsInfo(WebDriver driver) {
		if (cachedTabs != null) {
			return cachedTabs;
		}
		return refreshTabsInfo(driver);
	}

	/**
	 * 这个方法是为了让getCurrentStatus 不会刷新页面，减少在Mac上主动唤起的次数 否则太烦了 ， 每个step要调起这个东西两次。 都会强制把 页面唤起到
	 * active啥事都没办法干了。
	 * @param driver
	 * @return
	 */
	private List<Map<String, Object>> refreshTabsInfo(WebDriver driver) {
		Set<String> windowHandles = driver.getWindowHandles();
		List<Map<String, Object>> tabs = new ArrayList<>();
		String currentHandle = driver.getWindowHandle();
		for (String handle : windowHandles) {
			driver.switchTo().window(handle);
			tabs.add(Map.of("url", driver.getCurrentUrl(), "title", driver.getTitle(), "id", handle));
		}
		driver.switchTo().window(currentHandle); // 切回原始标签页
		this.cachedTabs = tabs;
		return tabs;
	}

	public Map<String, Object> getCurrentState() {
		WebDriver driver = getDriver();
		Map<String, Object> state = new HashMap<>();

		try {
			// 等待页面加载完成
			driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));

			// 获取基本信息
			String currentUrl = driver.getCurrentUrl();
			String title = driver.getTitle();
			state.put("url", currentUrl);
			state.put("title", title);

			// 获取标签页信息

			List<Map<String, Object>> tabs = getTabsInfo(driver);
			state.put("tabs", tabs);

			// 获取viewport和滚动信息
			JavascriptExecutor js = (JavascriptExecutor) driver;
			Long scrollTop = (Long) js.executeScript("return window.pageYOffset;");
			Long scrollHeight = (Long) js.executeScript("return document.documentElement.scrollHeight;");
			Long viewportHeight = (Long) js.executeScript("return window.innerHeight;");

			Map<String, Object> scrollInfo = new HashMap<>();
			scrollInfo.put("pixels_above", scrollTop);
			scrollInfo.put("pixels_below", Math.max(0, scrollHeight - (scrollTop + viewportHeight)));
			scrollInfo.put("total_height", scrollHeight);
			scrollInfo.put("viewport_height", viewportHeight);
			state.put("scroll_info", scrollInfo);

			// 获取可交互元素
			String elementsInfo = getInteractiveElementsInfo(driver);
			state.put("interactive_elements", elementsInfo);

			// 捕获截图
			TakesScreenshot screenshot = (TakesScreenshot) driver;
			String base64Screenshot = screenshot.getScreenshotAs(OutputType.BASE64);
			state.put("screenshot", base64Screenshot);

			// 添加帮助信息
			state.put("help", "[0], [1], [2], etc., represent clickable indices corresponding to the elements listed. "
					+ "Clicking on these indices will navigate to or interact with the respective content behind them.");

			return state;

		}
		catch (Exception e) {
			log.error("Failed to get browser state", e);
			state.put("error", "Failed to get browser state: " + e.getMessage());
			return state;
		}
	}

	@Override
	public ToolExecuteResult apply(String t, ToolContext u) {

		return run(t);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<?> getInputType() {
		return String.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public BaseAgent getAgent() {
		return this.agent;
	}

	@Override
	public String getCurrentToolStateString() {
		Map<String, Object> state = getCurrentState();

		// 构建URL和标题信息
		String urlInfo = String.format("\n   URL: %s\n   Title: %s", state.get("url"), state.get("title"));

		// 构建标签页信息
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> tabs = (List<Map<String, Object>>) state.get("tabs");
		String tabsInfo = (tabs != null) ? String.format("\n   %d tab(s) available", tabs.size()) : "";

		// 获取滚动信息
		@SuppressWarnings("unchecked")
		Map<String, Object> scrollInfo = (Map<String, Object>) state.get("scroll_info");
		String contentAbove = "";
		String contentBelow = "";
		if (scrollInfo != null) {
			Long pixelsAbove = (Long) scrollInfo.get("pixels_above");
			Long pixelsBelow = (Long) scrollInfo.get("pixels_below");
			contentAbove = pixelsAbove > 0 ? String.format(" (%d pixels)", pixelsAbove) : "";
			contentBelow = pixelsBelow > 0 ? String.format(" (%d pixels)", pixelsBelow) : "";
		}

		// 获取交互元素信息
		String elementsInfo = (String) state.get("interactive_elements");

		// 构建最终的状态字符串
		String retString = String.format("""
				When you see [Current state starts here], focus on the following:
				- Current URL and page title:
				%s

				- Available tabs:
				%s

				- Interactive elements and their indices:
				%s

				- Content above%s or below%s the viewport (if indicated)

				- Any action results or errors:
				%s
				""", urlInfo, tabsInfo, elementsInfo != null ? elementsInfo : "", contentAbove, contentBelow,
				state.containsKey("error") ? state.get("error") : "");

		return retString;
	}

	// @Override
	// public ChromeDriver getInstance(String planId) {
	// return this.chromeDriverService.getDriver(planId);
	// }

	// cleanup 方法已经存在，只需确保它符合接口规范
	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up Chrome resources for plan: {}", planId);
			this.chromeDriverService.closeDriverForPlan(planId);
		}
	}

}
