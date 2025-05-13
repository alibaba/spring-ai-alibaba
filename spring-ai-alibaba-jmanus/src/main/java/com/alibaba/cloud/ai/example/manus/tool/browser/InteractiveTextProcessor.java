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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门处理网页中交互式文本元素的工具类。 提供了查找、分析和处理网页中文本内容的功能。
 */
public class InteractiveTextProcessor {

	private static final Logger log = LoggerFactory.getLogger(InteractiveTextProcessor.class);
	
	/**
	 * 用于存储交互式元素的缓存列表
	 */
	private List<ElementHandle> interactiveElementsCache;

	/**
	 * 用于选择交互式元素的CSS选择器
	 */
	private static final String INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea[type='search'], textarea, "
			+ "[role='button'], [role='link'], [role='textbox'], [role='search'], [role='searchbox']";

	public void refreshCache(Page page) {
		// 清空现有缓存
		this.interactiveElementsCache = new ArrayList<>();

		// 等待页面完全加载
		waitForPageLoad(page);

		// 先获取主文档中的元素
		List<ElementHandle> mainDocElements = getInteractiveElementsInner(page);
		this.interactiveElementsCache.addAll(mainDocElements);

		// 获取并处理所有iframe中的元素
		processIframes(page);
	}

	/**
	 * 等待页面完全加载，包括iframe和动态内容
	 * @param page Page实例
	 */
	private void waitForPageLoad(Page page) {
		try {
			page.waitForLoadState(); // 等待页面达到'load'状态
			log.info("页面加载成功");
		} catch (Exception e) {
			log.warn("等待页面加载时出错: {}", e.getMessage());
		}
	}

	/**
	 * 递归处理页面中的所有iframe元素
	 * @param page Page实例
	 */
	private void processIframes(Page page) {
		List<Frame> frames = page.frames();
		log.info("找到 {} 个iframe", frames.size());
		for (Frame frame : frames) {
			try {
				// 获取iframe中的交互元素并添加到缓存
				List<ElementHandle> iframeElements = getInteractiveElementsInner(frame);
				this.interactiveElementsCache.addAll(iframeElements);
			} catch (Exception e) {
				log.warn("处理iframe时出错: {}", e.getMessage());
			}
		}
	}

	/**
	 * 获取网页中所有可交互的元素
	 * @param page Page实例
	 * @return 包装后的可交互元素列表
	 */
	public List<ElementHandle> getInteractiveElements(Page page) {
		if (interactiveElementsCache == null) {
			refreshCache(page);
		}
		return interactiveElementsCache;
	}

	/**
	 * 刷新缓存中的可交互元素
	 * @param page Page实例
	 * @return 当前文档中的交互元素列表
	 */
	private List<ElementHandle> getInteractiveElementsInner(Page page) {
		try {
			return page.querySelectorAll(INTERACTIVE_ELEMENTS_SELECTOR);
		} catch (Exception e) {
			log.warn("获取交互元素时出错: {}", e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * 刷新缓存中的可交互元素
	 * @param frame Frame实例
	 * @return 当前iframe中的交互元素列表
	 */
	private List<ElementHandle> getInteractiveElementsInner(Frame frame) {
		try {
			return frame.querySelectorAll(INTERACTIVE_ELEMENTS_SELECTOR);
		} catch (Exception e) {
			log.warn("Error fetching interactive elements from frame: {}", e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * 获取网页中所有可交互元素的详细信息
	 * @param page Page实例
	 * @return 格式化后的元素信息字符串
	 */
	public String getInteractiveElementsInfo(Page page) {
		StringBuilder resultInfo = new StringBuilder();
		List<ElementHandle> interactiveElements = getInteractiveElements(page);

		// 使用全局索引计数，不会在iframe内重置
		for (int i = 0; i < interactiveElements.size(); i++) {
			// 获取原始信息
			String originalInfo = (String) interactiveElements.get(i).evaluate("element => element.outerHTML");
			
			// 如果是空字符串则跳过
			if (originalInfo == null || originalInfo.isEmpty()) {
				continue;
			}
			
			// 替换索引，确保全局唯一
			// 这里假设原始信息的格式是 "[index] <tag...>text</tag>"
			String formattedInfo = originalInfo.replaceFirst("\\[\\d+\\]", "[" + i + "]");
			resultInfo.append(formattedInfo);
		}

		return resultInfo.toString();
	}

}
