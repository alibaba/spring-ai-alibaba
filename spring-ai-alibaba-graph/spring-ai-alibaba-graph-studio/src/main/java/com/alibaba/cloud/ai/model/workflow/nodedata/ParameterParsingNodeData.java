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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NodeData for ParameterParsingNode, which contains three fields: inputTextKey,
 * parameters, and outputKey.
 */
public class ParameterParsingNodeData extends NodeData {

	private String inputTextKey;

	private List<Map<String, String>> parameters;

	private String outputKey;

	public ParameterParsingNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public ParameterParsingNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
		super(inputs, outputs);
	}

	public String getInputTextKey() {
		return inputTextKey;
	}

	public ParameterParsingNodeData setInputTextKey(String inputTextKey) {
		this.inputTextKey = inputTextKey;
		return this;
	}

	public List<Map<String, String>> getParameters() {
		return parameters;
	}

	public ParameterParsingNodeData setParameters(List<Map<String, String>> parameters) {
		this.parameters = parameters;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public ParameterParsingNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

}
