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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.DriverWrapper;
import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

public abstract class BrowserAction {

	private final static Logger log = LoggerFactory.getLogger(BrowserAction.class);

	public abstract ToolExecuteResult execute(BrowserRequestVO request) throws Exception;

	private final BrowserUseTool browserUseTool;

	public BrowserAction(BrowserUseTool browserUseTool) {
		this.browserUseTool = browserUseTool;
	}

	public BrowserUseTool getBrowserUseTool() {
		return browserUseTool;
	}

	/**
	 * 获取浏览器操作的超时时间配置
	 * @return 超时时间（毫秒），如果未配置则返回默认值30秒
	 */
	protected Integer getBrowserTimeoutMs() {
		Integer timeout = getBrowserUseTool().getManusProperties().getBrowserRequestTimeout();
		return (timeout != null ? timeout : 30) * 1000; // 转换为毫秒
	}

	/**
	 * 获取浏览器操作的超时时间配置
	 * @return 超时时间（秒），如果未配置则返回默认值30秒
	 */
	protected Integer getBrowserTimeoutSec() {
		Integer timeout = getBrowserUseTool().getManusProperties().getBrowserRequestTimeout();
		return timeout != null ? timeout : 30; // 默认超时时间为 30 秒
	}

	/**
	 * 模拟人类行为
	 * @param element Playwright的ElementHandle实例
	 */
	protected void simulateHumanBehavior(ElementHandle element) {
		try {
			// 添加随机延迟
			Thread.sleep(new Random().nextInt(500) + 200);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 获取 DriverWrapper 实例
	 * @return DriverWrapper
	 */
	protected DriverWrapper getDriverWrapper() {

		return browserUseTool.getDriver();
	}

	/**
	 * 获取当前页面 Page 实例
	 * @return 当前 Playwright 的 Page 实例
	 */
	protected Page getCurrentPage() {
		DriverWrapper driverWrapper = getDriverWrapper();
		return driverWrapper.getCurrentPage();
	}

	/**
	 * 获取可交互元素
	 * @param page Playwright的Page实例
	 * @return 可交互元素列表
	 */
	protected List<InteractiveElement> getInteractiveElements(Page page) {
		DriverWrapper driverWrapper = browserUseTool.getDriver();
		return driverWrapper.getInteractiveElementRegistry().getAllElements(page);
	}

	protected String clickAndSwitchToNewTabIfOpened(Page pageToClickOn, Runnable clickLambda) {
		Page newPageFromPopup = null;
		String originalPageUrl = pageToClickOn.url();
		BrowserContext context = pageToClickOn.context();
		List<Page> pagesBeforeClick = context.pages();
		Set<String> urlsBeforeClick = pagesBeforeClick.stream().map(Page::url).collect(Collectors.toSet());

		try {
			Integer timeout = getBrowserTimeoutMs();
			Page.WaitForPopupOptions popupOptions = new Page.WaitForPopupOptions().setTimeout(Math.min(timeout, 3000));

			newPageFromPopup = pageToClickOn.waitForPopup(popupOptions, clickLambda);

			if (newPageFromPopup != null) {
				log.info("waitForPopup detected new page: {}", newPageFromPopup.url());
				if (getDriverWrapper().getCurrentPage() != newPageFromPopup) {
					getDriverWrapper().setCurrentPage(newPageFromPopup);
				}
				return "and opened in new tab: " + newPageFromPopup.url();
			}

			// Fallback if newPageFromPopup is null but no exception (unlikely for
			// waitForPopup)
			if (!pageToClickOn.isClosed() && !pageToClickOn.url().equals(originalPageUrl)) {
				log.info("Page navigated in the same tab (fallback check): {}", pageToClickOn.url());
				return "and navigated in the same tab to: " + pageToClickOn.url();
			}
			return "successfully.";

		}
		catch (TimeoutError e) {
			log.warn(
					"No popup detected by waitForPopup within timeout. Click action was performed. Checking page states...");

			List<Page> pagesAfterTimeout = context.pages();
			List<Page> newPagesByDiff = pagesAfterTimeout.stream()
				.filter(p -> !urlsBeforeClick.contains(p.url()))
				.collect(Collectors.toList());

			if (!newPagesByDiff.isEmpty()) {
				Page newlyFoundPage = newPagesByDiff.get(0);
				log.info("New tab found by diffing URLs after waitForPopup timeout: {}", newlyFoundPage.url());
				getDriverWrapper().setCurrentPage(newlyFoundPage);
				return "and opened in new tab: " + newlyFoundPage.url();
			}

			if (!pageToClickOn.isClosed() && !pageToClickOn.url().equals(originalPageUrl)) {
				if (getDriverWrapper().getCurrentPage() != pageToClickOn) {
					getDriverWrapper().setCurrentPage(pageToClickOn);
				}
				log.info("Page navigated in the same tab after timeout: {}", pageToClickOn.url());
				return "and navigated in the same tab to: " + pageToClickOn.url();
			}

			Page currentPageInWrapper = getDriverWrapper().getCurrentPage();
			if (pageToClickOn.isClosed() && currentPageInWrapper != null && !currentPageInWrapper.isClosed()
					&& !urlsBeforeClick.contains(currentPageInWrapper.url())) {
				log.info("Original page closed, current page is now: {}", currentPageInWrapper.url());
				return "and current page changed to: " + currentPageInWrapper.url();
			}
			log.info("No new tab or significant navigation detected after timeout.");
			return "successfully, but no new tab was detected by waitForPopup or URL diff.";
		}
		catch (Exception e) {
			log.error("Exception during click or popup handling: {}", e.getMessage(), e);

			List<Page> pagesAfterError = context.pages();
			List<Page> newPagesByDiffAfterError = pagesAfterError.stream()
				.filter(p -> !urlsBeforeClick.contains(p.url()))
				.collect(Collectors.toList());
			if (!newPagesByDiffAfterError.isEmpty()) {
				Page newlyFoundPage = newPagesByDiffAfterError.get(0);
				log.info("New tab found by diffing URLs after an error: {}", newlyFoundPage.url());
				getDriverWrapper().setCurrentPage(newlyFoundPage);
				return "with error '" + e.getMessage() + "' but opened new tab: " + newlyFoundPage.url();
			}
			return "with error: " + e.getMessage();
		}
	}

}
