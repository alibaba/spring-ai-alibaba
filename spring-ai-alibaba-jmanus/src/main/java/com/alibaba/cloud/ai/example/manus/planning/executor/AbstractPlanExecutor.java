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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for plan executors. Contains common logic and basic functionality
 * for all executor types.
 */
public abstract class AbstractPlanExecutor implements PlanExecutorInterface {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPlanExecutor.class);

	protected final PlanExecutionRecorder recorder;

	// Pattern to match square brackets at the beginning of a string, supports Chinese and
	// other characters
	protected final Pattern pattern = Pattern.compile("^\\s*\\[([^\\]]+)\\]");

	protected final List<DynamicAgentEntity> agents;

	protected final AgentService agentService;

	protected ILlmService llmService;

	protected final ManusProperties manusProperties;

	// Define static final strings for the keys used in executorParams
	public static final String PLAN_STATUS_KEY = "planStatus";

	public static final String CURRENT_STEP_INDEX_KEY = "currentStepIndex";

	public static final String STEP_TEXT_KEY = "stepText";

	public static final String EXTRA_PARAMS_KEY = "extraParams";

	public static final String EXECUTION_ENV_STRING_KEY = "current_step_env_data";

	public AbstractPlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties) {
		this.agents = agents;
		this.recorder = recorder;
		this.agentService = agentService;
		this.llmService = llmService;
		this.manusProperties = manusProperties;
	}

	/**
	 * General logic for executing a single step.
	 * @param step The execution step
	 * @param context The execution context
	 * @return The step executor
	 */
	protected BaseAgent executeStep(ExecutionStep step, ExecutionContext context) {
		try {
			String stepType = getStepFromStepReq(step.getStepRequirement());
			int stepIndex = step.getStepIndex();
			String columnsInString = step.getTerminateColumns();
			List<String> columns = parseColumns(columnsInString);

			String planStatus = context.getPlan().getPlanExecutionStateStringFormat(true);
			String stepText = step.getStepRequirement();

			Map<String, Object> initSettings = new HashMap<>();
			initSettings.put(PLAN_STATUS_KEY, planStatus);
			initSettings.put(CURRENT_STEP_INDEX_KEY, String.valueOf(stepIndex));
			initSettings.put(STEP_TEXT_KEY, stepText);
			initSettings.put(EXTRA_PARAMS_KEY, context.getPlan().getExecutionParams());

			BaseAgent executor = getExecutorForStep(stepType, context, initSettings, columns);
			if (executor == null) {
				logger.error("No executor found for step type: {}", stepType);
				step.setResult("No executor found for step type: " + stepType);
				return null;
			}

			step.setAgent(executor);
			executor.setState(AgentState.IN_PROGRESS);

			recorder.recordStepStart(step, context);
			String stepResultStr = executor.run();
			step.setResult(stepResultStr);

			return executor;
		}
		catch (Exception e) {
			logger.error("Error executing step: {}", e.getMessage(), e);
			step.setResult("Execution failed: " + e.getMessage());
		}
		finally {
			recorder.recordStepEnd(step, context);
		}
		return null;
	}

	/**
	 * Extract the step type from the step requirement string.
	 */
	protected String getStepFromStepReq(String stepRequirement) {
		Matcher matcher = pattern.matcher(stepRequirement);
		if (matcher.find()) {
			return matcher.group(1).trim().toLowerCase();
		}
		return "DEFAULT_AGENT";
	}

	/**
	 * Get the executor for the step.
	 */
	protected BaseAgent getExecutorForStep(String stepType, ExecutionContext context, Map<String, Object> initSettings,
			List<String> columns) {
		for (DynamicAgentEntity agent : agents) {
			if (agent.getAgentName().equalsIgnoreCase(stepType)) {
				BaseAgent executor = agentService.createDynamicBaseAgent(agent.getAgentName(),
						context.getPlan().getCurrentPlanId(), context.getPlan().getRootPlanId(), initSettings, columns);
				// Set thinkActRecordId from context for sub-plan executions
				if (context.getThinkActRecordId() != null) {
					executor.setThinkActRecordId(context.getThinkActRecordId());
				}
				return executor;
			}
		}
		throw new IllegalArgumentException(
				"No Agent Executor found for step type, check your agents list : " + stepType);
	}

	protected PlanExecutionRecorder getRecorder() {
		return recorder;
	}

	/**
	 * Parse columns string by splitting with comma or Chinese comma.
	 * @param columnsInString the columns string to parse
	 * @return list of column names
	 */
	protected List<String> parseColumns(String columnsInString) {
		List<String> columns = new ArrayList<>();
		if (columnsInString == null || columnsInString.trim().isEmpty()) {
			return columns;
		}

		// Split by comma (,) or Chinese comma (，)
		String[] parts = columnsInString.split("[,，]");
		for (String part : parts) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				columns.add(trimmed);
			}
		}

		return columns;
	}

	/**
	 * Cleanup work after execution is completed.
	 */
	protected void performCleanup(ExecutionContext context, BaseAgent lastExecutor) {
		String planId = context.getCurrentPlanId();
		llmService.clearAgentMemory(planId);
		if (lastExecutor != null) {
			lastExecutor.clearUp(planId);
		}
	}

}
