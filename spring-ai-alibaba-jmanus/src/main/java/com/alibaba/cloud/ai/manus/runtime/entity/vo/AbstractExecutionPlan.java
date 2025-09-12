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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract execution plan base class containing common properties and basic
 * implementations for all execution plan types
 */
public abstract class AbstractExecutionPlan implements PlanInterface {

	protected String currentPlanId;

	protected String rootPlanId;

	protected String planTemplateId;

	/**
	 * Plan title
	 */
	protected String title;

	/**
	 * Planning thinking process
	 */
	@JsonIgnore
	protected String planningThinking;

	/**
	 * Execution parameters
	 */
	@JsonIgnore
	protected String executionParams;

	private String userRequest;

	/**
	 * Default constructor
	 */
	public AbstractExecutionPlan() {
		this.executionParams = "";
	}

	/**
	 * Constructor with parameters
	 * @param planId Plan ID
	 * @param title Plan title
	 */
	public AbstractExecutionPlan(String currentPlanId, String rootPlanId, String title) {
		this();
		this.currentPlanId = currentPlanId;
		this.rootPlanId = rootPlanId;
		this.title = title;
	}

	// Implementation of PlanInterface basic properties

	@Override
	public String getCurrentPlanId() {
		return currentPlanId;
	}

	@Override
	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	@Override
	public String getRootPlanId() {
		return rootPlanId;
	}

	@Override
	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getPlanningThinking() {
		return planningThinking;
	}

	@Override
	public void setPlanningThinking(String planningThinking) {
		this.planningThinking = planningThinking;
	}

	@Override
	public String getExecutionParams() {
		return executionParams;
	}

	@Override
	public void setExecutionParams(String executionParams) {
		this.executionParams = executionParams != null ? executionParams : "";
	}

	// Abstract methods - must be implemented by subclasses

	/**
	 * Get flat list of all execution steps
	 * @return All execution steps
	 */
	@Override
	public abstract List<ExecutionStep> getAllSteps();

	/**
	 * Get total step count
	 * @return Total step count
	 */
	@Override
	public abstract int getTotalStepCount();

	/**
	 * Add execution step
	 * @param step Execution step
	 */
	@Override
	public abstract void addStep(ExecutionStep step);

	/**
	 * Remove execution step
	 * @param step Execution step
	 */
	@Override
	public abstract void removeStep(ExecutionStep step);

	/**
	 * Check if plan is empty
	 * @return Return true if plan is empty
	 */
	@Override
	public abstract boolean isEmpty();

	/**
	 * Get string format of plan execution status
	 * @param onlyCompletedAndFirstInProgress When true, only output all completed steps
	 * and first in-progress step
	 * @return Plan status string
	 */
	@Override
	public abstract String getPlanExecutionStateStringFormat(boolean onlyCompletedAndFirstInProgress);

	// Common implementation methods

	@Override
	public void clear() {
		clearSteps();
		planningThinking = null;
		executionParams = "";
	}

	/**
	 * Get user request
	 * @return User request string
	 */
	public String getUserRequest() {
		return userRequest;
	}

	/**
	 * Set user request
	 * @param userRequest User request string
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}

	/**
	 * Whether it's direct feedback mode
	 */
	protected boolean directResponse = false;

	@Override
	public boolean isDirectResponse() {
		return directResponse;
	}

	@Override
	public void setDirectResponse(boolean directResponse) {
		this.directResponse = directResponse;
	}

	public String getPlanTemplateId() {
		return planTemplateId;
	}

	public void setPlanTemplateId(String planTemplateId) {
		this.planTemplateId = planTemplateId;
	}

	/**
	 * Abstract method to clear steps. Subclasses need to implement specific step clearing
	 * logic
	 */
	protected abstract void clearSteps();

	@Override
	public String toString() {
		return "AbstractExecutionPlan{" + "rootPlanId='" + rootPlanId + '\'' + ", currentPlanId='" + currentPlanId
				+ '\'' + ", title='" + title + '\'' + ", planningThinking='" + planningThinking + '\''
				+ ", executionParams='" + executionParams + '\'' + ", userRequest='" + userRequest + '\'' + '}';
	}

}
