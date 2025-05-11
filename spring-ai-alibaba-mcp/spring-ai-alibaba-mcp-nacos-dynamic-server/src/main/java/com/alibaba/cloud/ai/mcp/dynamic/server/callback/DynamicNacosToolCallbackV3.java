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

package com.alibaba.cloud.ai.mcp.dynamic.server.callback;

import com.alibaba.cloud.ai.mcp.dynamic.server.definition.DynamicNacosToolDefinitionV3;
import com.alibaba.cloud.ai.mcp.dynamic.server.utils.SpringBeanUtils;
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
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicNacosToolCallbackV3 implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(DynamicNacosToolCallbackV3.class);

	private final ToolDefinition toolDefinition;

	private final NamingService namingService;

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w]*)\\s*\\}\\}");

	private final WebClient.Builder webClientBuilder;

	static ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

	public DynamicNacosToolCallbackV3(final ToolDefinition toolDefinition) {
		this.webClientBuilder = SpringBeanUtils.getInstance().getBean(WebClient.Builder.class);
		this.toolDefinition = toolDefinition;
		this.namingService = SpringBeanUtils.getInstance().getBean(NamingService.class);
	}

	/**
	 * 解析工具配置并执行请求
	 */
	public Mono<String> executeToolRequest(String configJson, Map<String, Object> args, String baseUrl) {
		try {
			logger.info("[executeToolRequest] configJson: {} args: {} baseUrl: {}", configJson, args, baseUrl);
			JsonNode rootNode = objectMapper.readTree(configJson);
			logger.info("[executeToolRequest] rootNode: {}", rootNode);
			JsonNode toolsNode = rootNode.path("tools");
			logger.info("[executeToolRequest] toolsNode: {}", toolsNode);
			return processToolRequest(rootNode, args, baseUrl);
		}
		catch (Exception e) {
			logger.error("Failed to parse tool configuration", e);
			return Mono.error(e);
		}
	}

	/**
	 * 处理工具请求
	 */
	private Mono<String> processToolRequest(String configJson, Map<String, Object> args, String baseUrl) {
		try {
			logger.info("[processToolRequest] configJson: {} args: {} baseUrl: {}", configJson, args, baseUrl);
			JsonNode rootNode = objectMapper.readTree(configJson);
			logger.info("[processToolRequest] rootNode: {}", rootNode);
			JsonNode toolsNode = rootNode.path("tools");
			logger.info("[processToolRequest] toolsNode: {}", toolsNode);
			return processToolRequest(rootNode, args, baseUrl);
		}
		catch (Exception e) {
			logger.error("Failed to process tool request", e);
			return Mono.error(e);
		}
	}

	/**
	 * 处理工具请求
	 */
	private Mono<String> processToolRequest(JsonNode toolConfig, Map<String, Object> args, String baseUrl) {
		try {
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
			WebClient client = webClientBuilder.baseUrl(baseUrl != null ? baseUrl : "http://localhost").build();

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

		String url = requestTemplate.path("url").asText();
		String method = requestTemplate.path("method").asText();
		HttpMethod httpMethod = HttpMethod.valueOf(method);

		// 处理URL中的路径参数
		String processedUrl = processTemplateString(url, args);
		logger.info("[buildAndExecuteRequest] original url template: {} processed url: {}", url, processedUrl);

		// 构建请求
		WebClient.RequestBodySpec requestBodySpec = client.method(httpMethod)
			.uri(builder -> buildUri(builder, processedUrl, requestTemplate, args));

		// 添加请求头
		addHeaders(requestBodySpec, requestTemplate.path("headers"), args);

		// 处理请求体
		WebClient.RequestHeadersSpec<?> headersSpec = addRequestBody(requestBodySpec, requestTemplate, args);

		// 输出最终请求信息
		String fullUrl = baseUrl.endsWith("/") && processedUrl.startsWith("/") ? baseUrl + processedUrl.substring(1)
				: baseUrl + processedUrl;
		logger.info("[buildAndExecuteRequest] final request: method={} url={} args={}", method, fullUrl, args);

		// 执行请求
		return headersSpec.retrieve()
			.bodyToMono(String.class)
			.map(responseBody -> processResponse(responseBody, responseTemplate, args));
	}

	/**
	 * 构建URI，处理查询参数
	 */
	private URI buildUri(UriBuilder builder, String processedUrl, JsonNode requestTemplate, Map<String, Object> args) {
		builder.path(processedUrl);

		// 处理argsToUrlParam选项
		boolean argsToUrlParam = requestTemplate.path("argsToUrlParam").asBoolean(false);
		if (argsToUrlParam) {
			for (Map.Entry<String, Object> entry : args.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (value != null) {
					if (value instanceof final Collection<?> collection) {
						for (Object item : collection) {
							builder.queryParam(key, item);
						}
					}
					else {
						builder.queryParam(key, value);
					}
				}
			}
		}

		return builder.build();
	}

	/**
	 * 添加请求头
	 */
	private void addHeaders(WebClient.RequestBodySpec requestSpec, JsonNode headersNode, Map<String, Object> args) {
		if (headersNode.isArray()) {
			for (JsonNode header : headersNode) {
				String key = header.path("key").asText();
				String valueTemplate = header.path("value").asText();

				// 处理请求头中的模板变量
				String value = processTemplateString(valueTemplate, args);
				requestSpec.header(key, value);
			}
		}
	}

	/**
	 * 添加请求体
	 */
	private WebClient.RequestHeadersSpec<?> addRequestBody(WebClient.RequestBodySpec requestSpec,
			JsonNode requestTemplate, Map<String, Object> args) {
		// 检查互斥选项
		boolean hasBody = requestTemplate.has("body") && !requestTemplate.path("body").asText().isEmpty();
		boolean argsToJsonBody = requestTemplate.path("argsToJsonBody").asBoolean(false);
		boolean argsToFormBody = requestTemplate.path("argsToFormBody").asBoolean(false);
		boolean argsToUrlParam = requestTemplate.path("argsToUrlParam").asBoolean(false);

		int optionCount = (hasBody ? 1 : 0) + (argsToJsonBody ? 1 : 0) + (argsToFormBody ? 1 : 0)
				+ (argsToUrlParam ? 1 : 0);
		if (optionCount > 1) {
			throw new IllegalArgumentException(
					"Only one of body, argsToJsonBody, argsToFormBody, or argsToUrlParam should be specified");
		}

		if (hasBody) {
			// 使用模板字符串作为请求体
			String bodyTemplate = requestTemplate.path("body").asText();
			String processedBody = processTemplateString(bodyTemplate, args);

			// 假设body是JSON格式
			return requestSpec.contentType(MediaType.APPLICATION_JSON).bodyValue(processedBody);
		}
		else if (argsToJsonBody) {
			// 使用参数作为JSON请求体
			try {
				String jsonBody = objectMapper.writeValueAsString(args);
				return requestSpec.contentType(MediaType.APPLICATION_JSON).bodyValue(jsonBody);
			}
			catch (JsonProcessingException e) {
				logger.error("Failed to create JSON request body", e);
				return requestSpec;
			}
		}
		else if (argsToFormBody) {
			// 使用参数作为表单请求体
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			args.forEach((key, value) -> {
				if (value != null) {
					formData.add(key, value.toString());
				}
			});

			return requestSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(formData));
		}

		// 如果没有请求体，返回requestSpec
		return requestSpec;
	}

	/**
	 * 处理响应
	 */
	@SuppressWarnings("unchecked")
	private String processResponse(String responseBody, JsonNode responseTemplate, Map<String, Object> args) {
		logger.info("[processResponse] received responseBody: {}", responseBody);
		String result = null;
		if (!responseTemplate.isEmpty()) {
			if (responseTemplate.has("body") && !responseTemplate.path("body").asText().isEmpty()) {
				String bodyTemplate = responseTemplate.path("body").asText();
				// 新增：支持jsonPath表达式
				if (bodyTemplate.startsWith("{{$") && bodyTemplate.endsWith("}}")) {
					String jsonPathExpr = bodyTemplate.substring(2, bodyTemplate.length() - 2).trim();
					// 判断是否为JSON
					String trimmed = responseBody.trim();
					boolean isJson = (trimmed.startsWith("{") && trimmed.endsWith("}"))
							|| (trimmed.startsWith("[") && trimmed.endsWith("]"));
					if (isJson) {
						try {
							Object value = com.jayway.jsonpath.JsonPath.read(responseBody, jsonPathExpr);
							result = value != null ? value.toString() : "";
							logger.info("[processResponse] jsonPath result: {}", result);
							return result;
						}
						catch (Exception e) {
							logger.warn("Failed to parse responseBody with jsonPath: {}", jsonPathExpr, e);
							result = "";
							logger.info("[processResponse] jsonPath result: {}", result);
							return result;
						}
					}
					else {
						logger.info("[processResponse] responseBody is not JSON, return as is for jsonPath template");
						result = responseBody;
						logger.info("[processResponse] result: {}", result);
						return result;
					}
				}
				try {
					JsonNode responseJson = objectMapper.readTree(responseBody);
					Map<String, Object> responseData = objectMapper.convertValue(responseJson, Map.class);
					Map<String, Object> templateData = new HashMap<>(args);
					templateData.putAll(responseData);
					result = processTemplateString(bodyTemplate, templateData);
					logger.info("[processResponse] template result: {}", result);
					return result;
				}
				catch (IOException e) {
					logger.info("[processResponse] responseBody is not JSON, fallback to raw response for template");
					// 关键：把 responseBody 作为 data["."] 传递
					Map<String, Object> rawMap = new HashMap<>();
					rawMap.put(".", responseBody);
					result = processTemplateString(bodyTemplate, rawMap);
					logger.info("[processResponse] template result: {}", result);
					return result;
				}
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
			if (input != null && !input.isEmpty()) {
				try {
					args = objectMapper.readValue(input, Map.class);
					logger.info("[call] parsed args: {}", args);
				}
				catch (Exception e) {
					logger.error("[call] Failed to parse input to args", e);
				}
			}

			DynamicNacosToolDefinitionV3 nacosToolDefinition = (DynamicNacosToolDefinitionV3) this.toolDefinition;
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
								return executeToolRequest(configJson, args, baseUrl).block();
							}
							catch (Exception e) {
								logger.error("Failed to execute tool request", e);
								return "";
							}
						}
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
