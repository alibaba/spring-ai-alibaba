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
 * 专门处理网页中交互式文本元素的工具类。
 * 提供了查找、分析和处理网页中文本内容的功能。
 */
public class InteractiveTextProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(InteractiveTextProcessor.class);
    /**
     * 用于存储交互式元素的缓存列表
     */
    private List<WebElementWrapper> interactiveElementsCache ;
    /**
     * 用于选择交互式元素的CSS选择器
     */
    private static final String INTERACTIVE_ELEMENTS_SELECTOR = 
        "a, button, input, select, textarea[type='search'], textarea, " +
        "[role='button'], [role='link'], [role='textbox'], [role='search'], [role='searchbox']";
    

    public void refreshCache(WebDriver driver) {
        this.interactiveElementsCache = getInteractiveElementsInner(driver);
    }
    /**
     * 获取网页中所有可交互的元素
     * 
     * @param driver WebDriver实例
     * @return 包装后的可交互元素列表
     */
    public List<WebElementWrapper> getInteractiveElements(WebDriver driver) {
        if (interactiveElementsCache == null) {
             throw new IllegalStateException("缓存未初始化，请先调用refreshCache方法。");
        }
        return interactiveElementsCache;
    }

    private List<WebElementWrapper> getInteractiveElementsInner(WebDriver driver) {
         try {
            return driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR))
                .stream()
                .filter(this::isElementVisible)
                .map(element -> {
                    // 获取元素信息字符串
                    String elementInfo = generateElementInfoString(element, driver);
                    // 创建并返回包装对象
                    return new WebElementWrapper(element, elementInfo);
                })
                .collect(Collectors.toList());
        }
        catch (StaleElementReferenceException e) {
            log.warn("元素在获取过程中过期，重试一次: {}", e.getMessage());
            // 如果发生异常，等待一下然后重试
            try {
                Thread.sleep(500);
                return driver.findElements(By.cssSelector(INTERACTIVE_ELEMENTS_SELECTOR))
                    .stream()
                    .filter(this::isElementVisible)
                    .map(element -> {
                        // 获取元素信息字符串
                        String elementInfo = generateElementInfoString(element, driver);
                        // 创建并返回包装对象
                        return new WebElementWrapper(element, elementInfo);
                    })
                    .collect(Collectors.toList());
            }
            catch (Exception retryEx) {
                log.error("重试获取元素失败: {}", retryEx.getMessage());
                return new ArrayList<>(); // 返回空列表而不是抛出异常
            }
        }
    }
    
    /**
     * 生成元素的详细信息字符串
     * 
     * @param element 要获取信息的元素
     * @param driver WebDriver实例
     * @return 元素的详细信息字符串
     */
    private String generateElementInfoString(WebElement element, WebDriver driver) {
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

            // 返回元素信息字符串
            return String.format("<%s%s>%s</%s>", tagName, attributes.toString(), text, tagName);
        }
        catch (Exception e) {
            log.warn("生成元素信息字符串失败: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * 判断元素是否可见且可交互
     * 
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
     *
     * @param driver WebDriver实例
     * @return 格式化后的元素信息字符串
     */
    public String getInteractiveElementsInfo(WebDriver driver) {
        StringBuilder resultInfo = new StringBuilder();
        List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);

        for (int i = 0; i < interactiveElements.size(); i++) {
            String formattedInfo = formatElementInfo(i, interactiveElements.get(i).getElement(), driver);
            if (formattedInfo != null && !formattedInfo.isEmpty()) {
                resultInfo.append(formattedInfo);
            }
        }

        return resultInfo.toString();
    }
    
    /**
     * 格式化元素信息为HTML格式的字符串表示
     * 
     * @param index 元素索引
     * @param element 要格式化的元素
     * @param driver WebDriver实例，用于执行JavaScript
     * @return 格式化后的元素信息字符串
     */
    private String formatElementInfo(int index, WebElement element, WebDriver driver) {
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
        catch (StaleElementReferenceException | NoSuchElementException e) {
            log.debug("忽略过期或不存在的元素: {}", e.getMessage());
            return "";
        }
        catch (Exception e) {
            log.warn("格式化元素信息失败，跳过当前元素: {}", e.getMessage());
            return "";
        }
    }
}
