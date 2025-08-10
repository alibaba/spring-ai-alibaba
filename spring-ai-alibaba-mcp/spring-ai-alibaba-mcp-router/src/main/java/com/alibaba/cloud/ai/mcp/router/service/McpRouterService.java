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
 *
 */

package com.alibaba.cloud.ai.mcp.router.service;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;

/**
 * MCP Router 核心服务 提供 MCP Server 发现、管理和请求代理功能
 */
@Service
public class McpRouterService {

	private static final Logger logger = LoggerFactory.getLogger(McpRouterService.class);

	private final McpServiceDiscovery mcpServiceDiscovery;

	private final McpServerVectorStore mcpServerVectorStore;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final Map<String, McpServerConnection> serverConnections = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper = new ObjectMapper();

	public McpRouterService(McpServiceDiscovery mcpServiceDiscovery, McpServerVectorStore mcpServerVectorStore,
			NacosMcpOperationService nacosMcpOperationService) {
		this.mcpServiceDiscovery = mcpServiceDiscovery;
		this.mcpServerVectorStore = mcpServerVectorStore;
		this.nacosMcpOperationService = nacosMcpOperationService;
	}

	/**
	 * 搜索 MCP Server 根据任务描述和关键词发现合适的 MCP Server
	 * 
	 * @param taskDescription 任务描述
	 * @param keywords        关键词（可选）
	 * @param limit           返回数量限制
	 * @return 匹配的 MCP Server 列表
	 */
	@Tool(description = "根据任务描述和关键词搜索合适的 MCP Server")
	public String searchMcpServer(@ToolParam(description = "任务描述") String taskDescription,
			@ToolParam(description = "关键词，多个关键词用逗号分隔", required = false) String keywords,
			@ToolParam(description = "返回数量限制，默认5", required = false) Integer limit) {
		try {
			if (limit == null || limit <= 0) {
				limit = 5; // 默认返回数量限制
			}
			// 构建搜索查询
			String searchQuery = taskDescription;
			if (keywords != null && !keywords.trim().isEmpty()) {
				searchQuery += " " + keywords;
			}

			// 使用向量存储进行语义搜索
			List<McpServerInfo> results = mcpServerVectorStore.search(searchQuery, limit);

			if (results.isEmpty()) {
				return "未找到匹配的 MCP Server。请尝试使用不同的关键词或更详细的任务描述。";
			}

			// 构建搜索结果
			StringBuilder response = new StringBuilder();
			response.append("找到以下匹配的 MCP Server：\n\n");

			for (int i = 0; i < results.size(); i++) {
				McpServerInfo server = results.get(i);
				response.append(String.format("%d. %s (相似度: %.2f)\n", i + 1, server.getName(), server.getScore()));
				response.append(String.format("   描述: %s\n",
						server.getDescription() != null ? server.getDescription() : "无描述"));
				response.append(String.format("   协议: %s\n", server.getProtocol()));
				response.append(String.format("   端点: %s\n", server.getEndpoint()));
				if (server.getTags() != null && !server.getTags().isEmpty()) {
					response.append(String.format("   标签: %s\n", String.join(", ", server.getTags())));
				}
				response.append("\n");
			}

			return response.toString();

		} catch (Exception e) {
			logger.error("搜索 MCP Server 时发生错误", e);
			return "搜索 MCP Server 时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 添加 MCP Server 将新的 MCP Server 添加到生态系统中
	 * 
	 * @param serviceName 服务名称
	 * @param description 服务描述（可选）
	 * @param tags        标签（可选，多个标签用逗号分隔）
	 * @return 添加结果
	 */
	@Tool(description = "添加新的 MCP Server 到生态系统")
	public String addMcpServer(@ToolParam(description = "服务名称") String serviceName,
			@ToolParam(description = "服务描述") String description, @ToolParam(description = "标签，多个标签用逗号分隔") String tags) {
		try {
			// 从服务发现获取服务信息
			McpServerInfo serverInfo = mcpServiceDiscovery.getService(serviceName);
			if (serverInfo == null) {
				return String.format("未找到服务 '%s'，请确保服务已在 Nacos 中注册。", serviceName);
			}

			// 如果提供了描述和标签，更新服务信息
			if (description != null && !description.trim().isEmpty()) {
				serverInfo.setDescription(description);
			}

			if (tags != null && !tags.trim().isEmpty()) {
				serverInfo.setTags(List.of(tags.split(",")));
			}

			// 添加到向量存储
			boolean success = mcpServerVectorStore.addServer(serverInfo);

			if (success) {
				// 尝试建立连接
				boolean connected = establishConnection(serverInfo);
				String connectionStatus = connected ? "并成功建立连接" : "但连接建立失败";

				return String.format("成功添加 MCP Server '%s' %s。\n" + "描述: %s\n" + "协议: %s\n" + "端点: %s", serviceName,
						connectionStatus, serverInfo.getDescription(), serverInfo.getProtocol(),
						serverInfo.getEndpoint());
			} else {
				return String.format("添加 MCP Server '%s' 失败，可能已存在。", serviceName);
			}

		} catch (Exception e) {
			logger.error("添加 MCP Server 时发生错误", e);
			return "添加 MCP Server 时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 添加并初始化 MCP Server，根据 Nacos 中的配置与该 MCP Server 建立连接，等待调用
	 * 
	 * @param mcp_server_name 需要添加的 MCP Server 名字
	 * @return MCP Server 工具列表及使用方法
	 */
	@Tool(description = "添加并初始化一个MCP Server，根据Nacos中的配置与该MCP Server建立连接，等待调用")
	public String add_mcp_server(@ToolParam(description = "需要添加的MCP Server名字") String mcp_server_name) {
		try {
			logger.info("开始添加并初始化 MCP Server: {}", mcp_server_name);

			// 1. 从 Nacos 服务发现获取服务信息
			McpServerInfo serverInfo = mcpServiceDiscovery.getService(mcp_server_name);
			if (serverInfo == null) {
				return String.format("未找到 MCP Server '%s'，请确保服务已在 Nacos 中注册。\n" +
						"请检查：\n" +
						"1. 服务名称是否正确\n" +
						"2. 服务是否已在 Nacos 中注册\n" +
						"3. Nacos 配置是否正确", mcp_server_name);
			}

			// 2. 添加到向量存储
			boolean added = mcpServerVectorStore.addServer(serverInfo);
			if (!added) {
				return String.format("添加 MCP Server '%s' 到向量存储失败，可能已存在。", mcp_server_name);
			}

			// 3. 建立连接
			boolean connected = establishConnection(serverInfo);
			if (!connected) {
				return String.format("无法建立与 MCP Server '%s' 的连接。\n" +
						"服务信息：\n" +
						"- 协议: %s\n" +
						"- 端点: %s\n" +
						"- 版本: %s\n" +
						"请检查服务是否正在运行。",
						mcp_server_name, serverInfo.getProtocol(),
						serverInfo.getEndpoint(), serverInfo.getVersion());
			}

			// 4. 获取工具列表
			List<McpToolInfo> tools = getMcpServerTools(mcp_server_name);
			if (tools.isEmpty()) {
				return String.format("成功添加 MCP Server '%s' 并建立连接，但未找到可用工具。\n" +
						"服务信息：\n" +
						"- 描述: %s\n" +
						"- 协议: %s\n" +
						"- 端点: %s\n" +
						"- 版本: %s\n" +
						"- 标签: %s",
						mcp_server_name,
						serverInfo.getDescription() != null ? serverInfo.getDescription() : "无描述",
						serverInfo.getProtocol(),
						serverInfo.getEndpoint(),
						serverInfo.getVersion(),
						serverInfo.getTags() != null ? String.join(", ", serverInfo.getTags()) : "无标签");
			}

			// 5. 构建工具列表和使用方法
			StringBuilder response = new StringBuilder();
			response.append(String.format("成功添加并初始化 MCP Server '%s'\n\n", mcp_server_name));
			response.append("服务信息：\n");
			response.append(String.format("- 描述: %s\n",
					serverInfo.getDescription() != null ? serverInfo.getDescription() : "无描述"));
			response.append(String.format("- 协议: %s\n", serverInfo.getProtocol()));
			response.append(String.format("- 端点: %s\n", serverInfo.getEndpoint()));
			response.append(String.format("- 版本: %s\n", serverInfo.getVersion()));
			if (serverInfo.getTags() != null && !serverInfo.getTags().isEmpty()) {
				response.append(String.format("- 标签: %s\n", String.join(", ", serverInfo.getTags())));
			}
			response.append(String.format("- 连接状态: %s\n\n", "已连接"));

			response.append(String.format("🛠️ 可用工具列表 (%d 个)：\n\n", tools.size()));
			for (int i = 0; i < tools.size(); i++) {
				McpToolInfo tool = tools.get(i);
				response.append(String.format("%d. %s\n", i + 1, tool.getName()));
				response.append(String.format("   描述: %s\n", tool.getDescription()));
				if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
					response.append("   参数:\n");
					for (Map.Entry<String, String> param : tool.getParameters().entrySet()) {
						response.append(String.format("     - %s: %s\n", param.getKey(), param.getValue()));
					}
				}
				response.append("\n");
			}

			response.append("使用方法：\n");
			response.append("1. 使用工具：调用 useTool 方法\n");
			response.append("   - 参数1: serviceName = \"" + mcp_server_name + "\"\n");
			response.append("   - 参数2: toolName = \"工具名称\"\n");
			response.append("   - 参数3: parameters = \"{\\\"参数名\\\":\\\"参数值\\\"}\"\n\n");
			response.append("2. 示例：\n");
			if (!tools.isEmpty()) {
				McpToolInfo firstTool = tools.get(0);
				response.append(String.format("   useTool(\"%s\", \"%s\", \"{}\")\n",
						mcp_server_name, firstTool.getName()));
			}
//			response.append("\n3. 查看所有服务：调用 getAllMcpServers 方法\n");
//			response.append("4. 搜索服务：调用 searchMcpServer 方法\n");

			return response.toString();

		} catch (Exception e) {
			logger.error("添加并初始化 MCP Server 时发生错误: {}", mcp_server_name, e);
			return String.format("添加并初始化 MCP Server '%s' 时发生错误: %s", mcp_server_name, e.getMessage());
		}
	}

	/**
	 * 使用 MCP Server 的工具 代理 LLM client 和目标 MCP Server 之间的请求
	 * 
	 * @param serviceName 目标服务名称
	 * @param toolName    工具名称
	 * @param parameters  工具参数（JSON 格式）
	 * @return 工具执行结果
	 */
	@Tool(description = "使用指定 MCP Server 的工具")
	public String useTool(@ToolParam(description = "目标服务名称") String serviceName,
			@ToolParam(description = "工具名称") String toolName,
			@ToolParam(description = "工具参数，JSON 格式") String parameters) {
		try {
			// 获取服务信息
			McpServerInfo serverInfo = mcpServerVectorStore.getServer(serviceName);
			if (serverInfo == null) {
				return String.format("未找到 MCP Server '%s'，请先添加该服务。", serviceName);
			}

			// 检查连接状态
			McpServerConnection connection = serverConnections.get(serviceName);
			if (connection == null || !connection.isConnected()) {
				boolean connected = establishConnection(serverInfo);
				if (!connected) {
					return String.format("无法连接到 MCP Server '%s'，请检查服务状态。", serviceName);
				}
			}

			// 构建请求
			String requestBody = buildToolRequest(toolName, parameters);

			// 发送请求到目标服务器
			String response = sendToolRequest(serviceName, requestBody);

			return String.format("工具 '%s' 执行结果:\n%s", toolName, response);

		} catch (Exception e) {
			logger.error("使用工具时发生错误", e);
			return "使用工具时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 获取所有可用的 MCP Server
	 * 
	 * @return 所有 MCP Server 列表
	 */
	@Tool(description = "获取所有可用的 MCP Server 列表")
	public String getAllMcpServers() {
		try {
			List<McpServerInfo> servers = mcpServerVectorStore.getAllServers();

			if (servers.isEmpty()) {
				return "当前没有可用的 MCP Server。请先添加一些服务。";
			}

			StringBuilder response = new StringBuilder();
			response.append(String.format("当前共有 %d 个可用的 MCP Server：\n\n", servers.size()));

			for (int i = 0; i < servers.size(); i++) {
				McpServerInfo server = servers.get(i);
				McpServerConnection connection = serverConnections.get(server.getName());
				String status = (connection != null && connection.isConnected()) ? "已连接" : "未连接";

				response.append(String.format("%d. %s (%s)\n", i + 1, server.getName(), status));
				response.append(String.format("   描述: %s\n",
						server.getDescription() != null ? server.getDescription() : "无描述"));
				response.append(String.format("   协议: %s\n", server.getProtocol()));
				response.append(String.format("   端点: %s\n", server.getEndpoint()));
				if (server.getTags() != null && !server.getTags().isEmpty()) {
					response.append(String.format("   标签: %s\n", String.join(", ", server.getTags())));
				}
				response.append("\n");
			}

			return response.toString();

		} catch (Exception e) {
			logger.error("获取 MCP Server 列表时发生错误", e);
			return "获取 MCP Server 列表时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 移除 MCP Server
	 * 
	 * @param serviceName 服务名称
	 * @return 移除结果
	 */
	@Tool(description = "移除指定的 MCP Server")
	public String removeMcpServer(@ToolParam(description = "服务名称") String serviceName) {
		try {
			// 关闭连接
			McpServerConnection connection = serverConnections.remove(serviceName);
			if (connection != null) {
				connection.close();
			}

			// 从向量存储中移除
			boolean success = mcpServerVectorStore.removeServer(serviceName);

			if (success) {
				return String.format("成功移除 MCP Server '%s'。", serviceName);
			} else {
				return String.format("移除 MCP Server '%s' 失败，可能不存在。", serviceName);
			}

		} catch (Exception e) {
			logger.error("移除 MCP Server 时发生错误", e);
			return "移除 MCP Server 时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 获取 MCP Server 的工具列表
	 * 
	 * @param serviceName 服务名称
	 * @return 工具列表
	 */
	private List<McpToolInfo> getMcpServerTools(String serviceName) {
		List<McpToolInfo> tools = new ArrayList<>();

		try {
			// 从 Nacos 获取服务详细信息
			McpServerDetailInfo serverDetail = getServerDetailFromNacos(serviceName);
			if (serverDetail != null && serverDetail.getToolSpec() != null) {
				McpToolSpecification toolSpec = serverDetail.getToolSpec();
				List<McpTool> toolsList = toolSpec.getTools();
				Map<String, McpToolMeta> toolsMeta = toolSpec.getToolsMeta();

				if (toolsList != null) {
					for (McpTool tool : toolsList) {
						String toolName = tool.getName();
						McpToolMeta metaInfo = toolsMeta != null ? toolsMeta.get(toolName) : null;

						// 检查工具是否启用
						boolean enabled = metaInfo == null || metaInfo.isEnabled();
						if (!enabled) {
							logger.debug("Tool {} is disabled, skipping", toolName);
							continue;
						}

						// 解析工具参数
						Map<String, String> parameters = parseToolParameters(tool.getInputSchema());

						McpToolInfo toolInfo = new McpToolInfo(
								toolName,
								tool.getDescription(),
								parameters);
						tools.add(toolInfo);
					}
				}
			}
		} catch (Exception e) {
			logger.error("获取 MCP Server 工具列表时发生错误: {}", serviceName, e);
		}

		return tools;
	}

	/**
	 * 从 Nacos 获取服务详细信息
	 * 
	 * @param serviceName 服务名称
	 * @return 服务详细信息
	 */
	private McpServerDetailInfo getServerDetailFromNacos(String serviceName) {
		try {
			// 这里需要注入 NacosMcpOperationService 来获取服务详情
			// 由于当前类没有直接访问，我们通过服务发现来获取基本信息
			return nacosMcpOperationService.getServerDetail(serviceName);
		} catch (Exception e) {
			logger.error("从 Nacos 获取服务详情时发生错误: {}", serviceName, e);
			return null;
		}
	}

	/**
	 * 解析工具参数
	 * 
	 * @param inputSchema 输入模式
	 * @return 参数映射
	 */
	private Map<String, String> parseToolParameters(Map<String, Object> inputSchema) {
		Map<String, String> parameters = new HashMap<>();

		try {
			if (inputSchema != null && inputSchema.containsKey("properties")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					String paramName = entry.getKey();
					@SuppressWarnings("unchecked")
					Map<String, Object> paramInfo = (Map<String, Object>) entry.getValue();

					String description = paramInfo.containsKey("description") ? (String) paramInfo.get("description") : "无描述";

					parameters.put(paramName, description);
				}
			}
		} catch (Exception e) {
			logger.error("解析工具参数时发生错误", e);
		}

		return parameters;
	}

	/**
	 * MCP 工具信息类
	 */
	private static class McpToolInfo {
		private final String name;
		private final String description;
		private final Map<String, String> parameters;

		public McpToolInfo(String name, String description, Map<String, String> parameters) {
			this.name = name;
			this.description = description;
			this.parameters = parameters;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}
	}

	/**
	 * 建立与 MCP Server 的连接
	 */
	private boolean establishConnection(McpServerInfo serverInfo) {
		try {
			// 这里应该根据协议类型建立不同的连接
			// 目前简化实现，实际应该支持 stdio 和 SSE 协议
			McpServerConnection connection = new McpServerConnection(serverInfo);
			boolean connected = connection.connect();

			if (connected) {
				serverConnections.put(serverInfo.getName(), connection);
				logger.info("成功建立与 MCP Server '{}' 的连接", serverInfo.getName());
			}

			return connected;

		} catch (Exception e) {
			logger.error("建立与 MCP Server '{}' 的连接时发生错误", serverInfo.getName(), e);
			return false;
		}
	}

	/**
	 * 构建工具请求
	 */
	private String buildToolRequest(String toolName, String parameters) {
		try {
			// 解析参数
			JsonNode paramsNode = objectMapper.readTree(parameters);

			// 构建 MCP 工具调用请求
			Map<String, Object> request = Map.of("jsonrpc", "2.0", "id", String.valueOf(System.currentTimeMillis()),
					"method", "tools/call", "params", Map.of("name", toolName, "arguments", paramsNode));

			return objectMapper.writeValueAsString(request);

		} catch (Exception e) {
			logger.error("构建工具请求时发生错误", e);
			throw new RuntimeException("构建工具请求失败", e);
		}
	}

	/**
	 * 发送工具请求到目标服务器
	 */
	private String sendToolRequest(String serviceName, String requestBody) {
		try {
			McpServerConnection connection = serverConnections.get(serviceName);
			if (connection == null) {
				throw new RuntimeException("未找到服务连接");
			}

			// 这里应该通过连接发送请求
			// 目前简化实现，返回模拟响应
			return String.format("模拟响应 - 服务: %s, 请求: %s", serviceName, requestBody);

		} catch (Exception e) {
			logger.error("发送工具请求时发生错误", e);
			throw new RuntimeException("发送工具请求失败", e);
		}
	}

	/**
	 * MCP Server 连接封装
	 */
	private static class McpServerConnection {

		private final McpServerInfo serverInfo;

		private boolean connected = false;

		public McpServerConnection(McpServerInfo serverInfo) {
			this.serverInfo = serverInfo;
		}

		public boolean connect() {
			// 这里应该根据协议类型实现实际的连接逻辑
			// 对于 stdio 协议，需要启动进程
			// 对于 SSE 协议，需要建立 HTTP 连接
			try {
				// 模拟连接过程
				Thread.sleep(100);
				this.connected = true;
				return true;
			} catch (Exception e) {
				this.connected = false;
				return false;
			}
		}

		public boolean isConnected() {
			return connected;
		}

		public void close() {
			// 关闭连接
			this.connected = false;
		}

	}

}
