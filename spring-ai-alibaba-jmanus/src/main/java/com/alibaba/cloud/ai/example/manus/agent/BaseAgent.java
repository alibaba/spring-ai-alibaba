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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import java.time.LocalDateTime;
import java.util.*;

/**
 * An abstract base class for implementing AI agents that can execute multi-step tasks.
 * This class provides the core functionality for managing agent state, conversation flow,
 * and step-by-step execution of tasks.
 *
 * <p>
 * The agent supports a finite number of execution steps and includes mechanisms for:
 * <ul>
 * <li>State management (idle, running, finished)</li>
 * <li>Conversation tracking</li>
 * <li>Step limitation and monitoring</li>
 * <li>Thread-safe execution</li>
 * <li>Stuck-state detection and handling</li>
 * </ul>
 *
 * <p>
 * Implementing classes must define:
 * <ul>
 * <li>{@link #getName()} - Returns the agent's name</li>
 * <li>{@link #getDescription()} - Returns the agent's description</li>
 * <li>{@link #getThinkMessage()} - Implements the thinking chain logic</li>
 * <li>{@link #getNextStepWithEnvMessage()} - Provides the next step's prompt
 * template</li>
 * <li>{@link #step()} - Implements the core logic for each execution step</li>
 * </ul>
 *
 * @see AgentState
 * @see LlmService
 */
public abstract class BaseAgent {

	private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

	private String currentPlanId = null;

	private String rootPlanId = null;

	// Think-act record ID for sub-plan executions triggered by tool calls
	private Long thinkActRecordId = null;

	private AgentState state = AgentState.NOT_STARTED;

	protected LlmService llmService;

	protected final ManusProperties manusProperties;

	protected final PromptService promptService;

	private int maxSteps;

	private int currentStep = 0;

	// Change the data map to an immutable object and initialize it properly
	private final Map<String, Object> initSettingData;

	private Map<String, Object> envData = new HashMap<>();

	protected PlanExecutionRecorder planExecutionRecorder;

	public abstract void clearUp(String planId);

	/**
	 * Get the name of the agent
	 *
	 * Implementation requirements: 1. Return a short but descriptive name 2. The name
	 * should reflect the main functionality or characteristics of the agent 3. The name
	 * should be unique for easy logging and debugging
	 *
	 * Example implementations: - ToolCallAgent returns "ToolCallAgent" - BrowserAgent
	 * returns "BrowserAgent"
	 * @return The name of the agent
	 */
	public abstract String getName();

	/**
	 * Get the detailed description of the agent
	 *
	 * Implementation requirements: 1. Return a detailed description of the agent's
	 * functionality 2. The description should include the agent's main responsibilities
	 * and capabilities 3. Should explain how this agent differs from other agents
	 *
	 * Example implementations: - ToolCallAgent: "Agent responsible for managing and
	 * executing tool calls, supporting multi-tool combination calls" - ReActAgent: "Agent
	 * that implements alternating execution of reasoning and acting"
	 * @return The detailed description text of the agent
	 */
	public abstract String getDescription();

	/**
	 * Add thinking prompts to the message list to build the agent's thinking chain
	 *
	 * Implementation requirements: 1. Generate appropriate system prompts based on
	 * current context and state 2. Prompts should guide the agent on how to think and
	 * make decisions 3. Can recursively build prompt chains to form hierarchical thinking
	 * processes 4. Return the added system prompt message object
	 *
	 * Subclass implementation reference: 1. ReActAgent: Implement basic thinking-action
	 * loop prompts 2. ToolCallAgent: Add tool selection and execution related prompts
	 * @return The added system prompt message object
	 */
	protected Message getThinkMessage() {
		// Get operating system information
		String osName = System.getProperty("os.name");
		String osVersion = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");

		// Get current date time, format as yyyy-MM-dd
		String currentDateTime = java.time.LocalDate.now().toString(); // Format as
																		// yyyy-MM-dd
		boolean isDebugModel = manusProperties.getDebugDetail();
		String detailOutput = "";
		if (isDebugModel) {
			detailOutput = """
					1. 使用工具调用时，必须给出解释说明，说明使用这个工具的理由和背后的思考
					2. 简述过去的所有步骤已经都做了什么事
					""";
		}
		else {
			detailOutput = """
					1. 使用工具调用时，不需要额外的任何解释说明！
					2. 不要在工具调用前提供推理或描述！
					""";

		}

		Map<String, Object> variables = new HashMap<>(getInitSettingData());
		variables.put("osName", osName);
		variables.put("osVersion", osVersion);
		variables.put("osArch", osArch);
		variables.put("currentDateTime", currentDateTime);
		variables.put("detailOutput", detailOutput);

		return promptService.createSystemMessage(PromptEnum.AGENT_STEP_EXECUTION.getPromptName(), variables);
	}

