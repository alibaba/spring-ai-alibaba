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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ApiParameterLocation;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ApiParameter;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionResult;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.base.manager.HttpClientManager;
import com.alibaba.cloud.ai.studio.core.base.domain.RpcResult;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the tool execution service. Handles the execution of tools and API
 * calls based on tool configurations.
 *
 * @since 1.0.0.3
 */
@Service
public class ToolExecutionServiceImpl implements ToolExecutionService {

	/** Service for managing plugins */
	private final PluginService pluginService;

	/** Manager for handling HTTP client operations */
	private final HttpClientManager httpClientManager;

	public ToolExecutionServiceImpl(PluginService pluginService, HttpClientManager httpClientManager) {
		this.pluginService = pluginService;
		this.httpClientManager = httpClientManager;
	}

	/**
	 * Executes a tool based on the provided request. Validates the request and retrieves
	 * necessary tool and plugin information.
	 * @param request The tool execution request containing arguments and tool information
	 * @return The result of the tool execution
	 */
	public ToolExecutionResult executeTool(ToolExecutionRequest request) {
		if (request == null) {
			throw new BizException(ErrorCode.TOOL_PARAMS_MISSING.toError("execution_request"));
		}

		Map<String, Object> inputParams = request.getArguments();
		if (inputParams == null) {
			throw new BizException(ErrorCode.TOOL_PARAMS_MISSING.toError("arguments"));
		}

		if (request.getTool() == null) {
			if (Objects.isNull(request.getToolId())) {
				throw new BizException(ErrorCode.TOOL_PARAMS_MISSING.toError("tool_id"));
			}

			Tool tool = pluginService.getTool(request.getToolId());
			request.setTool(tool);

			if (tool.getPlugin() == null) {
				if (Objects.isNull(request.getPluginId())) {
					throw new BizException(ErrorCode.TOOL_PARAMS_MISSING.toError("plugin_id"));
				}

				Plugin plugin = pluginService.getPlugin(request.getPluginId());
				tool.setPlugin(plugin);
			}
		}

		return callOpenApi(request);
	}

