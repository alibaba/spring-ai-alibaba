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
package com.alibaba.cloud.ai.manus.agent;

import java.util.Map;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.prompt.service.PromptService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;

/**
 * Base class for ReAct (Reasoning + Acting) pattern agents. Implements an agent pattern
 * where thinking (Reasoning) and acting (Acting) are executed alternately.
 */
public abstract class ReActAgent extends BaseAgent {

	/**
	 * Constructor
	 * @param llmService LLM service instance for handling natural language interactions
	 * @param planExecutionRecorder plan execution recorder for recording execution
	 * process
	 * @param manusProperties Manus configuration properties
	 */

	public ReActAgent(ILlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, Map<String, Object> initialAgentSetting, PromptService promptService,
			ExecutionStep step, PlanIdDispatcher planIdDispatcher) {
		super(llmService, planExecutionRecorder, manusProperties, initialAgentSetting, promptService, step,
				planIdDispatcher);
	}

	/**
	 * Execute thinking process and determine whether action needs to be taken
	 *
	 * Subclass implementation requirements: 1. Analyze current state and context 2.
	 * Perform logical reasoning to decide on next action 3. Return whether action
	 * execution is needed
	 *
	 * Example implementation: - Return true if tools need to be called - Return false if
	 * current step is completed
	 * @return true indicates action execution is needed, false indicates no action is
	 * currently needed
	 */
	protected abstract boolean think();

	/**
	 * Execute specific actions
	 *
	 * Subclass implementation requirements: 1. Execute specific operations based on
	 * think() decisions 2. Can be tool calls, state updates, or other specific behaviors
	 * 3. Return description of execution results
	 *
	 * Example implementations: - ToolCallAgent: execute selected tool calls -
	 * BrowserAgent: execute browser operations
	 * @return description of action execution results
	 */
	protected abstract AgentExecResult act();

	/**
	 * Execute a complete think-act step
	 * @return returns thinking complete message if no action is needed, otherwise returns
	 * action execution result
	 */
	@Override
	public AgentExecResult step() {

		boolean shouldAct = think();
		if (!shouldAct) {
			AgentExecResult result = new AgentExecResult("Thinking complete - no action needed",
					AgentState.IN_PROGRESS);

			return result;
		}
		return act();
	}

}
