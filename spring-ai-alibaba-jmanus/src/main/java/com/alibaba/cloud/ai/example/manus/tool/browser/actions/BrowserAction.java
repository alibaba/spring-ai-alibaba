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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.InteractiveTextProcessor;
import com.alibaba.cloud.ai.example.manus.tool.browser.WebElementWrapper;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public abstract class BrowserAction {
    public abstract ToolExecuteResult execute(BrowserRequestVO request) throws Exception;

    protected final BrowserUseTool browserUseTool;
    protected final InteractiveTextProcessor interactiveTextProcessor = new InteractiveTextProcessor();

    public BrowserAction(BrowserUseTool browserUseTool) {
        this.browserUseTool = browserUseTool;
    }

    /**
     * 这个方法是为了让getCurrentStatus 不会刷新页面，减少在Mac上主动唤起的次数 否则太烦了 ， 每个step要调起这个东西两次。 都会强制把
     * 页面唤起到
     * active啥事都没办法干了。
     * 
     * @param driver
     * @return
     */
    public List<Map<String, Object>> refreshTabsInfo(WebDriver driver) {
        Set<String> windowHandles = driver.getWindowHandles();
        List<Map<String, Object>> tabs = new ArrayList<>();
        String currentHandle = driver.getWindowHandle();
        for (String handle : windowHandles) {
            driver.switchTo().window(handle);
            tabs.add(Map.of("url", driver.getCurrentUrl(), "title", driver.getTitle(), "id", handle));
        }
        driver.switchTo().window(currentHandle); // 切回原始标签页
        browserUseTool.setTabsInfo(tabs);
        return tabs;
    }

    protected void simulateHumanBehavior(WebElement element) {
        try {

            // 添加随机延迟
            Thread.sleep(new Random().nextInt(500) + 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 通过InteractiveTextProcessor获取可交互元素
     * 
     * @param driver WebDriver实例
     * @return 可交互元素列表
     */
    protected List<WebElementWrapper> getInteractiveElements(WebDriver driver) {
        return interactiveTextProcessor.getInteractiveElements(driver);
    }

}
