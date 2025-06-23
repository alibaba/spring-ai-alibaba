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

/**
 * NodeData for ToolNode, in addition to the original llmResponseKey, outputKey,
 * toolNames.
 */
public class ToolNodeData extends NodeData {

	private String llmResponseKey;

	private String outputKey;

	private List<String> toolNames;

	private List<String> toolCallbacks;

	public ToolNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public ToolNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
		super(inputs, outputs);
	}

	public String getLlmResponseKey() {
		return llmResponseKey;
	}

	public ToolNodeData setLlmResponseKey(String llmResponseKey) {
		this.llmResponseKey = llmResponseKey;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public ToolNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public List<String> getToolNames() {
		return toolNames;
	}

	public ToolNodeData setToolNames(List<String> toolNames) {
		this.toolNames = toolNames;
		return this;
	}

	public List<String> getToolCallbacks() {
		return toolCallbacks;
	}

	public ToolNodeData setToolCallbacks(List<String> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
		return this;
	}

}
