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

/**
 * @author vlsmb
 * @since 2025/7/21
 */
public class IterationNodeData extends NodeData {

	private String id;

	private String inputType;

	private String outputType;

	private VariableSelector inputSelector;

	private VariableSelector outputSelector;

	private String startNodeId;

	private String endNodeId;

	private String outputKey;

	private String inputKey;

	private String startNodeName;

	private String endNodeName;

	// 内部临时变量名
	private String innerArrayKey;

	private String innerStartFlagKey;

	private String innerEndFlagKey;

	private String innerItemKey;

	private String innerItemResultKey;

	private String innerIndexKey;

	private Variable output;

	public IterationNodeData(String id, String inputType, String outputType, VariableSelector inputSelector,
			VariableSelector outputSelector, String startNodeId, String endNodeId, String inputKey, String outputKey) {
		this.id = id;
		this.inputType = inputType;
		this.outputType = outputType;
		this.inputSelector = inputSelector;
		this.outputSelector = outputSelector;
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.inputKey = inputKey;
		this.outputKey = outputKey;
		this.setVarName(id);
		this.output = new Variable(outputKey, outputType);
		this.setInputs(List.of(inputSelector));
		this.setOutputs(List.of(output));
	}

	@Override
	public void setVarName(String varName) {
		this.varName = varName;
		this.innerArrayKey = this.varName + "_array";
		this.innerStartFlagKey = this.varName + "_start_flag";
		this.innerEndFlagKey = this.varName + "_end_flag";
		this.innerItemKey = this.varName + "_item";
		this.innerItemResultKey = this.varName + "_item_result";
		this.innerIndexKey = this.varName + "_index";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public VariableSelector getInputSelector() {
		return inputSelector;
	}

	public void setInputSelector(VariableSelector inputSelector) {
		this.inputSelector = inputSelector;
	}

	public VariableSelector getOutputSelector() {
		return outputSelector;
	}

	public void setOutputSelector(VariableSelector outputSelector) {
		this.outputSelector = outputSelector;
	}

	public String getStartNodeId() {
		return startNodeId;
	}

	public void setStartNodeId(String startNodeId) {
		this.startNodeId = startNodeId;
	}

	public String getEndNodeId() {
		return endNodeId;
	}

	public void setEndNodeId(String endNodeId) {
		this.endNodeId = endNodeId;
	}

	public String getInputKey() {
		return inputKey;
	}

	public void setInputKey(String inputKey) {
		this.inputKey = inputKey;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public String getInnerArrayKey() {
		return innerArrayKey;
	}

	public void setInnerArrayKey(String innerArrayKey) {
		this.innerArrayKey = innerArrayKey;
	}

	public String getInnerStartFlagKey() {
		return innerStartFlagKey;
	}

	public void setInnerStartFlagKey(String innerStartFlagKey) {
		this.innerStartFlagKey = innerStartFlagKey;
	}

	public String getInnerEndFlagKey() {
		return innerEndFlagKey;
	}

	public void setInnerEndFlagKey(String innerEndFlagKey) {
		this.innerEndFlagKey = innerEndFlagKey;
	}

	public String getInnerItemKey() {
		return innerItemKey;
	}

	public void setInnerItemKey(String innerItemKey) {
		this.innerItemKey = innerItemKey;
	}

	public String getInnerItemResultKey() {
		return innerItemResultKey;
	}

	public void setInnerItemResultKey(String innerItemResultKey) {
		this.innerItemResultKey = innerItemResultKey;
	}

	public String getInnerIndexKey() {
		return innerIndexKey;
	}

	public void setInnerIndexKey(String innerIndexKey) {
		this.innerIndexKey = innerIndexKey;
	}

	public Variable getOutput() {
		return output;
	}

	public void setOutput(Variable output) {
		this.output = output;
	}

	public String getStartNodeName() {
		return startNodeName;
	}

	public void setStartNodeName(String startNodeName) {
		this.startNodeName = startNodeName;
	}

	public String getEndNodeName() {
		return endNodeName;
	}

	public void setEndNodeName(String endNodeName) {
		this.endNodeName = endNodeName;
	}

	public static class Builder {

		private String id;

		private String inputType;

		private String outputType;

		private VariableSelector inputSelector;

		private VariableSelector outputSelector;

		private String startNodeId;

		private String endNodeId;

		private String inputKey;

		private String outputKey;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder inputType(String inputType) {
			this.inputType = inputType;
			return this;
		}

		public Builder outputType(String outputType) {
			this.outputType = outputType;
			return this;
		}

		public Builder inputSelector(VariableSelector inputSelector) {
			this.inputSelector = inputSelector;
			return this;
		}

		public Builder outputSelector(VariableSelector outputSelector) {
			this.outputSelector = outputSelector;
			return this;
		}

		public Builder startNodeId(String startNodeId) {
			this.startNodeId = startNodeId;
			return this;
		}

		public Builder endNodeId(String endNodeId) {
			this.endNodeId = endNodeId;
			return this;
		}

		public Builder inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public IterationNodeData build() {
			return new IterationNodeData(id, inputType, outputType, inputSelector, outputSelector, startNodeId,
					endNodeId, inputKey, outputKey);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}
