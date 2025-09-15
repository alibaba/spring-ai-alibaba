/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

import java.util.List;
import java.util.Map;

public class AgentNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("text", VariableType.STRING);
	}

	private Map<String, Object> agentParameterMap;

	private String agentStrategyName;

	private String instructionPrompt;

	private String queryPrompt;

	private List<ToolData> toolList;

	private Integer maxIterations;

	public record ToolData(Map<String, Object> parameters, String toolName, String toolDescription) {
	}

	public Map<String, Object> getAgentParameterMap() {
		return agentParameterMap;
	}

	public void setAgentParameterMap(Map<String, Object> agentParameterMap) {
		this.agentParameterMap = agentParameterMap;
	}

	public String getAgentStrategyName() {
		return agentStrategyName;
	}

	public void setAgentStrategyName(String agentStrategyName) {
		// todo: 支持更多的策略，如MCP，多轮对话
		this.agentStrategyName = switch (agentStrategyName) {
			case "function_calling" -> "TOOL_CALLING";
			default -> "REACT";
		};
	}

	public String getInstructionPrompt() {
		return instructionPrompt;
	}

	public void setInstructionPrompt(String instructionPrompt) {
		this.instructionPrompt = instructionPrompt;
	}

	public String getQueryPrompt() {
		return queryPrompt;
	}

	public void setQueryPrompt(String queryPrompt) {
		this.queryPrompt = queryPrompt;
	}

	public List<ToolData> getToolList() {
		return toolList;
	}

	public void setToolList(List<ToolData> toolList) {
		this.toolList = toolList;
	}

	public Integer getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(Integer maxIterations) {
		this.maxIterations = maxIterations;
	}

}
