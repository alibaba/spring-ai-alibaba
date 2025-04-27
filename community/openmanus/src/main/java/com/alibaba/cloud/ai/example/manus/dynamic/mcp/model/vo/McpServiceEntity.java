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

import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;

public class McpServiceEntity {

	private McpAsyncClient mcpAsyncClient;

	private AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider;

	private String serviceGroup;

	/**
	 * 创建一个 McpServiceEntity 实例
	 * @param mcpAsyncClient MCP异步客户端
	 * @param asyncMcpToolCallbackProvider 异步MCP工具回调提供者
	 * @param serviceGroup 服务组
	 */
	public McpServiceEntity(McpAsyncClient mcpAsyncClient, AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider,
			String serviceGroup) {
		this.mcpAsyncClient = mcpAsyncClient;
		this.asyncMcpToolCallbackProvider = asyncMcpToolCallbackProvider;
		this.serviceGroup = serviceGroup;
	}

	public McpAsyncClient getMcpAsyncClient() {
		return mcpAsyncClient;
	}

	public void setMcpAsyncClient(McpAsyncClient mcpAsyncClient) {
		this.mcpAsyncClient = mcpAsyncClient;
	}

	public AsyncMcpToolCallbackProvider getAsyncMcpToolCallbackProvider() {
		return asyncMcpToolCallbackProvider;
	}

	public void setAsyncMcpToolCallbackProvider(AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider) {
		this.asyncMcpToolCallbackProvider = asyncMcpToolCallbackProvider;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

}
