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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.WebElementWrapper;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class KeyEnterAction extends BrowserAction {

    public KeyEnterAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        WebDriver driver = browserUseTool.getDriver();
        Integer index = request.getIndex();
        if (index == null) {
            return new ToolExecuteResult("Index is required for 'key_enter' action");
        }
        List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);
        if (index < 0 || index >= interactiveElements.size()) {
            return new ToolExecuteResult("Element with index " + index + " not found");
        }
        WebElementWrapper enterElementWrapper = interactiveElements.get(index);
        enterElementWrapper.prepareForInteraction(driver);
        WebElement enterElement = enterElementWrapper.getElement();
        enterElement.sendKeys(Keys.RETURN);
        refreshTabsInfo(driver); // 刷新标签页信息
        interactiveTextProcessor.refreshCache(driver);
        return new ToolExecuteResult("Hit the enter key at index " + index);
    }

}
