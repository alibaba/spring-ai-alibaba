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

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 表示单个交互式元素的类，持有元素的Locator。
 */
public class InteractiveElement {

	private static final Logger log = LoggerFactory.getLogger(InteractiveElement.class);

	// 全局索引
	private final int index;

	// 元素定位器
	private final Locator locator;

	// 元素类型信息
	private final String tagName;

	// 元素文本信息（如果有）
	private String text;

	// HTML结构信息
	private String outerHtml;

	/**
	 * @param index 全局索引
	 * @param frame 元素所在的frame
	 * @param elementMap 元素的其余参数
	 */
	public InteractiveElement(int index, Frame frame, Map<String, Object> elementMap) {
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
	}

	/**
	 * 获取元素的全局索引
	 * @return 元素索引
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * 获取元素的Locator
	 * @return 元素定位器
	 */
	public Locator getLocator() {
		return locator;
	}

	/**
	 * 获取元素的标签名
	 * @return 元素的HTML标签名
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * 获取元素的文本内容
	 * @return 元素文本
	 */
	public String getText() {
		return text;
	}

	/**
	 * 获取元素的HTML结构
	 * @return 元素的outerHTML
	 */
	public String getOuterHtml() {
		return outerHtml;
	}

	/**
	 * 创建元素信息的字符串表示形式
	 * @return 格式化的元素信息
	 */
	@Override
	public String toString() {
		String content = text.isEmpty() ? outerHtml : text;

		// 如果使用的是outerHtml，移除jmanus-id属性和style属性
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
