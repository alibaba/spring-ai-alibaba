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

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.WaitForPopupOptions;
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

		// 获取点击前的所有页面，用于后续比较
		Page newPage = null;
		try {

			BrowserContext context = page.context();
			// 获取点击前的所有页面和它们的URL
			List<Page> pagesBefore = context.pages();
			// 存储现有页面的URL集合，用于后续比较
			java.util.Set<String> existingPageUrls = new java.util.HashSet<>();
			for (Page existingPage : pagesBefore) {
				existingPageUrls.add(existingPage.url());
			}
			log.info("Pages before click: {} with URLs: {}", pagesBefore.size(), existingPageUrls);

			// 直接在当前线程中使用waitForPopup
			try {
				Integer timeout = getBrowserUseTool().getManusProperties().getBrowserRequestTimeout();
				// 设置监听器和超时
				// 设置predicate，只要有新tab弹出就立即返回
				WaitForPopupOptions options = new WaitForPopupOptions().setTimeout(2000).setPredicate(p -> true); // 只要有新tab就立即返回

				log.info("Setting up popup listener and executing click...");
				// 执行waitForPopup，它会同时监听popup事件并执行点击操作
				newPage = page.waitForPopup(options, () -> {
					try {
						log.info("Executing click action on: {}", element.getText());
						element.getLocator().click();
						log.info("Click action completed");
					}
					catch (Exception e) {
						log.error("Error during click: {}", e.getMessage());
						throw new RuntimeException(e);
					}
				});

				if (newPage != null) {
					log.info("Popup detected with URL: {}", newPage.url());
				}
			}
			catch (Exception e) {
				log.info("No popup detected or error: {}", e.getMessage());
			}

			if (newPage != null) {
				newPage.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
				log.info("New tab detected, switched to: {}", newPage.url());
				getDriverWrapper().setCurrentPage(newPage);
				return new ToolExecuteResult("Clicked element and opened in new tab: " + newPage.url());
			}
		}
		catch (Exception e) {
			log.warn("Exception while waiting for new page: {}", e.getMessage());
		}

		// 如果没有明显变化，返回普通点击成功消息
		return new ToolExecuteResult("Clicked element at index " + index);
	}

}
