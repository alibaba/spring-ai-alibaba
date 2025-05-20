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

import com.alibaba.cloud.ai.example.manus.tool.browser.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class WaitAction extends BrowserAction {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetTextAction.class);

    public WaitAction(BrowserUseTool browserUseTool) {
        super(browserUseTool);
    }


    @Override
    public ToolExecuteResult execute(BrowserRequestVO request) throws Exception {
        try {
            if (request.getWaitSeconds() == null) {
                log.warn("Wait seconds is null, set to 60 seconds");
                request.setWaitSeconds(60);
            }
            String msg = "🕒 等待 " + request.getWaitSeconds() + " 秒";
            log.info(msg);

            // 执行等待
            Thread.sleep(request.getWaitSeconds() * 1000);
            return new ToolExecuteResult(msg);
        } catch (InterruptedException e) {
            String errorMsg = "等待操作被中断: " + e.getMessage();
            log.error(errorMsg, e);
            return new ToolExecuteResult(errorMsg);

        }
    }
}
