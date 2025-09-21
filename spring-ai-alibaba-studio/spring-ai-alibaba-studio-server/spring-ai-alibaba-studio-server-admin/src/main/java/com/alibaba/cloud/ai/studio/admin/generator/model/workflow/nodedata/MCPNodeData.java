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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

import java.util.List;

// 本类仅考虑Studio的MCP使用，Dify的MCP使用ToolNode定义
public class MCPNodeData extends NodeData {

	private String toolName;

	private String serverName;

	private String serverCode;

	private String inputJsonTemplate = "";

	private List<String> inputKeys = List.of();

	private String outputKey = "output";

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerCode() {
		return serverCode;
	}

	public void setServerCode(String serverCode) {
		this.serverCode = serverCode;
	}

	public String getInputJsonTemplate() {
		return inputJsonTemplate;
	}

	public void setInputJsonTemplate(String inputJsonTemplate) {
		this.inputJsonTemplate = inputJsonTemplate;
	}

	public List<String> getInputKeys() {
		return inputKeys;
	}

	public void setInputKeys(List<String> inputKeys) {
		this.inputKeys = inputKeys;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
