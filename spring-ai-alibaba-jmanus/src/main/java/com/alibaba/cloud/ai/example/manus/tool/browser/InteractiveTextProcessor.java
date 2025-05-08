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

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专门处理网页中交互式文本元素的工具类。 提供了查找、分析和处理网页中文本内容的功能。
 */
public class InteractiveTextProcessor {

	private static final Logger log = LoggerFactory.getLogger(InteractiveTextProcessor.class);

	/**
	 * 用于存储交互式元素的缓存列表
	 */
	private List<WebElementWrapper> interactiveElementsCache;

	/**
	 * 用于选择交互式元素的CSS选择器
	 */
	private static final String INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea[type='search'], textarea, "
			+ "[role='button'], [role='link'], [role='textbox'], [role='search'], [role='searchbox']";

	public void refreshCache(WebDriver driver) {
		// 清空现有缓存
		this.interactiveElementsCache = new ArrayList<>();

		// 等待页面完全加载
		waitForPageLoad(driver);

		// 先获取主文档中的元素
		List<WebElementWrapper> mainDocElements = getInteractiveElementsInner(driver);
		this.interactiveElementsCache.addAll(mainDocElements);

		// 获取并处理所有iframe中的元素
		processIframes(driver, "", null);
	}

	/**
	 * 等待页面完全加载，包括iframe和动态内容
	 * @param driver WebDriver实例
	 */
	private void waitForPageLoad(WebDriver driver) {
		try {
			// 等待主文档完成加载
			log.info("等待主文档完成加载...");
			JavascriptExecutor js = (JavascriptExecutor) driver;

			// 最多等待30秒直到document.readyState变为"complete"
			long startTime = System.currentTimeMillis();
			long timeout = 30000; // 30秒超时

			while (System.currentTimeMillis() - startTime < timeout) {
				String readyState = (String) js.executeScript("return document.readyState");
				log.debug("当前页面加载状态: {}", readyState);

				if ("complete".equals(readyState)) {
					break;
				}

				// 短暂等待后再次检查
				Thread.sleep(500);
			}

		}
		catch (Exception e) {
			log.warn("等待页面加载时出错: {}", e.getMessage());
		}
	}

	/**
	 * 递归处理页面中的所有iframe元素
	 * @param driver WebDriver实例
	 * @param parentPath 父iframe的路径
	 * @param parentIframe 父iframe元素
	 */
	private void processIframes(WebDriver driver, String parentPath, WebElement parentIframe) {
		// 查找当前上下文中的所有iframe
		List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
		log.info("找到 {} 个iframe", iframes.size());
		for (int i = 0; i < iframes.size(); i++) {

			WebElement iframe = iframes.get(i);

			// 确保iframe已加载完成
			boolean isIframeLoaded = false;
			int maxAttempts = 20; // 最大尝试次数
			int attempts = 0;
			while (!isIframeLoaded && attempts < maxAttempts) {
				try {
					// 尝试获取iframe的document.readyState
					JavascriptExecutor js = (JavascriptExecutor) driver;
					driver.switchTo().frame(iframe);
					String readyState = (String) js.executeScript("return document.readyState");
					log.info("iframe readyState: {}, attempts: {}", readyState, attempts);

					if ("complete".equals(readyState)) {
						isIframeLoaded = true;
						driver.switchTo().parentFrame(); // 切回父frame
						log.info("iframe loaded successfully");
						break;
					}

					driver.switchTo().parentFrame(); // 切回父frame
					attempts++;
					Thread.sleep(500); // 等待500毫秒
				}
				catch (Exception e) {
					log.info("等待iframe加载第{}次失败: {}", attempts, e.getMessage());
					attempts++;
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}

			if (!isIframeLoaded) {
				log.warn("iframe加载超时，跳过处理此iframe");
				return;
			}

			// 构建iframe路径
			String currentPath = parentPath.isEmpty() ? String.valueOf(i) : parentPath + "/" + i;

			try {
				// 切换到iframe
				driver.switchTo().frame(iframe);

				// 获取iframe中的交互元素并添加到缓存
				List<WebElementWrapper> iframeElements = getInteractiveElementsInner(driver);
				for (WebElementWrapper wrapper : iframeElements) {
					// 设置iframe信息
					wrapper.setIframeElement(iframe);
					wrapper.setIframePath(currentPath);
					this.interactiveElementsCache.add(wrapper);
				}

				// 递归处理嵌套iframe
				processIframes(driver, currentPath, iframe);

				// 切回父级上下文
				if (parentIframe == null) {
					driver.switchTo().defaultContent();
				}
				else {
					driver.switchTo().parentFrame();
				}
			}
			catch (Exception e) {
				log.warn("处理iframe失败，路径: {}, 错误: {}", currentPath, e.getMessage());
				// 确保即使处理某个iframe失败，也回到正确的上下文
				if (parentIframe == null) {
					driver.switchTo().defaultContent();
				}
				else {
					driver.switchTo().parentFrame();
				}
			}
		}
	}

	/**
	 * 获取网页中所有可交互的元素
	 * @param driver WebDriver实例
	 * @return 包装后的可交互元素列表
	 */
	public List<WebElementWrapper> getInteractiveElements(WebDriver driver) {
		if (interactiveElementsCache == null) {
			refreshCache(driver);
		}
		return interactiveElementsCache;
	}

	/**
	 * 刷新缓存中的可交互元素
	 * @param driver WebDriver实例
	 * @return 当前文档中的交互元素列表
	 */
	private List<WebElementWrapper> getInteractiveElementsInner(WebDriver driver) {
		try {
			List<WebElement> elements = driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR));
			List<WebElement> visibleElements = elements.stream()
				.filter(this::isElementVisible)
				.collect(Collectors.toList());

			List<WebElementWrapper> result = new ArrayList<>();
			for (int i = 0; i < visibleElements.size(); i++) {
				WebElement element = visibleElements.get(i);
				// 获取元素信息字符串，传递索引参数
				String elementInfo = generateElementInfoString(i, element, driver);
				// 创建并返回包装对象
				result.add(new WebElementWrapper(element, elementInfo));
			}
			return result;
		}
		catch (StaleElementReferenceException e) {
			log.warn("元素在获取过程中过期，重试一次: {}", e.getMessage());
			// 如果发生异常，等待一下然后重试
			try {
				Thread.sleep(500);
				List<WebElement> elements = driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR));
				List<WebElement> visibleElements = elements.stream()
					.filter(this::isElementVisible)
					.collect(Collectors.toList());

