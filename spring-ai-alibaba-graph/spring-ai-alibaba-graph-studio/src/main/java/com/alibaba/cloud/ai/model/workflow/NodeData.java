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
package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;

import java.util.List;

/**
 * NodeData defines the behavior of a node. Each subclass represents the behavior of the
 * node.
 */
public class NodeData {

	/**
	 * The inputs of the node is the output reference of the previous node
	 */
	protected List<VariableSelector> inputs;

	/**
	 * The output variables of a node
	 */
	protected List<Variable> outputs;

	public NodeData() {

	}

	protected NodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
	}

	public List<VariableSelector> getInputs() {
		return inputs;
	}

	public NodeData setInputs(List<VariableSelector> inputs) {
		this.inputs = inputs;
		return this;
	}

	public List<Variable> getOutputs() {
		return outputs;
	}

	public NodeData setOutputs(List<Variable> outputs) {
		this.outputs = outputs;
		return this;
	}

	public static String defaultOutputKey(String nodeId) {
		return nodeId + "_output";
	}

}
