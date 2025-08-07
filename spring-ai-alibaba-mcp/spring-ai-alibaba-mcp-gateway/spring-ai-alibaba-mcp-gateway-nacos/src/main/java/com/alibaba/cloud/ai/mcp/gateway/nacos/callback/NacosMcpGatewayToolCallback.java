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

package com.alibaba.cloud.ai.mcp.gateway.nacos.callback;

import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate.RequestTemplateInfo;
import com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate.RequestTemplateParser;
import com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate.ResponseTemplateParser;
import com.alibaba.cloud.ai.mcp.gateway.core.utils.SpringBeanUtils;
import com.alibaba.cloud.ai.mcp.gateway.nacos.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
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
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NacosMcpGatewayToolCallback implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolCallback.class);

	private final NacosMcpGatewayToolDefinition toolDefinition;

	private final NacosMcpOperationService nacosMcpOperationService;

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w]*)\\s*\\}\\}");

	private final WebClient.Builder webClientBuilder;

	static ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

	public NacosMcpGatewayToolCallback(final McpGatewayToolDefinition toolDefinition) {
		this.webClientBuilder = SpringBeanUtils.getInstance().getBean(WebClient.Builder.class);
		this.toolDefinition = (NacosMcpGatewayToolDefinition) toolDefinition;
		this.nacosMcpOperationService = SpringBeanUtils.getInstance().getBean(NacosMcpOperationService.class);

		// 尝试获取配置属性
		// try {
		// NacosMcpGatewayProperties properties = SpringBeanUtils.getInstance()
		// .getBean(NacosMcpGatewayProperties.class);
		// if (properties != null) {
		// logger.info("Loaded gateway properties: maxConnections={},
		// connectionTimeout={}, readTimeout={}",
		// properties.getMaxConnections(), properties.getConnectionTimeout(),
		// properties.getReadTimeout());
		// }
		// }
		// catch (Exception e) {
		// logger.debug("Failed to load gateway properties, using defaults", e);
		// }
	}

	/**
	 * 处理工具请求
	 */
	private Mono<String> processToolRequest(String configJson, Map<String, Object> args, String baseUrl) {
		try {
			JsonNode toolConfig = objectMapper.readTree(configJson);
			logger.info("[processToolRequest] toolConfig: {} args: {} baseUrl: {}", toolConfig, args, baseUrl);

			// 验证配置完整性
			if (toolConfig == null || toolConfig.isEmpty()) {
				return Mono.error(new IllegalArgumentException("Tool configuration is empty or invalid"));
			}

			JsonNode argsNode = toolConfig.path("args");
			Map<String, Object> processedArgs;
			if (!argsNode.isMissingNode() && argsNode.isArray() && argsNode.size() > 0) {
				processedArgs = processArguments(argsNode, args);
				logger.info("[processToolRequest] processedArgs from args: {}", processedArgs);
			}
			else if (!toolConfig.path("inputSchema").isMissingNode() && toolConfig.path("inputSchema").isObject()) {
				// 从 inputSchema.properties 解析参数
				JsonNode properties = toolConfig.path("inputSchema").path("properties");
				if (properties.isObject()) {
					processedArgs = new HashMap<>();
					properties.fieldNames().forEachRemaining(field -> {
						if (args.containsKey(field)) {
							processedArgs.put(field, args.get(field));
						}
					});
					logger.info("[processToolRequest] processedArgs from inputSchema: {}", processedArgs);
				}
				else {
					processedArgs = args;
					logger.info("[processToolRequest] inputSchema.properties missing, use original args: {}",
							processedArgs);
				}
			}
			else {
				processedArgs = args;
				logger.info("[processToolRequest] no args or inputSchema, use original args: {}", processedArgs);
			}

			JsonNode requestTemplate = toolConfig.path("requestTemplate");
			String url = requestTemplate.path("url").asText();
			String method = requestTemplate.path("method").asText();
			logger.info("[processToolRequest] requestTemplate: {} url: {} method: {}", requestTemplate, url, method);

			// 检查URL和方法
			if (url.isEmpty() || method.isEmpty()) {
				return Mono.error(new IllegalArgumentException("URL and method are required in requestTemplate"));
			}

			// 验证HTTP方法
			try {
				HttpMethod.valueOf(method.toUpperCase());
			}
			catch (IllegalArgumentException e) {
				return Mono.error(new IllegalArgumentException("Invalid HTTP method: " + method));
			}

			// 创建WebClient
			baseUrl = baseUrl != null ? baseUrl : "http://localhost";
			WebClient client = webClientBuilder.baseUrl(baseUrl).build();

			// 构建并执行请求
			return buildAndExecuteRequest(client, requestTemplate, toolConfig.path("responseTemplate"), processedArgs,
					baseUrl)
				.onErrorResume(e -> {
					logger.error("Failed to execute tool request: {}", e.getMessage(), e);
					return Mono.error(new RuntimeException("Tool execution failed: " + e.getMessage(), e));
				});
		}
		catch (Exception e) {
			logger.error("Failed to process tool request", e);
			return Mono.error(new RuntimeException("Failed to process tool request: " + e.getMessage(), e));
		}
	}

	/**
	 * 处理参数定义和值
	 */
	private Map<String, Object> processArguments(JsonNode argsDefinition, Map<String, Object> providedArgs) {
		Map<String, Object> processedArgs = new HashMap<>();

		if (argsDefinition.isArray()) {
			for (JsonNode argDef : argsDefinition) {
				String name = argDef.path("name").asText();
				boolean required = argDef.path("required").asBoolean(false);
				Object defaultValue = argDef.has("default")
						? objectMapper.convertValue(argDef.path("default"), Object.class) : null;

				// 检查参数
				if (providedArgs.containsKey(name)) {
					processedArgs.put(name, providedArgs.get(name));
				}
				else if (defaultValue != null) {
					processedArgs.put(name, defaultValue);
				}
				else if (required) {
					throw new IllegalArgumentException("Required argument missing: " + name);
				}
			}
		}

		return processedArgs;
	}

	/**
	 * 构建并执行WebClient请求
	 */
	private Mono<String> buildAndExecuteRequest(WebClient client, JsonNode requestTemplate, JsonNode responseTemplate,
			Map<String, Object> args, String baseUrl) {

		RequestTemplateInfo info = RequestTemplateParser.parseRequestTemplate(requestTemplate);
		String url = info.url;
		String method = info.method;
		HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

		// 处理URL中的路径参数
		String processedUrl = processTemplateString(url, args);
		logger.info("[buildAndExecuteRequest] original url template: {} processed url: {}", url, processedUrl);

		// 构建请求
		WebClient.RequestBodySpec requestBodySpec = client.method(httpMethod)
			.uri(builder -> RequestTemplateParser.buildUri(builder, processedUrl, info, args));

		// 添加请求头
		RequestTemplateParser.addHeaders(requestBodySpec, info.headers, args, this::processTemplateString);

		// 处理请求体
		WebClient.RequestHeadersSpec<?> headersSpec = RequestTemplateParser.addRequestBody(requestBodySpec, info, args,
				this::processTemplateString, objectMapper, logger);

		// 输出最终请求信息
		String fullUrl = baseUrl.endsWith("/") && processedUrl.startsWith("/") ? baseUrl + processedUrl.substring(1)
				: baseUrl + processedUrl;
		logger.info("[buildAndExecuteRequest] final request: method={} url={} args={}", method, fullUrl, args);

		return headersSpec.retrieve()
			.onStatus(status -> status.is4xxClientError(),
					response -> Mono.error(new RuntimeException("Client error: " + response.statusCode())))
			.onStatus(status -> status.is5xxServerError(),
					response -> Mono.error(new RuntimeException("Server error: " + response.statusCode())))
			.bodyToMono(String.class)
			.timeout(getTimeoutDuration()) // 使用配置的超时时间
			.doOnNext(responseBody -> logger.info("[buildAndExecuteRequest] received responseBody: {}", responseBody))
			.map(responseBody -> processResponse(responseBody, responseTemplate, args))
			.onErrorResume(e -> {
				logger.error("[buildAndExecuteRequest] Request failed: {}", e.getMessage(), e);
				return Mono.error(new RuntimeException("HTTP request failed: " + e.getMessage(), e));
			});
	}

	/**
	 * 处理响应
	 */
	private String processResponse(String responseBody, JsonNode responseTemplate, Map<String, Object> args) {
		logger.info("[processResponse] received responseBody: {}", responseBody);
		String result = null;
		if (!responseTemplate.isEmpty()) {
			if (responseTemplate.has("body") && !responseTemplate.path("body").asText().isEmpty()) {
				String bodyTemplate = responseTemplate.path("body").asText();
				// 统一交给 ResponseTemplateParser 处理
				result = ResponseTemplateParser.parse(responseBody, bodyTemplate);
				logger.info("[processResponse] ResponseTemplateParser result: {}", result);
				return result;
			}
			else if (responseTemplate.has("prependBody") || responseTemplate.has("appendBody")) {
				String prependText = responseTemplate.path("prependBody").asText("");
				String appendText = responseTemplate.path("appendBody").asText("");
				result = processTemplateString(prependText, args) + responseBody
						+ processTemplateString(appendText, args);
				logger.info("[processResponse] prepend/append result: {}", result);
				return result;
			}
		}
		result = responseBody;
		logger.info("[processResponse] default result: {}", result);
		return result;
	}

	/**
	 * 处理模板字符串中的变量
	 */
	private String processTemplateString(String template, Map<String, Object> data) {
		logger.debug("[processTemplateString] template: {} data: {}", template, data);
		if (template == null || template.isEmpty()) {
			return "";
		}

		Matcher matcher = TEMPLATE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String variable = matcher.group(1);
			String replacement;
			if ("".equals(variable) || ".".equals(variable)) {
				// 特殊处理{{.}}，输出data唯一值或整个data
				if (data != null && data.size() == 1) {
					replacement = String.valueOf(data.values().iterator().next());
				}
				else if (data != null && !data.isEmpty()) {
					replacement = data.toString();
				}
				else {
					replacement = "";
				}
			}
			else {
				Object value = data != null ? data.get(variable) : null;
				if (value == null) {
					logger.warn("[processTemplateString] Variable '{}' not found in data, using empty string",
							variable);
					replacement = "";
				}
				else {
					replacement = value.toString();
				}
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		String finalResult = result.toString();
		logger.debug("[processTemplateString] final result: {}", finalResult);

		// 验证是否还存在未被替换的{{.}}，如有则输出警告
		if (finalResult.contains("{{.}}")) {
			logger.warn("[processTemplateString] WARNING: {{.}} was not replaced in result: {}", finalResult);
		}

		return finalResult;
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

			// 根据协议类型分发到不同的处理方法
			if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
				McpServerRemoteServiceConfig remoteServerConfig = this.toolDefinition.getRemoteServerConfig();
				if (remoteServerConfig == null) {
					throw new IllegalStateException("Remote server config is null");
				}

				return handleHttpHttpsProtocol(args, remoteServerConfig, protocol);
			}
			else if ("mcp-sse".equalsIgnoreCase(protocol)) {
				McpServerRemoteServiceConfig remoteServerConfig = this.toolDefinition.getRemoteServerConfig();
				if (remoteServerConfig == null) {
					throw new IllegalStateException("Remote server config is null");
				}
				return handleMcpStreamProtocol(args, remoteServerConfig, protocol);
			}
			else if ("mcp-streamable".equalsIgnoreCase(protocol)) {
				McpServerRemoteServiceConfig remoteServerConfig = this.toolDefinition.getRemoteServerConfig();
				if (remoteServerConfig == null) {
					throw new IllegalStateException("Remote server config is null");
				}
				return handleMcpStreamableProtocol(args, remoteServerConfig, protocol);
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
	 * 处理HTTP/HTTPS协议的工具调用
	 */
	private String handleHttpHttpsProtocol(Map<String, Object> args, McpServerRemoteServiceConfig remoteServerConfig,
			String protocol) throws NacosException {
		McpServiceRef serviceRef = remoteServerConfig.getServiceRef();
		if (serviceRef != null) {
			McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
			if (mcpEndpointInfo == null) {
				throw new RuntimeException("No available endpoint found for service: " + serviceRef.getServiceName());
			}

			logger.info("Tool callback instance: {}", JacksonUtils.toJson(mcpEndpointInfo));
			McpToolMeta toolMeta = this.toolDefinition.getToolMeta();
			String baseUrl = protocol + "://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();

			if (toolMeta != null && toolMeta.getTemplates() != null) {
				Map<String, Object> templates = toolMeta.getTemplates();
				if (templates != null && templates.containsKey("json-go-template")) {
					Object jsonGoTemplate = templates.get("json-go-template");
					try {
						logger.info("[handleHttpHttpsProtocol] json-go-template: {}",
								objectMapper.writeValueAsString(jsonGoTemplate));
					}
					catch (JsonProcessingException e) {
						logger.error("[handleHttpHttpsProtocol] Failed to serialize json-go-template", e);
					}
					try {
						// 调用executeToolRequest
						String configJson = objectMapper.writeValueAsString(jsonGoTemplate);
						logger.info("[handleHttpHttpsProtocol] configJson: {} args: {} baseUrl: {}", configJson, args,
								baseUrl);
						return processToolRequest(configJson, args, baseUrl).block();
					}
					catch (Exception e) {
						logger.error("Failed to execute tool request", e);
						return "Error: " + e.getMessage();
					}
				}
				else {
					logger.warn("[handleHttpHttpsProtocol] json-go-template not found in templates");
					return "Error: json-go-template not found in tool configuration";
				}
			}
			else {
				logger.warn("[handleHttpHttpsProtocol] templates not found in toolsMeta");
				return "Error: templates not found in tool metadata";
			}
		}
		else {
			logger.error("[handleHttpHttpsProtocol] serviceRef is null");
			return "Error: service reference is null";
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
			String baseUrl;
			if ("mcp-sse".equalsIgnoreCase(protocol)) {
				baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
			}
			else {
				// mcp-streamable 或其他协议
				baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
			}

			logger.info("[handleMcpStreamProtocol] Processing {} protocol with args: {} and baseUrl: {}", protocol,
					args, baseUrl);

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
				String sseEndpoint = "/sse";
				if (exportPath != null && !exportPath.isEmpty()) {
					sseEndpoint = exportPath;
				}

				HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(baseUrl)
					.sseEndpoint(sseEndpoint);

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
	 * 处理MCP Streamable HTTP协议的工具调用
	 */
	private String handleMcpStreamableProtocol(Map<String, Object> args,
			McpServerRemoteServiceConfig remoteServerConfig, String protocol) throws NacosException {
		McpServiceRef serviceRef = remoteServerConfig.getServiceRef();
		if (serviceRef != null) {
			McpEndpointInfo mcpEndpointInfo = nacosMcpOperationService.selectEndpoint(serviceRef);
			if (mcpEndpointInfo == null) {
				throw new RuntimeException("No available endpoint found for service: " + serviceRef.getServiceName());
			}

			logger.info("[handleMcpStreamableProtocol] Tool callback instance: {}",
					JacksonUtils.toJson(mcpEndpointInfo));
			String exportPath = remoteServerConfig.getExportPath();

			// 构建基础URL
			String baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();

			// 构建streamable endpoint
			String streamableEndpoint = "/streamable";
			if (exportPath != null && !exportPath.isEmpty()) {
				streamableEndpoint = exportPath;
			}

			logger.info(
					"[handleMcpStreamableProtocol] Processing {} protocol with args: {} and baseUrl: {} endpoint: {}",
					protocol, args, baseUrl, streamableEndpoint);

			try {
				// 获取工具名称
				String toolDefinitionName = this.toolDefinition.name();
				if (toolDefinitionName == null || toolDefinitionName.isEmpty()) {
					throw new RuntimeException("Tool definition name is not available");
				}

				String toolName;
				if (toolDefinitionName.contains("_tools_")) {
					toolName = toolDefinitionName.substring(toolDefinitionName.lastIndexOf("_tools_") + 7);
				}
				else {
					toolName = toolDefinitionName;
				}

				if (toolName.isEmpty()) {
					throw new RuntimeException("Extracted tool name is empty");
				}

				// HTTP协议版本

				// 创建MCP同步客户端，使用Streamable HTTP传输
				HttpClientStreamableHttpTransport.Builder transportBuilder = HttpClientStreamableHttpTransport
					.builder(baseUrl)
					.endpoint(streamableEndpoint);

				HttpClientStreamableHttpTransport transport = transportBuilder.build();
				McpSyncClient client = McpClient.sync(transport).build();

				try {
					// 初始化客户端
					InitializeResult initializeResult = client.initialize();
					logger.info("[handleMcpStreamableProtocol] MCP Client initialized: {}", initializeResult);

					// 调用工具
					McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, args);
					logger.info("[handleMcpStreamableProtocol] CallToolRequest: {}", request);

					CallToolResult result = client.callTool(request);
					logger.info("[handleMcpStreamableProtocol] tool call result: {}", result);

					// 处理结果
					Object content = result.content();
					if (content instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
						Object first = list.get(0);
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
						logger.warn("[handleMcpStreamableProtocol] Failed to close MCP client", e);
					}
				}
			}
			catch (Exception e) {
				logger.error("[handleMcpStreamableProtocol] MCP streamable call failed:", e);
				return "Error: MCP streamable call failed - " + e.getMessage();
			}
		}
		else {
			logger.error("[handleMcpStreamableProtocol] serviceRef is null");
			return "Error: service reference is null";
		}
	}

	private java.time.Duration getTimeoutDuration() {

		return java.time.Duration.ofSeconds(30); // 默认超时时间
	}

}
