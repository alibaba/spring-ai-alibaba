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

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Frame;

import com.alibaba.cloud.ai.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

public class GetElementPositionByNameAction extends BrowserAction {

	private final ObjectMapper objectMapper;

	private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetElementPositionByNameAction.class);

	public GetElementPositionByNameAction(BrowserUseTool browserUseTool, ObjectMapper objectMapper) {
		super(browserUseTool);
		this.objectMapper = objectMapper;
	}

	/**
	 * Element position information class for storing global position and text information
	 * of each matched element
	 */
	public static class ElementPosition {

		private double x; // It holds the absolute x coordinate

		private double y; // It holds the absolute y coordinate

		private String elementText; // Element text content

		// Default constructor
		public ElementPosition() {
		}

		// Constructor with only necessary fields
		public ElementPosition(double x, double y, String elementText) {
			this.x = x;
			this.y = y;
			this.elementText = elementText;
		}

		// Getters and Setters
		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
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
		boolean isDebug = getBrowserUseTool().getManusProperties().getDebugDetail();
		String elementName = request.getElementName();
		if (elementName == null || elementName.isEmpty()) {
			return new ToolExecuteResult("Element name is required for 'get_element_position' action");
		}

		Page page = getCurrentPage(); // Get Playwright Page instance

		// Set for deduplication
		Set<String> uniqueSet = new HashSet<>();
		List<ElementPosition> positionResults = new ArrayList<>();

		// Uniformly handle all frames (including main page and all iframes)
		for (Frame frame : page.frames()) {
			findAndProcessElementsByLocatorForFrame(frame, elementName, positionResults, uniqueSet, isDebug);
		}
		// Add exception handling for JSON serialization
		try {
			String resultJson = objectMapper.writeValueAsString(positionResults);
			return new ToolExecuteResult(resultJson);
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error serializing JSON: " + e.getMessage());
		}
	}

	private void findAndProcessElementsByLocatorForFrame(Frame frame, String elementName, List<ElementPosition> results,
			Set<String> uniqueSet, boolean isDebug) {

		// Only find visible elements that contain elementName in text, excluding style
		// tags
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
					// int x = (int) (box.x + box.width / 2);
					// int y = (int) (box.y + box.height / 2);
					double x = (double) box.x + (double) box.width / 2;
					double y = (double) box.y + (double) box.height / 2;
					if (isDebug) {
						// Add red border to element and display elementText in top-right
						// corner (red background, white text)
						try {
							String elementTextFinal = text.trim();
							elementTextFinal = " (" + x + "," + y + ")" + elementTextFinal;
							Object result = nthLocator.evaluate(
									"(el, text) => {\n  el.style.border = '2px solid red';\n  // Create or update top-right corner tag\n  let tag = el.querySelector('[data-element-text-tag]');\n  if (!tag) {\n    tag = document.createElement('div');\n    tag.setAttribute('data-element-text-tag', '1');\n    tag.style.position = 'absolute';\n    tag.style.top = '0';\n    tag.style.right = '0';\n    tag.style.background = 'red';\n    tag.style.color = 'white';\n    tag.style.fontSize = '12px';\n    tag.style.padding = '2px 6px';\n    tag.style.borderBottomLeftRadius = '6px';\n    tag.style.zIndex = '9999';\n    tag.style.pointerEvents = 'none';\n    tag.style.fontWeight = 'bold';\n    tag.style.maxWidth = '120px';\n    tag.style.overflow = 'hidden';\n    tag.style.textOverflow = 'ellipsis';\n    tag.style.whiteSpace = 'nowrap';\n    el.style.position = el.style.position || 'relative';\n    el.appendChild(tag);\n  }\n  tag.textContent = text;\n}",
									elementTextFinal);

							log.info("Debug: Added red border and text tag for element. result: {}, x: {}, y: {}",
									result, x, y);
						}
						catch (Exception e) {
							// ignore style set error
						}
					}
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
