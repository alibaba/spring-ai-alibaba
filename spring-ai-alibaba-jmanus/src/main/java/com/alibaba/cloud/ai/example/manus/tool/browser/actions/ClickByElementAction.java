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
package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.WebElementWrapper;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class ClickByElementAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClickByElementAction.class);

    public ClickByElementAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        Integer index = request.getIndex();
        if (index == null) {
            return new ToolExecuteResult("Index is required for 'click' action");
        }
        WebDriver driver = browserUseTool.getDriver();
        List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);

        if (index < 0 || index >= interactiveElements.size()) {
            return new ToolExecuteResult("Element with index " + index + " not found");
        }

        WebElementWrapper elementWrapper = interactiveElements.get(index);
        elementWrapper.prepareForInteraction(driver);
        WebElement element = elementWrapper.getElement();
        log.info("Clicking element: {}", (element != null ? element.getText() : "No text"));

        // 记录点击前的窗口状态
        Set<String> beforeWindowHandles = driver.getWindowHandles();
        String currentUrl = driver.getCurrentUrl();

        // 执行点击操作
        simulateHumanBehavior(element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // 如果普通点击失败，尝试使用 JavaScript 点击
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
        }

        // 等待页面变化（最多等待10秒）
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            // 检查是否有新窗口打开
            Set<String> afterWindowHandles = driver.getWindowHandles();
            if (afterWindowHandles.size() > beforeWindowHandles.size()) {
                // 找出新打开的窗口
                afterWindowHandles.removeAll(beforeWindowHandles);
                String newHandle = afterWindowHandles.iterator().next();

                // 切换到新窗口
                driver.switchTo().window(newHandle);
                log.info("New tab detected, switched to: {}", driver.getCurrentUrl());
                refreshTabsInfo(driver); // 刷新标签页信息
                return new ToolExecuteResult(
                        "Clicked element and opened in new tab: " + driver.getCurrentUrl());
            }

            // 检查URL是否发生变化
            boolean urlChanged = wait.until(d -> !d.getCurrentUrl().equals(currentUrl));
            if (urlChanged) {
                log.info("Page navigated to: {}", driver.getCurrentUrl());
                refreshTabsInfo(driver); // 刷新标签页信息
                return new ToolExecuteResult("Clicked element and navigated to: " + driver.getCurrentUrl());
            }
            refreshTabsInfo(driver); // 刷新标签页信息
            interactiveTextProcessor.refreshCache(driver);
            // 如果没有明显变化，返回普通点击成功消息
            return new ToolExecuteResult("Clicked element at index " + index);

        } catch (TimeoutException e) {
            // 如果超时，检查是否仍在原页面
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                return new ToolExecuteResult("Clicked and page changed to: " + driver.getCurrentUrl());
            }
            return new ToolExecuteResult(
                    "Clicked element at index " + index + " (no visible navigation occurred)");
        }
    }

}
