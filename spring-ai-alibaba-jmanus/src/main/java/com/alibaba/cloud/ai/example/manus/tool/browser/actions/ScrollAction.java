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

import com.microsoft.playwright.Page;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class ScrollAction extends BrowserAction {

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScrollAction.class);

	public ScrollAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer scrollAmount = request.getScrollAmount();

		if (scrollAmount == null) {
			return new ToolExecuteResult("Scroll amount is required for 'scroll' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		page.evaluate("window.scrollBy(0, arguments[0])", scrollAmount); // 使用 Playwright
																			// 执行滚动

		String direction = scrollAmount > 0 ? "down" : "up";
		return new ToolExecuteResult("Scrolled " + direction + " by " + Math.abs(scrollAmount) + " pixels");
	}

}
