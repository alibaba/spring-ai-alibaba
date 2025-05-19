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
						// 高亮该元素（带边框和中心点+文本）
						try {
							String highlightScript = String.format("""
										(function() {
											var box = {left: %f, top: %f, width: %f, height: %f};
											var x = %d, y = %d, label = %s;
											var highlight = document.createElement('div');
											highlight.style.position = 'fixed';
											highlight.style.left = box.left + 'px';
											highlight.style.top = box.top + 'px';
											highlight.style.width = box.width + 'px';
											highlight.style.height = box.height + 'px';
											highlight.style.border = '3px solid #ff0000';
											highlight.style.background = 'rgba(255,0,0,0.08)';
											highlight.style.zIndex = 999999;
											highlight.style.pointerEvents = 'none';
											highlight.style.boxSizing = 'border-box';
											// 中心点
											var center = document.createElement('div');
											center.style.position = 'fixed';
											center.style.left = (x - 8) + 'px';
											center.style.top = (y - 8) + 'px';
											center.style.width = '16px';
											center.style.height = '16px';
											center.style.borderRadius = '50%';
											center.style.background = '#ff0000';
											center.style.zIndex = 1000000;
											center.style.pointerEvents = 'none';
											// 文本标签
											var tag = document.createElement('div');
											tag.style.position = 'fixed';
											tag.style.left = (x + 10) + 'px';
											tag.style.top = (y - 18) + 'px';
											tag.style.color = '#fff';
											tag.style.background = 'rgba(255,0,0,0.85)';
											tag.style.padding = '2px 8px';
											tag.style.fontSize = '14px';
											tag.style.borderRadius = '6px';
											tag.style.zIndex = 1000001;
											tag.style.pointerEvents = 'none';
											tag.innerText = label;
											document.body.appendChild(highlight);
											document.body.appendChild(center);
											document.body.appendChild(tag);
											setTimeout(function() {
												try { document.body.removeChild(highlight); } catch(e){}
												try { document.body.removeChild(center); } catch(e){}
												try { document.body.removeChild(tag); } catch(e){}
											}, 1200);
										})();
									""", box.x, box.y, box.width, box.height, x, y, JSON.toJSONString(elementText));
							frame.evaluate(highlightScript);
						}
						catch (Exception e) {
							// 忽略高亮异常
						}
					}
				}
			}
		}
	}

}
