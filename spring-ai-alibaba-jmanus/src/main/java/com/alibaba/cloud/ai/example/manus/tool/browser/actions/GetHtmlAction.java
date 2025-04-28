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

public class GetHtmlAction extends BrowserAction {

    private final int MAX_LENGTH = 50000;

    public GetHtmlAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }

    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        WebDriver driver = browserUseTool.getDriver();
        String html = driver.getPageSource();
        interactiveTextProcessor.refreshCache(driver);
        return new ToolExecuteResult(
                html.length() > MAX_LENGTH ? html.substring(0, MAX_LENGTH) + "..." : html);
    }

}
