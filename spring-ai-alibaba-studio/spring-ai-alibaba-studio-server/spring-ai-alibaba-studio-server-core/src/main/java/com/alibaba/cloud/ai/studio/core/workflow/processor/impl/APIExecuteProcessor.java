/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.workflow.processor.impl;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.RetryConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.TimeoutConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.TryCatchConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.HttpClientManager;
import com.alibaba.cloud.ai.studio.core.base.domain.RpcResult;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * API Node Processor
 * <p>
 * This processor is responsible for executing API calls within workflows, supporting
 * various HTTP methods, authentication types, and request/response formats.
 * <p>
 * Key Features: 1. Supports multiple HTTP methods (GET, POST, PUT, DELETE, etc.) 2.
 * Handles various authentication types (Bearer, API Key) 3. Supports different request
 * body formats (JSON, form-data) 4. Manages request headers and query parameters 5.
 * Handles response parsing and formatting 6. Supports timeout and retry configurations 7.
 * Provides error handling and validation 8. Supports template variable substitution in
 * requests
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Slf4j
@Component("APIExecuteProcessor")
public class APIExecuteProcessor extends AbstractExecuteProcessor {

	private final HttpClientManager httpClientManager;

	public APIExecuteProcessor(HttpClientManager httpClientManager, RedisManager redisManager,
			WorkflowInnerService workflowInnerService, ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.httpClientManager = httpClientManager;
	}

	/**
	 * Executes the API node in the workflow
	 * @param graph The workflow graph
	 * @param node The API node to execute
	 * @param context The workflow context
	 * @return NodeResult containing API call status and response
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		// init and refresh context
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		String method = config.getMethod();
		String url = config.getUrl();
		url = replaceTemplateContent(url, context);
		List<Node.InputParam> headersArray = config.getHeaders();
		List<Node.InputParam> paramsArray = config.getParams();
		Map<String, Object> body = config.getBody();
		List<Map<String, Object>> params = buildParams(paramsArray, context);
		Map<String, Object> form = buildBodyFormParams(body, context);
		Map<String, Object> headers = buildHeaders(headersArray, context);
		// handle authorization
		handleAuthorization(config, headers, params, context);
		nodeResult.setInput(JsonUtils.toJson(convertToInputParams(method, url, headers, params, body)));
		RpcResult result = httpClientManager.doApiNodeWithRequestId(
				HttpClientManager.Method.valueOf(method.toUpperCase()), context.getRequestId(), url, headers, params,
				body, form, config.getTimeout());
		// Build result node
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeType(node.getType());
		nodeResult.setUsages(null);

		// Node execution result
		if (result != null && result.isSuccess() && result.getCode() == 200) {
			String outputType = config.getOutputType() == null ? "json" : config.getOutputType();
			Map<String, Object> outputParamsMap;
			Object resultObj;
			if (JsonUtils.isValidJson((String) result.getResponse())) {
				String response = (String) result.getResponse();
				if (response.trim().startsWith("[")) {
					resultObj = "primitive".equals(outputType)
							? decorateOutput(JsonUtils.fromJsonToList(response, Object.class))
							: JsonUtils.fromJsonToList(response, Object.class);
				}
				else {
					resultObj = "primitive".equals(outputType) ? decorateOutput(JsonUtils.fromJsonToMap(response))
							: JsonUtils.fromJsonToMap(response);
				}
			}
			else {
				resultObj = "primitive".equals(outputType) ? decorateOutput(result.getResponse())
						: result.getResponse();
			}
			outputParamsMap = constructOutputParamsMap(node, resultObj, context);
			nodeResult.setOutput(JsonUtils.toJson(outputParamsMap));
		}
		else {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setOutput(null);
			log.error("invoke api failed, requestId={},flowConfig={},result={}", context.getRequestId(),
					node.getConfig(), JsonUtils.toJson(result));
			if (result != null) {
				nodeResult.setErrorInfo(
						"api node exception info:" + result.getMessage() + ",response:" + result.getResponse());
				nodeResult.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR
					.toError("api node exception info:" + result.getMessage() + ",response:" + result.getResponse()));
			}
			else {
				nodeResult.setErrorInfo("api node exception info: response is null");
				nodeResult
					.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("api node exception info: response is null"));
			}
		}
		return nodeResult;
	}

	/**
	 * Handles API authorization configuration
	 * @param config Node configuration
	 * @param headers Request headers
	 * @param params Query parameters
	 * @param context Workflow context
	 */
	private void handleAuthorization(NodeParam config, Map<String, Object> headers, List<Map<String, Object>> params,
			WorkflowContext context) {
		Authorization authorization = config.getAuthorization();
		if (authorization != null) {
			String authType = StringUtils.isBlank(authorization.getAuthType())
					? Authorization.AuthTypeEnum.NoAuth.name() : authorization.getAuthType();
			if (Authorization.AuthTypeEnum.BearerAuth.name().equals(authType)) {
				Object value = authorization.getAuthConfig().getValue();
				if (value != null && value instanceof String) {
					String token = replaceTemplateContent((String) value, context);
					headers.put("Authorization", "Bearer " + token);
				}
				else {
					headers.put("Authorization", "Bearer " + null);
				}
			}
			else if (Authorization.AuthTypeEnum.ApiKeyAuth.name().equals(authType)) {
				String addTo = StringUtils.isBlank(authorization.getAuthConfig().getAddTo())
						? Authorization.AddToEnum.Header.name() : authorization.getAuthConfig().getAddTo();
				if (addTo.equals(Authorization.AddToEnum.Header.name())) {
					Object value = authorization.getAuthConfig().getValue();
					if (value != null && value instanceof String) {
						String token = replaceTemplateContent((String) value, context);
						headers.put(authorization.getAuthConfig().getKey(), token);
					}
					else {
						headers.put(authorization.getAuthConfig().getKey(), null);
					}
				}
				else if (addTo.equals(Authorization.AddToEnum.QueryParams.name())) {
					Object value = authorization.getAuthConfig().getValue();
					String token = null;
					if (value != null && value instanceof String) {
						token = replaceTemplateContent((String) value, context);
					}
					Map<String, Object> paramNew = new HashMap<>();
					paramNew.put("key", authorization.getAuthConfig().getKey());
					paramNew.put("value", token);
					params.add(paramNew);
				}
			}
		}
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.API.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.API.getDesc();
	}

