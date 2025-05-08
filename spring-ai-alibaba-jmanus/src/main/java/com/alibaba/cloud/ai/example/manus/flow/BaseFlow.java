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
package com.alibaba.cloud.ai.example.manus.flow;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;

public abstract class BaseFlow {

	protected List<BaseAgent> agents;

	protected PlanExecutionRecorder recorder;

	public BaseFlow(List<BaseAgent> agents, Map<String, Object> data, PlanExecutionRecorder recorder) {
		this.recorder = recorder;
		this.agents = agents;
		data.put("agents", agents);
	}

	public abstract String execute(String inputText);

	public abstract List<ToolCallback> getToolCallList();

	protected PlanExecutionRecorder getRecorder() {
		return recorder;
	}

}
