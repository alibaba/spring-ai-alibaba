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
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Workspace;
import com.alibaba.cloud.ai.studio.core.base.service.WorkspaceService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Controller for managing workspaces. Provides CRUD operations for workspace resources.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "workspace")
@RequestMapping("/console/v1/workspaces")
public class WorkspaceController {

	/** Service for workspace operations */
	private final WorkspaceService workspaceService;

	public WorkspaceController(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	/**
	 * Creates a new workspace
	 * @param workspace Workspace information
	 * @return Result containing the created workspace ID
	 */
	@PostMapping()
	public Result<String> createWorkspace(@RequestBody Workspace workspace) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(workspace)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace"));
		}

		if (StringUtils.isBlank(workspace.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		String workspaceId = workspaceService.createWorkspace(workspace);
		return Result.success(context.getRequestId(), workspaceId);
	}

	/**
	 * Updates an existing workspace
	 * @param workspaceId ID of the workspace to update
	 * @param workspace Updated workspace information
	 * @return Result indicating success
	 */
	@PutMapping("/{workspaceId}")
	public Result<String> updateWorkspace(@PathVariable("workspaceId") String workspaceId,
			@RequestBody Workspace workspace) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(workspace)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace"));
		}

		if (Objects.isNull(workspaceId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspaceId"));
		}

		if (StringUtils.isBlank(workspace.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		workspace.setWorkspaceId(workspaceId);
		workspaceService.updateWorkspace(workspace);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a workspace
	 * @param workspaceId ID of the workspace to delete
	 * @return Result indicating success
	 */
	@DeleteMapping("/{workspaceId}")
	public Result<Void> deleteWorkspace(@PathVariable("workspaceId") String workspaceId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(workspaceId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspaceId"));
		}

		workspaceService.deleteWorkspace(workspaceId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a specific workspace
	 * @param workspaceId ID of the workspace to retrieve
	 * @return Result containing the workspace information
	 */
	@GetMapping("/{workspaceId}")
	public Result<Workspace> getWorkspace(@PathVariable("workspaceId") String workspaceId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(workspaceId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspaceId"));
		}

		Workspace workspace = workspaceService.getWorkspace(workspaceId);
		return Result.success(context.getRequestId(), workspace);
	}

	/**
	 * Lists workspaces with pagination
	 * @param query Query parameters for filtering and pagination
	 * @return Result containing a paginated list of workspaces
	 */
	@GetMapping()
	public Result<PagingList<Workspace>> listWorkspaces(@ModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(query)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		PagingList<Workspace> workspaces = workspaceService.listWorkspaces(query);
		return Result.success(context.getRequestId(), workspaces);
	}

}
