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

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;

/**
 * A wrapper class for WebElement, which contains both the element itself and its detailed
 * information. This class allows obtaining an ElementHandle and its related information
 * in one operation when processing web elements.
 */
public class WebElementWrapper {

	private final ElementHandle element;

	private final String elementInfoString;

	private Frame iframeElement; // The iframe element that the element belongs to, or
									// null if in the main document

	private String iframePath; // The path of the iframe, such as "0/2/1" representing the
								// second iframe in the third iframe in the first iframe

	/**
	 * Construct a new WebElementWrapper instance
	 * @param element ElementHandle object
	 * @param elementInfoString The detailed information string of the element
	 */
	public WebElementWrapper(ElementHandle element, String elementInfoString) {
		this.element = element;
		this.elementInfoString = elementInfoString;
		this.iframeElement = null;
		this.iframePath = null;
	}

	/**
	 * Construct a new WebElementWrapper instance, including iframe information
	 * @param element ElementHandle object
	 * @param elementInfoString The detailed information string of the element
	 * @param iframeElement The iframe element that the element belongs to
	 * @param iframePath The path of the iframe, such as "0/2/1" representing the second
	 * iframe in the third iframe in the first iframe
	 */
	public WebElementWrapper(ElementHandle element, String elementInfoString, Frame iframeElement, String iframePath) {
		this.element = element;
		this.elementInfoString = elementInfoString;
		this.iframeElement = iframeElement;
		this.iframePath = iframePath;
	}

	/**
	 * Get the wrapped ElementHandle object
	 * @return ElementHandle object
	 */
	public ElementHandle getElement() {
		return element;
	}

	/**
	 * Get the detailed information string of the element
	 * @return The detailed information string of the element
	 */
	public String getElementInfoString() {
		return elementInfoString;
	}

	/**
	 * Get the iframe element that the element belongs to
	 * @return The iframe element that the element belongs to, or null if in the main
	 * document
	 */
	public Frame getIframeElement() {
		return iframeElement;
	}

	/**
	 * Set the iframe element that the element belongs to
	 * @param iframeElement The iframe element that the element belongs to
	 */
	public void setIframeElement(Frame iframeElement) {
		this.iframeElement = iframeElement;
	}

	/**
	 * Get the path of the iframe
	 * @return The path of the iframe, such as "0/2/1" representing the second iframe in
	 * the third iframe in the first iframe
	 */
	public String getIframePath() {
		return iframePath;
	}

	/**
	 * Set the path of the iframe
	 * @param iframePath The path of the iframe
	 */
	public void setIframePath(String iframePath) {
		this.iframePath = iframePath;
	}

	/**
	 * Switch to the correct iframe context before interaction
	 * @param page The Playwright Page instance
	 */
	public void prepareForInteraction(Page page) {
		if (iframePath != null && !iframePath.isEmpty()) {
			// First switch to the top-level document
			Frame currentFrame = page.mainFrame();

			// Switch to the iframe according to the path
			String[] indices = iframePath.split("/");
			for (String index : indices) {
				int frameIndex = Integer.parseInt(index);
				currentFrame = currentFrame.childFrames().get(frameIndex);
			}

			this.iframeElement = currentFrame;
		}
	}

	/**
	 * Get a short description of the element, which can be used for logging and debugging
	 * @return The string representation of the element
	 */
	@Override
	public String toString() {
		return "WebElementWrapper{" + "element=" + element + ", elementInfo='" + elementInfoString + '\''
				+ ", iframePath='" + iframePath + '\'' + '}';
	}

}
