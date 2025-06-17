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

		String clickResultMessage = clickAndSwitchToNewTabIfOpened(page, () -> {
			try {
				log.info("Executing click action on: {}", element.getText());
				element.getLocator().click();
				log.info("Click action completed for element: {}", element.getText());
			}
			catch (Exception e) {
				log.error("Error during click on element {}: {}", element.getText(), e.getMessage());
				// It's important to rethrow or handle appropriately.
				// The clickAndSwitchToNewTabIfOpened method expects a Runnable that might
				// throw.
				// If it's a checked exception not declared by Runnable.run(), wrap it.
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException("Error clicking element: " + element.getText(), e);
			}
		});

		return new ToolExecuteResult("Successfully clicked element at index " + index + " " + clickResultMessage);
	}

}
