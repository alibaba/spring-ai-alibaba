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
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Workspace;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.WorkspaceService;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.WorkspaceEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.WorkspaceMapper;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_EMPTY_ID;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_WORKSPACE_UID_PREFIX;

/**
 * Implementation of workspace service that handles workspace CRUD operations. Provides
 * functionality for creating, updating, deleting, and querying workspaces.
 *
 * @since 1.0.0.3
 */
@Service
public class WorkspaceServiceImpl extends ServiceImpl<WorkspaceMapper, WorkspaceEntity> implements WorkspaceService {

	/** Maximum number of workspaces allowed per account */
	private static final int MAX_WORKSPACE_PER_ACCOUNT = 10;

	/** Mapper for workspace database operations */
	private final WorkspaceMapper workspaceMapper;

	/** Manager for Redis cache operations */
	private final RedisManager redisManager;

	public WorkspaceServiceImpl(WorkspaceMapper workspaceMapper, RedisManager redisManager) {
		this.workspaceMapper = workspaceMapper;
		this.redisManager = redisManager;
	}

	/**
	 * Creates a new workspace
	 * @param workspace Workspace information
	 * @return ID of the created workspace
	 */
	@Override
	public String createWorkspace(Workspace workspace) {
		if (StringUtils.isBlank(workspace.getAccountId())) {
			RequestContext context = RequestContextHolder.getRequestContext();
			workspace.setAccountId(context.getAccountId());
		}

		// check if workspace name exists
		WorkspaceEntity entity = getWorkspaceByName(workspace.getAccountId(), workspace.getName());
		if (entity != null) {
			throw new BizException(ErrorCode.WORKSPACE_NAME_EXISTS.toError());
		}

		long workspaceCount = getWorkspaceCount(workspace.getAccountId());
		if (workspaceCount > MAX_WORKSPACE_PER_ACCOUNT) {
			throw new BizException(ErrorCode.INVALID_REQUEST
				.toError("workspace can not be more than " + MAX_WORKSPACE_PER_ACCOUNT + "."));
		}

		String workspaceId = IdGenerator.idStr();
		entity = BeanCopierUtils.copy(workspace, WorkspaceEntity.class);
		entity.setWorkspaceId(workspaceId);
		entity.setAccountId(workspace.getAccountId());
		entity.setGmtCreate(new Date());
		entity.setGmtModified(new Date());
		entity.setCreator(workspace.getAccountId());
		entity.setModifier(workspace.getAccountId());

		this.save(entity);

		// cache it
		String key = getWorkspaceCacheKey(workspace.getAccountId(), entity.getWorkspaceId());
		redisManager.put(key, entity);

		return workspaceId;
	}

	/**
	 * Updates an existing workspace
	 * @param workspace Updated workspace information
	 */
	@Override
	public void updateWorkspace(Workspace workspace) {
		RequestContext context = RequestContextHolder.getRequestContext();

		WorkspaceEntity entity = getWorkspaceById(context.getAccountId(), workspace.getWorkspaceId());
		if (entity == null) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		// check if workspace name exists
		WorkspaceEntity workspaceEntity = getWorkspaceByName(context.getAccountId(), workspace.getName());
		if (workspaceEntity != null && !workspaceEntity.getId().equals(entity.getId())) {
			throw new BizException(ErrorCode.WORKSPACE_NAME_EXISTS.toError());
		}

		entity.setName(workspace.getName());
		entity.setDescription(workspace.getDescription());
		entity.setConfig(JsonUtils.toJson(workspace.getConfig()));
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());

		this.updateById(entity);