	/**
	 * Makes an API call based on the tool configuration. Handles authentication,
	 * parameter mapping, and HTTP request execution.
	 * @param request The tool execution request
	 * @return The result of the API call
	 */
	public ToolExecutionResult callOpenApi(ToolExecutionRequest request) {
		try {
			this.validateInputs(request);

			Plugin.PluginConfig pluginConfig = request.getTool().getPlugin().getConfig();
			Tool.ToolConfig toolConfig = request.getTool().getConfig();

			String server = pluginConfig.getServer();
			StringBuilder requestUrl = new StringBuilder(String.format("%s%s", server, toolConfig.getPath()));
			String method = toolConfig.getRequestMethod();

			Map<String, Object> headers = new HashMap<>();
			Map<String, Object> queryParameters = new HashMap<>();

			// populate plugin common headers
			if (!CollectionUtils.isEmpty(pluginConfig.getHeaders())) {
				headers.putAll(pluginConfig.getHeaders());
			}

			// populate auth info to either header or query
			if (pluginConfig.getAuth() != null && pluginConfig.getAuth().getType() != Plugin.ApiAuthType.NONE) {
				Plugin.ApiAuth auth = pluginConfig.getAuth();
				Plugin.AuthorizationType authType = auth.getAuthorizationType();
				if (authType == Plugin.AuthorizationType.BEARER) {
					headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + auth.getAuthorizationValue());
				}
				else if (authType == Plugin.AuthorizationType.BASIC) {
					headers.put(HttpHeaders.AUTHORIZATION, "Basic " + auth.getAuthorizationValue());
				}
				else if (authType == Plugin.AuthorizationType.CUSTOM) {
					if (auth.getAuthorizationPosition() == Plugin.AuthorizationPosition.HEADER) {
						headers.put(auth.getAuthorizationKey(), auth.getAuthorizationValue());
					}
					else if (auth.getAuthorizationPosition() == Plugin.AuthorizationPosition.QUERY) {
						queryParameters.put(auth.getAuthorizationKey(), auth.getAuthorizationValue());
					}
					else {
						throw new BizException(ErrorCode.TOOL_PARAMS_INVALID.toError("authorization_position",
								"position must be header or query"));
					}
				}
				else {
					throw new BizException(ErrorCode.TOOL_PARAMS_INVALID.toError("authorization_type",
							"type must be basic, bearer or custom"));
				}
			}

			// populate input params to header, path, query or body
			List<ApiParameter> apiParameters = toolConfig.getInputParams();
			Map<String, Object> paramValues = request.getArguments();
			for (ApiParameter apiParameter : apiParameters) {
				ApiParameterLocation location = ApiParameterLocation
					.of(StringUtils.lowerCase(apiParameter.getLocation()));
				if (location == null) {
					continue;
				}

				String key = apiParameter.getKey();
				if (location == ApiParameterLocation.HEADER) {
					headers.put(key, paramValues.get(key));
					paramValues.remove(key);
				}
				else if (location == ApiParameterLocation.QUERY) {
					queryParameters.put(key, paramValues.get(key));
					paramValues.remove(key);
				}
				else if (location == ApiParameterLocation.PATH) {
					requestUrl.append("/").append(paramValues.get(key));
					paramValues.remove(key);
				}
			}

			RpcResult result;
			Map<String, Object> parameters = request.getArguments();
			if (method.equalsIgnoreCase(HttpClientManager.Method.GET.getValue())) {
				parameters.putAll(queryParameters);

				result = httpClientManager.doGetWithRequestId(request.getRequestId(), requestUrl.toString(), headers,
						parameters);
			}
			else if (method.equalsIgnoreCase(HttpClientManager.Method.POST.getValue())) {
				String encodedParams = HttpClientManager.encodingParams(queryParameters,
						StandardCharsets.UTF_8.toString());
				if (StringUtils.isNotBlank(encodedParams)) {
					if (requestUrl.toString().contains("?")) {
						requestUrl.append("&").append(encodedParams);
					}
					else {
						requestUrl.append("?").append(encodedParams);
					}
				}

				String contentType = toolConfig.getContentType();
				if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType)) {
					result = httpClientManager.doPostFormWithRequestId(request.getRequestId(), requestUrl.toString(),
							headers, parameters);
				}
				else if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(contentType)) {
					result = httpClientManager.doPostJsonWithRequestId(request.getRequestId(), requestUrl.toString(),
							headers, parameters);
				}
				else {
					throw new BizException(ErrorCode.TOOL_PARAMS_INVALID.toError("contentType",
							"contentType " + contentType + " not supported"));
				}
			}
			else {
				throw new BizException(ErrorCode.TOOL_PARAMS_INVALID.toError("method", "method must be get or post"));
			}

			if (!result.isSuccess()) {
				LogUtils.error("call openapi failed, request: {}, result: {}", JsonUtils.toJson(request),
						JsonUtils.toJson(result));

				return ToolExecutionResult.builder()
					.success(false)
					.error(Error.builder().statusCode(result.getCode()).message(result.getMessage()).build())
					.build();

			}

			// Map<String, Object> outputs =
			// buildOutputs(JsonUtils.fromJsonToMap(result.getResponse().toString()),
			// toolConfig.getOutputParams());
			String output = result.getResponse().toString();
			return ToolExecutionResult.builder().success(true).output(output).build();
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.TOOL_EXECUTION_ERROR.toError(e.getMessage()), e);
		}
	}

	/**
	 * Validates the input parameters against the tool's configuration. Checks for
	 * required parameters and their types.
	 * @param request The tool execution request to validate
	 */
	public void validateInputs(ToolExecutionRequest request) {
		List<ApiParameter> inputParams = request.getTool().getConfig().getInputParams();
		Map<String, Object> paramValues = request.getArguments();
		if (!CollectionUtils.isEmpty(inputParams)) {
			Map<String, ApiParameter> parameterMap = inputParams.stream()
				.filter(param -> StringUtils.isNotBlank(param.getKey()))
				.collect(Collectors.toMap(ApiParameter::getKey, Function.identity()));
			for (Map.Entry<String, ApiParameter> entry : parameterMap.entrySet()) {
				Object paramValue = paramValues.get(entry.getKey());
				if (entry.getValue().isRequired() && paramValue == null) {
					throw new BizException(ErrorCode.TOOL_PARAMS_MISSING.toError(entry.getKey()));
				}

				String paramType = entry.getValue().getType();
				ParameterType apiParamType = ParameterType.of(paramType);
				if (apiParamType == null) {
					throw new BizException(
							ErrorCode.TOOL_PARAMS_INVALID.toError(entry.getKey(), "param type not supported"));
				}

				if (paramValue != null && !apiParamType.isValidType(paramValue)) {
					throw new BizException(
							ErrorCode.TOOL_PARAMS_INVALID.toError(entry.getKey(), "param type and value not match"));
				}
			}
		}
	}

	/**
	 * Builds the output map from the API response.
	 * @param respJSONObj The JSON response object
	 * @param outputParams The expected output parameters
	 * @return Map containing the processed output values
	 */
	public Map<String, Object> buildOutputs(Map<String, Object> respJSONObj, List<ApiParameter> outputParams) {
		Map<String, Object> outputs = new HashMap<String, Object>();

		try {
			outputs = respJSONObj;
			// constructOutputs(respJSONObj, outputs, outputParams);
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.BUILD_TOOL_RESULT_ERROR.toError(), e);
		}

		return outputs;
	}

	/**
	 * Recursively constructs output objects based on parameter types. Handles different
	 * data types including objects and arrays.
	 * @param sourceObject The source data object
	 * @param targetObj The target object to populate
	 * @param outputParams The output parameter definitions
	 */
	private void constructOutputs(Map<String, Object> sourceObject, Map<String, Object> targetObj,
			List<ApiParameter> outputParams) {
		if (CollectionUtils.isEmpty(outputParams)) {
			return;
		}

		for (ApiParameter apiParam : outputParams) {
			String key = apiParam.getKey();
			ParameterType type = ParameterType.of(apiParam.getType());
			if (type == null) {
				continue;
			}

			switch (type) {
				case STRING: {
					targetObj.put(key, sourceObject.get(key));
					break;
				}

				case NUMBER: {
					targetObj.put(key, sourceObject.get(key));
					break;
				}

				case BOOLEAN: {
					targetObj.put(key, sourceObject.get(key));
					break;
				}

				case OBJECT: {
					Map<String, Object> jsonObject;
					if (!targetObj.containsKey(key)) {
						jsonObject = new HashMap<>();
						targetObj.put(key, jsonObject);
					}
					else {
						jsonObject = (Map<String, Object>) targetObj.get(key);
					}

					constructOutputs((Map<String, Object>) sourceObject.get(key), jsonObject, apiParam.getProperties());
					targetObj.put(key, jsonObject);
					break;
				}

				case ARRAY_STRING, ARRAY_NUMBER, ARRAY_BOOLEAN: {
					targetObj.put(key, targetObj.get(key));
					break;
				}

				case ARRAY_OBJECT: {
					List<Object> jsonArray;
					if (!targetObj.containsKey(key)) {
						jsonArray = new ArrayList<>();
						targetObj.put(key, jsonArray);
					}
					else {
						jsonArray = (List<Object>) targetObj.get(key);
					}

					List<Object> sourceJsonArray = (List<Object>) sourceObject.get(key);
					for (int i = 0; i < sourceJsonArray.size(); i++) {
						Map<String, Object> subSourceObj = (Map<String, Object>) sourceJsonArray.get(i);
						Map<String, Object> subTargetObj = new HashMap<>();
						jsonArray.add(subTargetObj);

						constructOutputs(subSourceObj, subTargetObj, apiParam.getProperties());
					}

					targetObj.put(key, jsonArray);
					break;
				}
			}
		}
	}

}
