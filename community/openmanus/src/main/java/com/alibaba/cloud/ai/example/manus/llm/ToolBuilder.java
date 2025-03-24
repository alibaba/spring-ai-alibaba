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
package com.alibaba.cloud.ai.example.manus.llm;

import java.util.List;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.FileSaver;
import com.alibaba.cloud.ai.example.manus.tool.GoogleSearch;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus.tool.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.Summary;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

@Service
public class ToolBuilder {
    private final ChromeDriverService chromeDriverService;

    public ToolBuilder(ChromeDriverService chromeDriverService) {
        this.chromeDriverService = chromeDriverService;
    }

    public List<FunctionCallback> getManusAgentToolCalls(BaseAgent agent, ChatMemory memory,
            String conversationId) {
        return List.of(
            //GoogleSearch.getFunctionToolCallback(), 
            BrowserUseTool.getFunctionToolCallback(chromeDriverService),
            FileSaver.getFunctionToolCallback(), 
            PythonExecute.getFunctionToolCallback(),
            Summary.getFunctionToolCallback(agent, memory, conversationId)
        );
    }

    public List<ToolCallback> getManusAgentToolCalls() {
        return List.of(
            //GoogleSearch.getFunctionToolCallback(), 
            BrowserUseTool.getFunctionToolCallback(chromeDriverService),
            FileSaver.getFunctionToolCallback(), 
            PythonExecute.getFunctionToolCallback()
        );
    }

    public List<ToolCallback> getPlanningAgentToolCallbacks() {
        return List.of(PlanningTool.getFunctionToolCallback());
    }

}
