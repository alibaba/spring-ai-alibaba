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

import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.vectorstore.McpServerVectorStore;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Router 核心服务 提供 MCP Server 发现、管理和请求代理功能
 */
public class McpRouterService {

	private static final Logger logger = LoggerFactory.getLogger(McpRouterService.class);

	private final McpServiceDiscovery mcpServiceDiscovery;

	private final McpServerVectorStore mcpServerVectorStore;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final McpProxyService mcpProxyService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public McpRouterService(McpServiceDiscovery mcpServiceDiscovery, McpServerVectorStore mcpServerVectorStore,
			NacosMcpOperationService nacosMcpOperationService, McpProxyService mcpProxyService) {
		this.mcpServiceDiscovery = mcpServiceDiscovery;
		this.mcpServerVectorStore = mcpServerVectorStore;
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.mcpProxyService = mcpProxyService;
	}

	/**
	 * 搜索 MCP Server 根据任务描述和关键词发现合适的 MCP Server
	 * @param taskDescription 任务描述
	 * @param keywords 关键词（可选）
	 * @param limit 返回数量限制
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

		}
		catch (Exception e) {
			logger.error("搜索 MCP Server 时发生错误", e);
			return "搜索 MCP Server 时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 添加并初始化 MCP Server，根据 Nacos 中的配置与该 MCP Server 建立连接，等待调用
	 * @param mcpServerName 需要添加的 MCP Server 名字
	 * @return MCP Server 工具列表及使用方法
	 */
	@Tool(description = "添加并初始化一个MCP Server，根据Nacos中的配置与该MCP Server建立连接，等待调用")
	public String addMcpServer(@ToolParam(description = "需要添加的MCP Server名字") String mcpServerName) {
		try {
			logger.info("开始添加并初始化 MCP Server: {}", mcpServerName);

			// 1. 从 Nacos 服务发现获取服务信息
			McpServerInfo serverInfo = mcpServiceDiscovery.getService(mcpServerName);
			if (serverInfo == null) {
				return String.format("未找到 MCP Server '%s'，请确保服务已在 Nacos 中注册。\n" + "请检查：\n" + "1. 服务名称是否正确\n"
						+ "2. 服务是否已在 Nacos 中注册\n" + "3. Nacos 配置是否正确", mcpServerName);
			}

			// 2. 添加到向量存储
			boolean added = mcpServerVectorStore.addServer(serverInfo);
			if (!added) {
				return String.format("添加 MCP Server '%s' 到向量存储失败，可能已存在。", mcpServerName);
			}

			// 3. 建立连接
			boolean connected = mcpProxyService.establishConnection(mcpServerName);
			if (!connected) {
				return String.format(
						"无法建立与 MCP Server '%s' 的连接。\n" + "服务信息：\n" + "- 协议: %s\n" + "- 端点: %s\n" + "- 版本: %s\n"
								+ "请检查服务是否正在运行。",
						mcpServerName, serverInfo.getProtocol(), serverInfo.getEndpoint(), serverInfo.getVersion());
			}

			// 4. 获取工具列表
			List<McpToolInfo> tools = getMcpServerTools(mcpServerName);
			if (tools.isEmpty()) {
				return String.format(
						"成功添加 MCP Server '%s' 并建立连接，但未找到可用工具。\n" + "服务信息：\n" + "- 描述: %s\n" + "- 协议: %s\n"
								+ "- 端点: %s\n" + "- 版本: %s\n" + "- 标签: %s",
						mcpServerName, serverInfo.getDescription() != null ? serverInfo.getDescription() : "无描述",
						serverInfo.getProtocol(), serverInfo.getEndpoint(), serverInfo.getVersion(),
						serverInfo.getTags() != null ? String.join(", ", serverInfo.getTags()) : "无标签");
			}

			// 5. 构建工具列表和使用方法
			StringBuilder response = new StringBuilder();
			response.append(String.format("成功添加并初始化 MCP Server '%s'\n\n", mcpServerName));
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
			response.append("   - 参数1: serviceName = \"" + mcpServerName + "\"\n");
			response.append("   - 参数2: toolName = \"工具名称\"\n");
			response.append("   - 参数3: parameters = \"{\\\"参数名\\\":\\\"参数值\\\"}\"\n\n");
			response.append("2. 示例：\n");
			if (!tools.isEmpty()) {
				McpToolInfo firstTool = tools.get(0);
				response
					.append(String.format("   useTool(\"%s\", \"%s\", \"{}\")\n", mcpServerName, firstTool.getName()));
			}
			// response.append("\n3. 查看所有服务：调用 getAllMcpServers 方法\n");
			// response.append("4. 搜索服务：调用 searchMcpServer 方法\n");

			return response.toString();

		}
		catch (Exception e) {
			logger.error("添加并初始化 MCP Server 时发生错误: {}", mcpServerName, e);
			return String.format("添加并初始化 MCP Server '%s' 时发生错误: %s", mcpServerName, e.getMessage());
		}
	}