	/**
	 * Builds request parameters from input configuration
	 * @param paramsArray List of parameter configurations
	 * @param context Workflow context
	 * @return List of parameter maps
	 */
	private List<Map<String, Object>> buildParams(List<Node.InputParam> paramsArray, WorkflowContext context) {
		if (Objects.isNull(paramsArray)) {
			return null;
		}
		List<Map<String, Object>> params = new ArrayList<>();
		paramsArray.stream().forEach(param -> {
			Object valueFromRequestContext = VariableUtils.getValueStringFromContext(param, context);
			Map<String, Object> paramNew = new HashMap<>();
			paramNew.put("key", param.getKey());
			paramNew.put("value", valueFromRequestContext);
			params.add(paramNew);
		});
		return params;
	}

	/**
	 * Builds form parameters from request body configuration
	 * @param body Request body configuration
	 * @param context Workflow context
	 * @return Map of form parameters
	 */
	private Map<String, Object> buildBodyFormParams(Map<String, Object> body, WorkflowContext context) {
		if (Objects.isNull(body)) {
			return null;
		}
		String type = MapUtils.getString(body, "type");
		Map<String, Object> params = Maps.newHashMap();
		if ("form-data".equalsIgnoreCase(type)) {
			Object o = body.get("data");
			List<Node.InputParam> data = JsonUtils.fromJsonToList(JsonUtils.toJson(o), Node.InputParam.class);
			data.forEach(param -> {
				Object valueFromRequestContext = VariableUtils.getValueStringFromContext(param, context);
				params.put(param.getKey(), valueFromRequestContext);
			});
		}
		else if ("json".equalsIgnoreCase(type)) {
			String data = MapUtils.getString(body, "data");
			Set<String> keys = VariableUtils.identifyVariableSetFromText(data);
			Map<String, Object> map = constructBodyParamsMap(keys, context);
			data = constructData(keys, map, data);
			boolean validJSON = JsonUtils.isValidJson(data);
			if (!validJSON) {
				log.info("log used for query jsonFormatData:{} ,requestID:{}", data, context.getRequestId());
				throw new BizException(
						ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("Invalid JSON format data, body:" + data));
			}
			params.put("json", data);

		}
		else if ("raw".equalsIgnoreCase(type)) {
			String data = MapUtils.getString(body, "data");
			Set<String> keys = VariableUtils.identifyVariableSetFromText(data);
			Map<String, Object> map = constructBodyParamsMap(keys, context);
			data = constructData(keys, map, data);
			params.put("raw", data);
		}

		return params;
	}

	/**
	 * Build variable value map from context
	 * @param keys
	 * @param context
	 * @return
	 */
	public Map<String, Object> constructBodyParamsMap(Set<String> keys, WorkflowContext context) {
		Map<String, Object> map = Maps.newHashMap();
		if (CollectionUtils.isEmpty(keys)) {
			return map;
		}
		ConcurrentHashMap<String, Object> variablesMap = context.getVariablesMap();
		keys.stream().forEach(str -> {

			if (str == null) {
				return;
			}

			Object finalValue = VariableUtils.getValueFromPayload(str, variablesMap);
			if (finalValue == null) {
				return;
			}
			map.put(str, finalValue);

		});
		return map;
	}

