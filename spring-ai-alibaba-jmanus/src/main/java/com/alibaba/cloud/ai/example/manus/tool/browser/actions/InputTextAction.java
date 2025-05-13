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
import java.util.Random;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.browser.WebElementWrapper;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class InputTextAction extends BrowserAction {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InputTextAction.class);

    public InputTextAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        Integer index = request.getIndex();
        String text = request.getText();

        WebDriver driver = browserUseTool.getDriver();
        if (index == null || text == null) {
            return new ToolExecuteResult("Index and text are required for 'input_text' action");
        }
        if (index == null || text == null) {
            return new ToolExecuteResult("Index and text are required for 'input_text' action");
        }
        List<WebElementWrapper> interactiveElements = getInteractiveElements(driver);
        if (index < 0 || index >= interactiveElements.size()) {
            return new ToolExecuteResult("Element with index " + index + " not found");
        }
        WebElementWrapper inputElementWrapper = interactiveElements.get(index);
        inputElementWrapper.prepareForInteraction(driver);
        WebElement inputElement = inputElementWrapper.getElement();
        if (!inputElement.getTagName().equals("input") && !inputElement.getTagName().equals("textarea")) {
            return new ToolExecuteResult("Element at index " + index + " is not an input element");
        }
        typeWithHumanDelay(inputElement, text);
        refreshTabsInfo(driver); // 刷新标签页信息
        interactiveTextProcessor.refreshCache(driver);
        return new ToolExecuteResult("Successfully input '" + text + "' into element at index " + index);
    }

    private void typeWithHumanDelay(WebElement element, String text) {
        simulateHumanBehavior(element);

        // 模拟人类输入速度
        Random random = new Random();
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            try {
                Thread.sleep(random.nextInt(100) + 50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
