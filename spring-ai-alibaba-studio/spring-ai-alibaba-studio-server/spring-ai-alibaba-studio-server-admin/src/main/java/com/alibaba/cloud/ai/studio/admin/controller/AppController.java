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

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AppQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.processor.ExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.runtime.WorkflowExecuteManager;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * REST controller for managing agent applications. Handles CRUD operations, version
 * management, and application lifecycle.
 *
 * @since 1.0.0.3
 */

@RestController
@Tag(name = "app")
@RequestMapping("/console/v1/apps")
public class AppController {

	/** Service for application management */
	private final AppService appService;

	/** Manager for workflow execution */
	private final WorkflowExecuteManager workflowExecuteManager;

	public AppController(AppService appService, WorkflowExecuteManager workflowExecuteManager) {
		this.appService = appService;
		this.workflowExecuteManager = workflowExecuteManager;
	}

	/**
	 * Creates a new application
	 * @param app Application details
	 * @return Created application ID
	 */
	@PostMapping()
	public Result<String> createApp(@RequestBody Application app) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(app)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("app"));
		}

		if (StringUtils.isBlank(app.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		if (Objects.isNull(app.getConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("config"));
		}

		String appId = appService.createApp(app);
		return Result.success(context.getRequestId(), appId);
	}

	/**
	 * Updates an existing application
	 * @param appId Application ID
	 * @param app Updated application details
	 * @return Success result
	 */
	@PutMapping("/{appId}")
	public Result<String> updateApp(@PathVariable("appId") String appId, @RequestBody Application app) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("app id"));
		}

		if (Objects.isNull(app)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("app"));
		}

		if (StringUtils.isBlank(app.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		app.setAppId(appId);
		appService.updateApp(app);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes an application
	 * @param appId Application ID
	 * @return Success result
	 */
	@DeleteMapping("/{appId}")
	public Result<Void> deleteApp(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}

		appService.deleteApp(appId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves an application by ID
	 * @param appId Application ID
	 * @return Application details
	 */
	@GetMapping("/{appId}")
	public Result<Application> getApplication(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}

		Application app = appService.getApp(appId);
		return Result.success(context.getRequestId(), app);
	}

	/**
	 * Lists applications with pagination
	 * @param query Query parameters
	 * @return Paginated list of applications
	 */
	@GetMapping()
	public Result<PagingList<Application>> listApps(@ApiModelAttribute AppQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		PagingList<Application> apps = appService.listApps(query);
		return Result.success(context.getRequestId(), apps);
	}

	/**
	 * Publishes an application
	 * @param appId Application ID
	 * @return Success result
	 */
	@PostMapping("/{appId}/publish")
	public Result<Void> publishApp(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}
		Application app = appService.getApp(appId);
		if (app.getType() == AppType.WORKFLOW) {
			ApplicationVersion applicationVersion = appService.getAppVersion(appId, "latest");
			WorkflowConfig appOrchestraConfig = JsonUtils.fromJson(applicationVersion.getConfig(),
					WorkflowConfig.class);
			ExecuteProcessor.CheckFlowParamResult checkFlowParamResult = workflowExecuteManager
				.checkWorkflowConfig(appOrchestraConfig);
			if (!checkFlowParamResult.isSuccess()) {
				StringBuilder stringBuilder = new StringBuilder();
				checkFlowParamResult.getCheckNodeParamResults()
					.forEach(nodeParamResult -> stringBuilder.append("Node【")
						.append(nodeParamResult.getNodeName())
						.append("】")
						.append("have a configuration error：\n")
						.append(String.join(";\n", nodeParamResult.getErrorInfos()))
						.append("\n"));
				throw new BizException(ErrorCode.WORKFLOW_CONFIG_ILLEGAL.toError(stringBuilder.toString()));
			}
		}

		appService.publishApp(appId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Lists application versions
	 * @param appId Application ID
	 * @param query Query parameters
	 * @return Paginated list of versions
	 */
	@GetMapping("/{appId}/versions")
	public Result<PagingList<ApplicationVersion>> listAppVersions(@PathVariable("appId") String appId,
			@ApiModelAttribute AppQuery query) {

		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}

		query.setAppId(appId);
		PagingList<ApplicationVersion> appVersions = appService.listAppVersions(query);
		return Result.success(context.getRequestId(), appVersions);
	}

	/**
	 * Gets a specific application version
	 * @param appId Application ID
	 * @param version Version number
	 * @return Version details
	 */
	@GetMapping("/{appId}/versions/{version}")
	public Result<ApplicationVersion> getApplication(@PathVariable("appId") String appId,
			@PathVariable("version") String version) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId) || Objects.isNull(version)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId or version"));
		}

		ApplicationVersion appVersion = appService.getAppVersion(appId, version);
		return Result.success(context.getRequestId(), appVersion);
	}

	/**
	 * Creates a copy of an application
	 * @param appId Application ID
	 * @return ID of the new application
	 */
	@PostMapping("/{appId}/copy")
	public Result<String> copyApp(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}

		String newAppId = appService.copyApp(appId);
		return Result.success(context.getRequestId(), newAppId);
	}

}
