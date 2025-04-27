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
 * 单个步骤的执行结果
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
	 * 将步骤转换为JSON字符串
	 * @return 步骤的JSON字符串表示
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
	 * 从JsonNode解析并创建ExecutionStep对象
	 * @param stepNode JsonNode对象
	 * @return 解析后的ExecutionStep对象
	 */
	public static ExecutionStep fromJson(com.fasterxml.jackson.databind.JsonNode stepNode) {
		ExecutionStep step = new ExecutionStep();

		// 设置步骤需求
		String stepRequirement = stepNode.has("stepRequirement") ? stepNode.get("stepRequirement").asText() : "未指定步骤";
		step.setStepRequirement(stepRequirement);

		// 设置步骤索引（如果有）
		if (stepNode.has("stepIndex")) {
			step.setStepIndex(stepNode.get("stepIndex").asInt());
		}

		// 设置步骤结果（如果有）
		if (stepNode.has("result")) {
			step.setResult(stepNode.get("result").asText());
		}

		return step;
	}

}
