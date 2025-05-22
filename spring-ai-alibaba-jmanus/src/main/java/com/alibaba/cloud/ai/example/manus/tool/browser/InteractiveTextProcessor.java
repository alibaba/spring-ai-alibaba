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

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 专门处理网页中交互式文本元素的工具类。 提供了查找、分析和处理网页中文本内容的功能。
 * 使用InteractiveElementRegistry管理页面中所有交互元素，提供全局索引访问能力。
 */
public class InteractiveTextProcessor {

	private static final Logger log = LoggerFactory.getLogger(InteractiveTextProcessor.class);

	/**
	 * 存储和管理页面中的交互式元素
	 */
	private final InteractiveElementRegistry elementRegistry;

	/**
	 * 构造函数
	 */
	public InteractiveTextProcessor() {
		this.elementRegistry = new InteractiveElementRegistry();
	}

	/**
	 * 刷新页面中的所有交互元素，包括iframe中的
	 * @param page 要处理的页面
	 */
	public void refreshCache(Page page) {
		// 使用registry刷新页面元素
		elementRegistry.refresh(page);
		log.info("已刷新页面元素，共找到 {} 个交互元素", elementRegistry.size());
	}

	/**
	 * 获取指定索引的交互元素
	 * @param index 全局索引
	 * @return 该索引对应的交互元素，如果不存在则返回空
	 */
	public Optional<InteractiveElement> getElementByIndex(int index) {
		return elementRegistry.getElementById(index);
	}

	/**
	 * 获取所有交互元素的列表
	 * @return 交互元素列表
	 */
	public List<InteractiveElement> getAllElements(Page page) {
		return elementRegistry.getAllElements(page);
	}

	/**
	 * 点击指定索引的元素
	 * @param index 元素全局索引
	 * @return 操作是否成功
	 */
	public boolean clickElement(int index) {
		return elementRegistry.performAction(index, element -> {
			element.getLocator().click();
			log.info("点击了索引为 {} 的元素: {}", index, element.toString());
		});
	}

	/**
	 * 在指定索引的输入元素中填写文本
	 * @param index 元素全局索引
	 * @param text 要填写的文本
	 * @return 操作是否成功
	 */
	public boolean fillText(int index, String text) {
		return elementRegistry.performAction(index, element -> {
			element.getLocator().fill(text);
			log.info("在索引为 {} 的元素中填写了文本: {}", index, text);
		});
	}

	/**
	 * 获取网页中所有可交互元素的详细信息
	 * @return 格式化后的元素信息字符串
	 */
	public String getInteractiveElementsInfo(Page page) {
		return elementRegistry.generateElementsInfoText(page);
	}

	/**
	 * 获取交互元素总数
	 * @return 元素数量
	 */
	public int getElementCount() {
		return elementRegistry.size();
	}

	/**
	 * 根据元素索引执行自定义操作
	 * @param index 元素索引
	 * @param action 要执行的操作
	 * @return 操作是否成功
	 */
	public boolean performAction(int index, InteractiveElementRegistry.ElementAction action) {
		return elementRegistry.performAction(index, action);
	}

}
