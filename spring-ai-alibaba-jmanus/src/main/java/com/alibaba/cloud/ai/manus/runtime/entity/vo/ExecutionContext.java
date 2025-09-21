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
package com.alibaba.cloud.ai.manus.runtime.entity.vo;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context class for passing and maintaining state information during the
 * creation, execution, and summarization of plans. This class serves as the core data
 * carrier in the plan execution process, passing between various stages of
 * {@link com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator}.
 *
 * Main responsibilities: - Store plan ID and plan entity information - Save user original
 * request - Maintain plan execution status - Store execution result summary - Control
 * whether execution summary generation is needed
 *
 * @see com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionPlan
 * @see com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator
 */
public class ExecutionContext {

	private Map<String, String> toolsContext = new HashMap<>();

	/**
	 * Tool context for storing context information of tool execution
	 */
	/** Unique identifier of the plan */
	private String currentPlanId;

	private String rootPlanId;

	private String parentPlanId;

	private String toolCallId;

	/** Execution plan entity containing detailed plan information and execution steps */
	private PlanInterface plan;

	/** User's original request content */
	private String userRequest;

	/**
	 * Whether to call large model to generate summary for execution results, true calls
	 * large model, false does not call and outputs results directly
	 */
	private boolean needSummary;

	/** Flag indicating whether plan execution was successful */
	private boolean success = false;

	/**
	 * The depth of the plan in the execution hierarchy (0 for root, 1 for first level,
	 * etc.)
	 */
	private int planDepth = 0;

	/**
	 * Whether to use memory, scenario is if only building plan, then memory should not be
	 * used, otherwise memory cannot be deleted
	 */
	private boolean useMemory = false;

	/**
	 * Memory ID for memory usage
	 */
	private String memoryId;

	/**
	 * Get plan ID
	 * @return Unique identifier of the plan
	 */
	public String getCurrentPlanId() {
		return currentPlanId;
	}

	/**
	 * Set plan ID
	 * @param currentPlanId Unique identifier of the plan
	 */
	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	public String getRootPlanId() {
		return rootPlanId;
	}

	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	/**
	 * Get parent plan ID
	 * @return Parent plan ID
	 */
	public String getParentPlanId() {
		return parentPlanId;
	}

	/**
	 * Set parent plan ID
	 * @param parentPlanId Parent plan ID
	 */
	public void setParentPlanId(String parentPlanId) {
		this.parentPlanId = parentPlanId;
	}

	/**
	 * Get tool call ID
	 * @return Tool call ID
	 */
	public String getToolCallId() {
		return toolCallId;
	}

	/**
	 * Set tool call ID
	 * @param toolCallId Tool call ID
	 */
	public void setToolCallId(String toolCallId) {
		this.toolCallId = toolCallId;
	}

	/**
	 * Get execution plan entity
	 * @return Execution plan entity object
	 */
	public PlanInterface getPlan() {
		return plan;
	}

	/**
	 * Set execution plan entity
	 * @param plan Execution plan entity object
	 */
	public void setPlan(PlanInterface plan) {
		this.plan = plan;
	}

	/**
	 * Check if execution result summary generation is needed
	 * @return Returns true if summary generation is needed, otherwise false
	 */
	public boolean isNeedSummary() {
		return needSummary;
	}

	/**
	 * Set whether execution result summary generation is needed
	 * @param needSummary Flag indicating whether summary generation is needed
	 */
	public void setNeedSummary(boolean needSummary) {
		this.needSummary = needSummary;
	}

	/**
	 * Check if plan execution was successful
	 * @return Returns true if execution was successful, otherwise false
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Set plan execution success status
	 * @param success Flag indicating execution success status
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Map<String, String> getToolsContext() {
		return toolsContext;
	}

	public void setToolsContext(Map<String, String> toolsContext) {
		this.toolsContext = toolsContext;
	}

	public void addToolContext(String toolsKey, String value) {
		this.toolsContext.put(toolsKey, value);
	}

	/**
	 * Get user's original request content
	 * @return User request string
	 */
	public String getUserRequest() {
		return userRequest;
	}

	/**
	 * Set user's original request content
	 * @param userRequest User request string
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	public boolean isUseMemory() {
		return useMemory;
	}

	public void setUseMemory(boolean useMemory) {
		this.useMemory = useMemory;
	}

	public String getMemoryId() {
		return memoryId;
	}

	public void setMemoryId(String memoryId) {
		this.memoryId = memoryId;
	}

	public int getPlanDepth() {
		return planDepth;
	}

	public void setPlanDepth(int planDepth) {
		this.planDepth = planDepth;
	}

}
