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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
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
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.GetElementPositionByNameAction;
import com.alibaba.cloud.ai.example.manus.tool.browser.actions.MoveToAndClickAction;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

public class BrowserUseTool implements ToolCallBiFunctionDef<BrowserRequestVO> {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private final ChromeDriverService chromeDriverService;

	private String planId;

	public BrowserUseTool(ChromeDriverService chromeDriverService) {
		this.chromeDriverService = chromeDriverService;
	}

	public DriverWrapper getDriver() {
		return chromeDriverService.getDriver(planId);
	}

	/**
	 * 获取浏览器操作的超时时间配置
	 * @return 超时时间（秒），如果未配置则返回默认值30秒
	 */
	private Integer getBrowserTimeout() {
		Integer timeout = getManusProperties().getBrowserRequestTimeout();
		return timeout != null ? timeout : 30; // 默认超时时间为 30 秒
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
			                "refresh",
			                "get_element_position",
			                "move_to_and_click"
			            ],
			            "description": "The browser action to perform"
			        },
			        "url": {
			            "type": "string",
			            "description": "URL for 'navigate' or 'new_tab' actions , don't support get_text and get_html"
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
			        },
			        "element_name": {
			            "type": "string",
			            "description": "Element name for 'get_element_position' action"
			        },
			        "position_x": {
			            "type": "integer",
			            "description": "X coordinate for 'move_to_and_click' action"
			        },
			        "position_y": {
			            "type": "integer",
			            "description": "Y coordinate for 'move_to_and_click' action"
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
			        ],
			        "get_element_position": [
			            "element_name"
			        ],
			        "move_to_and_click": [
			            "position_x",
			            "position_y"
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
			- 'get_html'：获取当前页面的HTML内容(不支持url参数)
			- 'get_text'：获取当前页面文本内容(不支持url参数)
			- 'execute_js'：执行JavaScript代码
			- 'scroll'：滚动页面
			- 'switch_tab'：切换到特定标签页
			- 'new_tab'：打开新标签页
			- 'close_tab'：关闭当前标签页
			- 'refresh'：刷新当前页面
			- 'get_element_position'：通过关键词获取元素的位置坐标(x,y)
			- 'move_to_and_click'：移动到指定的绝对位置(x,y)并点击
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

	public ToolExecuteResult run(BrowserRequestVO requestVO) {
		log.info("BrowserUseTool requestVO: action={}", requestVO.getAction());

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
				case "get_element_position": {
					return new GetElementPositionByNameAction(this).execute(requestVO);
				}
				case "move_to_and_click": {
					return new MoveToAndClickAction(this).execute(requestVO);
				}
				default:
					return new ToolExecuteResult("Unknown action: " + action);
			}
		}
		catch (Exception e) {
			log.error("Browser action '" + action + "' failed", e);
			return new ToolExecuteResult("Browser action '" + action + "' failed: " + e.getMessage());
		}
	}

	private List<Map<String, Object>> getTabsInfo(Page page) {
		return page.context().pages().stream().map(p -> {
			Map<String, Object> tabInfo = new HashMap<>();
			tabInfo.put("url", p.url());
			tabInfo.put("title", p.title());

			return tabInfo;
		}).toList();
	}

	public Map<String, Object> getCurrentState(Page page) {
		Map<String, Object> state = new HashMap<>();

		try {
			// 等待页面加载完成，避免在导航过程中获取信息时出现上下文销毁错误
			try {
				Integer timeout = getBrowserTimeout();
				page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED,
						new Page.WaitForLoadStateOptions().setTimeout(timeout * 1000));
			}
			catch (Exception loadException) {
				log.warn("Page load state wait timeout or failed, continuing anyway: {}", loadException.getMessage());
			}

			// 获取基本信息
			String currentUrl = page.url();
			String title = page.title();
			state.put("url", currentUrl);
			state.put("title", title);

			// 获取标签页信息
			List<Map<String, Object>> tabs = getTabsInfo(page);
			state.put("tabs", tabs);

			String interactiveElements = chromeDriverService.getDriver(planId)
				.getInteractiveElementRegistry()
				.generateElementsInfoText(page);
			state.put("interactive_elements", interactiveElements);

			return state;

		}
		catch (Exception e) {
			log.error("Failed to get browser state", e);
			state.put("error", "Failed to get browser state: " + e.getMessage());
			return state;
		}
	}

	@Override
	public ToolExecuteResult apply(BrowserRequestVO requestVO, ToolContext u) {
		return run(requestVO);
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
	public Class<BrowserRequestVO> getInputType() {
		return BrowserRequestVO.class;
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
		DriverWrapper driver = getDriver();
		Map<String, Object> state = getCurrentState(driver.getCurrentPage());
		// 构建URL和标题信息
		String urlInfo = String.format("\n   URL: %s\n   Title: %s", state.get("url"), state.get("title"));

		// 构建标签页信息
		List<Map<String, Object>> tabs = (List<Map<String, Object>>) state.get("tabs");
		String tabsInfo = (tabs != null) ? String.format("\n   %d tab(s) available", tabs.size()) : "";
		if (tabs != null) {
			for (int i = 0; i < tabs.size(); i++) {
				Map<String, Object> tab = tabs.get(i);
				String tabUrl = (String) tab.get("url");
				String tabTitle = (String) tab.get("title");
				tabsInfo += String.format("\n   [%d] %s: %s", i, tabTitle, tabUrl);
			}
		}
		// 获取滚动信息
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

	// cleanup 方法已经存在，只需确保它符合接口规范
	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up Chrome resources for plan: {}", planId);
			this.chromeDriverService.closeDriverForPlan(planId);
		}
	}

	public ManusProperties getManusProperties() {
		return this.chromeDriverService.getManusProperties();
	}

}
