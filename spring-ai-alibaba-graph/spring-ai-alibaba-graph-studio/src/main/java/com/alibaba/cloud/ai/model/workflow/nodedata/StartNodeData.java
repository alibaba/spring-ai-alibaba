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

import java.util.List;

public class StartNodeData extends NodeData {

	private List<StartInput> startInputs;

	public StartNodeData() {
	}

	public StartNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public List<StartInput> getStartInputs() {
		return startInputs;
	}

	public StartNodeData setStartInputs(List<StartInput> startInputs) {
		this.startInputs = startInputs;
		return this;
	}

	public static class StartInput {

		private String label;

		private String type;

		private String variable;

		private Integer maxLength;

		private List<String> options;

		private Boolean required;

		public String getLabel() {
			return label;
		}

		public StartInput setLabel(String label) {
			this.label = label;
			return this;
		}

		public String getType() {
			return type;
		}

		public StartInput setType(String type) {
			this.type = type;
			return this;
		}

		public String getVariable() {
			return variable;
		}

		public StartInput setVariable(String variable) {
			this.variable = variable;
			return this;
		}

		public Integer getMaxLength() {
			return maxLength;
		}

		public StartInput setMaxLength(Integer maxLength) {
			this.maxLength = maxLength;
			return this;
		}

		public List<String> getOptions() {
			return options;
		}

		public StartInput setOptions(List<String> options) {
			this.options = options;
			return this;
		}

		public Boolean getRequired() {
			return required;
		}

		public StartInput setRequired(Boolean required) {
			this.required = required;
			return this;
		}

	}

}
