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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 表示单个交互式元素的类，持有元素的Locator和所在的Frame/Page信息 同时提供管理页面中所有交互元素的能力
 */
public class InteractiveElement {

	private static final Logger log = LoggerFactory.getLogger(InteractiveElement.class);

	/**
	 * 用于选择交互式元素的CSS选择器
	 */
	private static final String INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea";

	/**
	 * 存储所有已索引的交互元素的列表
	 */
	private static final List<InteractiveElement> allElements = new ArrayList<>();

	/**
	 * 快速通过索引查找元素
	 */
	private final Map<Integer, InteractiveElement> elementIndex = new HashMap<>();

	// 全局索引
	private final int index;

	// 元素定位器
	private final Locator locator;

	// 元素所在的Frame（如果在iframe中）
	private final Frame frame;

	// 是否在主页面（不在iframe中）
	private final boolean isInMainPage;

	// 元素类型信息
	private final String tagName;

	// 元素文本信息（如果有）
	private String text;

	// HTML结构信息
	private String outerHtml;

	/**
	 * 创建主页面中的交互元素
	 * @param index 全局索引
	 * @param locator 元素定位器
	 * @param page 所在页面
	 */
	public InteractiveElement(int index, Locator locator, Page page) {
		this.index = index;
		this.locator = locator;
		this.frame = null;
		this.isInMainPage = true;

		// 获取元素的标签名
		this.tagName = locator.evaluate("el => el.tagName.toLowerCase()").toString();

		// 尝试获取元素的文本内容
		try {
			this.text = locator.innerText();
		}
		catch (Exception e) {
			this.text = "";
		}

		// 获取HTML结构
		try {
			this.outerHtml = locator.evaluate("el => el.outerHTML").toString();
		}
		catch (Exception e) {
			this.outerHtml = "";
		}
	}

