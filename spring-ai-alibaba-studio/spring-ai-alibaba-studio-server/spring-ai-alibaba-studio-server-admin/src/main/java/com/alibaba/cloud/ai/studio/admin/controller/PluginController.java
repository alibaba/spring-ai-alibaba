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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionResult;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolQuery;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolTestStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controller for managing plugins and tools. Provides REST APIs for plugin and tool CRUD
 * operations, testing, and publishing.
 *
 * @since 1.0.0.3
 */

@RestController
@Tag(name = "plugin")
@RequestMapping("/console/v1")
public class PluginController {

	/** Supported HTTP methods for tool requests */
	private static final List<String> SUPPORT_METHOD = Arrays.asList("post", "get");

	/** Service for plugin management operations */
	private final PluginService pluginService;

	/** Service for tool execution operations */
	private final ToolExecutionService toolExecutionService;

	public PluginController(PluginService pluginService, ToolExecutionService toolExecutionService) {
		this.pluginService = pluginService;
		this.toolExecutionService = toolExecutionService;
	}

	/**
	 * Creates a new plugin
	 * @param request Plugin creation request
	 * @return Plugin ID
	 */
	@PostMapping("/plugins")
	public Result<String> createPlugin(@RequestBody Plugin request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		validatePlugin(request);

		String id = pluginService.createPlugin(request);
		return Result.success(context.getRequestId(), id);
	}

