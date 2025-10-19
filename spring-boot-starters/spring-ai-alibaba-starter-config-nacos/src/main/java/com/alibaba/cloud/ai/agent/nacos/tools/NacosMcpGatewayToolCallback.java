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

	// Match {{ ${nacos.dataId/group} }} or {{ ${nacos.dataId/group}.key1.key2 }}
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
	 * Resolve Nacos reference
	 * @param nacosRef Reference string, format: dataId/group
	 * @param dotNotation Dot notation part, format: .key1.key2 (may be null)
	 * @return Resolved value
	 */
	private String resolveNacosReference(String nacosRef, String dotNotation) {
		if (!org.springframework.util.StringUtils.hasText(nacosRef)) {
			return null;
		}

		try {
			// Parse dataId and group
			String[] configParts = nacosRef.split("/");
			if (configParts.length != 2) {
				throw new IllegalArgumentException(
						"Invalid Nacos config reference format: " + nacosRef + ". Expected format: dataId/group");
			}

			String dataId = configParts[0];
			String group = configParts[1];

			// Get config content
			String configContent = getConfigContent(dataId, group);
			if (!org.springframework.util.StringUtils.hasText(configContent)) {
				logger.warn("[resolveNacosReference] No content found for dataId: {}, group: {}", dataId, group);
				return null;
			}

			// If no dot notation, return config content directly
			if (!org.springframework.util.StringUtils.hasText(dotNotation)) {
				return configContent;
			}

			// If dot notation exists, remove leading dot and parse JSON to extract specified field
			String jsonPath = dotNotation.startsWith(".") ? dotNotation.substring(1) : dotNotation;
			return extractJsonValueFromNacos(configContent, jsonPath);

		}
		catch (Exception e) {
			// Log but don't interrupt processing
			logger.error("[resolveNacosReference] Failed to resolve Nacos reference: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to resolve Nacos reference: " + e.getMessage(), e);
		}
	}

	/**
	 * Get Nacos config content
	 * @param dataId Config ID
	 * @param group Group
	 * @return Config content
	 * @throws NacosException Nacos exception
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
	 * Extract value from JSON string by specified path
	 * @param jsonString JSON string
	 * @param jsonPath JSON path, e.g. key1.key2
	 * @return Extracted value
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

			// Return appropriate value based on node type
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
				// For complex objects, return JSON string
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
			// Get full path, e.g. .args.name or .data.key1.key2
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
	 * Resolve value by path
	 * @param fullPath Full path, e.g. .args.name or .data.key1.key2
	 * @param args Parameter data mapping
	 * @param extendedData Extended data (JSON string)
	 * @return Resolved value
	 */
	private String resolvePathValue(String fullPath, Map<String, Object> args, String extendedData) {
		if (fullPath == null || fullPath.isEmpty()) {
			return "";
		}
		// Remove leading dot
		if (fullPath.startsWith(".")) {
			fullPath = fullPath.substring(1);
		}

		String[] pathParts = fullPath.split("\\.");
		if (pathParts.length == 0) {
			return "";
		}

		// Determine data source
		Object dataSource;
		if (pathParts[0].equals("args")) {
			// Get value from args
			dataSource = args;
			// If only args, no specific field name
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
			// Get value from extendedData
			// First parse extendedData string as JSON object
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
				// If parsing fails, treat extendedData as plain string
				if (pathParts.length == 1 && fullPath.equals("extendedData")) {
					return extendedData != null ? extendedData : "";
				}
				return "";
			}

			// Special handling for direct access to extendedData
			if (pathParts.length == 1 && fullPath.equals("extendedData")) {
				return extendedData != null ? extendedData : "";
			}
		}

		// If data source is null
		if (dataSource == null) {
			return "";
		}
		// Handle nested paths
		Object currentValue = dataSource;
		int startIndex = pathParts[0].equals("args") ? 1 : 0;
		// If args, start from index 1; otherwise start from index 0

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

			// Parameter validation
			if (this.toolDefinition == null) {
				throw new IllegalStateException("Tool definition is null");
			}

			// Input parsing
			logger.info("[call] input string: {}", input);
			Map<String, Object> args = new HashMap<>();
			if (!input.isEmpty()) {
				try {
					args = objectMapper.readValue(input, Map.class);
					logger.info("[call] parsed args: {}", args);
				}
				catch (Exception e) {
					logger.error("[call] Failed to parse input to args", e);
					// If parsing fails, try to handle as single parameter
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
	 * Handle MCP streaming protocol tool calls (mcp-sse, mcp-streamable)
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

			// Build base URL, adjust according to protocol type
			String transportProtocol = StringUtil.isNotBlank(serviceRef.getTransportProtocol()) ? serviceRef.getTransportProtocol() : "http";
			StringBuilder baseUrl;
			if ("mcp-sse".equalsIgnoreCase(protocol)) {
				baseUrl = new StringBuilder(transportProtocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort());
			}
			else {
				// mcp-streamable or other protocols
				baseUrl = new StringBuilder(transportProtocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort());
			}

			logger.info("[handleMcpStreamProtocol] Processing {} protocol with args: {} and baseUrl: {}", protocol,
					args, baseUrl.toString());

			try {
				// Get tool name - extract actual tool name from tool definition name
				String toolDefinitionName = this.toolDefinition.name();
				if (toolDefinitionName == null || toolDefinitionName.isEmpty()) {
					throw new RuntimeException("Tool definition name is not available");
				}

				// Tool definition name format: serverName_tools_toolName
				// Need to extract the final toolName part
				String toolName;
				if (toolDefinitionName.contains("_tools_")) {
					toolName = toolDefinitionName.substring(toolDefinitionName.lastIndexOf("_tools_") + 7);
				}
				else {
					// If no _tools_ separator, use the entire name
					toolName = toolDefinitionName;
				}

				if (toolName.isEmpty()) {
					throw new RuntimeException("Extracted tool name is empty");
				}

				// Build transport layer
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

				// Add custom request headers (if needed)
				// Here you can add authentication headers etc. as needed
				HttpClientSseClientTransport transport = transportBuilder.build();

				// Create MCP sync client
				McpSyncClient client = McpClient.sync(transport).build();
				try {
					// Initialize client
					InitializeResult initializeResult = client.initialize();
					logger.info("[handleMcpStreamProtocol] MCP Client initialized: {}", initializeResult);

					// Call tool
					McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
					logger.info("[handleMcpStreamProtocol] CallToolRequest: {}", request);

					CallToolResult result = client.callTool(request);
					logger.info("[handleMcpStreamProtocol] tool call result: {}", result);

					// Handle result
					Object content = result.content();
					if (content instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
						Object first = list.get(0);
						// Compatible with TextContent's text field
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
					// Clean up resources
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