	/**
	 * 创建iframe中的交互元素
	 * @param index 全局索引
	 * @param locator 元素定位器
	 * @param frame 所在的iframe
	 */
	public InteractiveElement(int index, Locator locator, Frame frame) {
		this.index = index;
		this.locator = locator;
		this.frame = frame;
		this.isInMainPage = false;

		// 获取元素的标签名
		this.tagName = locator.evaluate("el => el.tagName.toLowerCase()").toString();

		// 尝试获取元素的文本内容
		try {
			this.text = locator.innerText();
		}
		catch (Exception e) {
			this.text = "";
		}

		// 获取HTML结构
		try {
			this.outerHtml = locator.evaluate("el => el.outerHTML").toString();
		}
		catch (Exception e) {
			this.outerHtml = "";
		}
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
	 * 获取元素所在的Frame
	 * @return 元素所在的Frame，如果在主页面则返回null
	 */
	public Frame getFrame() {
		return frame;
	}

	/**
	 * 判断元素是否在主页面中
	 * @return 如果元素在主页面则返回true，在iframe中则返回false
	 */
	public boolean isInMainPage() {
		return isInMainPage;
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
	 * 刷新元素的文本内容
	 */
	public void refreshText() {
		try {
			this.text = locator.innerText();
		}
		catch (Exception e) {
			// 保持原有值不变
		}
	}

	/**
	 * 刷新页面中的所有交互元素，包括iframe中的
	 * @param page 要处理的页面
	 */
	public void refreshElements(Page page) {
		clearCache();
		waitForPageLoad(page);
		processMainPageElements(page);
		processIframeElements(page);
		log.info("已加载 {} 个交互式元素", allElements.size());
	}

	/**
	 * 清空当前缓存
	 */
	private void clearCache() {
		allElements.clear();
		elementIndex.clear();
	}

	/**
	 * 等待页面完全加载
	 * @param page Page实例
	 */
	private void waitForPageLoad(Page page) {
		try {
			page.waitForLoadState(LoadState.DOMCONTENTLOADED);
			log.info("页面已加载完成");
		}
		catch (Exception e) {
			log.warn("等待页面加载时出错: {}", e.getMessage());
		}
	}

	/**
	 * 处理主页面中的交互元素
	 * @param page Page实例
	 */
	private void processMainPageElements(Page page) {
		try {
			Locator elementLocator = page.locator(INTERACTIVE_ELEMENTS_SELECTOR);
			int count = elementLocator.count();
			log.info("找到 {} 个主页面交互元素", count);

			for (int i = 0; i < count; i++) {
				Locator locator = elementLocator.nth(i);
				int globalIndex = allElements.size();
				InteractiveElement element = new InteractiveElement(globalIndex, locator, page);
				allElements.add(element);
				elementIndex.put(globalIndex, element);
			}
		}
		catch (Exception e) {
			log.warn("处理主页面元素时出错: {}", e.getMessage());
		}
	}

	/**
	 * 处理页面中所有iframe的交互元素
	 * @param page Page实例
	 */
	private void processIframeElements(Page page) {
		List<Frame> frames = page.frames();
		log.info("找到 {} 个iframe", frames.size());

		// 排除主框架
		frames.stream().filter(frame -> frame != page.mainFrame()).forEach(this::processFrameElements);
	}

	/**
	 * 处理单个iframe中的交互元素
	 * @param frame Frame实例
	 */
	private void processFrameElements(Frame frame) {
		try {
			Locator elementLocator = frame.locator(INTERACTIVE_ELEMENTS_SELECTOR);
			int count = elementLocator.count();
			log.info("在iframe中找到 {} 个交互元素", count);

			for (int i = 0; i < count; i++) {
				Locator locator = elementLocator.nth(i);
				int globalIndex = allElements.size();
				InteractiveElement element = new InteractiveElement(globalIndex, locator, frame);
				allElements.add(element);
				elementIndex.put(globalIndex, element);
			}
		}
		catch (Exception e) {
			log.warn("处理iframe元素时出错: {}", e.getMessage());
		}
	}

	/**
	 * 获取所有交互元素列表
	 * @return 交互元素列表
	 */
	public List<InteractiveElement> getAllElements() {
		return new ArrayList<>(allElements);
	}

	/**
	 * 根据全局索引获取交互元素
	 * @param index 全局索引
	 * @return 对应的交互元素，如果不存在则返回空
	 */
	public Optional<InteractiveElement> getElementById(int index) {
		return Optional.ofNullable(elementIndex.get(index));
	}

	/**
	 * 获取当前注册的元素数量
	 * @return 元素数量
	 */
	public int size() {
		return allElements.size();
	}

	/**
	 * 生成所有元素的详细信息文本
	 * @return 格式化的元素信息字符串
	 */
	public String generateElementsInfoText() {
		StringBuilder result = new StringBuilder();
		for (InteractiveElement element : allElements) {
			result.append(element.toString()).append("\n");
		}
		return result.toString();
	}
	/**
	 * 点击指定索引的元素
	 * @param index 元素全局索引
	 * @return 操作是否成功
	 */
	public boolean clickElement(int index) {
		Optional<InteractiveElement> elementOpt = getElementById(index);
		if (elementOpt.isPresent()) {
			try {
				InteractiveElement element = elementOpt.get();
				element.getLocator().click();
				log.info("点击了索引为 {} 的元素: {}", index, element.toString());
				return true;
			}
			catch (Exception e) {
				log.error("点击元素时出错: {}", e.getMessage());
				return false;
			}
		}
		log.warn("未找到索引为 {} 的元素", index);
		return false;
	}

	/**
	 * 在指定索引的输入元素中填写文本
	 * @param index 元素全局索引
	 * @param text 要填写的文本
	 * @return 操作是否成功
	 */
	public boolean fillText(int index, String text) {
		Optional<InteractiveElement> elementOpt = getElementById(index);
		if (elementOpt.isPresent()) {
			try {
				InteractiveElement element = elementOpt.get();
				element.getLocator().fill(text);
				log.info("在索引为 {} 的元素中填写了文本: {}", index, text);
				return true;
			}
			catch (Exception e) {
				log.error("填写文本时出错: {}", e.getMessage());
				return false;
			}
		}
		log.warn("未找到索引为 {} 的元素", index);
		return false;
	}

	/**
	 * 操作特定索引的元素
	 * @param index 元素的全局索引
	 * @param action 要执行的操作，例如点击、填写等
	 * @return 操作是否成功
	 */
	public boolean performAction(int index, ElementAction action) {
		Optional<InteractiveElement> elementOpt = getElementById(index);
		if (elementOpt.isPresent()) {
			InteractiveElement element = elementOpt.get();
			try {
				// 执行指定动作
				action.execute(element);
				return true;
			}
			catch (Exception e) {
				log.error("执行元素动作时出错: {}", e.getMessage());
				return false;
			}
		}
		log.warn("未找到索引为 {} 的元素", index);
		return false;
	}

	/**
	 * 元素操作接口
	 */
	public interface ElementAction {

		/**
		 * 在元素上执行操作
		 * @param element 要操作的元素
		 */
		void execute(InteractiveElement element);

	}

	/**
	 * 创建元素信息的字符串表示形式
	 * @return 格式化的元素信息
	 */
	@Override
	public String toString() {
		String content = text.isEmpty() ? outerHtml : text;
		if (content.length() > 500) {
			content = content.substring(0, 500) + " ... " + content.substring(content.length() - 100);
		}
		return String.format("[%d] %s: %s", index, tagName, content);
	}

}
