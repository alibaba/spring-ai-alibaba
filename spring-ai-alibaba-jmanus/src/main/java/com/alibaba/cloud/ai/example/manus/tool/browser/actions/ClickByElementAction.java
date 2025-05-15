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

		// 获取点击前的所有页面，用于后续比较
		Page newPage = null;
		try {
			com.microsoft.playwright.BrowserContext context = page.context();
			// 获取点击前的所有页面和它们的URL
			List<Page> pagesBefore = context.pages();
			// 存储现有页面的URL集合，用于后续比较
			java.util.Set<String> existingPageUrls = new java.util.HashSet<>();
			for (Page existingPage : pagesBefore) {
				existingPageUrls.add(existingPage.url());
			}
			log.info("Pages before click: {} with URLs: {}", pagesBefore.size(), existingPageUrls);
			
			// 执行点击操作
			element.getLocator().click();
			
			// 等待并检查是否有新页面
			long start = System.currentTimeMillis();
			while (System.currentTimeMillis() - start < 10000) { // 最多等待10秒
				List<Page> pagesAfter = context.pages();
				
				// 如果页面数量增加，寻找是哪个新页面
				if (pagesAfter.size() > pagesBefore.size()) {
					// 检查每个页面，找出URL不在原来集合中的页面
					for (Page candidatePage : pagesAfter) {
						String url = candidatePage.url();
						// 如果这个URL不在之前的集合中，那么这是一个新页面
						if (!existingPageUrls.contains(url)) {
							newPage = candidatePage;
							log.info("New page detected with URL: {}", url);
							break;
						}
					}
					// 如果找到了新页面，退出循环
					if (newPage != null) {
						break;
					}
				}
				Thread.sleep(100);
			}
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
