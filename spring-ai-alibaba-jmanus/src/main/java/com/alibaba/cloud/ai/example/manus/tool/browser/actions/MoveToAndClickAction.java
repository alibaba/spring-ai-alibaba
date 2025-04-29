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
import java.util.Set;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class MoveToAndClickAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MoveToAndClickAction.class);

    public MoveToAndClickAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        Integer x = request.getPositionX();
        Integer y = request.getPositionY();
        
        if (x == null || y == null) {
            return new ToolExecuteResult("X and Y coordinates are required for 'move_to_and_click' action");
        }
        
        WebDriver driver = browserUseTool.getDriver();
        
        // 记录点击前的窗口状态
        Set<String> beforeWindowHandles = driver.getWindowHandles();
        String currentUrl = driver.getCurrentUrl();
        
        try {
            // 方法1: 使用Actions API (推荐，但有些浏览器可能不支持)
            Actions actions = new Actions(driver);
            actions.moveByOffset(x, y).click().perform();
            log.info("Clicked at position ({}, {})", x, y);
        } catch (Exception e) {
            log.warn("Failed to click using Actions API, falling back to JavaScript", e);
            
            // 方法2: 使用JavaScript (备选方案)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                "var evt = document.createEvent('MouseEvents');" +
                "evt.initMouseEvent('click', true, true, window, 0, 0, 0, arguments[0], arguments[1], false, false, false, false, 0, null);" +
                "document.elementFromPoint(arguments[0], arguments[1]).dispatchEvent(evt);", 
                x, y
            );
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
                        "Clicked at position (" + x + ", " + y + ") and opened in new tab: " + driver.getCurrentUrl());
            }

            // 检查URL是否发生变化
            boolean urlChanged = wait.until(d -> !d.getCurrentUrl().equals(currentUrl));
            if (urlChanged) {
                log.info("Page navigated to: {}", driver.getCurrentUrl());
                refreshTabsInfo(driver); // 刷新标签页信息
                return new ToolExecuteResult("Clicked at position (" + x + ", " + y + ") and navigated to: " + driver.getCurrentUrl());
            }
            
            refreshTabsInfo(driver); // 刷新标签页信息
            interactiveTextProcessor.refreshCache(driver);
            // 如果没有明显变化，返回普通点击成功消息
            return new ToolExecuteResult("Clicked at position (" + x + ", " + y + ")");

        } catch (TimeoutException e) {
            // 如果超时，检查是否仍在原页面
            if (!driver.getCurrentUrl().equals(currentUrl)) {
                return new ToolExecuteResult("Clicked at position (" + x + ", " + y + ") and page changed to: " + driver.getCurrentUrl());
            }
            return new ToolExecuteResult(
                    "Clicked at position (" + x + ", " + y + ") (no visible navigation occurred)");
        }
    }
}
