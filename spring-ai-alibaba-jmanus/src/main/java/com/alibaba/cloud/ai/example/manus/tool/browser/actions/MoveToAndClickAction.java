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

import java.util.List;

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
		Integer x = request.getPositionX();
		Integer y = request.getPositionY();

		if (x == null || y == null) {
			return new ToolExecuteResult("X and Y coordinates are required for 'move_to_and_click' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例

		// 记录点击前的窗口状态
		List<String> beforeWindowHandles = page.context().pages().stream().map(Page::url).toList();
		boolean isDebug = getBrowserUseTool().getManusProperties().getBrowserDebug();
		try {
			// 1. 滚动到目标位置（让目标点尽量在视窗中央）
			int scrollX = Math.max(0, x - page.viewportSize().width / 2);
			int scrollY = Math.max(0, y - page.viewportSize().height / 2);
			page.evaluate("(args) => window.scrollTo({left: args[0], top: args[1], behavior: 'instant'})",
					new Object[] { scrollX, scrollY });

			String markerId = "__move_click_marker__";
			if (isDebug) {
				// 2. 注入大红点（仅debug模式）
				page.evaluate("(args) => {\n" + "  const [x, y, id] = args;\n"
						+ "  let dot = document.getElementById(id);\n" + "  if (!dot) {\n"
						+ "    dot = document.createElement('div');\n" + "    dot.id = id;\n"
						+ "    dot.style.position = 'absolute';\n" + "    dot.style.left = x + 'px';\n"
						+ "    dot.style.top = y + 'px';\n" + "    dot.style.width = '24px';\n"
						+ "    dot.style.height = '24px';\n" + "    dot.style.background = 'red';\n"
						+ "    dot.style.borderRadius = '50%';\n" + "    dot.style.zIndex = 99999;\n"
						+ "    dot.style.boxShadow = '0 0 8px 4px #f00';\n" + "    dot.style.pointerEvents = 'none';\n"
						+ "    document.body.appendChild(dot);\n" + "  }\n" + "}", new Object[] { x, y, markerId });
			}

			// 3. 鼠标移动并点击
			page.mouse().move(x, y);
			page.mouse().click(x, y);
			log.info("Clicked at position ({}, {})", x, y);

			if (isDebug) {
				// 4. 移除大红点（仅debug模式）
				page.evaluate("(id) => { const dot = document.getElementById(id); if (dot) dot.remove(); }",
						new Object[] { markerId });
			}
		}
		catch (Exception e) {
			log.error("Failed to click at position ({}, {})", x, y, e);
			return new ToolExecuteResult("Failed to click at position (" + x + ", " + y + ")");
		}

		// 检查是否有新窗口打开
		List<String> afterWindowHandles = page.context().pages().stream().map(Page::url).toList();
		if (afterWindowHandles.size() > beforeWindowHandles.size()) {
			// 找出新打开的窗口
			afterWindowHandles.removeAll(beforeWindowHandles);
			String newHandle = afterWindowHandles.get(0);

			// 切换到新窗口
			Page newPage = page.context()
				.pages()
				.stream()
				.filter(p -> p.url().equals(newHandle))
				.findFirst()
				.orElse(null);
			if (newPage != null) {
				log.info("New tab detected, switched to: {}", newPage.url());
				return new ToolExecuteResult(
						"Clicked at position (" + x + ", " + y + ") and opened in new tab: " + newPage.url());
			}
		}
		// 如果没有明显变化，返回普通点击成功消息
		return new ToolExecuteResult("Clicked at position (" + x + ", " + y + ")");
	}

}