	/**
	 * Get the next step prompt message
	 *
	 * Implementation requirements: 1. Generate a prompt message that guides the agent to
	 * perform the next step 2. The prompt should be based on the current execution state
	 * and context 3. The message should clearly guide the agent on what task to perform
	 *
	 * Subclass implementation reference: 1. ToolCallAgent: Return prompts related to tool
	 * selection and execution 2. ReActAgent: Return prompts related to reasoning or
	 * action decision
	 * @return The next step prompt message object
	 */
	protected abstract Message getNextStepWithEnvMessage();

	public abstract List<ToolCallback> getToolCallList();

	public abstract ToolCallBackContext getToolCallBackContext(String toolKey);

	public BaseAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
			ManusProperties manusProperties, Map<String, Object> initialAgentSetting, PromptService promptService) {
		this.llmService = llmService;
		this.planExecutionRecorder = planExecutionRecorder;
		this.manusProperties = manusProperties;
		this.promptService = promptService;
		this.maxSteps = manusProperties.getMaxSteps();
		this.initSettingData = Collections.unmodifiableMap(new HashMap<>(initialAgentSetting));
	}

	public String run() {
		currentStep = 0;
		if (state != AgentState.IN_PROGRESS) {
			throw new IllegalStateException("Cannot run agent from state: " + state);
		}

		PlanExecutionRecord planRecord = null;

		// Create agent execution record
		AgentExecutionRecord agentRecord = new AgentExecutionRecord(getCurrentPlanId(), getName(), getDescription());
		agentRecord.setMaxSteps(maxSteps);
		agentRecord.setStatus(state.toString());
		// Record execution in recorder if we have a plan ID
		if (currentPlanId != null && planExecutionRecorder != null) {
			// Use unified method that handles both main plan and sub-plan cases
			planRecord = planExecutionRecorder.getExecutionRecord(currentPlanId, rootPlanId, thinkActRecordId);

			if (planRecord != null) {
				planExecutionRecorder.recordAgentExecution(planRecord, agentRecord);
			}
		}
		List<String> results = new ArrayList<>();
		try {
			state = AgentState.IN_PROGRESS;
			agentRecord.setStatus(state.toString());

			while (currentStep < maxSteps && !state.equals(AgentState.COMPLETED)) {
				currentStep++;
				log.info("Executing round {}/{}", currentStep, maxSteps);

				AgentExecResult stepResult = step();

				if (isStuck()) {
					handleStuckState(agentRecord);
				}
				else {
					// Update global state for consistency
					log.info("Agent state: {}", stepResult.getState());
					state = stepResult.getState();
				}

				results.add("Round " + currentStep + ": " + stepResult.getResult());

				// Update agent record after each step
				agentRecord.setCurrentStep(currentStep);
			}

			if (currentStep >= maxSteps) {
				results.add("Terminated: Reached max rounds (" + maxSteps + ")");
			}

			// Set final state in record
			agentRecord.setEndTime(LocalDateTime.now());
			agentRecord.setStatus(state.toString());
			agentRecord.setCompleted(state.equals(AgentState.COMPLETED));

			// Calculate execution time in seconds
			long executionTimeSeconds = java.time.Duration.between(agentRecord.getStartTime(), agentRecord.getEndTime())
				.getSeconds();
			String status = agentRecord.isCompleted() ? "成功" : (agentRecord.isStuck() ? "执行卡住" : "未完成");
			agentRecord.setResult(String.format("执行%s [耗时%d秒] [消耗步骤%d] ", status, executionTimeSeconds, currentStep));

		}
		catch (Exception e) {
			log.error("Agent execution failed", e);
			// Record exception information to agentRecord
			agentRecord.setErrorMessage(e.getMessage());
			agentRecord.setCompleted(false);
			agentRecord.setEndTime(LocalDateTime.now());
			agentRecord.setResult(String.format("执行失败 [错误: %s]", e.getMessage()));
			results.add("Execution failed: " + e.getMessage());
			throw e; // Re-throw the exception to let the caller know that an error
			// occurred
		}
		finally {
			state = AgentState.COMPLETED; // Reset state after execution

			agentRecord.setStatus(state.toString());
			if (planRecord != null) {
				planExecutionRecorder.recordAgentExecution(planRecord, agentRecord);
			}
			llmService.clearAgentMemory(currentPlanId);
		}
		return results.isEmpty() ? "" : results.get(results.size() - 1);
	}

	protected abstract AgentExecResult step();

	private void handleStuckState(AgentExecutionRecord agentRecord) {
		log.warn("Agent stuck detected - Missing tool calls");

		// End current step
		setState(AgentState.COMPLETED);

		String stuckPrompt = """
				Agent response detected missing required tool calls.
				Please ensure each response includes at least one tool call to progress the task.
				Current step: %d
				Execution status: Force terminated
				""".formatted(currentStep);

		// Update agent record
		agentRecord.setStuck(true);
		agentRecord.setErrorMessage(stuckPrompt);
		agentRecord.setStatus(state.toString());

		log.error(stuckPrompt);
	}

	/**
	 * Check if the agent is stuck
	 * @return true if the agent is stuck, false otherwise
	 */
	protected boolean isStuck() {
		// Currently, if the agent does not call the tool three times, it is considered
		// stuck and the current step is exited.
		List<Message> memoryEntries = llmService.getAgentMemory(manusProperties.getMaxMemory()).get(getCurrentPlanId());
		int zeroToolCallCount = 0;
		for (Message msg : memoryEntries) {
			if (msg instanceof AssistantMessage) {
				AssistantMessage assistantMsg = (AssistantMessage) msg;
				if (assistantMsg.getToolCalls() == null || assistantMsg.getToolCalls().isEmpty()) {
					zeroToolCallCount++;
				}
			}
		}
		return zeroToolCallCount >= 3;
	}

	public void setState(AgentState state) {
		this.state = state;
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}

	public void setCurrentPlanId(String planId) {
		this.currentPlanId = planId;
	}

	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	public String getRootPlanId() {
		return rootPlanId;
	}

	public Long getThinkActRecordId() {
		return thinkActRecordId;
	}

	public void setThinkActRecordId(Long thinkActRecordId) {
		this.thinkActRecordId = thinkActRecordId;
	}

	/**
	 * Check if this agent is executing a sub-plan triggered by a tool call
	 * @return true if this is a sub-plan execution, false otherwise
	 */
	public boolean isSubPlanExecution() {
		return thinkActRecordId != null;
	}

	public AgentState getState() {
		return state;
	}

	/**
	 * Get the data context of the agent
	 *
	 * Implementation requirements: 1. Return all the context data needed for the agent's
	 * execution 2. Data can include: - Current execution state - Step information -
	 * Intermediate results - Configuration parameters 3. Data is set through setData()
	 * when run() is executed
	 *
	 * Do not modify the implementation of this method. If you need to pass context,
	 * inherit and modify setData() to improve getData() efficiency.
	 * @return A Map object containing the agent's context data
	 */
	protected final Map<String, Object> getInitSettingData() {
		return initSettingData;
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	public static class AgentExecResult {

		private String result;

		private AgentState state;

		public AgentExecResult(String result, AgentState state) {
			this.result = result;
			this.state = state;
		}

		public String getResult() {
			return result;
		}

		public AgentState getState() {
			return state;
		}

	}

	public Map<String, Object> getEnvData() {
		return envData;
	}

	public void setEnvData(Map<String, Object> envData) {
		this.envData = Collections.unmodifiableMap(new HashMap<>(envData));
	}

}
