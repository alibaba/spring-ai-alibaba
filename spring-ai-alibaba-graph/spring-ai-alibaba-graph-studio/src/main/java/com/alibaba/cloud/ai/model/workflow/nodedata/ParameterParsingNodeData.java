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

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.List;
import java.util.Map;

/**
 * NodeData for ParameterParsingNode, which contains three fields: inputTextKey,
 * parameters, and outputKey.
 */
public class ParameterParsingNodeData extends NodeData {

	private String inputTextKey;

	private List<Map<String, Object>> parameters;

	private String instruction;

	private String outputKey;

	public ParameterParsingNodeData(String inputTextKey, List<Map<String, Object>> parameters, String instruction,
			String outputKey, VariableSelector input) {
		super(List.of(input), List.of());
		this.inputTextKey = inputTextKey;
		this.parameters = parameters;
		this.instruction = instruction;
		this.outputKey = outputKey;
	}

	public String getInputTextKey() {
		return inputTextKey;
	}

	public void setInputTextKey(String inputTextKey) {
		this.inputTextKey = inputTextKey;
	}

	public List<Map<String, Object>> getParameters() {
		return parameters;
	}

	public void setParameters(List<Map<String, Object>> parameters) {
		this.parameters = parameters;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
