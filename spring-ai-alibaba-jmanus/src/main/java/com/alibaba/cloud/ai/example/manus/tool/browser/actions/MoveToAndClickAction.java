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
package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

// import java.util.List; // Already imported by BrowserAction or not needed directly

import com.microsoft.playwright.Page;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class MoveToAndClickAction extends BrowserAction {

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MoveToAndClickAction.class);

	public MoveToAndClickAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Double x = request.getPositionX();
		Double y = request.getPositionY();

		if (x == null || y == null) {
			return new ToolExecuteResult("X and Y coordinates are required for 'move_to_and_click' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		boolean isDebug = getBrowserUseTool().getManusProperties().getBrowserDebug();

		String clickResultMessage = clickAndSwitchToNewTabIfOpened(page, () -> {
			try {
				// 1. 滚动到目标位置（让目标点尽量在视窗中央）

				Object result = null;
				String markerId = "__move_click_marker__";
				if (isDebug) {
					// 2. 注入大红点（仅debug模式）
					result = page.evaluate(
							"(args) => {\n" + "  const [x, y, id] = args;\n"
									+ "  let dot = document.getElementById(id);\n" + "  if (!dot) {\n"
									+ "    dot = document.createElement('div');\n" + "    dot.id = id;\n"
									+ "    dot.style.position = 'absolute';\n" + "    dot.style.left = (x) + 'px';\n"
									+ "    dot.style.top = (y) + 'px';\n" + "    dot.style.width = '24px';\n"
									+ "    dot.style.height = '24px';\n" + "    dot.style.background = 'red';\n"
									+ "    dot.style.borderRadius = '50%';\n" + "    dot.style.zIndex = 99999;\n"
									+ "    dot.style.boxShadow = '0 0 8px 4px #f00';\n"
									+ "    dot.style.pointerEvents = 'none';\n"
									+ "    document.body.appendChild(dot);\n" + "  }\n" + "}",
							new Object[] { x, y, markerId });
					if (result != null) {
						log.info("Debug: Created red dot at position ({}, {}) , result <{}>", x, y, result);
					}
					else {
						log.warn("Debug: Failed to create red dot at position ({}, {}) , result <{}>", x, y, result);
					}
					log.info("Element at position ({}, {}): {}", x, y);
				}

				page.mouse().click(x, y);
				log.info("Clicked at position ({}, {})", x, y);

			}
			catch (Exception e) {
				log.error("Failed to move to and click at position ({}, {}): {}", x, y, e.getMessage(), e);
				// Let the common method handle the result string for errors.
				// The clickAndSwitchToNewTabIfOpened method expects a Runnable that might
				// throw.
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException("Failed to move to and click at position (" + x + ", " + y + ")", e);
			}
		});

		return new ToolExecuteResult("Clicked at position (" + x + ", " + y + ") " + clickResultMessage);
	}

}
