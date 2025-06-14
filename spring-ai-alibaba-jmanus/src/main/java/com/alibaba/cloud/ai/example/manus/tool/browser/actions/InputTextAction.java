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

import com.microsoft.playwright.Locator;
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

		// 获取交互元素（InteractiveElement），支持所有 frame（包括 iframe）
		List<InteractiveElement> interactiveElements = getInteractiveElements(page); // 已支持递归
																						// frame
		if (index < 0 || index >= interactiveElements.size()) {
			return new ToolExecuteResult("Element with index " + index + " not found");
		}

		InteractiveElement inputElement = interactiveElements.get(index);

		String tagName = inputElement.getTagName();
		if (!"input".equals(tagName) && !"textarea".equals(tagName)) {
			return new ToolExecuteResult("Element at index " + index + " is not an input element");
		}

		// 获取元素定位器
		Locator elementLocator = inputElement.getLocator();
		// 3. 尝试 fill
		try {
			elementLocator.fill(""); // 先清空
			// 设置每个字符输入间隔 100ms，可根据需要调整
			com.microsoft.playwright.Locator.PressSequentiallyOptions options = new com.microsoft.playwright.Locator.PressSequentiallyOptions()
				.setDelay(100);
			elementLocator.pressSequentially(text, options);

		}
		catch (Exception e) {
			// 4. fill 失败，尝试 pressSequentially
			try {
				elementLocator.fill(""); // 再清空一次
				elementLocator.fill(text); // 直接填充
			}
			catch (Exception e2) {
				// 5. 还不行，直接用 JS 赋值并触发 input 事件
				try {
					elementLocator.evaluate(
							"(el, value) => { el.value = value; el.dispatchEvent(new Event('input', { bubbles: true })); }",
							text);
				}
				catch (Exception e3) {
					return new ToolExecuteResult("输入失败: " + e3.getMessage());
				}
			}
		}
		return new ToolExecuteResult("成功输入: '" + text + "' 到指定的对象.其索引编号为 ： " + index);
	}

}
