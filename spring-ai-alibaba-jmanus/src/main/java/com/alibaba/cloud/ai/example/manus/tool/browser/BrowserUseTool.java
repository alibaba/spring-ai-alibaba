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

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.BrowserRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.ClickByElementAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.CloseTabAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.ExecuteJsAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetHtmlAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetTextAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.InputTextAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.KeyEnterAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.NavigateAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.NewTabAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.RefreshAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.ScreenShotAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.ScrollAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.SwitchTabAction;
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
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class BrowserUseTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private final ChromeDriverService chromeDriverService;

	private final InteractiveTextProcessor interactiveTextProcessor = new InteractiveTextProcessor();

	// 添加标签页缓存字段
	private List<Map<String, Object>> cachedTabs;

	private String planId;

	public BrowserUseTool(ChromeDriverService chromeDriverService) {
		this.chromeDriverService = chromeDriverService;
	}

	public WebDriver getDriver() {
		return chromeDriverService.getDriver(planId);
	}

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
			与网页浏览器交互，执行各种操作，如导航、元素交互、内容提取和标签页管理。搜索类优先考虑此工具。
			支持的操作包括：
			- 'navigate'：访问特定URL，默认使用https://baidu.com
			- 'click'：按索引点击元素
			- 'input_text'：在元素中输入文本，对于百度(Baidu)，输入框的索引是
			- 'key_enter'：按回车键
			- 'screenshot'：捕获屏幕截图
			- 'get_html'：获取页面HTML内容
			- 'get_text'：获取页面文本内容
			- 'execute_js'：执行JavaScript代码
			- 'scroll'：滚动页面
			- 'switch_tab'：切换到特定标签页
			- 'new_tab'：打开新标签页
			- 'close_tab'：关闭当前标签页
			- 'refresh'：刷新当前页面
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

	public FunctionToolCallback getFunctionToolCallback(ChromeDriverService chromeDriverService) {
		return FunctionToolCallback.builder(name, getInstance(chromeDriverService))
				.description(description)
				.inputSchema(PARAMETERS)
				.inputType(String.class)
				.build();
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("BrowserUseTool toolInput:" + toolInput);

		// 直接将JSON字符串解析为BrowserRequestVO对象
		BrowserRequestVO requestVO = JSON.parseObject(toolInput, BrowserRequestVO.class);

		// 从RequestVO中获取参数
		String action = requestVO.getAction();
		try {
			if (action == null) {
				return new ToolExecuteResult("Action parameter is required");
			}
			switch (action) {
				case "navigate": {
					return new NavigateAction(this).execute(requestVO);
				}
				case "click": {
					return new ClickByElementAction(this).execute(requestVO);
				}
				case "input_text": {
					return new InputTextAction(this).execute(requestVO);
				}
				case "key_enter": {
					return new KeyEnterAction(this).execute(requestVO);
				}
				case "screenshot": {
					return new ScreenShotAction(this).execute(requestVO);
				}
				case "get_html": {
					return new GetHtmlAction(this).execute(requestVO);
				}
				case "get_text": {
					return new GetTextAction(this).execute(requestVO);
				}
				case "execute_js": {
					return new ExecuteJsAction(this).execute(requestVO);
				}
				case "scroll": {
					return new ScrollAction(this).execute(requestVO);
				}
				case "new_tab": {
					return new NewTabAction(this).execute(requestVO);
				}
				case "close_tab": {
					return new CloseTabAction(this).execute(requestVO);
				}
				case "switch_tab": {
					return new SwitchTabAction(this).execute(requestVO);
				}
				case "refresh": {
					return new RefreshAction(this).execute(requestVO);
				}
				default:
					return new ToolExecuteResult("Unknown action: " + action);
			}
		} catch (Exception e) {
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

	private String getInteractiveElementsInfo(WebDriver driver) {
		return interactiveTextProcessor.getInteractiveElementsInfo(driver);
	}

	private List<Map<String, Object>> getTabsInfo(WebDriver driver) {
		if (cachedTabs != null) {
			return cachedTabs;
		}
		return new RefreshAction(this).refreshTabsInfo(driver);
	}

	public void setTabsInfo(List<Map<String, Object>> tabs) {
		this.cachedTabs = tabs;
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

		} catch (Exception e) {
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
	public String getServiceGroup() {
		return "default-service-group";
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
	public void setPlanId(String planId) {
		this.planId = planId;
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
