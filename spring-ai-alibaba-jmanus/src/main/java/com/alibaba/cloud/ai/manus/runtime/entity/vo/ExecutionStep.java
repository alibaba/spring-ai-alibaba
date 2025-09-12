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

import com.alibaba.cloud.ai.manus.agent.AgentState;
import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.UUID;

/**
 * The result of a single step execution
 */
public class ExecutionStep {

	/**
	 * Unique identifier for this execution step
	 */
	private final String stepId;

	/**
	 * Default constructor that generates a unique step ID
	 */
	public ExecutionStep() {
		this.stepId = "step-" + UUID.randomUUID().toString();
	}

	public ExecutionStep(String stepId) {
		this.stepId = stepId;
	}

	@JsonIgnore
	private Integer stepIndex;

	private String stepRequirement;

	private String agentName;

	private List<String> selectedToolKeys;

	private String modelName;

	@JsonIgnore
	private String result;

	@JsonIgnore
	private BaseAgent agent;

	private String terminateColumns;

	public Integer getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(Integer stepIndex) {
		this.stepIndex = stepIndex;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@JsonIgnore
	public String getStepId() {
		return stepId;
	}

	public String getTerminateColumns() {
		return terminateColumns;
	}

	public void setTerminateColumns(String terminateColumns) {
		this.terminateColumns = terminateColumns;
	}

	@JsonIgnore
	public AgentState getStatus() {
		return agent == null ? AgentState.NOT_STARTED : agent.getState();
	}

	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public BaseAgent getAgent() {
		return this.agent;
	}

	public String getStepRequirement() {
		return stepRequirement;
	}

	public void setStepRequirement(String stepRequirement) {
		this.stepRequirement = stepRequirement;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public List<String> getSelectedToolKeys() {
		return selectedToolKeys;
	}

	public void setSelectedToolKeys(List<String> selectedToolKeys) {
		this.selectedToolKeys = selectedToolKeys;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	@JsonIgnore
	public String getStepInStr() {
		String agentState = null;
		if (agent != null) {
			agentState = agent.getState().toString();
		}
		else {
			agentState = AgentState.NOT_STARTED.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(stepIndex);
		sb.append(". ");
		sb.append("[").append(agentState).append("]");
		sb.append(" ");
		sb.append(stepRequirement);

		return sb.toString();
	}

}
