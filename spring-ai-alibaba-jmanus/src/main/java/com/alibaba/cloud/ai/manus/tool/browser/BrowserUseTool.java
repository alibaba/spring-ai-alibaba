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
package com.alibaba.cloud.ai.manus.tool.browser;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;

import com.alibaba.cloud.ai.manus.tool.browser.actions.BrowserRequestVO;
import com.alibaba.cloud.ai.manus.tool.browser.actions.ClickByElementAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.CloseTabAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.ExecuteJsAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.GetElementPositionByNameAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.GetHtmlAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.GetTextAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.InputTextAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.KeyEnterAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.MoveToAndClickAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.NavigateAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.NewTabAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.RefreshAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.ScreenShotAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.ScrollAction;
import com.alibaba.cloud.ai.manus.tool.browser.actions.SwitchTabAction;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.innerStorage.SmartContentSavingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserUseTool extends AbstractBaseTool<BrowserRequestVO> {

	private static final Logger log = LoggerFactory.getLogger(BrowserUseTool.class);

	private final ChromeDriverService chromeDriverService;

	private final SmartContentSavingService innerStorageService;

	private final ObjectMapper objectMapper;

	public BrowserUseTool(ChromeDriverService chromeDriverService, SmartContentSavingService innerStorageService,
			ObjectMapper objectMapper) {
		this.chromeDriverService = chromeDriverService;
		this.innerStorageService = innerStorageService;
		this.objectMapper = objectMapper;
	}

	public DriverWrapper getDriver() {
		return chromeDriverService.getDriver(currentPlanId);
	}

	/**
	 * Get browser operation timeout configuration
	 * @return Timeout in seconds, returns default value of 30 seconds if not configured
	 */
	private Integer getBrowserTimeout() {
		Integer timeout = getManusProperties().getBrowserRequestTimeout();
		return timeout != null ? timeout : 30; // Default timeout is 30 seconds
	}

	private final String name = "browser_use";

	public static synchronized BrowserUseTool getInstance(ChromeDriverService chromeDriverService,
			SmartContentSavingService innerStorageService, ObjectMapper objectMapper) {
		BrowserUseTool instance = new BrowserUseTool(chromeDriverService, innerStorageService, objectMapper);
		return instance;
	}

	public ToolExecuteResult run(BrowserRequestVO requestVO) {
		log.info("BrowserUseTool requestVO: action={}", requestVO.getAction());

		// Get parameters from RequestVO
		String action = requestVO.getAction();
		try {
			if (action == null) {
				return new ToolExecuteResult("Action parameter is required");
			}

			ToolExecuteResult result;
			switch (action) {
				case "navigate": {
					result = new NavigateAction(this).execute(requestVO);
					break;
				}
				case "click": {
					result = new ClickByElementAction(this).execute(requestVO);
					break;
				}
				case "input_text": {
					result = new InputTextAction(this).execute(requestVO);
					break;
				}
				case "key_enter": {
					result = new KeyEnterAction(this).execute(requestVO);
					break;
				}
				case "screenshot": {
					result = new ScreenShotAction(this).execute(requestVO);
					break;
				}
				case "get_html": {
					result = new GetHtmlAction(this).execute(requestVO);
					// HTML content is usually long, use intelligent processing
					SmartContentSavingService.SmartProcessResult processedResult = innerStorageService
						.processContent(currentPlanId, result.getOutput(), "get_html");
					return new ToolExecuteResult(processedResult.getSummary());
				}
				case "get_text": {
					result = new GetTextAction(this).execute(requestVO);
					// Text content may be long, use intelligent processing
					SmartContentSavingService.SmartProcessResult processedResult = innerStorageService
						.processContent(currentPlanId, result.getOutput(), "get_text");
					return new ToolExecuteResult(processedResult.getSummary());
				}
				case "execute_js": {
					result = new ExecuteJsAction(this).execute(requestVO);
					// JS execution results may be long, use intelligent processing
					SmartContentSavingService.SmartProcessResult processedResult = innerStorageService
						.processContent(currentPlanId, result.getOutput(), "execute_js");
					return new ToolExecuteResult(processedResult.getSummary());
				}
				case "scroll": {
					result = new ScrollAction(this).execute(requestVO);
					break;
				}
				case "new_tab": {
					result = new NewTabAction(this).execute(requestVO);
					break;
				}
				case "close_tab": {
					result = new CloseTabAction(this).execute(requestVO);
					break;
				}
				case "switch_tab": {
					result = new SwitchTabAction(this).execute(requestVO);
					break;
				}
				case "refresh": {
					result = new RefreshAction(this).execute(requestVO);
					break;
				}
				case "get_element_position": {
					result = new GetElementPositionByNameAction(this, objectMapper).execute(requestVO);
					break;
				}
				case "move_to_and_click": {
					result = new MoveToAndClickAction(this).execute(requestVO);
					break;
				}
				default:
					return new ToolExecuteResult("Unknown action: " + action);
			}

			// For other operations, also perform intelligent processing (but thresholds
			// usually won't be exceeded)
			SmartContentSavingService.SmartProcessResult processedResult = innerStorageService
				.processContent(currentPlanId, result.getOutput(), action);
			return new ToolExecuteResult(processedResult.getSummary());
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
			// Wait for page to load completely to avoid context destruction errors when
			// getting information during navigation
			try {
				Integer timeout = getBrowserTimeout();
				page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED,
						new Page.WaitForLoadStateOptions().setTimeout(timeout * 1000));
			}
			catch (Exception loadException) {
				log.warn("Page load state wait timeout or failed, continuing anyway: {}", loadException.getMessage());
			}

			// Get basic information
			String currentUrl = page.url();
			String title = page.title();
			state.put("url", currentUrl);
			state.put("title", title);

			// Get tab information
			List<Map<String, Object>> tabs = getTabsInfo(page);
			state.put("tabs", tabs);

			String interactiveElements = chromeDriverService.getDriver(currentPlanId)
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
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return """
				Interact with web browser to perform various operations such as navigation, element interaction, content extraction and tab management. Prioritize this tool for search-related tasks.

				Supported operations include:
				- 'navigate': Visit specific URL
				- 'click': Click element by index
				- 'input_text': Input text in element
				- 'key_enter': Press Enter key
				- 'screenshot': Capture screenshot
				- 'get_html': Get HTML content of current page
				- 'get_text': Get text content of current page
				- 'execute_js': Execute JavaScript code
				- 'scroll': Scroll page up/down
				- 'refresh': Refresh current page
				- 'new_tab': Open new tab
				- 'close_tab': Close current tab
				- 'switch_tab': Switch to specific tab
				- 'get_element_position_by_name': Get element position by name
				- 'move_to_and_click': Move to coordinates and click

				Note: Browser operations have timeout configuration, default is 30 seconds.
				""";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "oneOf": [
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "navigate"
				                },
				                "url": {
				                    "type": "string",
				                    "description": "URL to navigate to"
				                }
				            },
				            "required": ["action", "url"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "click"
				                },
				                "index": {
				                    "type": "integer",
				                    "description": "Element index to click"
				                }
				            },
				            "required": ["action", "index"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "input_text"
				                },
				                "index": {
				                    "type": "integer",
				                    "description": "Element index to input text"
				                },
				                "text": {
				                    "type": "string",
				                    "description": "Text to input"
				                }
				            },
				            "required": ["action", "index", "text"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "key_enter"
				                },
				                "index": {
				                    "type": "integer",
				                    "description": "Element index to press enter"
				                }
				            },
				            "required": ["action", "index"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "screenshot"
				                }
				            },
				            "required": ["action"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "get_html"
				                }
				            },
				            "required": ["action"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "get_text"
				                }
				            },
				            "required": ["action"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "execute_js"
				                },
				                "script": {
				                    "type": "string",
				                    "description": "JavaScript code to execute"
				                }
				            },
				            "required": ["action", "script"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "scroll"
				                },
				                "direction": {
				                    "type": "string",
				                    "enum": ["up", "down"],
				                    "description": "Scroll direction"
				                }
				            },
				            "required": ["action", "direction"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "switch_tab"
				                },
				                "tab_id": {
				                    "type": "integer",
				                    "description": "Tab ID to switch to"
				                }
				            },
				            "required": ["action", "tab_id"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "new_tab"
				                },
				                "url": {
				                    "type": "string",
				                    "description": "URL to open in new tab"
				                }
				            },
				            "required": ["action", "url"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "close_tab"
				                }
				            },
				            "required": ["action"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "refresh"
				                }
				            },
				            "required": ["action"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "get_element_position"
				                },
				                "element_name": {
				                    "type": "string",
				                    "description": "Element name to get position"
				                }
				            },
				            "required": ["action", "element_name"],
				            "additionalProperties": false
				        },
				        {
				            "type": "object",
				            "properties": {
				                "action": {
				                    "type": "string",
				                    "const": "move_to_and_click"
				                },
				                "position_x": {
				                    "type": "integer",
				                    "description": "X coordinate to move to and click"
				                },
				                "position_y": {
				                    "type": "integer",
				                    "description": "Y coordinate to move to and click"
				                }
				            },
				            "required": ["action", "position_x", "position_y"],
				            "additionalProperties": false
				        }
				    ]
				}
				""";
	}

	@Override
	public Class<BrowserRequestVO> getInputType() {
		return BrowserRequestVO.class;
	}

	@Override
	public String getCurrentToolStateString() {
		DriverWrapper driver = getDriver();
		Map<String, Object> state = getCurrentState(driver.getCurrentPage());
		// Build URL and title information
		String urlInfo = String.format("\n   URL: %s\n   Title: %s", state.get("url"), state.get("title"));

		// Build tab information
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
		// Get scroll information
		Map<String, Object> scrollInfo = (Map<String, Object>) state.get("scroll_info");
		String contentAbove = "";
		String contentBelow = "";
		if (scrollInfo != null) {
			Long pixelsAbove = (Long) scrollInfo.get("pixels_above");
			Long pixelsBelow = (Long) scrollInfo.get("pixels_below");
			contentAbove = pixelsAbove > 0 ? String.format(" (%d pixels)", pixelsAbove) : "";
			contentBelow = pixelsBelow > 0 ? String.format(" (%d pixels)", pixelsBelow) : "";
		}

		// Get interactive element information
		String elementsInfo = (String) state.get("interactive_elements");

		// Build final status string
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

	// cleanup method already exists, just ensure it conforms to interface specification
	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up Chrome resources for plan: {}", planId);
			this.chromeDriverService.closeDriverForPlan(planId);
		}
	}

	public ManusProperties getManusProperties() {
		return (ManusProperties) this.chromeDriverService.getManusProperties();
	}

}
