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

package com.alibaba.cloud.ai.mcp.nacos2.registry.model;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

/**
 * @author Sunrisea
 */
public class McpToolsInfo {

	private List<McpSchema.Tool> tools;

	private Map<String, ToolMetaInfo> toolsMeta;

	public List<McpSchema.Tool> getTools() {
		return tools;
	}

	public void setTools(List<McpSchema.Tool> tools) {
		this.tools = tools;
	}

	public Map<String, ToolMetaInfo> getToolsMeta() {
		return toolsMeta;
	}

	public void setToolsMeta(Map<String, ToolMetaInfo> toolsMeta) {
		this.toolsMeta = toolsMeta;
	}

}
