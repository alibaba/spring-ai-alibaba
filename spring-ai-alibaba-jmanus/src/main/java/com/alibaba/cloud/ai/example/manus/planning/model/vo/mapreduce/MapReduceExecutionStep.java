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
package com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;

/**
 * MapReduce模式的单个执行步骤
 */
public class MapReduceExecutionStep {

	private Integer stepIndex;

	private String stepRequirement;

	private String outputColumns;

	private String result;

	private BaseAgent agent;

	private MapReduceStepType stepType;

	public MapReduceExecutionStep() {
	}

	public MapReduceExecutionStep(String stepRequirement, String outputColumns, MapReduceStepType stepType) {
		this.stepRequirement = stepRequirement;
		this.outputColumns = outputColumns;
		this.stepType = stepType;
	}

	public Integer getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(Integer stepIndex) {
		this.stepIndex = stepIndex;
	}

	public String getStepRequirement() {
		return stepRequirement;
	}

	public void setStepRequirement(String stepRequirement) {
		this.stepRequirement = stepRequirement;
	}

	public String getOutputColumns() {
		return outputColumns;
	}

	public void setOutputColumns(String outputColumns) {
		this.outputColumns = outputColumns;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public BaseAgent getAgent() {
		return agent;
	}

	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public MapReduceStepType getStepType() {
		return stepType;
	}

	public void setStepType(MapReduceStepType stepType) {
		this.stepType = stepType;
	}

	public AgentState getStatus() {
		return agent == null ? AgentState.NOT_STARTED : agent.getState();
	}

	/**
	 * 获取步骤的字符串表示
	 * @return 步骤的字符串表示
	 */
	public String getStepInStr() {
		String agentState = getStatus().toString();
		StringBuilder sb = new StringBuilder();
		sb.append(stepIndex);
		sb.append(". ");
		sb.append("[").append(agentState).append("]");
		sb.append(" ");
		sb.append("[").append(stepType.name()).append("]");
		sb.append(" ");
		sb.append(stepRequirement);

		if (outputColumns != null && !outputColumns.isEmpty()) {
			sb.append(" [输出列: ").append(outputColumns).append("]");
		}

		return sb.toString();
	}

	/**
	 * 将步骤转换为JSON字符串
	 * @return 步骤的JSON字符串表示
	 */
	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("    {");
		json.append("\"stepRequirement\": \"").append(stepRequirement.replace("\"", "\\\"")).append("\"");

		if (outputColumns != null && !outputColumns.isEmpty()) {
			json.append(", \"outputColumns\": \"").append(outputColumns.replace("\"", "\\\"")).append("\"");
		}

		if (stepType != null) {
			json.append(", \"stepType\": \"").append(stepType.name()).append("\"");
		}

		if (result != null && !result.isEmpty()) {
			json.append(", \"result\": \"").append(result.replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
		}

		json.append("}");
		return json.toString();
	}

	/**
	 * 从JsonNode解析并创建MapReduceExecutionStep对象
	 * @param stepNode JsonNode对象
	 * @param stepType 步骤类型
	 * @return 解析后的MapReduceExecutionStep对象
	 */
	public static MapReduceExecutionStep fromJson(com.fasterxml.jackson.databind.JsonNode stepNode, MapReduceStepType stepType) {
		MapReduceExecutionStep step = new MapReduceExecutionStep();

		// 设置步骤需求
		String stepRequirement = stepNode.has("stepRequirement") ? stepNode.get("stepRequirement").asText() : "未指定步骤";
		step.setStepRequirement(stepRequirement);

		// 设置输出列
		if (stepNode.has("outputColumns")) {
			step.setOutputColumns(stepNode.get("outputColumns").asText());
		}

		// 设置步骤类型
		step.setStepType(stepType);

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

	@Override
	public String toString() {
		return getStepInStr();
	}
}
