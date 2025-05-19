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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Frame;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

public class GetElementPositionByNameAction extends BrowserAction {

	public GetElementPositionByNameAction(BrowserUseTool browserUseTool) {
		super(browserUseTool);
	}

	/**
	 * 元素位置信息类，用于存储每个匹配元素的全局位置和文本信息
	 */
	public static class ElementPosition {

		private int x; // It holds the absolute x coordinate

		private int y; // It holds the absolute y coordinate

		private String elementText; // Element text content

		// 构造函数
		public ElementPosition() {
		}

		// 构造函数，只包含必要字段
		public ElementPosition(int x, int y, String elementText) {
			this.x = x;
			this.y = y;
			this.elementText = elementText;
		}

		// Getters and Setters
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public String getElementText() {
			return elementText;
		}

		public void setElementText(String elementText) {
			this.elementText = elementText;
		}

	}

	@Override
	public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
		boolean isDebug = getBrowserUseTool().getManusProperties().getBrowserDebug();
		String elementName = request.getElementName();
		if (elementName == null || elementName.isEmpty()) {
			return new ToolExecuteResult("Element name is required for 'get_element_position' action");
		}

		Page page = getCurrentPage(); // 获取 Playwright 的 Page 实例

		// 用于去重的集合
		Set<String> uniqueSet = new HashSet<>();
		List<ElementPosition> positionResults = new ArrayList<>();

		// 统一处理所有 frame（包括主页面和所有iframe）
		for (Frame frame : page.frames()) {
			findAndProcessElementsByLocatorForFrame(frame, elementName, positionResults, uniqueSet, isDebug);
		}
		String resultJson = JSON.toJSONString(positionResults);
		return new ToolExecuteResult(resultJson);
	}

	private void findAndProcessElementsByLocatorForFrame(Frame frame, String elementName, List<ElementPosition> results,
			Set<String> uniqueSet, boolean isDebug) {
		// 只查找可见且文本包含elementName的元素，不包含style标签
		com.microsoft.playwright.Locator locator = frame.getByText(elementName);
		int count = locator.count();
		for (int i = 0; i < count; i++) {
			com.microsoft.playwright.Locator nthLocator = locator.nth(i);
			String text = null;
			try {
				text = nthLocator.textContent();
			}
			catch (Exception e) {
				continue;
			}
			if (text != null) {
				com.microsoft.playwright.options.BoundingBox box = null;
				try {
					box = nthLocator.boundingBox();
				}
				catch (Exception e) {
					continue;
				}
				if (box != null) {
					int x = (int) (box.x + box.width / 2);
					int y = (int) (box.y + box.height / 2);
					String elementText = text.trim();
					String uniqueKey = x + "," + y + "," + elementText;
					if (!uniqueSet.contains(uniqueKey)) {
						ElementPosition position = new ElementPosition(x, y, elementText);
						results.add(position);
						uniqueSet.add(uniqueKey);
					}
				}
			}
		}
	}

}