	/**
	 * Build body data
	 * @param keys
	 * @param localVariableMap
	 * @param data
	 * @return
	 */
	private String constructData(Set<String> keys, Map<String, Object> localVariableMap, String data) {
		for (String key : keys) {
			Object o = localVariableMap.get(key);
			key = key.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
			if (o == null) {
				// If there is no value for this variable, directly set the value to blank
				// for replacement
				data = data.replaceAll("\\$\\{" + key + "}", "");
			}
			else {
				if (o instanceof Map || o instanceof List) {
					data = data.replaceAll("\\$\\{" + key + "}", Matcher.quoteReplacement(JsonUtils.toJson(o)));
				}
				else {
					data = data.replaceAll("\\$\\{" + key + "}", Matcher.quoteReplacement("" + o));
				}
			}
		}
		return data;
	}

	/**
	 * Builds request headers from header configuration
	 * @param headersArray List of header configurations
	 * @param context Workflow context
	 * @return Map of headers
	 */
	private Map<String, Object> buildHeaders(List<Node.InputParam> headersArray, WorkflowContext context) {
		if (Objects.isNull(headersArray)) {
			return null;
		}
		Map<String, Object> headers = Maps.newHashMap();
		headersArray.stream().forEach(header -> {
			Object valueFromRequestContext = VariableUtils.getValueStringFromContext(header, context);
			headers.put(header.getKey(), valueFromRequestContext);
		});
		return headers;
	}

	/**
	 * Configuration parameters for API node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("method")
		private String method;

		@JsonProperty("url")
		private String url;

		@JsonProperty("authorization")
		private Authorization authorization;

		@JsonProperty("headers")
		private List<Node.InputParam> headers;

		@JsonProperty("params")
		private List<Node.InputParam> params;

		@JsonProperty("body")
		private Map<String, Object> body;

		@JsonProperty("output_type")
		private String outputType;

		@JsonProperty("timeout")
		private TimeoutConfig timeout;

		// retry config when failed
		@JsonProperty("retry_config")
		private RetryConfig retryConfig;

		// catch exception then operation
		@JsonProperty("try_catch_config")
		private TryCatchConfig tryCatchConfig;

	}

	/**
	 * Authorization configuration for API node
	 */
	@Data
	public static class Authorization {

		/**
		 * @see AuthTypeEnum
		 */
		@JsonProperty("auth_type")
		private String authType;

		@JsonProperty("auth_config")
		private AuthConfig authConfig;

		/**
		 * Authentication configuration details
		 */
		@EqualsAndHashCode(callSuper = true)
		@Data
		public static class AuthConfig extends Node.InputParam {

			/**
			 * @see AddToEnum
			 */
			@JsonProperty("add_to")
			private String addTo;

		}

		/**
		 * Supported authentication types
		 */
		public enum AuthTypeEnum {

			NoAuth, ApiKeyAuth, BearerAuth

		}

		/**
		 * Supported authentication locations
		 */
		public enum AddToEnum {

			Header, QueryParams

		}

	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		CheckNodeParamResult checkParamsResult = checkInputParams(config.getParams());

		if (checkParamsResult != null && !checkParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(checkParamsResult.getErrorInfos());
		}

		CheckNodeParamResult checkHeaderResult = checkInputParams(config.getHeaders());
		if (checkHeaderResult != null && !checkHeaderResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(checkHeaderResult.getErrorInfos());
		}

		if (!"post".equalsIgnoreCase(config.getMethod()) && !"get".equalsIgnoreCase(config.getMethod())
				&& !"put".equalsIgnoreCase(config.getMethod()) && !"delete".equalsIgnoreCase(config.getMethod())
				&& !"patch".equalsIgnoreCase(config.getMethod())) {
			result.setSuccess(false);
			result.getErrorInfos().add("only POST/GET/PUT/DELETE/PATCH support");
		}

		return result;
	}

	private Map<String, Object> convertToInputParams(String method, String url, Map<String, Object> headerMap,
			List<Map<String, Object>> params, Map<String, Object> body) {
		Map<String, Object> inputJson = new HashMap<>();
		inputJson.put("method", method.toLowerCase());
		inputJson.put("url", url);
		inputJson.put("headers", headerMap);
		Map<String, Object> paramMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(params)) {
			for (Map<String, Object> param : params) {
				String key = MapUtils.getString(param, "key");
				Object value = param.get("value");
				paramMap.put(key, value);
			}
		}
		inputJson.put("params", paramMap);
		inputJson.put("body", body);
		return inputJson;
	}

}
