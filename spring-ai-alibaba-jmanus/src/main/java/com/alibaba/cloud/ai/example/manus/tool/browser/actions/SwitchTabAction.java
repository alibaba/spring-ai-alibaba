
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

import org.openqa.selenium.WebDriver;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class SwitchTabAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SwitchTabAction.class);

    public SwitchTabAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        Integer tabId = request.getTabId();
        WebDriver driver = browserUseTool.getDriver();
        if (tabId == null || tabId < 0) {
            return new ToolExecuteResult("Tab ID is out of range for 'switch_tab' action");
        }
        Object[] windowHandles = driver.getWindowHandles().toArray();
        driver.switchTo().window(windowHandles[tabId].toString());
        refreshTabsInfo(driver); // 刷新标签页信息
        interactiveTextProcessor.refreshCache(driver);
        return new ToolExecuteResult("Switched to tab " + tabId);
    }

}
