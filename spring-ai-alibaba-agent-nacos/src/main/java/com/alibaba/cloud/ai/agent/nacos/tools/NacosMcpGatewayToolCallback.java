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

package com.alibaba.cloud.ai.agent.nacos.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.gateway.nacos.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.apache.poi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class NacosMcpGatewayToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(com.alibaba.cloud.ai.mcp.gateway.nacos.callback.NacosMcpGatewayToolCallback.class);

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*(\\.[\\w]+(?:\\.[\\w]+)*)\\s*\\}\\}");

	// 匹配 {{ ${nacos.dataId/group} }} 或 {{ ${nacos.dataId/group}.key1.key2 }}
	private static final Pattern NACOS_TEMPLATE_PATTERN = Pattern
			.compile("\\{\\{\\s*\\$\\{nacos\\.([^}]+)\\}(\\.[\\w]+(?:\\.[\\w]+)*)?\\s*}}");

	/**
	 * The Object mapper.
	 */
	static ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

	private final NacosMcpGatewayToolDefinition toolDefinition;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final HashMap<String, AbstractListener> nacosConfigListeners = new HashMap<>();

	private final HashMap<String, String> nacosConfigContent = new HashMap<>();

	McpServersVO.McpServerVO mcpServerVO;

	/**
	 * Instantiates a new Nacos mcp gateway tool callback.
	 * @param toolDefinition the tool definition
	 */
	public NacosMcpGatewayToolCallback(final McpGatewayToolDefinition toolDefinition, NacosMcpOperationService nacosMcpOperationService, McpServersVO.McpServerVO mcpServersVO) {
		this.toolDefinition = (NacosMcpGatewayToolDefinition) toolDefinition;
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.mcpServerVO = mcpServersVO;
	}

	/**
	 * Process nacos config ref template string.
	 * @param template the template
	 * @return the string
	 */
	public String processNacosConfigRefTemplate(String template) {
		if (!org.springframework.util.StringUtils.hasText(template)) {
			return template;
		}

		StringBuffer result = new StringBuffer();
		Matcher matcher = NACOS_TEMPLATE_PATTERN.matcher(template);

		while (matcher.find()) {
			String nacosRef = matcher.group(1);
			String dotNotation = matcher.group(2);
			String replacement = resolveNacosReference(nacosRef, dotNotation);
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : ""));
		}
		matcher.appendTail(result);

		return result.toString();
	}

	/**
	 * 解析Nacos引用
	 * @param nacosRef 引用字符串，格式为 dataId/group
	 * @param dotNotation 点语法部分，格式为 .key1.key2（可能为null）
	 * @return 解析后的值
	 */
	private String resolveNacosReference(String nacosRef, String dotNotation) {
		if (!org.springframework.util.StringUtils.hasText(nacosRef)) {
			return null;
		}

		try {
			// 解析dataId和group
			String[] configParts = nacosRef.split("/");
			if (configParts.length != 2) {
				throw new IllegalArgumentException(
						"Invalid Nacos config reference format: " + nacosRef + ". Expected format: dataId/group");
			}

			String dataId = configParts[0];
			String group = configParts[1];

			// 获取配置内容
			String configContent = getConfigContent(dataId, group);
			if (!org.springframework.util.StringUtils.hasText(configContent)) {
				logger.warn("[resolveNacosReference] No content found for dataId: {}, group: {}", dataId, group);
				return null;
			}

			// 如果没有点语法，直接返回配置内容
			if (!org.springframework.util.StringUtils.hasText(dotNotation)) {
				return configContent;
			}

			// 如果有点语法，去掉开头的点号，然后解析JSON并提取指定字段
			String jsonPath = dotNotation.startsWith(".") ? dotNotation.substring(1) : dotNotation;
			return extractJsonValueFromNacos(configContent, jsonPath);

		}
		catch (Exception e) {
			// 记录日志但不中断处理
			logger.error("[resolveNacosReference] Failed to resolve Nacos reference: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to resolve Nacos reference: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取Nacos配置内容
	 * @param dataId 配置ID
	 * @param group 分组
	 * @return 配置内容
	 * @throws NacosException Nacos异常
	 */
	private String getConfigContent(String dataId, String group) throws NacosException {
		String cacheKey = dataId + "@@" + group;
		if (nacosConfigContent.containsKey(cacheKey)) {
			return nacosConfigContent.get(cacheKey);
		}
		else {
			AbstractListener listener = new AbstractListener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					nacosConfigContent.put(cacheKey, configInfo);
				}
			};
			AbstractListener oldListener = nacosConfigListeners.putIfAbsent(cacheKey, listener);
			if (oldListener == null) {
				try {
					nacosMcpOperationService.getConfigService().addListener(dataId, group, listener);
				}
				catch (Exception e) {
					nacosConfigListeners.remove(cacheKey);
					logger.error("Failed to add listener for Nacos config: {}", e.getMessage(), e);
				}
			}
			return nacosMcpOperationService.getConfigService().getConfig(dataId, group, 3000);
		}
	}

	/**
	 * 从JSON字符串中提取指定路径的值
	 * @param jsonString JSON字符串
	 * @param jsonPath JSON路径，如 key1.key2
	 * @return 提取的值
	 */
	private String extractJsonValueFromNacos(String jsonString, String jsonPath) throws JsonProcessingException {

		try {
			JsonNode rootNode = objectMapper.readTree(jsonString);
			String[] pathParts = jsonPath.split("\\.");

			JsonNode currentNode = rootNode;
			for (String part : pathParts) {
				if (currentNode == null || currentNode.isMissingNode()) {
					logger.warn("[extractJsonValueFromNacos] Path '{}' not found in JSON", jsonPath);
					return null;
				}
				currentNode = currentNode.get(part);
			}

			if (currentNode == null || currentNode.isMissingNode()) {
				logger.warn("[extractJsonValueFromNacos] Final path '{}' not found in JSON", jsonPath);
				return null;
			}

			// 根据节点类型返回合适的值
			if (currentNode.isTextual()) {
				return currentNode.asText();
			}
			else if (currentNode.isNumber()) {
				return currentNode.asText();
			}
			else if (currentNode.isBoolean()) {
				return String.valueOf(currentNode.asBoolean());
			}
			else {
				// 对于复杂对象，返回JSON字符串
				return currentNode.toString();
			}
		}
		catch (JsonProcessingException e) {
			logger.error("[extractJsonValueFromNacos] Failed to parse JSON from Nacos config. Content: {}, Error: {}",
					jsonString, e.getMessage());
			throw new RuntimeException(
					"Nacos config content is not valid JSON, but dot notation was used. Please ensure the config is in JSON format or remove the dot notation. Content: "
							+ jsonString,
					e);
		}
		catch (Exception e) {
			logger.error("[extractJsonValueFromNacos] Failed to extract JSON value from Nacos config: {}",
					e.getMessage(), e);
			throw e;
		}
	}

	private String processTemplateString(String template, Map<String, Object> params) {
		Map<String, Object> args = (Map<String, Object>) params.get("args");
		String extendedData = (String) params.get("extendedData");
		logger.debug("[processTemplateString] template: {} args: {} extendedData: {}", template, args, extendedData);
		if (template == null || template.isEmpty()) {
			return "";
		}
		Matcher matcher = TEMPLATE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			// 获取完整路径，如 .args.name 或 .data.key1.key2
			String fullPath = matcher.group(1);
			String replacement = resolvePathValue(fullPath, args, extendedData);
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		String finalResult = result.toString();
		finalResult = processNacosConfigRefTemplate(finalResult);
		logger.debug("[processTemplateString] final result: {}", finalResult);

		return finalResult;
	}

	/**
	 * 根据路径解析值
	 * @param fullPath 完整路径，如 .args.name 或 .data.key1.key2
	 * @param args 参数数据映射
	 * @param extendedData 扩展数据（JSON字符串）
	 * @return 解析后的值
	 */
	private String resolvePathValue(String fullPath, Map<String, Object> args, String extendedData) {
		if (fullPath == null || fullPath.isEmpty()) {
			return "";
		}
		// 移除开头的点号
		if (fullPath.startsWith(".")) {
			fullPath = fullPath.substring(1);
		}

		String[] pathParts = fullPath.split("\\.");
		if (pathParts.length == 0) {
			return "";
		}

		// 确定数据源
		Object dataSource;
		if (pathParts[0].equals("args")) {
			// 从args中取值
			dataSource = args;
			// 如果只有args，没有具体字段名
			if (pathParts.length == 1) {
				if (args != null && args.size() == 1) {
					return String.valueOf(args.values().iterator().next());
				}
				else if (args != null && !args.isEmpty()) {
					return args.toString();
				}
				else {
					return "";
				}
			}
		}
		else {
			// 从extendedData中取值
			// 首先将extendedData字符串解析为JSON对象
			try {
				if (StringUtils.hasText(extendedData)) {
					dataSource = objectMapper.readValue(extendedData, Map.class);
				}
				else {
					dataSource = null;
				}
			}
			catch (Exception e) {
				logger.warn("[resolvePathValue] Failed to parse extendedData as JSON: {}", e.getMessage());
				// 如果解析失败，将extendedData作为普通字符串处理
				if (pathParts.length == 1 && fullPath.equals("extendedData")) {
					return extendedData != null ? extendedData : "";
				}
				return "";
			}

			// 特殊处理直接访问extendedData的情况
			if (pathParts.length == 1 && fullPath.equals("extendedData")) {
				return extendedData != null ? extendedData : "";
			}
		}

		// 如果数据源为空
		if (dataSource == null) {
			return "";
		}
		// 处理嵌套路径
		Object currentValue = dataSource;
		int startIndex = pathParts[0].equals("args") ? 1 : 0;
		// 如果是args，从索引1开始；否则从索引0开始

		for (int i = startIndex; i < pathParts.length; i++) {
			String key = pathParts[i];
			if (currentValue instanceof Map) {
				Map<String, Object> currentMap = (Map<String, Object>) currentValue;
				currentValue = currentMap.get(key);
			}
			else {
				logger.warn("[resolvePathValue] Cannot access key '{}' from non-map value", key);
				return "";
			}

			if (currentValue == null) {
				logger.warn("[resolvePathValue] Key '{}' not found in nested path", key);
				return "";
			}
		}
		return currentValue.toString();
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public String call(@NonNull final String input) {
		return call(input, new ToolContext(Maps.newHashMap()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public String call(@NonNull final String input, final ToolContext toolContext) {
		try {
			logger.info("[call] input: {} toolContext: {}", input, JacksonUtils.toJson(toolContext));

			// 参数验证
			if (this.toolDefinition == null) {
				throw new IllegalStateException("Tool definition is null");
			}

			// input解析
			logger.info("[call] input string: {}", input);
			Map<String, Object> args = new HashMap<>();
			if (!input.isEmpty()) {
				try {
					args = objectMapper.readValue(input, Map.class);
					logger.info("[call] parsed args: {}", args);
				}
				catch (Exception e) {
					logger.error("[call] Failed to parse input to args", e);
					// 如果解析失败，尝试作为单个参数处理
					args.put("input", input);
				}
			}

			String protocol = this.toolDefinition.getProtocol();
			if (protocol == null) {
				throw new IllegalStateException("Protocol is null");
			}

			if ("mcp-sse".equalsIgnoreCase(protocol)) {
				McpServerRemoteServiceConfig remoteServerConfig = this.toolDefinition.getRemoteServerConfig();
				if (remoteServerConfig == null) {
					throw new IllegalStateException("Remote server config is null");
				}
				return handleMcpStreamProtocol(args, remoteServerConfig, protocol);
			}
			else if ("mcp-streamable".equalsIgnoreCase(protocol)) {

				logger.error("[call] Unsupported protocol: {}", protocol);
				return "Error: Unsupported protocol " + protocol;
				// McpServerRemoteServiceConfig remoteServerConfig =
				// this.toolDefinition.getRemoteServerConfig();
				// if (remoteServerConfig == null) {
				// throw new IllegalStateException("Remote server config is null");
				// }
				// return handleMcpStreamableProtocol(args, remoteServerConfig, protocol);
			}
			else {
				logger.error("[call] Unsupported protocol: {}", protocol);
				return "Error: Unsupported protocol " + protocol;
			}
		}
		catch (Exception e) {
			logger.error("[call] Unexpected error occurred", e);
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * 处理MCP流式协议的工具调用 (mcp-sse, mcp-streamable)
	 */
	private String handleMcpStreamProtocol(Map<String, Object> args, McpServerRemoteServiceConfig remoteServerConfig,
			String protocol) throws NacosException {
		McpServiceRef serviceRef = remoteServerConfig.getServiceRef();
		if (serviceRef != null) {
			McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
			if (mcpEndpointInfo == null) {
				throw new RuntimeException("No available endpoint found for service: " + serviceRef.getServiceName());
			}

			logger.info("[handleMcpStreamProtocol] Tool callback instance: {}", JacksonUtils.toJson(mcpEndpointInfo));
			String exportPath = remoteServerConfig.getExportPath();

			// 构建基础URL，根据协议类型调整
			String transportProtocol = StringUtil.isNotBlank(serviceRef.getTransportProtocol()) ? serviceRef.getTransportProtocol() : "http";
			StringBuilder baseUrl;
			if ("mcp-sse".equalsIgnoreCase(protocol)) {
				baseUrl = new StringBuilder(transportProtocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort());
			}
			else {
				// mcp-streamable 或其他协议
				baseUrl = new StringBuilder(transportProtocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort());
			}

			logger.info("[handleMcpStreamProtocol] Processing {} protocol with args: {} and baseUrl: {}", protocol,
					args, baseUrl.toString());

			try {
				// 获取工具名称 - 从工具定义名称中提取实际的工具名称
				String toolDefinitionName = this.toolDefinition.name();
				if (toolDefinitionName == null || toolDefinitionName.isEmpty()) {
					throw new RuntimeException("Tool definition name is not available");
				}

				// 工具定义名称格式为: serverName_tools_toolName
				// 需要提取最后的 toolName 部分
				String toolName;
				if (toolDefinitionName.contains("_tools_")) {
					toolName = toolDefinitionName.substring(toolDefinitionName.lastIndexOf("_tools_") + 7);
				}
				else {
					// 如果没有 _tools_ 分隔符，使用整个名称
					toolName = toolDefinitionName;
				}

				if (toolName.isEmpty()) {
					throw new RuntimeException("Extracted tool name is empty");
				}

				// 构建传输层
				StringBuilder sseEndpoint = new StringBuilder("/sse");
				if (exportPath != null && !exportPath.isEmpty()) {
					sseEndpoint = new StringBuilder(exportPath);
					if (mcpServerVO.getQueryParams() != null) {


						if (!sseEndpoint.toString().contains("?")) {
							sseEndpoint.append("?");
						}
						Iterator<Map.Entry<String, String>> iterator = mcpServerVO.getQueryParams().entrySet()
								.iterator();
						while (iterator.hasNext()) {
							Map.Entry<String, String> next = iterator.next();
							sseEndpoint.append(next.getKey()).append("=").append(next.getValue())
									.append(iterator.hasNext() ? "&" : "");
						}
					}
				}

				HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(baseUrl.toString())
						.sseEndpoint(sseEndpoint.toString());
				if (mcpServerVO.getHeaders()!=null){
					transportBuilder.customizeRequest(requestBuilder -> {
						for (Map.Entry<String, String> headerName : mcpServerVO.getHeaders().entrySet())
							requestBuilder.header(headerName.getKey(), headerName.getValue());
					});
				}

				// 添加自定义请求头（如果需要）
				// 这里可以根据需要添加认证头等
				HttpClientSseClientTransport transport = transportBuilder.build();

				// 创建MCP同步客户端
				McpSyncClient client = McpClient.sync(transport).build();
				try {
					// 初始化客户端
					InitializeResult initializeResult = client.initialize();
					logger.info("[handleMcpStreamProtocol] MCP Client initialized: {}", initializeResult);

					// 调用工具
					McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
					logger.info("[handleMcpStreamProtocol] CallToolRequest: {}", request);

					CallToolResult result = client.callTool(request);
					logger.info("[handleMcpStreamProtocol] tool call result: {}", result);

					// 处理结果
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
				finally {
					// 清理资源
					try {
						if (client != null) {
							client.close();
						}
					}
					catch (Exception e) {
						logger.warn("[handleMcpStreamProtocol] Failed to close MCP client", e);
					}
				}
			}
			catch (Exception e) {
				logger.error("[handleMcpStreamProtocol] MCP call failed:", e);
				return "Error: MCP call failed - " + e.getMessage();
			}
		}
		else {
			logger.error("[handleMcpStreamProtocol] serviceRef is null");
			return "Error: service reference is null";
		}
	}

	/**
	 * Close.
	 */
	public void close() {

		for (Map.Entry<String, AbstractListener> entry : nacosConfigListeners.entrySet()) {
			String cacheKey = entry.getKey();
			String dataId = cacheKey.split("@@")[0];
			String group = cacheKey.split("@@")[1];
			nacosMcpOperationService.getConfigService().removeListener(dataId, group, entry.getValue());
		}
	}

}
