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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;

/**
 * The result of a single step execution
 */
public class ExecutionStep {

	private Integer stepIndex;

	private String stepRequirement;

	private String result;

	private BaseAgent agent;

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

	public AgentState getStatus() {
		return agent == null ? AgentState.NOT_STARTED : agent.getState();
	}

	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public String getStepRequirement() {
		return stepRequirement;
	}

	public void setStepRequirement(String stepRequirement) {
		this.stepRequirement = stepRequirement;
	}

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

	/**
	 * Convert the step to a JSON string
	 * @return the JSON string representation of the step
	 */
	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("    {");
		json.append("\"stepRequirement\": \"").append(stepRequirement.replace("\"", "\\\"")).append("\" ");

		if (result != null && !result.isEmpty()) {
			json.append(", \"result\": \"").append(result.replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
		}

		json.append("}");
		return json.toString();
	}

	/**
	 * Parse and create an ExecutionStep object from a JsonNode
	 * @param stepNode JsonNode object
	 * @return the parsed ExecutionStep object
	 */
	public static ExecutionStep fromJson(com.fasterxml.jackson.databind.JsonNode stepNode) {
		ExecutionStep step = new ExecutionStep();

		// Set the step requirement
		String stepRequirement = stepNode.has("stepRequirement") ? stepNode.get("stepRequirement").asText() : "未指定步骤";
		step.setStepRequirement(stepRequirement);

		// Set the step index (if any)
		if (stepNode.has("stepIndex")) {
			step.setStepIndex(stepNode.get("stepIndex").asInt());
		}

		// Set the step result (if any)
		if (stepNode.has("result")) {
			step.setResult(stepNode.get("result").asText());
		}

		return step;
	}

}
