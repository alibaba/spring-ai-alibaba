/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

/**
 * MCP配置请求值对象，用于接收前端传递的配置信息
 */
public class McpConfigRequestVO {

	/**
	 * 连接类型：STUDIO, STREAMING, SSE
	 */
	private String connectionType;

	/**
	 * MCP服务器配置的JSON字符串
	 */
	private String configJson;

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getConfigJson() {
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
	}

}
