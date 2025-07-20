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

package com.alibaba.cloud.ai.mcp.gateway.core;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Gateway 工具回调抽象基类 提供了通用的工具回调功能实现
 */
public abstract class AbstractMcpGatewayToolCallback implements ToolCallback {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractMcpGatewayToolCallback.class);

	protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w]*)\\s*\\}\\}");

	protected static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

	protected final McpGatewayToolDefinition toolDefinition;

	protected final WebClient.Builder webClientBuilder;

	public AbstractMcpGatewayToolCallback(McpGatewayToolDefinition toolDefinition) {
		this.toolDefinition = toolDefinition;
		this.webClientBuilder = getWebClientBuilder();
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return createToolDefinition();
	}

	@Override
	public String call(@NonNull String input) {
		return call(input, new ToolContext(new HashMap<>()));
	}

	@Override
	public String call(@NonNull String input, ToolContext toolContext) {
		try {
			logger.info("[call] input: {} toolContext: {}", input, toolContext);

			// 参数验证
			if (this.toolDefinition == null) {
				throw new IllegalStateException("Tool definition is null");
			}

			// 解析输入参数
			Map<String, Object> args = parseInput(input);

			// 获取服务端点
			McpEndpointInfo endpointInfo = getEndpointInfo();
			if (endpointInfo == null) {
				throw new RuntimeException("No available endpoint found for service: " + getServiceName());
			}

			// 构建基础URL
			String baseUrl = buildBaseUrl(endpointInfo);

			// 处理工具请求
			return processToolRequest(args, baseUrl);

		}
		catch (Exception e) {
			logger.error("[call] Unexpected error occurred", e);
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * 获取WebClient构建器
	 * @return WebClient构建器
	 */
	protected abstract WebClient.Builder getWebClientBuilder();

	/**
	 * 创建工具定义
	 * @return 工具定义
	 */
	protected abstract ToolDefinition createToolDefinition();

	/**
	 * 获取端点信息
	 * @return 端点信息
	 */
	protected abstract McpEndpointInfo getEndpointInfo();

	/**
	 * 获取服务名称
	 * @return 服务名称
	 */
	protected abstract String getServiceName();

	/**
	 * 处理工具请求
	 * @param args 参数
	 * @param baseUrl 基础URL
	 * @return 处理结果
	 */
	protected abstract String processToolRequest(Map<String, Object> args, String baseUrl);

	/**
	 * 解析输入参数
	 * @param input 输入字符串
	 * @return 参数映射
	 */
	protected Map<String, Object> parseInput(String input) {
		Map<String, Object> args = new HashMap<>();
		if (!input.isEmpty()) {
			try {
				args = objectMapper.readValue(input, Map.class);
				logger.info("[parseInput] parsed args: {}", args);
			}
			catch (Exception e) {
				logger.error("[parseInput] Failed to parse input to args", e);
				// 如果解析失败，尝试作为单个参数处理
				args.put("input", input);
			}
		}
		return args;
	}

	/**
	 * 构建基础URL
	 * @param endpointInfo 端点信息
	 * @return 基础URL
	 */
	protected String buildBaseUrl(McpEndpointInfo endpointInfo) {
		String protocol = this.toolDefinition.getProtocol();
		return protocol + "://" + endpointInfo.getAddress() + ":" + endpointInfo.getPort();
	}

	/**
	 * 处理模板字符串中的变量
	 * @param template 模板字符串
	 * @param data 数据
	 * @return 处理后的字符串
	 */
	protected String processTemplateString(String template, Map<String, Object> data) {
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

	/**
	 * 获取超时时间
	 * @return 超时时间
	 */
	protected abstract java.time.Duration getTimeoutDuration();

}
