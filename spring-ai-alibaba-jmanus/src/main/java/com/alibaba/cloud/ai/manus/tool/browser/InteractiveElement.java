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
package com.alibaba.cloud.ai.manus.tool.browser;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Class representing a single interactive element, holding the element's Locator.
 */
public class InteractiveElement {

	private static final Logger log = LoggerFactory.getLogger(InteractiveElement.class);

	// Global index
	private int index;

	// Element locator
	private Locator locator;

	// Element type information
	private String tagName;

	// Element text information (if any)
	private String text;

	// HTML structure information
	private String outerHtml;

	// Frame text
	private String frameText;

	/**
	 * Construct an InteractiveElement instance
	 * @param index Global index
	 * @param frame Frame where the element is located
	 * @param elementMap Other parameters of the element
	 * @param frameText
	 */
	public InteractiveElement(int index, Frame frame, Map<String, Object> elementMap, String frameText) {
		this.index = index;
		if (elementMap.containsKey("jManusId")) {
			String jManusId = (String) elementMap.get("jManusId");
			this.locator = frame.locator("[jmanus-id=\"" + jManusId + "\"]");
		}
		else {
			String xpath = (String) elementMap.get("xpath");
			this.locator = frame.locator("//" + xpath);
		}
		this.tagName = (String) elementMap.get("tagName");
		this.text = (String) elementMap.get("text");
		this.outerHtml = (String) elementMap.get("outerHtml");
		this.frameText = frameText;
	}

	/**
	 * Get the global index of the element
	 * @return Element index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Get the element's Locator
	 * @return Element locator
	 */
	public Locator getLocator() {
		return locator;
	}

	/**
	 * Get the element's tag name
	 * @return Element's HTML tag name
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * Get the element's text content
	 * @return Element text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Get the element's HTML structure
	 * @return Element's outerHTML
	 */
	public String getOuterHtml() {
		return outerHtml;
	}

	public String getFrameText() {
		return frameText;
	}

	/**
	 * Create a string representation of element information
	 * @return Formatted element information
	 */
	@Override
	public String toString() {
		String content = text.isEmpty() ? outerHtml : text;

		// If using outerHtml, remove jmanus-id attribute and style attribute
		if (text.isEmpty() && content != null) {
			content = content.replaceAll("\\s+jmanus-id=\"[^\"]*\"", "");
			content = content.replaceAll("\\s+style=\"[^\"]*\"", "");
		}

		if (content.length() > 500) {
			content = content.substring(0, 500) + " ... " + content.substring(content.length() - 100);
		}
		return String.format("[%d] %s: %s", index, tagName, content);
	}

}