	/**
	 * 使用 MCP Server 的工具 代理 LLM client 和目标 MCP Server 之间的请求
	 * @param serviceName 目标服务名称
	 * @param toolName 工具名称
	 * @param parameters 工具参数（JSON 格式）
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
			if (!mcpProxyService.isConnected(serviceName)) {
				boolean connected = mcpProxyService.establishConnection(serviceName);
				if (!connected) {
					return String.format("无法连接到 MCP Server '%s'，请检查服务状态。", serviceName);
				}
			}

			// 解析参数
			Map<String, Object> args = parseParameters(parameters);

			// 使用代理服务调用工具
			String result = mcpProxyService.callTool(serviceName, toolName, args);

			return String.format("工具 '%s' 执行结果:\n%s", toolName, result);

		}
		catch (Exception e) {
			logger.error("使用工具时发生错误", e);
			return "使用工具时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 获取 MCP Server 的工具列表
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

						McpToolInfo toolInfo = new McpToolInfo(toolName, tool.getDescription(), parameters);
						tools.add(toolInfo);
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("获取 MCP Server 工具列表时发生错误: {}", serviceName, e);
		}

		return tools;
	}

	/**
	 * 从 Nacos 获取服务详细信息
	 * @param serviceName 服务名称
	 * @return 服务详细信息
	 */
	private McpServerDetailInfo getServerDetailFromNacos(String serviceName) {
		try {
			// 这里需要注入 NacosMcpOperationService 来获取服务详情
			// 由于当前类没有直接访问，我们通过服务发现来获取基本信息
			return nacosMcpOperationService.getServerDetail(serviceName);
		}
		catch (Exception e) {
			logger.error("从 Nacos 获取服务详情时发生错误: {}", serviceName, e);
			return null;
		}
	}

	/**
	 * 解析工具参数
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

					String description = paramInfo.containsKey("description") ? (String) paramInfo.get("description")
							: "无描述";

					parameters.put(paramName, description);
				}
			}
		}
		catch (Exception e) {
			logger.error("解析工具参数时发生错误", e);
		}

		return parameters;
	}

	/**
	 * 解析工具参数
	 */
	private Map<String, Object> parseParameters(String parameters) {
		try {
			if (parameters == null || parameters.trim().isEmpty()) {
				return new HashMap<>();
			}
			JsonNode paramsNode = objectMapper.readTree(parameters);
			return objectMapper.convertValue(paramsNode, Map.class);
		}
		catch (Exception e) {
			logger.error("解析工具参数时发生错误", e);
			return new HashMap<>();
		}
	}

	/**
	 * 调试 MCP 服务连接状态
	 * @param serviceName 服务名称
	 * @return 调试信息
	 */
	@Tool(description = "调试 MCP 服务连接状态，帮助诊断连接问题")
	public String debugMcpService(@ToolParam(description = "服务名称") String serviceName) {
		try {
			StringBuilder result = new StringBuilder();
			result.append("=== MCP Service Debug ===\n\n");

			// 1. 检查向量存储中的服务信息
			McpServerInfo serverInfo = mcpServerVectorStore.getServer(serviceName);
			if (serverInfo == null) {
				result.append("❌ 服务未在向量存储中找到\n");
				result.append("请先使用 addMcpServer 方法添加服务\n");
				return result.toString();
			}
			result.append("✅ 服务在向量存储中找到\n");
			result.append("服务信息：\n");
			result.append("- 名称: ").append(serverInfo.getName()).append("\n");
			result.append("- 描述: ").append(serverInfo.getDescription()).append("\n");
			result.append("- 协议: ").append(serverInfo.getProtocol()).append("\n");
			result.append("- 端点: ").append(serverInfo.getEndpoint()).append("\n");
			result.append("- 版本: ").append(serverInfo.getVersion()).append("\n\n");

			// 2. 使用代理服务进行详细调试
			String proxyDebugInfo = mcpProxyService.debugServiceConnection(serviceName);
			result.append(proxyDebugInfo);

			return result.toString();

		}
		catch (Exception e) {
			logger.error("调试服务时发生错误", e);
			return "调试服务时发生错误: " + e.getMessage();
		}
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

}
