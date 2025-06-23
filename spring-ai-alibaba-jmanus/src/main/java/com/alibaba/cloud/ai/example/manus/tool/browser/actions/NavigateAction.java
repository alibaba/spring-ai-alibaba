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
import com.microsoft.playwright.Page.WaitForLoadStateOptions;
import com.microsoft.playwright.options.LoadState;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class NavigateAction extends BrowserAction {

	public NavigateAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		String url = request.getUrl();
		Integer timeoutMs = getBrowserTimeoutMs();

		if (url == null) {
			return new ToolExecuteResult("URL is required for 'navigate' action");
		}
		// 自动补全 URL 前缀
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "https://" + url;
		}
		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		page.navigate(url, new Page.NavigateOptions().setTimeout(timeoutMs));

		// 在调用 page.content() 之前，确保页面已完全加载
		page.waitForLoadState(LoadState.DOMCONTENTLOADED, new WaitForLoadStateOptions().setTimeout(timeoutMs));

		return new ToolExecuteResult("successfully navigated to " + url);
	}

}
