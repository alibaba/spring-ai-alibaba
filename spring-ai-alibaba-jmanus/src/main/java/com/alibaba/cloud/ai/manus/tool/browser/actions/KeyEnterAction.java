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

import com.alibaba.cloud.ai.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.manus.tool.browser.InteractiveElement;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

public class KeyEnterAction extends BrowserAction {

	public KeyEnterAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		Integer index = request.getIndex();
		if (index == null) {
			return new ToolExecuteResult("Index is required for 'key_enter' action");
		}
		InteractiveElement enterElement = getInteractiveElement(index);
		if (enterElement == null) {
			return new ToolExecuteResult("Element with index " + index + " not found");
		}
		// Execute the enter operation
		try {
			enterElement.getLocator().press("Enter");
		}
		catch (Exception e) {
			return new ToolExecuteResult("Failed to press Enter on element at index " + index + ": " + e.getMessage());
		}
		return new ToolExecuteResult("Hit the enter key at index " + index);
	}

}
