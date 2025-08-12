/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.service;

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * MCP 代理服务 参考 spring-ai-alibaba-mcp-gateway-nacos 的实现，提供完整的 MCP 服务代理功能
 */
public class McpProxyService {

	private static final Logger logger = LoggerFactory.getLogger(McpProxyService.class);

	private final NacosMcpOperationService nacosMcpOperationService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// 缓存已建立的连接
	private final Map<String, McpSyncClient> clientConnections = new ConcurrentHashMap<>();

	public McpProxyService(NacosMcpOperationService nacosMcpOperationService) {
		this.nacosMcpOperationService = nacosMcpOperationService;
	}

	/**
	 * 调用 MCP 工具
	 * @param serviceName 服务名称
	 * @param toolName 工具名称
	 * @param args 工具参数
	 * @return 工具执行结果
	 */
	public String callTool(String serviceName, String toolName, Map<String, Object> args) {
		try {
			// 1. 获取服务详情
			McpServerDetailInfo serverDetail = nacosMcpOperationService.getServerDetail(serviceName);
			if (serverDetail == null) {
				throw new RuntimeException("Service not found: " + serviceName);
			}

			String protocol = serverDetail.getProtocol();
			McpServerRemoteServiceConfig remoteConfig = serverDetail.getRemoteServerConfig();

			// 2. 将工具名称添加到参数中
			Map<String, Object> enrichedArgs = new HashMap<>(args);
			enrichedArgs.put("toolName", toolName);

			// 3. 根据协议类型处理
			switch (protocol.toLowerCase()) {
				case "http":
				case "https":
					return handleHttpHttpsProtocol(enrichedArgs, remoteConfig, protocol);
				case "mcp-sse":
				case "mcp-streamable":
					return handleMcpStreamProtocol(enrichedArgs, remoteConfig, protocol);
				default:
					throw new RuntimeException("Unsupported protocol: " + protocol);
			}
		}
		catch (Exception e) {
			logger.error("Failed to call tool: {} on service: {}", toolName, serviceName, e);
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * 处理 HTTP/HTTPS 协议
	 */
	private String handleHttpHttpsProtocol(Map<String, Object> args, McpServerRemoteServiceConfig remoteServerConfig,
			String protocol) throws NacosException {
		McpServiceRef serviceRef = remoteServerConfig.getServiceRef();
		if (serviceRef == null) {
			throw new RuntimeException("Service reference is null");
		}

		McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
		if (mcpEndpointInfo == null) {
			throw new RuntimeException("No available endpoint found for service: " + serviceRef.getServiceName());
		}

		logger.info("HTTP/HTTPS Tool callback instance: {}", JacksonUtils.toJson(mcpEndpointInfo));
		String baseUrl = protocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();

		// 这里可以实现 HTTP/HTTPS 协议的具体处理逻辑
		// 目前返回模拟响应
		return String.format("HTTP/HTTPS protocol response - BaseUrl: %s, Args: %s", baseUrl, args);
	}

	/**
	 * 处理 MCP 流式协议 (mcp-sse, mcp-stream)
	 */
	private String handleMcpStreamProtocol(Map<String, Object> args, McpServerRemoteServiceConfig remoteServerConfig,
			String protocol) throws NacosException {
		McpServiceRef serviceRef = remoteServerConfig.getServiceRef();
		if (serviceRef == null) {
			throw new RuntimeException("Service reference is null");
		}

		McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
		if (mcpEndpointInfo == null) {
			throw new RuntimeException("No available endpoint found for service: " + serviceRef.getServiceName());
		}

		logger.info("MCP Stream Tool callback instance: {}", JacksonUtils.toJson(mcpEndpointInfo));
		String exportPath = remoteServerConfig.getExportPath();

		// 构建基础URL
		String baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
		String sseEndpoint = exportPath != null && !exportPath.isEmpty() ? exportPath : "/sse";

		logger.info("Processing {} protocol with args: {} and baseUrl: {} endpoint: {}", protocol, args, baseUrl,
				sseEndpoint);

		// 验证连接可用性
		if (!isEndpointReachable(baseUrl + sseEndpoint)) {
			String diagnosis = diagnoseEndpoint(baseUrl, sseEndpoint);
			return String.format("Error: Cannot reach MCP Server endpoint at %s%s\n\n%s", baseUrl, sseEndpoint,
					diagnosis);
		}

		McpSyncClient client = null;
		try {
			// 构建传输层
			HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(baseUrl)
				.sseEndpoint(sseEndpoint);

			HttpClientSseClientTransport transport = transportBuilder.build();

			// 创建MCP同步客户端
			client = McpClient.sync(transport).build();

			// 初始化客户端
			logger.info("MCP Client initializing: baseUrl {} sseEndpoint {}", baseUrl, sseEndpoint);
			InitializeResult initializeResult = client.initialize();
			logger.info("MCP Client initialized: {}", initializeResult);

			// 从参数中提取工具名称，如果没有提供则使用默认值
			String toolName = extractToolNameFromArgs(args);
			if (toolName == null || toolName.isEmpty()) {
				return "Error: Tool name not provided in arguments";
			}

			// 调用工具
			McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
			logger.info("CallToolRequest: {}", request);

			CallToolResult result = client.callTool(request);
			logger.info("Tool call result: {}", result);

			// 处理结果
			return processToolResult(result);

		}
		catch (Exception e) {
			logger.error("MCP stream call failed:", e);

			// 提供详细的错误诊断
			StringBuilder errorInfo = new StringBuilder();
			errorInfo.append("Error: MCP stream call failed - ").append(e.getMessage()).append("\n\n");

			// 如果是连接相关错误，提供诊断信息
			if (e.getMessage().contains("Failed to wait for the message endpoint") || e.getMessage().contains("502")
					|| e.getMessage().contains("connection")) {
				errorInfo.append("=== Connection Diagnosis ===\n");
				errorInfo.append("Target URL: ").append(baseUrl).append(sseEndpoint).append("\n");
				errorInfo.append("Protocol: ").append(protocol).append("\n");
				errorInfo.append("Service: ").append(serviceRef.getServiceName()).append("\n\n");

				String diagnosis = diagnoseEndpoint(baseUrl, sseEndpoint);
				errorInfo.append(diagnosis);

				errorInfo.append("\n=== Troubleshooting Steps ===\n");
				errorInfo.append("1. Verify the target server is running\n");
				errorInfo.append("2. Check if the server is accessible from this machine\n");
				errorInfo.append("3. Verify the endpoint path is correct\n");
				errorInfo.append("4. Check server logs for any errors\n");
				errorInfo.append("5. Verify network connectivity and firewall settings\n");
			}

			return errorInfo.toString();
		}
		finally {
			// 清理资源
			if (client != null) {
				try {
					client.close();
				}
				catch (Exception e) {
					logger.warn("Failed to close MCP client", e);
				}
			}
		}
	}

	/**
	 * 处理工具调用结果
	 */
	private String processToolResult(CallToolResult result) {
		Object content = result.content();
		if (content instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
			Object first = list.get(0);
			// 兼容TextContent的text字段
			if (first instanceof TextContent textContent) {
				return textContent.text();
			}
			else if (first instanceof Map<?, ?> map && map.containsKey("text")) {
				return map.get("text").toString();
			}
			else {
				return first.toString();
			}
		}
		else {
			return content != null ? content.toString() : "No content returned";
		}
	}

	/**
	 * 从参数中提取工具名称
	 */
	public String extractToolNameFromArgs(Map<String, Object> args) {
		// 尝试从参数中获取工具名称
		if (args.containsKey("toolName")) {
			return args.get("toolName").toString();
		}
		if (args.containsKey("tool_name")) {
			return args.get("tool_name").toString();
		}
		if (args.containsKey("name")) {
			return args.get("name").toString();
		}
		return null;
	}

	/**
	 * 验证端点是否可达
	 */
	private boolean isEndpointReachable(String endpointUrl) {
		try {
			logger.info("Checking endpoint reachability: {}", endpointUrl);

			java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(endpointUrl))
				.method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
				.timeout(java.time.Duration.ofSeconds(10))
				.build();

			java.net.http.HttpResponse<Void> response = client.send(request,
					java.net.http.HttpResponse.BodyHandlers.discarding());

			int statusCode = response.statusCode();
			logger.info("Endpoint {} returned status code: {}", endpointUrl, statusCode);

			// 接受 2xx 和 3xx 状态码，拒绝 4xx 和 5xx
			boolean reachable = statusCode >= 200 && statusCode < 400;
			if (!reachable) {
				logger.warn("Endpoint {} is not reachable, status code: {}", endpointUrl, statusCode);
			}

			return reachable;
		}
		catch (Exception e) {
			logger.warn("Endpoint {} is not reachable: {}", endpointUrl, e.getMessage());
			return false;
		}
	}

	/**
	 * 详细的端点诊断
	 */
	private String diagnoseEndpoint(String baseUrl, String endpoint) {
		StringBuilder diagnosis = new StringBuilder();
		String fullUrl = baseUrl + endpoint;

		diagnosis.append("=== Endpoint Diagnosis ===\n");
		diagnosis.append("Full URL: ").append(fullUrl).append("\n");

		try {
			// 1. 检查基础URL可达性
			diagnosis.append("1. Checking base URL: ").append(baseUrl).append("\n");
			boolean baseReachable = isEndpointReachable(baseUrl);
			diagnosis.append("   Base URL reachable: ").append(baseReachable ? "✅" : "❌").append("\n");

			// 2. 检查完整端点URL
			diagnosis.append("2. Checking full endpoint: ").append(fullUrl).append("\n");
			boolean endpointReachable = isEndpointReachable(fullUrl);
			diagnosis.append("   Endpoint reachable: ").append(endpointReachable ? "✅" : "❌").append("\n");

			// 3. 尝试GET请求获取更多信息
			if (!endpointReachable) {
				diagnosis.append("3. Attempting GET request for more details...\n");
				try {
					java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
					java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
						.uri(java.net.URI.create(fullUrl))
						.method("GET", java.net.http.HttpRequest.BodyPublishers.noBody())
						.timeout(java.time.Duration.ofSeconds(5))
						.build();

					java.net.http.HttpResponse<String> response = client.send(request,
							java.net.http.HttpResponse.BodyHandlers.ofString());

					diagnosis.append("   Status Code: ").append(response.statusCode()).append("\n");
					diagnosis.append("   Response Headers: ").append(response.headers()).append("\n");

					if (response.statusCode() == 502) {
						diagnosis.append("   ❌ 502 Bad Gateway - Server is not responding properly\n");
						diagnosis.append("   Possible causes:\n");
						diagnosis.append("   - Target server is down\n");
						diagnosis.append("   - Server configuration error\n");
						diagnosis.append("   - Network connectivity issues\n");
						diagnosis.append("   - Proxy/gateway configuration problem\n");
					}

				}
				catch (Exception e) {
					diagnosis.append("   GET request failed: ").append(e.getMessage()).append("\n");
				}
			}

		}
		catch (Exception e) {
			diagnosis.append("Diagnosis failed: ").append(e.getMessage()).append("\n");
		}

		return diagnosis.toString();
	}

	/**
	 * 建立与 MCP Server 的连接
	 * @param serviceName 服务名称
	 * @return 是否成功建立连接
	 */
	public boolean establishConnection(String serviceName) {
		try {
			McpServerDetailInfo serverDetail = nacosMcpOperationService.getServerDetail(serviceName);
			if (serverDetail == null) {
				logger.error("Service not found: {}", serviceName);
				return false;
			}

			String protocol = serverDetail.getProtocol();
			McpServerRemoteServiceConfig remoteConfig = serverDetail.getRemoteServerConfig();

			if (remoteConfig == null || remoteConfig.getServiceRef() == null) {
				logger.error("Remote config or service ref is null for service: {}", serviceName);
				return false;
			}

			McpServiceRef serviceRef = remoteConfig.getServiceRef();
			McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
			if (mcpEndpointInfo == null) {
				logger.error("No available endpoint found for service: {}", serviceName);
				return false;
			}

			// 根据协议类型建立连接
			McpSyncClient client = createClient(protocol, mcpEndpointInfo, remoteConfig);
			if (client != null) {
				clientConnections.put(serviceName, client);
				logger.info("Successfully established connection to service: {}", serviceName);
				return true;
			}

			return false;
		}
		catch (Exception e) {
			logger.error("Failed to establish connection to service: {}", serviceName, e);
			return false;
		}
	}

	/**
	 * 创建 MCP 客户端
	 */
	private McpSyncClient createClient(String protocol, McpEndpointInfo endpointInfo,
			McpServerRemoteServiceConfig remoteConfig) {
		try {
			String baseUrl = "http://" + endpointInfo.getAddress() + ":" + endpointInfo.getPort();
			String exportPath = remoteConfig.getExportPath();

			switch (protocol.toLowerCase()) {
				case "mcp-sse":
				case "mcp-stream":
					String sseEndpoint = exportPath != null && !exportPath.isEmpty() ? exportPath : "/sse";
					HttpClientSseClientTransport sseTransport = HttpClientSseClientTransport.builder(baseUrl)
						.sseEndpoint(sseEndpoint)
						.build();
					return McpClient.sync(sseTransport).build();

				default:
					logger.warn("Unsupported protocol for client creation: {}", protocol);
					return null;
			}
		}
		catch (Exception e) {
			logger.error("Failed to create MCP client for protocol: {}", protocol, e);
			return null;
		}
	}

	/**
	 * 关闭与指定服务的连接
	 * @param serviceName 服务名称
	 */
	public void closeConnection(String serviceName) {
		McpSyncClient client = clientConnections.remove(serviceName);
		if (client != null) {
			try {
				client.close();
				logger.info("Closed connection to service: {}", serviceName);
			}
			catch (Exception e) {
				logger.warn("Failed to close connection to service: {}", serviceName, e);
			}
		}
	}

	/**
	 * 关闭所有连接
	 */
	public void closeAllConnections() {
		for (Map.Entry<String, McpSyncClient> entry : clientConnections.entrySet()) {
			try {
				entry.getValue().close();
				logger.info("Closed connection to service: {}", entry.getKey());
			}
			catch (Exception e) {
				logger.warn("Failed to close connection to service: {}", entry.getKey(), e);
			}
		}
		clientConnections.clear();
	}

	/**
	 * 检查连接状态
	 * @param serviceName 服务名称
	 * @return 是否已连接
	 */
	public boolean isConnected(String serviceName) {
		return clientConnections.containsKey(serviceName);
	}

	/**
	 * 获取连接数量
	 * @return 当前连接数量
	 */
	public int getConnectionCount() {
		return clientConnections.size();
	}

	/**
	 * 调试方法：检查服务连接状态
	 * @param serviceName 服务名称
	 * @return 调试信息
	 */
	public String debugServiceConnection(String serviceName) {
		try {
			StringBuilder debugInfo = new StringBuilder();
			debugInfo.append("=== MCP Service Connection Debug ===\n");
			debugInfo.append("Service Name: ").append(serviceName).append("\n\n");

			// 1. 检查服务详情
			McpServerDetailInfo serverDetail = nacosMcpOperationService.getServerDetail(serviceName);
			if (serverDetail == null) {
				debugInfo.append("❌ Service not found in Nacos\n");
				return debugInfo.toString();
			}
			debugInfo.append("✅ Service found in Nacos\n");
			debugInfo.append("Protocol: ").append(serverDetail.getProtocol()).append("\n");

			// 2. 检查远程配置
			McpServerRemoteServiceConfig remoteConfig = serverDetail.getRemoteServerConfig();
			if (remoteConfig == null) {
				debugInfo.append("❌ Remote config is null\n");
				return debugInfo.toString();
			}
			debugInfo.append("✅ Remote config found\n");

			// 3. 检查服务引用
			McpServiceRef serviceRef = remoteConfig.getServiceRef();
			if (serviceRef == null) {
				debugInfo.append("❌ Service ref is null\n");
				return debugInfo.toString();
			}
			debugInfo.append("✅ Service ref found\n");
			debugInfo.append("Service Ref Name: ").append(serviceRef.getServiceName()).append("\n");

			// 4. 检查端点信息
			McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
			if (mcpEndpointInfo == null) {
				debugInfo.append("❌ No available endpoint found\n");
				return debugInfo.toString();
			}
			debugInfo.append("✅ Endpoint found\n");
			debugInfo.append("Address: ").append(mcpEndpointInfo.getAddress()).append("\n");
			debugInfo.append("Port: ").append(mcpEndpointInfo.getPort()).append("\n");

			// 5. 构建连接URL
			String baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
			String exportPath = remoteConfig.getExportPath();
			String endpoint = exportPath != null && !exportPath.isEmpty() ? exportPath : "/sse";
			String fullUrl = baseUrl + endpoint;

			debugInfo.append("Full URL: ").append(fullUrl).append("\n");

			// 6. 检查端点可达性
			boolean reachable = isEndpointReachable(fullUrl);
			debugInfo.append("Endpoint reachable: ").append(reachable ? "✅" : "❌").append("\n");

			// 7. 如果端点不可达，提供详细诊断
			if (!reachable) {
				debugInfo.append("\n");
				String diagnosis = diagnoseEndpoint(baseUrl, endpoint);
				debugInfo.append(diagnosis);
			}

			// 8. 检查连接状态
			boolean connected = isConnected(serviceName);
			debugInfo.append("Connection cached: ").append(connected ? "✅" : "❌").append("\n");

			return debugInfo.toString();

		}
		catch (Exception e) {
			return "Error during debug: " + e.getMessage();
		}
	}

	/**
	 * 获取客户端连接（用于调试）
	 * @param serviceName 服务名称
	 * @return MCP客户端
	 */
	public McpSyncClient getClient(String serviceName) {
		return clientConnections.get(serviceName);
	}

}
