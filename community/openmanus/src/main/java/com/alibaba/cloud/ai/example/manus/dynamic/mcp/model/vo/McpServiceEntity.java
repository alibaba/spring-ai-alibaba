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
