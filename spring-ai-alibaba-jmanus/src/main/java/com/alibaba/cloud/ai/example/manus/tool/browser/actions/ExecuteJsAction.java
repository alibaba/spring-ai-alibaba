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

public class ExecuteJsAction extends BrowserAction {

	public ExecuteJsAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		String script = request.getScript();
		if (script == null) {
			return new ToolExecuteResult("Script is required for 'execute_js' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		Object result = page.evaluate(script);

		if (result == null) {
			return new ToolExecuteResult("Successfully executed JavaScript code.");
		}
		else {
			return new ToolExecuteResult(result.toString());
		}
	}

}
