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

public class SwitchTabAction extends BrowserAction {

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SwitchTabAction.class);

	public SwitchTabAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer tabId = request.getTabId();
		if (tabId == null || tabId < 0) {
			return new ToolExecuteResult("Tab ID is out of range for 'switch_tab' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		Page targetPage = page.context().pages().get(tabId); // 切换到指定的标签页
		if (targetPage == null) {
			return new ToolExecuteResult("Tab ID " + tabId + " does not exist");
		}
		return new ToolExecuteResult("Successfully switched to tab " + tabId);
	}

}
