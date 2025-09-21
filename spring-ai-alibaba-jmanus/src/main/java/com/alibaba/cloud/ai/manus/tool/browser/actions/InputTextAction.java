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
package com.alibaba.cloud.ai.manus.tool.browser.actions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.alibaba.cloud.ai.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

public class InputTextAction extends BrowserAction {

	public InputTextAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer index = request.getIndex();
		String text = request.getText();

		Page page = getCurrentPage(); // Get Playwright Page instance
		if (index == null || text == null) {
			return new ToolExecuteResult("Index and text are required for 'input_text' action");
		}

		// Get interactive elements (InteractiveElement), supports all frames (including
		// iframe)
		// Already supports recursion frame
		InteractiveElement inputElement = getInteractiveElement(index);
		if (inputElement == null) {
			return new ToolExecuteResult("Element with index " + index + " not found");
		}
		String tagName = inputElement.getTagName();
		if (!"input".equals(tagName) && !"textarea".equals(tagName)) {
			return new ToolExecuteResult("Element at index " + index + " is not an input element");
		}
		// Get element locator
		Locator elementLocator = inputElement.getLocator();
		// 3. Try fill
		try {
			elementLocator.fill(""); // Clear first
			// Set character input delay to 100ms, adjustable as needed
			Locator.PressSequentiallyOptions options = new Locator.PressSequentiallyOptions().setDelay(100);
			elementLocator.pressSequentially(text, options);
		}
		catch (Exception e) {
			// 4. If fill fails, try pressSequentially
			try {
				elementLocator.fill(""); // Clear again
				elementLocator.fill(text); // Direct fill
			}
			catch (Exception e2) {
				// 5. If still fails, use JS assignment and trigger input event
				try {
					elementLocator.evaluate(
							"(el, value) => { el.value = value; el.dispatchEvent(new Event('input', { bubbles: true })); }",
							text);
				}
				catch (Exception e3) {
					return new ToolExecuteResult("Input failed: " + e3.getMessage());
				}
			}
		}
		return new ToolExecuteResult(
				"Successfully input: '" + text + "' to the specified element with index: " + index);
	}

}
