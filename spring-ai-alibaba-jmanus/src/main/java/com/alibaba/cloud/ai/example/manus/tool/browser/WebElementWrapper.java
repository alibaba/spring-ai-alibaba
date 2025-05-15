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
package com.alibaba.cloud.ai.example.manus.tool.browser;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;

/**
 * WebElement的包装类，同时包含元素本身和元素的详细信息。 这个类使得在处理网页元素时能够一次获取ElementHandle及其相关信息。
 */
public class WebElementWrapper {

	private final ElementHandle element;

	private final String elementInfoString;

	private Frame iframeElement; // 元素所属的iframe元素，如果在主文档中则为null

	private String iframePath; // iframe的路径，如"0/2/1"表示第一个iframe中的第三个iframe中的第二个iframe

	/**
	 * 构造一个新的WebElementWrapper实例
	 * @param element ElementHandle对象
	 * @param elementInfoString 元素的详细信息字符串
	 */
	public WebElementWrapper(ElementHandle element, String elementInfoString) {
		this.element = element;
		this.elementInfoString = elementInfoString;
		this.iframeElement = null;
		this.iframePath = null;
	}

	/**
	 * 构造一个新的WebElementWrapper实例，包含iframe信息
	 * @param element ElementHandle对象
	 * @param elementInfoString 元素的详细信息字符串
	 * @param iframeElement 元素所属的iframe元素
	 * @param iframePath iframe的路径
	 */
	public WebElementWrapper(ElementHandle element, String elementInfoString, Frame iframeElement, String iframePath) {
		this.element = element;
		this.elementInfoString = elementInfoString;
		this.iframeElement = iframeElement;
		this.iframePath = iframePath;
	}

	/**
	 * 获取包装的ElementHandle对象
	 * @return ElementHandle对象
	 */
	public ElementHandle getElement() {
		return element;
	}

	/**
	 * 获取元素的详细信息字符串
	 * @return 元素详细信息字符串
	 */
	public String getElementInfoString() {
		return elementInfoString;
	}

	/**
	 * 获取元素所属的iframe元素
	 * @return 元素所属的iframe元素，如果在主文档中则为null
	 */
	public Frame getIframeElement() {
		return iframeElement;
	}

	/**
	 * 设置元素所属的iframe元素
	 * @param iframeElement 元素所属的iframe元素
	 */
	public void setIframeElement(Frame iframeElement) {
		this.iframeElement = iframeElement;
	}

	/**
	 * 获取iframe的路径
	 * @return iframe的路径，如"0/2/1"表示第一个iframe中的第三个iframe中的第二个iframe
	 */
	public String getIframePath() {
		return iframePath;
	}

	/**
	 * 设置iframe的路径
	 * @param iframePath iframe的路径
	 */
	public void setIframePath(String iframePath) {
		this.iframePath = iframePath;
	}

	/**
	 * 在交互前切换到正确的iframe上下文
	 * @param page Playwright的Page实例
	 */
	public void prepareForInteraction(Page page) {
		if (iframePath != null && !iframePath.isEmpty()) {
			// 首先切换到顶层文档
			Frame currentFrame = page.mainFrame();

			// 按路径依次切换iframe
			String[] indices = iframePath.split("/");
			for (String index : indices) {
				int frameIndex = Integer.parseInt(index);
				currentFrame = currentFrame.childFrames().get(frameIndex);
			}

			this.iframeElement = currentFrame;
		}
	}

	/**
	 * 获取元素的简短描述，可用于日志记录和调试
	 * @return 元素的字符串表示
	 */
	@Override
	public String toString() {
		return "WebElementWrapper{" + "element=" + element + ", elementInfo='" + elementInfoString + '\''
				+ ", iframePath='" + iframePath + '\'' + '}';
	}

}
