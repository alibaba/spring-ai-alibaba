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
package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VariableAggregatorNodeData extends NodeData {

	private List<List<String>> variables;

	private String outputType;

	private AdvancedSettings advancedSettings;

	public List<List<String>> getVariables() {
		return variables;
	}

	public VariableAggregatorNodeData setVariables(List<List<String>> variables) {
		this.variables = variables;
		return this;
	}

	public String getOutputType() {
		return outputType;
	}

	public VariableAggregatorNodeData setOutputType(String outputType) {
		this.outputType = outputType;
		return this;
	}

	public AdvancedSettings getAdvancedSettings() {
		return advancedSettings;
	}

	public VariableAggregatorNodeData setAdvancedSettings(AdvancedSettings advancedSettings) {
		this.advancedSettings = advancedSettings;
		return this;
	}

	public VariableAggregatorNodeData() {
	}

	public VariableAggregatorNodeData(List<VariableSelector> inputs, List<Variable> outputs,
			List<List<String>> variables, String outputType, AdvancedSettings advancedSettings) {
		super(inputs, outputs);
		this.variables = variables;
		this.outputType = outputType;
		this.advancedSettings = advancedSettings;
	}

	public static class Groups {

		@JsonProperty("output_type")
		private String outputType;

		private List<List<String>> variables;

		@JsonProperty("group_name")
		private String groupName;

		private String groupId;

		public String getOutputType() {
			return outputType;
		}

		public Groups setOutputType(String outputType) {
			this.outputType = outputType;
			return this;
		}

		public List<List<String>> getVariables() {
			return variables;
		}

		public Groups setVariables(List<List<String>> variables) {
			this.variables = variables;
			return this;
		}

		public String getGroupName() {
			return groupName;
		}

		public Groups setGroupName(String groupName) {
			this.groupName = groupName;
			return this;
		}

		public String getGroupId() {
			return groupId;
		}

		public Groups setGroupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

	}

	public static class AdvancedSettings {

		private boolean groupEnabled;

		private List<Groups> groups;

		public boolean isGroupEnabled() {
			return groupEnabled;
		}

		public AdvancedSettings setGroupEnabled(boolean groupEnabled) {
			this.groupEnabled = groupEnabled;
			return this;
		}

		public List<Groups> getGroups() {
			return groups;
		}

		public AdvancedSettings setGroups(List<Groups> groups) {
			this.groups = groups;
			return this;
		}

	}

}
