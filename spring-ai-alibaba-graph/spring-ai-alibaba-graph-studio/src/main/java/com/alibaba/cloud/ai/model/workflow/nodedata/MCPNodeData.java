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
 * NodeData for McpNode: Contains fields such as url, tool, headers, params, outputKey,
 * inputParamKeys, etc.
 */
public class MCPNodeData extends NodeData {

	private String url;

	private String tool;

	private Map<String, String> headers;

	private Map<String, Object> params;

	private String outputKey;

	private List<String> inputParamKeys;

	public MCPNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public MCPNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
		super(inputs, outputs);
	}

	public String getUrl() {
		return url;
	}

	public MCPNodeData setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getTool() {
		return tool;
	}

	public MCPNodeData setTool(String tool) {
		this.tool = tool;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public MCPNodeData setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public MCPNodeData setParams(Map<String, Object> params) {
		this.params = params;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public MCPNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public List<String> getInputParamKeys() {
		return inputParamKeys;
	}

	public MCPNodeData setInputParamKeys(List<String> inputParamKeys) {
		this.inputParamKeys = inputParamKeys;
		return this;
	}

}
