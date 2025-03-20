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

import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class BrowserUseTool implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private WebDriver driver;

	private static final int MAX_LENGTH = 3000;

	private static final String PARAMETERS = "{\n" + "\t\"type\": \"object\",\n" + "\t\"properties\": {\n"
			+ "\t\t\"action\": {\n" + "\t\t\t\"type\": \"string\",\n" + "\t\t\t\"enum\": [\n"
			+ "\t\t\t\t\"navigate\",\n" + "\t\t\t\t\"click\",\n" + "\t\t\t\t\"input_text\",\n"
			+ "\t\t\t\t\"key_enter\",\n" + "\t\t\t\t\"screenshot\",\n" + "\t\t\t\t\"get_html\",\n"
			+ "\t\t\t\t\"get_text\",\n" + "\t\t\t\t\"execute_js\",\n" + "\t\t\t\t\"scroll\",\n"
			+ "\t\t\t\t\"switch_tab\",\n" + "\t\t\t\t\"new_tab\",\n" + "\t\t\t\t\"close_tab\",\n"
			+ "\t\t\t\t\"refresh\"\n" + "\t\t\t],\n" + "\t\t\t\"description\": \"The browser action to perform\"\n"
			+ "\t\t},\n" + "\t\t\"url\": {\n" + "\t\t\t\"type\": \"string\",\n"
			+ "\t\t\t\"description\": \"URL for 'navigate' or 'new_tab' actions\"\n" + "\t\t},\n" + "\t\t\"index\": {\n"
			+ "\t\t\t\"type\": \"integer\",\n"
			+ "\t\t\t\"description\": \"Element index for 'click' or 'input_text' actions\"\n" + "\t\t},\n"
			+ "\t\t\"text\": {\n" + "\t\t\t\"type\": \"string\",\n"
			+ "\t\t\t\"description\": \"Text for 'input_text' action\"\n" + "\t\t},\n" + "\t\t\"script\": {\n"
			+ "\t\t\t\"type\": \"string\",\n" + "\t\t\t\"description\": \"JavaScript code for 'execute_js' action\"\n"
			+ "\t\t},\n" + "\t\t\"scroll_amount\": {\n" + "\t\t\t\"type\": \"integer\",\n"
			+ "\t\t\t\"description\": \"Pixels to scroll (positive for down, negative for up) for 'scroll' action\"\n"
			+ "\t\t},\n" + "\t\t\"tab_id\": {\n" + "\t\t\t\"type\": \"integer\",\n"
			+ "\t\t\t\"description\": \"Tab ID for 'switch_tab' action\"\n" + "\t\t}\n" + "\t},\n"
			+ "\t\"required\": [\n" + "\t\t\"action\"\n" + "\t],\n" + "\t\"dependencies\": {\n"
			+ "\t\t\"navigate\": [\n" + "\t\t\t\"url\"\n" + "\t\t],\n" + "\t\t\"click\": [\n" + "\t\t\t\"index\"\n"
			+ "\t\t],\n" + "\t\t\"input_text\": [\n" + "\t\t\t\"index\",\n" + "\t\t\t\"text\"\n" + "\t\t],\n"
			+ "\t\t\"key_enter\": [\n" + "\t\t\t\"index\"\n" + "\t\t],\n" + "\t\t\"execute_js\": [\n"
			+ "\t\t\t\"script\"\n" + "\t\t],\n" + "\t\t\"switch_tab\": [\n" + "\t\t\t\"tab_id\"\n" + "\t\t],\n"
			+ "\t\t\"new_tab\": [\n" + "\t\t\t\"url\"\n" + "\t\t],\n" + "\t\t\"scroll\": [\n"
			+ "\t\t\t\"scroll_amount\"\n" + "\t\t]\n" + "\t}\n" + "}";

	private static final String name = "browser_use";

	private static final String description = "Interact with a web browser to perform various actions such as navigation, element interaction,搜索类优先考虑此工具\n"
			+ "content extraction, and tab management. Supported actions include:\n"
			+ "- 'navigate': Go to a specific URL, use https://baidu.com by default\n"
			+ "- 'click': Click an element by index\n"
			+ "- 'input_text': Input text into an element, for 百度(Baidu), the index of the input button is \n"
			+ "- 'key_enter': Hit the Enter key\n" + "- 'screenshot': Capture a screenshot\n"
			+ "- 'get_html': Get page HTML content\n" + "- 'get_text': Get text content of the page\n" +
			// "- 'read_links': Get all links on the page\n" +
			"- 'execute_js': Execute JavaScript code\n" + "- 'scroll': Scroll the page\n"
			+ "- 'switch_tab': Switch to a specific tab\n" + "- 'new_tab': Open a new tab\n"
			+ "- 'close_tab': Close the current tab\n" + "- 'refresh': Refresh the current page";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, browserUseTool)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	private static final BrowserUseTool browserUseTool = new BrowserUseTool();

	public BrowserUseTool() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--remote-allow-origins=*");
		// options.setPageLoadStrategy(PageLoadStrategy.EAGER);
		// options.addArguments("--headless");
		// options.addArguments("--incognito");
		// options.addArguments("--no-sandbox");
		// options.addArguments("--disable-extensions");
		// options.addArguments("--start-maximized");
		driver = new ChromeDriver(options);
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
					elements.get(index).click();
					return new ToolExecuteResult("Clicked element at index " + index);

				case "input_text":
					if (index == null || text == null) {
						return new ToolExecuteResult("Index and text are required for 'input_text' action");
					}
					WebElement inputElement = driver.findElements(By.cssSelector("input, textarea")).get(index);
					inputElement.sendKeys(text);
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

	public void close() {
		if (driver != null) {
			driver.quit();
			driver = null;
		}
		System.out.println("Browser resources have been cleaned up.");
	}

	public WebDriver getDriver() {
		return driver;
	}

	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}

	@Override
	public ToolExecuteResult apply(String s) {
		return run(s);
	}

}