		// cache it
		String key = getWorkspaceCacheKey(context.getAccountId(), workspace.getWorkspaceId());
		redisManager.put(key, entity);
	}

	/**
	 * Deletes a workspace by its ID
	 * @param workspaceId ID of the workspace to delete
	 */
	@Override
	public void deleteWorkspace(String workspaceId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		// delete from db
		WorkspaceEntity entity = getWorkspaceById(context.getAccountId(), workspaceId);
		if (entity == null) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		this.removeById(entity);

		// delete from cache
		String cacheKey = getWorkspaceCacheKey(context.getAccountId(), workspaceId);
		redisManager.delete(cacheKey);
	}

	/**
	 * Retrieves a workspace by its ID
	 * @param workspaceId ID of the workspace to retrieve
	 * @return Workspace information
	 */
	@Override
	public Workspace getWorkspace(String workspaceId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		String key = getWorkspaceCacheKey(context.getAccountId(), workspaceId);
		WorkspaceEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return toWorkspaceDTO(entity);
		}

		entity = getWorkspaceById(context.getAccountId(), workspaceId);
		if (entity == null) {
			entity = new WorkspaceEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		redisManager.put(key, entity);
		return toWorkspaceDTO(entity);
	}

	/**
	 * Lists workspaces with pagination
	 * @param query Query parameters including pagination info
	 * @return Paginated list of workspaces
	 */
	@Override
	public PagingList<Workspace> listWorkspaces(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		LambdaQueryWrapper<WorkspaceEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(WorkspaceEntity::getAccountId, context.getAccountId())
			.ne(WorkspaceEntity::getStatus, CommonStatus.DELETED.getValue());
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(WorkspaceEntity::getName, query.getName());
		}
		queryWrapper.orderByDesc(WorkspaceEntity::getId);

		Page<WorkspaceEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<WorkspaceEntity> pageResult = this.page(page, queryWrapper);

		List<Workspace> workspaces;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			workspaces = new ArrayList<>();
		}
		else {
			workspaces = pageResult.getRecords().stream().map(this::toWorkspaceDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), workspaces);
	}

	/**
	 * Retrieves a workspace by account ID and workspace ID
	 * @param uid Account ID
	 * @param workspaceId Workspace ID
	 * @return Workspace information
	 */
	@Override
	public Workspace getWorkspace(String uid, String workspaceId) {
		WorkspaceEntity entity = getWorkspaceById(uid, workspaceId);
		if (entity == null) {
			throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND.toError());
		}

		return toWorkspaceDTO(entity);
	}

	@Override
	public Workspace getDefaultWorkspace(String uid) {
		String key = getWorkspaceCacheKey(uid, "-");
		WorkspaceEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return toWorkspaceDTO(entity);
		}

		LambdaQueryWrapper<WorkspaceEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(WorkspaceEntity::getAccountId, uid)
			.ne(WorkspaceEntity::getStatus, CommonStatus.DELETED.getValue())
			.orderByAsc(WorkspaceEntity::getId)
			.last("limit 1");

		Optional<WorkspaceEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new WorkspaceEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		entity = entityOptional.get();
		redisManager.put(key, entity);

		return toWorkspaceDTO(entity);
	}

	/**
	 * Generates cache key for workspace
	 * @param accountId Account ID
	 * @param workspaceId Workspace ID
	 * @return Cache key string
	 */
	public static String getWorkspaceCacheKey(String accountId, String workspaceId) {
		return String.format(CACHE_WORKSPACE_UID_PREFIX, accountId, workspaceId);
	}

	/**
	 * Retrieves workspace entity by ID from cache or database
	 * @param accountId Account ID
	 * @param workspaceId Workspace ID
	 * @return Workspace entity
	 */
	private WorkspaceEntity getWorkspaceById(String accountId, String workspaceId) {
		String key = getWorkspaceCacheKey(accountId, workspaceId);
		WorkspaceEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<WorkspaceEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(WorkspaceEntity::getWorkspaceId, workspaceId)
			.eq(WorkspaceEntity::getAccountId, accountId)
			.ne(WorkspaceEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<WorkspaceEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new WorkspaceEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		entity = entityOptional.get();
		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Retrieves workspace entity by name
	 * @param accountId Account ID
	 * @param workspaceName Workspace name
	 * @return Workspace entity
	 */
	private WorkspaceEntity getWorkspaceByName(String accountId, String workspaceName) {
		LambdaQueryWrapper<WorkspaceEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(WorkspaceEntity::getAccountId, accountId)
			.eq(WorkspaceEntity::getName, workspaceName)
			.ne(WorkspaceEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<WorkspaceEntity> entityOptional = this.getOneOpt(queryWrapper);
		return entityOptional.orElse(null);
	}

	/**
	 * Gets the count of workspaces for an account
	 * @param accountId Account ID
	 * @return Number of workspaces
	 */
	private long getWorkspaceCount(String accountId) {
		LambdaQueryWrapper<WorkspaceEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(WorkspaceEntity::getAccountId, accountId)
			.ne(WorkspaceEntity::getStatus, CommonStatus.DELETED.getStatus());
		return workspaceMapper.selectCount(queryWrapper);
	}

	/**
	 * Converts workspace entity to DTO
	 * @param entity Workspace entity
	 * @return Workspace DTO
	 */
	private Workspace toWorkspaceDTO(WorkspaceEntity entity) {
		if (entity == null) {
			return null;
		}

		Workspace workspace = BeanCopierUtils.copy(entity, Workspace.class);
		String config = entity.getConfig();
		if (StringUtils.isNotBlank(config)) {
			Map<String, Object> jsonObject = JsonUtils.fromJsonToMap(config);
			workspace.setConfig(jsonObject);
		}

		return workspace;
	}

}
