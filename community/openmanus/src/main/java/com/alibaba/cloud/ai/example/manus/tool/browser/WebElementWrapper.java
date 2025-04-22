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

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * WebElement的包装类，同时包含元素本身和元素的详细信息。
 * 这个类使得在处理网页元素时能够一次获取WebElement及其相关信息。
 */
public class WebElementWrapper {

    private final WebElement element;
    private final String elementInfoString;

    /**
     * 构造一个新的WebElementWrapper实例
     *
     * @param element WebElement对象
     * @param elementInfoString 元素的详细信息字符串
     */
    public WebElementWrapper(WebElement element, String elementInfoString) {
        this.element = element;
        this.elementInfoString = elementInfoString;
    }

    /**
     * 获取包装的WebElement对象
     *
     * @return WebElement对象
     */
    public WebElement getElement() {
        return element;
    }

    /**
     * 获取元素的详细信息字符串
     *
     * @return 元素详细信息字符串
     */
    public String getElementInfoString() {
        return elementInfoString;
    }

    /**
     * 获取元素的简短描述，可用于日志记录和调试
     *
     * @return 元素的字符串表示
     */
    @Override
    public String toString() {
        return "WebElementWrapper{" +
                "element=" + element +
                ", elementInfo='" + elementInfoString + '\'' +
                '}';
    }
}
