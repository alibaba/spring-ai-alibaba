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
import com.alibaba.cloud.ai.mcp.router.model.response.McpDebugResponse;
import com.alibaba.cloud.ai.mcp.router.model.response.McpServerAddResponse;
import com.alibaba.cloud.ai.mcp.router.model.response.McpServerSearchResponse;
import com.alibaba.cloud.ai.mcp.router.model.response.McpToolExecutionResponse;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;

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
import java.util.stream.Collectors;

/**
 * MCP Router 核心服务
 *
 * <p>
 * 提供 MCP (Model Context Protocol) Server 的发现、管理和请求代理功能。 作为 LLM 客户端和各种 MCP Server
 * 之间的智能路由层，支持：
 * </p>
 *
 * <ul>
 * <li><strong>语义搜索</strong> - 根据任务描述智能发现匹配的 MCP Server</li>
 * <li><strong>服务管理</strong> - 添加、初始化和管理 MCP Server 连接</li>
 * <li><strong>工具代理</strong> - 代理 LLM 和 MCP Server 之间的工具调用</li>
 * <li><strong>连接诊断</strong> - 提供详细的连接状态和问题排查信息</li>
 * </ul>
 *
 * <p>
 * <strong>MCP 相关注释标记：</strong>
 * </p>
 * <ul>
 * <li>@McpRouter - 标识此类为 MCP 路由器核心服务</li>
 * <li>@McpTool - 标识方法为 MCP 工具，可被 LLM 调用</li>
 * <li>@McpDescription - 提供详细的描述和使用说明</li>
 * </ul>
 *
 * @author spring-ai-alibaba
 * @since 2025.0.0
 * @see McpServerSearchResponse 搜索响应模型
 * @see McpServerAddResponse 添加服务响应模型
 * @see McpToolExecutionResponse 工具执行响应模型
 * @see McpDebugResponse 调试响应模型
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
	 * 搜索 MCP Server
	 *
	 * <p>
	 * 根据任务描述和关键词进行语义搜索，智能发现匹配的 MCP Server。 使用向量相似度搜索和关键词匹配相结合的方式，提供最相关的服务建议。
	 * </p>
	 * @param taskDescription 任务描述，用于语义匹配
	 * @param keywords 关键词（可选），多个关键词用逗号分隔
	 * @param limit 返回数量限制，默认5个
	 * @return {@link McpServerSearchResponse} 搜索结果，包含匹配的服务列表和相似度分数
	 *
	 */

	@Tool(description = "根据任务描述和关键词搜索合适的 MCP Server，返回结构化的搜索结果")
	public McpServerSearchResponse searchMcpServer(@ToolParam(description = "任务描述，用于语义匹配") String taskDescription,
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

			logger.info("执行 MCP Server 搜索: query='{}', limit={}", searchQuery, limit);

			// 使用向量存储进行语义搜索
			List<McpServerInfo> results = mcpServerVectorStore.search(searchQuery, limit);

			if (results.isEmpty()) {
				// 提供搜索建议
				List<String> suggestions = List.of("尝试使用更具体的任务描述", "添加相关的技术关键词（如：数据库、文件、API等）", "检查是否已添加相关的 MCP Server",
						"使用 addMcpServer 方法添加新的服务");

				McpServerSearchResponse response = McpServerSearchResponse.error(searchQuery, "未找到匹配的 MCP Server");
				response.setSuggestions(suggestions);
				return response;
			}

			// 转换搜索结果
			List<McpServerSearchResponse.McpServerSearchResult> searchResults = results.stream()
				.map(McpServerSearchResponse.McpServerSearchResult::new)
				.collect(Collectors.toList());

			logger.info("找到 {} 个匹配的 MCP Server", results.size());
			return McpServerSearchResponse.success(searchQuery, searchResults);

		}
		catch (Exception e) {
			logger.error("搜索 MCP Server 时发生错误", e);
			return McpServerSearchResponse.error(taskDescription != null ? taskDescription : "",
					"搜索 MCP Server 时发生错误: " + e.getMessage());
		}
	}

	/**
	 * 添加并初始化 MCP Server
	 *
	 * <p>
	 * 根据 Nacos 中的配置发现并添加 MCP Server，建立连接并获取可用工具列表。 该方法会执行以下操作：
	 * </p>
	 *
	 * <ol>
	 * <li>从 Nacos 服务发现获取服务配置信息</li>
	 * <li>将服务添加到向量存储，支持后续语义搜索</li>
	 * <li>建立与 MCP Server 的连接</li>
	 * <li>获取并解析可用工具列表</li>
	 * <li>提供工具使用指南和示例</li>
	 * </ol>
	 * @param mcpServerName 需要添加的 MCP Server 名称，必须在 Nacos 中已注册
	 * @return {@link McpServerAddResponse} 添加结果，包含服务信息、连接状态和工具列表
	 *
	 */

	@Tool(description = "添加并初始化一个MCP Server，根据Nacos中的配置与该MCP Server建立连接，返回详细的服务信息和工具列表")
	public McpServerAddResponse addMcpServer(@ToolParam(description = "需要添加的MCP Server名字") String mcpServerName) {
		try {
			logger.info("开始添加并初始化 MCP Server: {}", mcpServerName);

			// 1. 从 Nacos 服务发现获取服务信息
			McpServerInfo serverInfo = mcpServiceDiscovery.getService(mcpServerName);
			if (serverInfo == null) {
				return McpServerAddResponse.error(mcpServerName,
						"未找到 MCP Server，请确保服务已在 Nacos 中注册。请检查：1. 服务名称是否正确 2. 服务是否已在 Nacos 中注册 3. Nacos 配置是否正确");
			}

			// 2. 添加到向量存储
			boolean added = mcpServerVectorStore.addServer(serverInfo);
			if (!added) {
				logger.warn("服务可能已存在于向量存储中: {}", mcpServerName);
			}

			// 3. 建立连接
			boolean connected = mcpProxyService.establishConnection(mcpServerName);
			String connectionMessage = connected ? "连接成功" : "连接失败，请检查服务是否正在运行";
			String connectionUrl = String.format("%s://%s", serverInfo.getProtocol(), serverInfo.getEndpoint());

			McpServerAddResponse.McpConnectionStatus connectionStatus = new McpServerAddResponse.McpConnectionStatus(
					connected, connectionUrl, connectionMessage);

			// 4. 获取工具列表
			List<McpToolInfo> tools = getMcpServerTools(mcpServerName);
			List<McpServerAddResponse.McpToolInfo> toolInfoList = convertToMcpToolInfoList(tools);

			// 5. 构建服务信息
			McpServerAddResponse.McpServerServiceInfo serviceInfo = new McpServerAddResponse.McpServerServiceInfo(
					serverInfo.getName(), serverInfo.getDescription(), serverInfo.getProtocol(),
					serverInfo.getVersion(), serverInfo.getEndpoint(), serverInfo.getTags());

			// 6. 构建使用指南
			McpServerAddResponse.McpUsageGuide usageGuide = createUsageGuide(mcpServerName, tools);

			// 7. 构建响应
			return McpServerAddResponse.success(mcpServerName, serviceInfo, connectionStatus, toolInfoList, usageGuide);

		}
		catch (Exception e) {
			logger.error("添加并初始化 MCP Server 时发生错误: {}", mcpServerName, e);
			return McpServerAddResponse.error(mcpServerName, "添加并初始化 MCP Server 时发生错误: " + e.getMessage());
		}
	}

	/**
	 * 使用 MCP Server 的工具
	 *
	 * <p>
	 * 代理 LLM 客户端和目标 MCP Server 之间的工具调用请求。 该方法提供完整的工具执行生命周期管理：
	 * </p>
	 *
	 * <ol>
	 * <li>验证目标服务的可用性</li>
	 * <li>检查并建立必要的连接</li>
	 * <li>解析和验证工具参数</li>
	 * <li>执行工具调用并收集执行元信息</li>
	 * <li>处理和格式化返回结果</li>
	 * </ol>
	 * @param serviceName 目标服务名称，必须是已添加的 MCP Server
	 * @param toolName 工具名称，必须是目标服务支持的工具
	 * @param parameters 工具参数，JSON 格式字符串
	 * @return {@link McpToolExecutionResponse} 执行结果，包含结果数据和执行元信息
	 *
	 */

	@Tool(description = "使用指定 MCP Server 的工具，返回详细的执行结果和元信息")
	public McpToolExecutionResponse useTool(@ToolParam(description = "目标服务名称") String serviceName,
			@ToolParam(description = "工具名称") String toolName,
			@ToolParam(description = "工具参数，JSON 格式") String parameters) {

		McpToolExecutionResponse.McpExecutionMeta executionMeta = null;
		Map<String, Object> args = new HashMap<>();

		try {
			// 解析参数
			args = parseParameters(parameters);

			// 获取服务信息
			McpServerInfo serverInfo = mcpServerVectorStore.getServer(serviceName);
			if (serverInfo == null) {
				return McpToolExecutionResponse.error(serviceName, toolName, args,
						"未找到 MCP Server，请先使用 addMcpServer 方法添加该服务");
			}

			// 初始化执行元信息
			String connectionUrl = String.format("%s://%s", serverInfo.getProtocol(), serverInfo.getEndpoint());
			executionMeta = McpToolExecutionResponse.McpExecutionMeta.start(serverInfo.getProtocol(), connectionUrl);

			// 检查连接状态
			if (!mcpProxyService.isConnected(serviceName)) {
				boolean connected = mcpProxyService.establishConnection(serviceName);
				if (!connected) {
					return McpToolExecutionResponse.error(serviceName, toolName, args, "无法连接到 MCP Server，请检查服务状态和网络连接");
				}
			}

			logger.info("执行 MCP 工具: service='{}', tool='{}', args={}", serviceName, toolName, args);

			// 使用代理服务调用工具
			String rawResult = mcpProxyService.callTool(serviceName, toolName, args);

			// 完成执行计时
			executionMeta.complete();

			// 处理执行结果
			McpToolExecutionResponse.McpToolExecutionResult result = processExecutionResult(rawResult);

			logger.info("MCP 工具执行完成: service='{}', tool='{}', duration={}ms", serviceName, toolName,
					executionMeta.getExecutionDurationMs());

			return McpToolExecutionResponse.success(serviceName, toolName, args, result, executionMeta);

		}
		catch (Exception e) {
			// 完成执行计时（即使失败）
			if (executionMeta != null) {
				executionMeta.complete();
			}

			logger.error("使用工具时发生错误: service='{}', tool='{}'", serviceName, toolName, e);
			return McpToolExecutionResponse.error(serviceName, toolName, args, "工具执行失败: " + e.getMessage());
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
				List<com.alibaba.nacos.api.ai.model.mcp.McpTool> toolsList = toolSpec.getTools();
				Map<String, McpToolMeta> toolsMeta = toolSpec.getToolsMeta();

				if (toolsList != null) {
					for (com.alibaba.nacos.api.ai.model.mcp.McpTool tool : toolsList) {
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
	@SuppressWarnings("unchecked")
	private Map<String, String> parseToolParameters(Map<String, Object> inputSchema) {
		Map<String, String> parameters = new HashMap<>();

		try {
			if (inputSchema != null && inputSchema.containsKey("properties")) {
				Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");

				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					String paramName = entry.getKey();
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
	@SuppressWarnings("unchecked")
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
	 *
	 * <p>
	 * 提供详细的 MCP 服务诊断信息，帮助排查连接和配置问题。 该方法会执行全面的服务健康检查：
	 * </p>
	 *
	 * <ol>
	 * <li>检查服务在向量存储中的状态</li>
	 * <li>验证 Nacos 服务发现配置</li>
	 * <li>测试网络连接和端点可达性</li>
	 * <li>分析连接错误并提供排查建议</li>
	 * <li>收集系统环境信息</li>
	 * </ol>
	 * @param serviceName 要诊断的服务名称
	 * @return {@link McpDebugResponse} 详细的诊断结果和排查建议
	 *
	 */

	@Tool(description = "调试 MCP 服务连接状态，提供详细的诊断信息和问题排查建议")
	public McpDebugResponse debugMcpService(@ToolParam(description = "服务名称") String serviceName) {

		try {
			logger.info("开始调试 MCP 服务: {}", serviceName);

			// 1. 检查服务状态
			McpDebugResponse.McpServiceStatus serviceStatus = checkServiceStatus(serviceName);

			// 2. 执行连接诊断
			McpDebugResponse.McpConnectionDiagnosis connectionDiagnosis = performConnectionDiagnosis(serviceName);

			// 3. 生成排查建议
			List<String> suggestions = generateTroubleshootingSuggestions(serviceStatus, connectionDiagnosis);

			logger.info("MCP 服务调试完成: {}", serviceName);

			return McpDebugResponse.success(serviceName, serviceStatus, connectionDiagnosis, suggestions);

		}
		catch (Exception e) {
			logger.error("调试服务时发生错误: {}", serviceName, e);
			return McpDebugResponse.error(serviceName, "调试服务时发生错误: " + e.getMessage());
		}
	}

	/**
	 * 转换工具信息列表
	 */
	private List<McpServerAddResponse.McpToolInfo> convertToMcpToolInfoList(List<McpToolInfo> tools) {
		return tools.stream().map(tool -> {
			Map<String, McpServerAddResponse.McpToolParameter> parameters = tool.getParameters()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey,
						entry -> new McpServerAddResponse.McpToolParameter("string", entry.getValue(), false, null)));
			return new McpServerAddResponse.McpToolInfo(tool.getName(), tool.getDescription(), parameters, true);
		}).collect(Collectors.toList());
	}

	/**
	 * 创建使用指南
	 */
	private McpServerAddResponse.McpUsageGuide createUsageGuide(String serviceName, List<McpToolInfo> tools) {
		String howToUse = String.format("使用 useTool 方法调用工具：\n" + "- 参数1: serviceName = \"%s\"\n"
				+ "- 参数2: toolName = \"工具名称\"\n" + "- 参数3: parameters = \"{\\\"参数名\\\":\\\"参数值\\\"}\"", serviceName);

		String exampleCall = "";
		if (!tools.isEmpty()) {
			McpToolInfo firstTool = tools.get(0);
			exampleCall = String.format("useTool(\"%s\", \"%s\", \"{}\")", serviceName, firstTool.getName());
		}

		List<String> operations = List.of("useTool - 执行工具", "searchMcpServer - 搜索服务", "debugMcpService - 调试服务连接");

		return new McpServerAddResponse.McpUsageGuide(howToUse, exampleCall, operations);
	}

	/**
	 * 处理执行结果
	 */
	private McpToolExecutionResponse.McpToolExecutionResult processExecutionResult(String rawResult) {
		if (rawResult == null) {
			return McpToolExecutionResponse.McpToolExecutionResult.text("No result returned");
		}

		// 尝试解析为JSON
		try {
			JsonNode jsonNode = objectMapper.readTree(rawResult);
			return McpToolExecutionResponse.McpToolExecutionResult.json(jsonNode, rawResult);
		}
		catch (Exception e) {
			// 如果不是JSON，作为文本处理
			return McpToolExecutionResponse.McpToolExecutionResult.text(rawResult);
		}
	}

	/**
	 * 检查服务状态
	 */
	private McpDebugResponse.McpServiceStatus checkServiceStatus(String serviceName) {
		McpDebugResponse.McpServiceStatus status = new McpDebugResponse.McpServiceStatus();
		Map<String, String> serviceInfo = new HashMap<>();

		// 检查向量存储
		McpServerInfo serverInfo = mcpServerVectorStore.getServer(serviceName);
		status.setFoundInVectorStore(serverInfo != null);

		if (serverInfo != null) {
			serviceInfo.put("name", serverInfo.getName());
			serviceInfo.put("description", serverInfo.getDescription());
			serviceInfo.put("protocol", serverInfo.getProtocol());
			serviceInfo.put("version", serverInfo.getVersion());
			serviceInfo.put("endpoint", serverInfo.getEndpoint());
		}

		// 检查Nacos
		try {
			McpServerDetailInfo nacosInfo = nacosMcpOperationService.getServerDetail(serviceName);
			status.setFoundInNacos(nacosInfo != null);
			status.setRemoteConfigValid(nacosInfo != null && nacosInfo.getRemoteServerConfig() != null);
			status.setServiceRefValid(nacosInfo != null && nacosInfo.getRemoteServerConfig() != null
					&& nacosInfo.getRemoteServerConfig().getServiceRef() != null);
		}
		catch (Exception e) {
			status.setFoundInNacos(false);
			status.setRemoteConfigValid(false);
			status.setServiceRefValid(false);
		}

		// 检查连接缓存
		status.setConnectionCached(mcpProxyService.isConnected(serviceName));
		status.setServiceInfo(serviceInfo);

		return status;
	}

	/**
	 * 执行连接诊断
	 */
	private McpDebugResponse.McpConnectionDiagnosis performConnectionDiagnosis(String serviceName) {
		McpDebugResponse.McpConnectionDiagnosis diagnosis = new McpDebugResponse.McpConnectionDiagnosis();
		List<String> details = new ArrayList<>();

		try {
			// 获取代理服务的详细诊断信息
			String proxyDebugInfo = mcpProxyService.debugServiceConnection(serviceName);

			// 从调试信息中提取结构化数据
			diagnosis.setFullUrl(extractUrlFromDebugInfo(proxyDebugInfo));
			diagnosis.setBaseUrlReachable(proxyDebugInfo.contains("Base URL reachable: ✅"));
			diagnosis.setEndpointReachable(proxyDebugInfo.contains("Endpoint reachable: ✅"));

			// 提取状态码
			Integer statusCode = extractStatusCodeFromDebugInfo(proxyDebugInfo);
			diagnosis.setHttpStatusCode(statusCode);

			// 添加诊断详情
			details.add("完整诊断信息：");
			details.add(proxyDebugInfo);

		}
		catch (Exception e) {
			diagnosis.setNetworkError("诊断过程中发生错误: " + e.getMessage());
			details.add("诊断失败: " + e.getMessage());
		}

		diagnosis.setDiagnosisDetails(details);
		return diagnosis;
	}

	/**
	 * 生成排查建议
	 */
	private List<String> generateTroubleshootingSuggestions(McpDebugResponse.McpServiceStatus serviceStatus,
			McpDebugResponse.McpConnectionDiagnosis connectionDiagnosis) {

		List<String> suggestions = new ArrayList<>();

		if (!serviceStatus.isFoundInVectorStore()) {
			suggestions.add("服务未在向量存储中找到，请使用 addMcpServer 方法添加服务");
		}

		if (!serviceStatus.isFoundInNacos()) {
			suggestions.add("服务未在 Nacos 中找到，请检查服务名称和 Nacos 配置");
		}

		if (!serviceStatus.isRemoteConfigValid()) {
			suggestions.add("远程配置无效，请检查 Nacos 中的服务配置");
		}

		if (!connectionDiagnosis.isEndpointReachable()) {
			suggestions.add("端点不可达，请检查目标服务是否正在运行");
			suggestions.add("检查网络连接和防火墙设置");
			suggestions.add("验证服务地址和端口配置");
		}

		if (connectionDiagnosis.getHttpStatusCode() != null && connectionDiagnosis.getHttpStatusCode() >= 500) {
			suggestions.add("服务器内部错误，请检查目标服务的日志");
		}

		if (suggestions.isEmpty()) {
			suggestions.add("服务状态正常，如有问题请检查具体的工具调用参数");
		}

		return suggestions;
	}

	/**
	 * 从调试信息中提取URL
	 */
	private String extractUrlFromDebugInfo(String debugInfo) {
		// 简单的字符串匹配，实际实现可能需要更复杂的解析
		String[] lines = debugInfo.split("\n");
		for (String line : lines) {
			if (line.contains("Full URL:")) {
				return line.substring(line.indexOf("Full URL:") + 9).trim();
			}
		}
		return null;
	}

	/**
	 * 从调试信息中提取状态码
	 */
	private Integer extractStatusCodeFromDebugInfo(String debugInfo) {
		String[] lines = debugInfo.split("\n");
		for (String line : lines) {
			if (line.contains("Status Code:")) {
				try {
					String statusStr = line.substring(line.indexOf("Status Code:") + 12).trim();
					return Integer.parseInt(statusStr);
				}
				catch (Exception e) {
					// 忽略解析错误
				}
			}
		}
		return null;
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
