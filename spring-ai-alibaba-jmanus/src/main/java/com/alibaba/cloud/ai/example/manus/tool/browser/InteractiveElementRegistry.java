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
 * 管理页面中所有交互式元素的集合类，提供全局索引访问能力
 */
public class InteractiveElementRegistry {

	private static final Logger log = LoggerFactory.getLogger(InteractiveElementRegistry.class);

	/**
	 * 用于选择交互式元素的CSS选择器
	 */
	private static final String INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea";

	// 可根据需要添加更多选择器
	// + "[role='button'], [role='link'], [role='textbox'], [role='search'],
	// [role='searchbox']";

	/**
	 * 存储所有交互元素的列表，按全局索引顺序排列
	 */
	private final List<InteractiveElement> interactiveElements = new ArrayList<>();

	/**
	 * 缓存索引到元素的快速查找
	 */
	private final Map<Integer, InteractiveElement> indexToElementMap = new HashMap<>();

	/**
	 * 刷新指定页面的所有交互元素
	 * @param page 要处理的页面
	 */
	public void refresh(Page page) {
		clearCache();
		waitForPageLoad(page);
		// 统一处理所有 frame（包括主页面和所有iframe）
		List<Frame> frames = page.frames();
		log.info("统一处理所有 frame, 共 {} 个", frames.size());
		for (Frame frame : frames) {
			processFrameElements(frame);
		}
		log.info("已加载 {} 个交互式元素", interactiveElements.size());
	}

	/**
	 * 清空当前缓存
	 */
	private void clearCache() {
		interactiveElements.clear();
		indexToElementMap.clear();
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
	 * 处理单个iframe中的交互元素
	 * @param frame Frame实例
	 */
	@SuppressWarnings("unchecked")
	private void processFrameElements(Frame frame) {
		try {
			Locator elementLocator = frame.locator(INTERACTIVE_ELEMENTS_SELECTOR);
			int count = elementLocator.count();
			log.info("在iframe中找到 {} 个交互元素", count);
			List<Map<String, Object>> elementMapList = (List<Map<String, Object>>) elementLocator.evaluateAll("""
					(elements) => elements.map((element, index) => {
					    return {
					        tagName: element.tagName.toLowerCase(),
					        text: element.innerText,
					        outerHtml: element.outerHTML,
					        index: index
					    };
					})
					""");
			for (Map<String, Object> elementMap : elementMapList) {
				Integer globalIndex = (Integer) elementMap.get("index");
				Locator locator = elementLocator.nth(globalIndex);
				InteractiveElement element = new InteractiveElement(globalIndex, locator, frame, elementMap);
				interactiveElements.add(element);
				indexToElementMap.put(globalIndex, element);
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
	public List<InteractiveElement> getAllElements(Page page) {
		refresh(page);
		return new ArrayList<>(interactiveElements);
	}

	/**
	 * 根据全局索引获取交互元素
	 * @param index 全局索引
	 * @return 对应的交互元素，如果不存在则返回空
	 */
	public Optional<InteractiveElement> getElementById(int index) {
		return Optional.ofNullable(indexToElementMap.get(index));
	}

	/**
	 * 获取当前注册的元素数量
	 * @return 元素数量
	 */
	public int size() {
		return interactiveElements.size();
	}

	/**
	 * 生成所有元素的详细信息文本
	 * @return 格式化的元素信息字符串
	 */
	public String generateElementsInfoText(Page page) {
		StringBuilder result = new StringBuilder();
		for (InteractiveElement element : getAllElements(page)) {
			result.append(element.toString().replace("\n", "")).append("\n");
		}
		return result.toString();
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

}
