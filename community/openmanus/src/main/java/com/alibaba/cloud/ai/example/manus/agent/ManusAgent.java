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
package com.alibaba.cloud.ai.example.manus.agent;

import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.Bash;
import com.alibaba.cloud.ai.example.manus.tool.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.FileSaver;
import com.alibaba.cloud.ai.example.manus.tool.GoogleSearch;
import com.alibaba.cloud.ai.example.manus.tool.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.Summary;

import java.util.List;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

/**
 * 默认的智能体实现，使用多种工具来解决各种任务 但prompt不够特化，所以可能执行上会存在不准确的问题。 目前倾向于作为默认的，通用智能体。
 */
public class ManusAgent extends ToolCallAgent {

	private final String workingDirectory;

	private final ChromeDriverService chromeDriverService;

	public ManusAgent(LlmService llmService, ToolCallingManager toolCallingManager,
			ChromeDriverService chromeDriverService, String workingDirectory, PlanExecutionRecorder record) {
		super(llmService, toolCallingManager,record);
		this.chromeDriverService = chromeDriverService;
		this.workingDirectory = workingDirectory;
	}


	@Override
	public String getName() {
		return "MANUS";
	}

	@Override
	public String getDescription() {
		return "A versatile agent that can solve various tasks using multiple tools , if can't decide which agent to use, use Manus agent";
	}

	public List<ToolCallback> getToolCallList() {
		return List.of( FileSaver.getFunctionToolCallback(),
				PythonExecute.getFunctionToolCallback(), Bash.getFunctionToolCallback(workingDirectory),
				BrowserUseTool.getFunctionToolCallback(chromeDriverService),
				Summary.getFunctionToolCallback(this, llmService.getMemory(), getConversationId()));
	}

}