	/**
	 * Updates an existing plugin
	 * @param pluginId ID of the plugin to update
	 * @param request Plugin update request
	 */
	@PutMapping("/plugins/{pluginId}")
	public Result<Void> updatePlugin(@PathVariable("pluginId") String pluginId, @RequestBody Plugin request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		validatePlugin(request);

		request.setPluginId(pluginId);
		pluginService.updatePlugin(request);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a plugin
	 * @param pluginId ID of the plugin to delete
	 */
	@DeleteMapping("/plugins/{pluginId}")
	public Result<Void> deletePlugin(@PathVariable("pluginId") String pluginId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		pluginService.deletePlugin(pluginId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a plugin by ID
	 * @param pluginId ID of the plugin to retrieve
	 * @return Plugin details
	 */
	@GetMapping("/plugins/{pluginId}")
	public Result<Plugin> getPlugin(@PathVariable("pluginId") String pluginId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		Plugin plugin = pluginService.getPlugin(pluginId);
		return Result.success(context.getRequestId(), plugin);
	}

	/**
	 * Lists plugins with pagination
	 * @param request Query parameters
	 * @return Paginated list of plugins
	 */
	@GetMapping("/plugins")
	public Result<PagingList<Plugin>> listPlugins(@ModelAttribute BaseQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		PagingList<Plugin> plugins = pluginService.listPlugins(request);
		return Result.success(context.getRequestId(), plugins);
	}

	/**
	 * Creates a new tool for a plugin
	 * @param pluginId ID of the parent plugin
	 * @param tool Tool creation request
	 * @return Tool ID
	 */
	@PostMapping("/plugins/{pluginId}/tools")
	public Result<String> createTool(@PathVariable("pluginId") String pluginId, @RequestBody Tool tool) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}
		validateTool(tool);

		tool.setPluginId(pluginId);
		String id = pluginService.createTool(tool);
		return Result.success(context.getRequestId(), id);
	}

	/**
	 * Updates an existing tool
	 * @param pluginId ID of the parent plugin
	 * @param toolId ID of the tool to update
	 * @param tool Tool update request
	 */
	@PutMapping("/plugins/{pluginId}/tools/{toolId}")
	public Result<String> updateTool(@PathVariable("pluginId") String pluginId, @PathVariable("toolId") String toolId,
			@RequestBody Tool tool) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		validateTool(tool);

		tool.setToolId(toolId);
		tool.setPluginId(pluginId);
		pluginService.updateTool(tool);

		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a tool
	 * @param pluginId ID of the parent plugin
	 * @param toolId ID of the tool to delete
	 */
	@DeleteMapping("/plugins/{pluginId}/tools/{toolId}")
	public Result<Void> deleteTool(@PathVariable("pluginId") String pluginId, @PathVariable("toolId") String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		pluginService.deleteTool(toolId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a tool by ID
	 * @param pluginId ID of the parent plugin
	 * @param toolId ID of the tool to retrieve
	 * @return Tool details
	 */
	@GetMapping("/plugins/{pluginId}/tools/{toolId}")
	public Result<Tool> getTool(@PathVariable("pluginId") String pluginId, @PathVariable("toolId") String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		Tool tool = pluginService.getTool(toolId);
		return Result.success(context.getRequestId(), tool);
	}

	/**
	 * Lists tools for a plugin with pagination
	 * @param pluginId ID of the parent plugin
	 * @param query Query parameters
	 * @return Paginated list of tools
	 */
	@GetMapping("/plugins/{pluginId}/tools")
	public Result<PagingList<Tool>> listTools(@PathVariable("pluginId") String pluginId,
			@ModelAttribute ToolQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		query.setPluginId(pluginId);
		PagingList<Tool> tools = pluginService.listTools(query);
		return Result.success(context.getRequestId(), tools);
	}

	/**
	 * Enables a tool
	 * @param toolId ID of the tool to enable
	 */
	@PostMapping("/tools/{toolId}/enable")
	public Result<Void> enableTool(@PathVariable("toolId") String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		pluginService.updateEnableStatus(toolId, true);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Disables a tool
	 * @param toolId ID of the tool to disable
	 */
	@PostMapping("/tools/{toolId}/disable")
	public Result<Void> disableTool(@PathVariable("toolId") String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		pluginService.updateEnableStatus(toolId, false);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Tests a tool execution
	 * @param pluginId ID of the parent plugin
	 * @param toolId ID of the tool to test
	 * @param request Tool execution request
	 * @return Tool execution result
	 */
	@PostMapping("/plugins/{pluginId}/tools/{toolId}/test")
	public Result<ToolExecutionResult> testTool(@PathVariable("pluginId") String pluginId,
			@PathVariable("toolId") String toolId, @RequestBody ToolExecutionRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		request.setPluginId(pluginId);
		request.setToolId(toolId);
		ToolExecutionResult result = toolExecutionService.executeTool(request);

		ToolTestStatus testStatus = result.isSuccess() ? ToolTestStatus.PASSED : ToolTestStatus.FAILED;
		pluginService.updateTestStatus(toolId, testStatus);

		return Result.success(context.getRequestId(), result);
	}

	/**
	 * Publishes a tool
	 * @param pluginId ID of the parent plugin
	 * @param toolId ID of the tool to publish
	 */
	@PostMapping("/plugins/{pluginId}/tools/{toolId}/publish")
	public Result<Void> publishTool(@PathVariable("pluginId") String pluginId, @PathVariable("toolId") String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(pluginId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("pluginId"));
		}

		if (Objects.isNull(toolId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolId"));
		}

		pluginService.publishTool(toolId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Queries tools by their IDs
	 * @param query Query containing tool IDs
	 * @return List of tools
	 */
	@PostMapping("/tools/query-by-ids")
	public Result<List<Tool>> queryToolsByIds(@RequestBody ToolQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query.getToolIds()) || query.getToolIds().isEmpty()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("toolIds"));
		}

		List<Tool> tools = pluginService.getTools(query.getToolIds());
		return Result.success(context.getRequestId(), tools);
	}

	/**
	 * Validates plugin creation/update request
	 * @param plugin Plugin to validate
	 */
	private void validatePlugin(Plugin plugin) {
		if (Objects.isNull(plugin) || Objects.isNull(plugin.getConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("plugin, config"));
		}

		if (StringUtils.isBlank(plugin.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		Plugin.PluginConfig pluginConfig = plugin.getConfig();
		if (StringUtils.isBlank(pluginConfig.getServer())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("server"));
		}

		if (pluginConfig.getAuth() == null || pluginConfig.getAuth().getType() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("auth_type"));
		}

		Plugin.ApiAuth apiAuth = pluginConfig.getAuth();
		if (apiAuth.getType() != Plugin.ApiAuthType.NONE) {
			if (apiAuth.getAuthorizationType() == null) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("authorization_type"));
			}

			if (apiAuth.getAuthorizationType() == Plugin.AuthorizationType.CUSTOM) {
				if (apiAuth.getAuthorizationPosition() == null) {
					throw new BizException(ErrorCode.MISSING_PARAMS.toError("authorization_position"));
				}

				if (apiAuth.getAuthorizationKey() == null) {
					throw new BizException(ErrorCode.MISSING_PARAMS.toError("authorization_key"));
				}
			}

			if (apiAuth.getAuthorizationValue() == null) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("authorization_value"));
			}
		}
	}

	/**
	 * Validates tool creation/update request
	 * @param tool Tool to validate
	 */
	private void validateTool(Tool tool) {
		if (Objects.isNull(tool) || Objects.isNull(tool.getConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("tool"));
		}

		String name = tool.getName();
		String description = tool.getDescription();
		if (StringUtils.isBlank(name)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		if (StringUtils.isBlank(description)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("description"));
		}

		String path = tool.getConfig().getPath();
		if (StringUtils.isBlank(path)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("path"));
		}
		if (!path.startsWith("/")) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("path", "start with /"));
		}

		String requestMethod = tool.getConfig().getRequestMethod();
		requestMethod = StringUtils.lowerCase(requestMethod);
		if (StringUtils.isBlank(requestMethod)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("requestMethod"));
		}

		if (!SUPPORT_METHOD.contains(requestMethod)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("requestMethod", "method not supported"));
		}

		if ("post".equalsIgnoreCase(requestMethod)) {
			String contentType = tool.getConfig().getContentType();
			if (contentType == null) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("contentType"));
			}

			if (!MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(contentType)
					&& !MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType)) {
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("contentType", "contentType not supported"));
			}
		}
	}

}
