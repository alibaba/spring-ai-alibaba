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

import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class BrowserUseTool implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private final ChromeDriverService chromeDriverService;

	private static BrowserUseTool instance;

	public BrowserUseTool(ChromeDriverService chromeDriverService) {
		this.chromeDriverService = chromeDriverService;
	}

	private WebDriver getDriver() {
		return chromeDriverService.getDriver();
	}

	private static final int MAX_LENGTH = 7000;

	private static final String PARAMETERS = """
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

	private static final String name = "browser_use";

	private static final String description = """
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

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static synchronized BrowserUseTool getInstance(ChromeDriverService chromeDriverService) {
		if (instance == null) {
			instance = new BrowserUseTool(chromeDriverService);
		}
		return instance;
	}

	public static FunctionToolCallback getFunctionToolCallback(ChromeDriverService chromeDriverService) {
		return FunctionToolCallback.builder(name, getInstance(chromeDriverService))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	private void simulateHumanBehavior(WebElement element) {
		try {
			// 模拟人类移动鼠标
			Actions actions = new Actions(getDriver());
			actions.moveToElement(element)
					.pause(Duration.ofMillis(new Random().nextInt(500) + 200))
					.build()
					.perform();

			// 添加随机延迟
			Thread.sleep(new Random().nextInt(1000) + 500);
		} catch (InterruptedException e) {
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
			} catch (InterruptedException e) {
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
			switch (action) {
				case "navigate":
					if (url == null) {
						return new ToolExecuteResult("URL is required for 'navigate' action");
					}
					// driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
					driver.get(url);
					return new ToolExecuteResult("Navigated to " + url);

				case "click":
					if (index == null) {
						return new ToolExecuteResult("Index is required for 'click' action");
					}
					List<WebElement> elements = driver.findElements(By.cssSelector("*"));
					if (index < 0 || index >= elements.size()) {
						return new ToolExecuteResult("Element with index " + index + " not found");
					}
					WebElement element = elements.get(index);
					simulateHumanBehavior(element);
					element.click();
					return new ToolExecuteResult("Clicked element at index " + index);

				case "input_text":
					if (index == null || text == null) {
						return new ToolExecuteResult("Index and text are required for 'input_text' action");
					}
					WebElement inputElement = driver.findElements(By.cssSelector("input, textarea")).get(index);
					typeWithHumanDelay(inputElement, text);
					return new ToolExecuteResult("Successfully input '" + text + "' into element at index " + index);

				case "key_enter":
					if (index == null) {
						return new ToolExecuteResult("Index are required for 'key_enter' action");
					}
					WebElement inputElement2 = driver.findElements(By.cssSelector("input, textarea")).get(index);
					inputElement2.sendKeys(Keys.RETURN);
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
					int counter = 0;
					String body = driver.findElement(By.tagName("body")).getText();
					log.info("get_text body is {}", body);
					if (body != null && body.contains("我们的系统检测到您的计算机网络中存在异常流量")) {
						while (counter++ < 5) {
							Thread.sleep(10000);
							body = driver.findElement(By.tagName("body")).getText();
							log.info("retry {} get_text body is {}", counter, body);
							if (body != null && body.contains("我们的系统检测到您的计算机网络中存在异常流量")) {
								continue;
							}
							return new ToolExecuteResult(body);
						}
					}
					return new ToolExecuteResult(body);

				case "execute_js":
					if (script == null) {
						return new ToolExecuteResult("Script is required for 'execute_js' action");
					}
					JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
					Object result = jsExecutor.executeScript(script);
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
					return new ToolExecuteResult("Opened new tab with URL " + url);

				case "close_tab":
					driver.close();
					return new ToolExecuteResult("Closed current tab");

				case "switch_tab":
					if (tabId == null) {
						return new ToolExecuteResult("Tab ID is out of range for 'switch_tab' action");
					}
					Object[] windowHandles = driver.getWindowHandles().toArray();
					driver.switchTo().window(windowHandles[tabId].toString());
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

	public Map<String, Object> getCurrentState() {
		WebDriver driver = getDriver();
		Map<String, Object> state = new HashMap<>();
		
		try {
			// 收集基本信息
			state.put("url", driver.getCurrentUrl());
			state.put("title", driver.getTitle());
			
			// 获取标签页信息
			Set<String> windowHandles = driver.getWindowHandles();
			List<Map<String, String>> tabs = new ArrayList<>();
			for (String handle : windowHandles) {
				String currentHandle = driver.getWindowHandle();
				driver.switchTo().window(handle);
				tabs.add(Map.of(
					"url", driver.getCurrentUrl(),
					"title", driver.getTitle()
				));
				driver.switchTo().window(currentHandle);
			}
			state.put("tabs", tabs);
			
			// 获取页面滚动信息
			JavascriptExecutor js = (JavascriptExecutor) driver;
			Long scrollTop = (Long) js.executeScript("return window.pageYOffset;");
			Long scrollHeight = (Long) js.executeScript("return document.documentElement.scrollHeight;");
			Long clientHeight = (Long) js.executeScript("return document.documentElement.clientHeight;");
			
			Map<String, Object> scrollInfo = new HashMap<>();
			scrollInfo.put("content_above", scrollTop);
			scrollInfo.put("content_below", scrollHeight - (scrollTop + clientHeight));
			scrollInfo.put("total_height", scrollHeight);
			scrollInfo.put("viewport_height", clientHeight);
			state.put("scroll_info", scrollInfo);
			
			// 获取可交互元素
			List<WebElement> interactiveElements = driver.findElements(
				By.cssSelector("a, button, input, select, textarea")
			);
			StringBuilder interactiveElementsStr = new StringBuilder();
			for (int i = 0; i < interactiveElements.size(); i++) {
				WebElement element = interactiveElements.get(i);
				String text = element.getText();
				String type = element.getTagName();
				if (text.isEmpty()) {
					text = element.getAttribute("placeholder");
				}
				if (text.isEmpty()) {
					text = element.getAttribute("value");
				}
				if (!text.isEmpty()) {
					interactiveElementsStr.append(String.format("[%d] %s (%s)\n", i, text, type));
				}
			}
			state.put("interactive_elements", interactiveElementsStr.toString());
			
			// 获取截图
			TakesScreenshot screenshot = (TakesScreenshot) driver;
			String base64Screenshot = screenshot.getScreenshotAs(OutputType.BASE64);
			state.put("screenshot", base64Screenshot);
			
			// 添加帮助信息
			state.put("help", "[0], [1], [2], etc., represent clickable indices corresponding to the elements listed. " +
					"Clicking on these indices will navigate to or interact with the respective content behind them.");
					
			return state;
			
		} catch (Exception e) {
			log.error("Error getting browser state", e);
			state.put("error", e.getMessage());
			return state;
		}
	}

	@Override
	public ToolExecuteResult apply(String s) {
		return run(s);
	}

}
