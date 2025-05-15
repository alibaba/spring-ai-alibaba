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

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class InputTextAction extends BrowserAction {

	public InputTextAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer index = request.getIndex();
		String text = request.getText();

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例
		if (index == null || text == null) {
			return new ToolExecuteResult("Index and text are required for 'input_text' action");
		}

		// 获取交互元素（InteractiveElement）
		List<InteractiveElement> interactiveElements = getInteractiveElements(page);
		if (index < 0 || index >= interactiveElements.size()) {
			return new ToolExecuteResult("Element with index " + index + " not found");
		}

		com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveElement inputElement = interactiveElements
			.get(index);
		String tagName = inputElement.getTagName();
		if (!"input".equals(tagName) && !"textarea".equals(tagName)) {
			return new ToolExecuteResult("Element at index " + index + " is not an input element");
		}

		typeWithHumanDelay(inputElement.getLocator().elementHandle(), text);
		// 直接通过 InteractiveElementRegistry 刷新缓存，避免使用已废弃方法
		refreshElements(page);
		return new ToolExecuteResult("成功输入: '" + text + "' 到指定的对象, 对象当前状态为 ：  " + inputElement.getOuterHtml() + " 。 其索引编号为 ： " + index);
	}

	private void typeWithHumanDelay(ElementHandle element, String text) {
		// 模拟人类输入速度
		Random random = new Random();
		for (char c : text.toCharArray()) {
			element.evaluate("(el, char) => el.value += char", String.valueOf(c)); // 使用
																					// evaluate
																					// 模拟输入
			try {
				Thread.sleep(random.nextInt(100) + 50);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
