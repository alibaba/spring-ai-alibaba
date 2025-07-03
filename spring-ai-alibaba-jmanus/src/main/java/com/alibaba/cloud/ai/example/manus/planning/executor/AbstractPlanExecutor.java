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
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 计划执行器的抽象基类 包含所有执行器类型的共同逻辑和基本功能
 */
public abstract class AbstractPlanExecutor implements PlanExecutorInterface {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPlanExecutor.class);

	protected final PlanExecutionRecorder recorder;

	// 匹配字符串开头的方括号，支持中文和其他字符
	protected final Pattern pattern = Pattern.compile("^\\s*\\[([^\\]]+)\\]");

	protected final List<DynamicAgentEntity> agents;

	protected final AgentService agentService;

	protected LlmService llmService;

	// Define static final strings for the keys used in executorParams
	public static final String PLAN_STATUS_KEY = "planStatus";

	public static final String CURRENT_STEP_INDEX_KEY = "currentStepIndex";

	public static final String STEP_TEXT_KEY = "stepText";

	public static final String EXTRA_PARAMS_KEY = "extraParams";

	public static final String EXECUTION_ENV_STRING_KEY = "current_step_env_data";

	public AbstractPlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, LlmService llmService) {
		this.agents = agents;
		this.recorder = recorder;
		this.agentService = agentService;
		this.llmService = llmService;
	}

	/**
	 * 执行单个步骤的通用逻辑
	 * @param step 执行步骤
	 * @param context 执行上下文
	 * @return 步骤执行器
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

			recordStepStart(step, context);
			String stepResultStr = executor.run();
			step.setResult(stepResultStr);

			return executor;
		}
		catch (Exception e) {
			logger.error("Error executing step: {}", e.getMessage(), e);
			step.setResult("Execution failed: " + e.getMessage());
		}
		finally {
			recordStepEnd(step, context);
		}
		return null;
	}

	/**
	 * 从步骤需求中提取步骤类型
	 */
	protected String getStepFromStepReq(String stepRequirement) {
		Matcher matcher = pattern.matcher(stepRequirement);
		if (matcher.find()) {
			return matcher.group(1).trim().toLowerCase();
		}
		return "DEFAULT_AGENT";
	}

	/**
	 * 获取步骤的执行器
	 */
	protected BaseAgent getExecutorForStep(String stepType, ExecutionContext context,
			Map<String, Object> initSettings,List<String> columns) {
		for (DynamicAgentEntity agent : agents) {
			if (agent.getAgentName().equalsIgnoreCase(stepType)) {
				BaseAgent executor = agentService.createDynamicBaseAgent(agent.getAgentName(), context.getPlan().getCurrentPlanId(),context.getPlan().getRootPlanId(),
						initSettings, columns);
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
	 * 记录计划执行开始
	 */
	protected void recordPlanExecutionStart(ExecutionContext context) {
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(context);

		record.setCurrentPlanId(context.getPlan().getCurrentPlanId());
		record.setStartTime(LocalDateTime.now());
		record.setTitle(context.getPlan().getTitle());
		record.setUserRequest(context.getUserRequest());
		retrieveExecutionSteps(context, record);
		getRecorder().recordPlanExecution(record);
	}

	/**
	 * 检索执行步骤信息
	 */
	protected void retrieveExecutionSteps(ExecutionContext context, PlanExecutionRecord record) {
		List<String> steps = new ArrayList<>();
		for (ExecutionStep step : context.getPlan().getAllSteps()) {
			steps.add(step.getStepInStr());
		}
		record.setSteps(steps);
	}

	/**
	 * 获取或创建计划执行记录
	 */
	protected PlanExecutionRecord getOrCreatePlanExecutionRecord(ExecutionContext context) {
		PlanExecutionRecord record = getRecorder().getOrCreatePlanExecutionRecord(context.getCurrentPlanId(), context.getRootPlanId(), context.getThinkActRecordId());
		return record;
	}

	/**
	 * 记录步骤执行开始
	 */
	protected void recordStepStart(ExecutionStep step, ExecutionContext context) {
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(context);
		if (record != null) {
			int currentStepIndex = step.getStepIndex();
			record.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, record);
			getRecorder().recordPlanExecution(record);
		}
	}

	/**
	 * 记录步骤执行完成
	 */
	protected void recordStepEnd(ExecutionStep step, ExecutionContext context) {
		PlanExecutionRecord record = getOrCreatePlanExecutionRecord(context);
		if (record != null) {
			int currentStepIndex = step.getStepIndex();
			record.setCurrentStepIndex(currentStepIndex);
			retrieveExecutionSteps(context, record);
			getRecorder().recordPlanExecution(record);
		}
	}

	/**
	 * Parse columns string by splitting with comma or Chinese comma
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
	 * 执行完成后的清理工作
	 */
	protected void performCleanup(ExecutionContext context, BaseAgent lastExecutor) {
		String planId = context.getCurrentPlanId();
		llmService.clearAgentMemory(planId);
		if (lastExecutor != null) {
			lastExecutor.clearUp(planId);
		}
	}

}
