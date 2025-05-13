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

import java.util.List;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
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

        Page page = browserUseTool.getDriver().newPage(); // 获取 Playwright 的 Page 实例
        List<ElementHandle> interactiveElements = page.querySelectorAll("[data-interactive]"); // 替代 Selenium 的 getInteractiveElements

        if (index < 0 || index >= interactiveElements.size()) {
            return new ToolExecuteResult("Element with index " + index + " not found");
        }

        ElementHandle element = interactiveElements.get(index);
        log.info("Clicking element: {}", element.textContent());

        // 记录点击前的窗口状态
        List<String> beforeWindowHandles = page.context().pages().stream().map(Page::url).toList();
        String currentUrl = page.url();

        // 执行点击操作
        element.click();

        // 等待页面变化（最多等待10秒）
        page.waitForTimeout(10000);

        // 检查是否有新窗口打开
        List<String> afterWindowHandles = page.context().pages().stream().map(Page::url).toList();
        if (afterWindowHandles.size() > beforeWindowHandles.size()) {
            // 找出新打开的窗口
            afterWindowHandles.removeAll(beforeWindowHandles);
            String newHandle = afterWindowHandles.get(0);

            // 切换到新窗口
            Page newPage = page.context().pages().stream().filter(p -> p.url().equals(newHandle)).findFirst().orElse(null);
            if (newPage != null) {
                log.info("New tab detected, switched to: {}", newPage.url());
                return new ToolExecuteResult("Clicked element and opened in new tab: " + newPage.url());
            }
        }

        browserUseTool.getInteractiveTextProcessor().refreshCache(page);
        // 如果没有明显变化，返回普通点击成功消息
        return new ToolExecuteResult("Clicked element at index " + index);
    }

}
