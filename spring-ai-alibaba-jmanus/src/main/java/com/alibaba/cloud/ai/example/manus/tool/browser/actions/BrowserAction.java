
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

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.DriverWrapper;
import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

public abstract class BrowserAction {

	public abstract ToolExecuteResult execute(BrowserRequestVO request) throws Exception;

	private final BrowserUseTool browserUseTool;

	public BrowserAction(BrowserUseTool browserUseTool) {
		this.browserUseTool = browserUseTool;
	}

	public BrowserUseTool getBrowserUseTool() {
		return browserUseTool;
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

}
