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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.callback;

import com.alibaba.cloud.ai.mcp.nacos2.gateway.definition.NacosMcpGatewayToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.jsontemplate.RequestTemplateParser;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.jsontemplate.ResponseTemplateParser;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.utils.SpringBeanUtils;
import com.alibaba.cloud.ai.mcp.nacos2.gateway.jsontemplate.RequestTemplateInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NacosMcpGatewayToolCallbackV3 implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpGatewayToolCallbackV3.class);

	private final ToolDefinition toolDefinition;

	private final NamingService namingService;

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w]*)\\s*\\}\\}");

	private final WebClient.Builder webClientBuilder;

	static ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

	public NacosMcpGatewayToolCallbackV3(final ToolDefinition toolDefinition) {
		this.webClientBuilder = SpringBeanUtils.getInstance().getBean(WebClient.Builder.class);
		this.toolDefinition = toolDefinition;
		this.namingService = SpringBeanUtils.getInstance().getBean(NamingService.class);
	}

	/**
	 * 处理工具请求
	 */
	private Mono<String> processToolRequest(String configJson, Map<String, Object> args, String baseUrl) {
		try {
			JsonNode toolConfig = objectMapper.readTree(configJson);
			logger.info("[executeToolRequest] toolConfig: {}", toolConfig);
			logger.info("[processToolRequest] toolConfig: {} args: {} baseUrl: {}", toolConfig, args, baseUrl);
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
				return Mono.error(new IllegalArgumentException("URL and method are required"));
			}

			// 创建WebClient
			baseUrl = baseUrl != null ? baseUrl : "http://localhost";
			WebClient client = webClientBuilder.baseUrl(baseUrl).build();

			// 构建并执行请求
			return buildAndExecuteRequest(client, requestTemplate, toolConfig.path("responseTemplate"), processedArgs,
					baseUrl);
		}
		catch (Exception e) {
			logger.error("Failed to process tool request", e);
			return Mono.error(e);
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
		HttpMethod httpMethod = HttpMethod.valueOf(method);

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
			.bodyToMono(String.class)
			.doOnNext(responseBody -> logger.info("[buildAndExecuteRequest] received responseBody: {}", responseBody))
			.map(responseBody -> processResponse(responseBody, responseTemplate, args));
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
		logger.info("[processTemplateString] template: {} data: {}", template, data);
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
				replacement = (value != null) ? value.toString() : "";
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		String finalResult = result.toString();
		logger.info("[processTemplateString] final result: {}", finalResult);
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
				}
			}

			NacosMcpGatewayToolDefinitionV3 nacosToolDefinition = (NacosMcpGatewayToolDefinitionV3) this.toolDefinition;
			logger.info("Tool callback toolDefinition: {}", JacksonUtils.toJson(nacosToolDefinition));
			Object remoteServerConfig = nacosToolDefinition.getRemoteServerConfig();
			String protocol = nacosToolDefinition.getProtocol();
			if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
				Map<String, Object> configMap = (Map<String, Object>) remoteServerConfig;
				Object serviceRef = configMap.get("serviceRef");
				if (serviceRef != null) {
					Map<String, Object> refMap = (Map<String, Object>) serviceRef;
					String serviceName = (String) refMap.get("serviceName");
					String groupName = (String) refMap.get("groupName");
					Instance instance = namingService.selectOneHealthyInstance(serviceName, groupName);
					logger.info("Tool callback instance: {}", JacksonUtils.toJson(instance));
					Map<String, Object> toolsMeta = (Map<String, Object>) nacosToolDefinition.getToolsMeta();
					String baseUrl = "http://" + instance.getIp() + ":" + instance.getPort();

					if (toolsMeta != null && toolsMeta.containsKey("templates")) {
						Map<String, Object> templates = (Map<String, Object>) toolsMeta.get("templates");
						if (templates != null && templates.containsKey("json-go-template")) {
							Object jsonGoTemplate = templates.get("json-go-template");
							try {
								logger.info("[call] json-go-template: {}",
										objectMapper.writeValueAsString(jsonGoTemplate));
							}
							catch (JsonProcessingException e) {
								logger.error("[call] Failed to serialize json-go-template", e);
							}
							try {
								// 调用executeToolRequest
								String configJson = objectMapper.writeValueAsString(jsonGoTemplate);
								logger.info("[executeToolRequest] configJson: {} args: {} baseUrl: {}", configJson,
										args, baseUrl);
								return processToolRequest(configJson, args, baseUrl).block();
							}
							catch (Exception e) {
								logger.error("Failed to execute tool request", e);
								return "";
							}
						}
					}
					else {
						logger.warn("[call] templates not found in toolsMeta");
					}
				}
			}

			return "";
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