				List<WebElementWrapper> result = new ArrayList<>();
				for (int i = 0; i < visibleElements.size(); i++) {
					WebElement element = visibleElements.get(i);
					// 获取元素信息字符串，传递索引参数
					String elementInfo = generateElementInfoString(i, element, driver);
					// 创建并返回包装对象
					result.add(new WebElementWrapper(element, elementInfo));
				}
				return result;
			}
			catch (Exception retryEx) {
				log.error("重试获取元素失败: {}", retryEx.getMessage());
				return new ArrayList<>(); // 返回空列表而不是抛出异常
			}
		}
	}

	/**
	 * 生成元素的详细信息字符串
	 * @param element 要获取信息的元素
	 * @param driver WebDriver实例
	 * @return 元素的详细信息字符串
	 */
	private String generateElementInfoString(int index, WebElement element, WebDriver driver) {
		try {
			// 使用JavaScript获取元素的详细信息
			JavascriptExecutor js = (JavascriptExecutor) driver;
			@SuppressWarnings("unchecked")
			Map<String, Object> props = (Map<String, Object>) js.executeScript("""
					function getElementInfo(el) {
					    try {
					        const style = window.getComputedStyle(el);
					        return {
					            tagName: el.tagName.toLowerCase(),
					            type: el.getAttribute('type'),
					            role: el.getAttribute('role'),
					            text: el.textContent.trim(),
					            value: el.value,
					            placeholder: el.getAttribute('placeholder'),
					            name: el.getAttribute('name'),
					            id: el.getAttribute('id'),
					            'aria-label': el.getAttribute('aria-label'),
					            isVisible: (
					                el.offsetWidth > 0 &&
					                el.offsetHeight > 0 &&
					                style.visibility !== 'hidden' &&
					                style.display !== 'none'
					            )
					        };
					    } catch(e) {
					        return null; // 如果获取元素信息失败，返回null
					    }
					}
					return getElementInfo(arguments[0]);
					""", element);

			if (props == null || !(Boolean) props.get("isVisible")) {
				return "";
			}

			// 构建HTML属性字符串
			StringBuilder attributes = new StringBuilder();

			// 添加基本属性
			if (props.get("type") != null) {
				attributes.append(" type=\"").append(props.get("type")).append("\"");
			}
			if (props.get("role") != null) {
				attributes.append(" role=\"").append(props.get("role")).append("\"");
			}
			if (props.get("placeholder") != null) {
				attributes.append(" placeholder=\"").append(props.get("placeholder")).append("\"");
			}
			if (props.get("name") != null) {
				attributes.append(" name=\"").append(props.get("name")).append("\"");
			}
			if (props.get("id") != null) {
				attributes.append(" id=\"").append(props.get("id")).append("\"");
			}
			if (props.get("aria-label") != null) {
				attributes.append(" aria-label=\"").append(props.get("aria-label")).append("\"");
			}
			if (props.get("value") != null) {
				attributes.append(" value=\"").append(props.get("value")).append("\"");
			}

			String tagName = (String) props.get("tagName");
			String text = (String) props.get("text");

			// 生成标准HTML格式输出
			return String.format("[%d] <%s%s>%s</%s>\n", index, tagName, attributes.toString(), text, tagName);
		}
		catch (Exception e) {
			log.warn("生成元素信息字符串失败: {}", e.getMessage());
			return "";
		}
	}

	/**
	 * 判断元素是否可见且可交互
	 * @param element 要检查的WebElement
	 * @return 如果元素可见且可交互，返回true；否则返回false
	 */
	private boolean isElementVisible(WebElement element) {
		try {
			return element.isDisplayed() && element.isEnabled();
		}
		catch (StaleElementReferenceException | NoSuchElementException e) {
			// 忽略过期或不存在的元素
			log.debug("忽略过期或不存在的元素: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * 获取网页中所有可交互元素的详细信息
	 * @param driver WebDriver实例
	 * @return 格式化后的元素信息字符串
	 */
	public String getInteractiveElementsInfo(WebDriver driver) {
		StringBuilder resultInfo = new StringBuilder();
		List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);

		for (int i = 0; i < interactiveElements.size(); i++) {
			String formattedInfo = interactiveElements.get(i).getElementInfoString();
			resultInfo.append(formattedInfo);
		}

		return resultInfo.toString();
	}

}
