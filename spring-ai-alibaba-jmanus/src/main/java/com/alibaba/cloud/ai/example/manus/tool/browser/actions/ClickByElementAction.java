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
import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement;
// import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class ClickByElementAction extends BrowserAction {

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClickByElementAction.class);

	public ClickByElementAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer index = request.getIndex();
		if (index == null) {
			return new ToolExecuteResult("Index is required for 'click' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例

		// 获取交互元素（InteractiveElement）
		List<InteractiveElement> interactiveElements = getInteractiveElements(page);

		if (index < 0 || index >= interactiveElements.size()) {
			return new ToolExecuteResult("Element with index " + index + " not found");
		}

		InteractiveElement element = interactiveElements.get(index);
		log.info("Clicking element: {}", element.getText());

		// 使用 Playwright 的事件监听等待新页面
		Page newPage = null;
		try {
			// 启动等待新页面事件（异步）
			com.microsoft.playwright.BrowserContext context = page.context();
			final Page[] newPageHolder = new Page[1];
			context.onPage(p -> newPageHolder[0] = p);

			// 执行点击操作
			element.getLocator().click();

			// 最多等待10秒新页面出现
			long start = System.currentTimeMillis();
			while (newPageHolder[0] == null && System.currentTimeMillis() - start < 10000) {
				Thread.sleep(100);
			}
			newPage = newPageHolder[0];
			if (newPage != null) {
				newPage.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
				log.info("New tab detected, switched to: {}", newPage.url());
				getDriverWrapper().setCurrentPage(newPage);
				refreshElements(newPage);
				return new ToolExecuteResult("Clicked element and opened in new tab: " + newPage.url());
			}
		}
		catch (Exception e) {
			log.warn("Exception while waiting for new page: {}", e.getMessage());
		}

		// 重新刷新页面元素
		refreshElements(page);
		// 如果没有明显变化，返回普通点击成功消息
		return new ToolExecuteResult("Clicked element at index " + index);
	}

}
