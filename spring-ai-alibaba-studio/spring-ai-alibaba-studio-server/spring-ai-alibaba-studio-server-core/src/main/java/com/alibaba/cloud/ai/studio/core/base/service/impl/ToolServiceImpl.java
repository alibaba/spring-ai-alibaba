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

import com.alibaba.cloud.ai.studio.core.base.entity.ToolEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.ToolMapper;
import com.alibaba.cloud.ai.studio.core.base.service.ToolService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Implementation of ToolService for CRUD operations.
 *
 * @since 1.0.0.3
 */
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, ToolEntity> implements ToolService {

	@Override
	public ToolEntity createTool(ToolEntity toolEntity) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();

		// Set default values
		if (StringUtils.isBlank(toolEntity.getToolId())) {
			toolEntity.setToolId(IdGenerator.generateToolId());
		}
		toolEntity.setWorkspaceId(requestContext.getWorkspaceId());
		toolEntity.setStatus(ToolStatus.PUBLISHED);
		toolEntity.setEnabled(true);
		toolEntity.setGmtCreate(new Date());
		toolEntity.setGmtModified(new Date());
		toolEntity.setCreator(requestContext.getAccountId());
		toolEntity.setModifier(requestContext.getAccountId());

		save(toolEntity);
		return toolEntity;
	}

	@Override
	public ToolEntity updateTool(ToolEntity toolEntity) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();

		ToolEntity existing = getById(toolEntity.getId());
		if (existing == null) {
			throw new IllegalArgumentException("Tool not found with id: " + toolEntity.getId());
		}

		// Update fields
		toolEntity.setGmtModified(new Date());
		toolEntity.setModifier(requestContext.getAccountId());

		updateById(toolEntity);
		return toolEntity;
	}

	@Override
	public void deleteTool(Long id) {
		removeById(id);
	}

	@Override
	public ToolEntity getToolById(Long id) {
		return getById(id);
	}

	@Override
	public List<ToolEntity> getToolsByWorkspaceId(String workspaceId) {
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, workspaceId);
		queryWrapper.orderByDesc(ToolEntity::getGmtModified);
		return list(queryWrapper);
	}

	@Override
	public PagingList<ToolEntity> getToolsByWorkspaceId(String workspaceId, Page<ToolEntity> page) {
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, workspaceId);
		queryWrapper.orderByDesc(ToolEntity::getGmtModified);

		Page<ToolEntity> result = page(page, queryWrapper);
		return PagingList.<ToolEntity>builder()
			.current((int) result.getCurrent())
			.size((int) result.getSize())
			.total(result.getTotal())
			.records(result.getRecords())
			.build();
	}

	@Override
	public List<ToolEntity> getToolsByName(String name, String workspaceId) {
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, workspaceId);
		queryWrapper.like(ToolEntity::getName, name);
		queryWrapper.orderByDesc(ToolEntity::getGmtModified);
		return list(queryWrapper);
	}

	@Override
	public void setToolEnabled(Long id, Boolean enabled) {
		ToolEntity tool = getById(id);
		if (tool == null) {
			throw new IllegalArgumentException("Tool not found with id: " + id);
		}

		tool.setEnabled(enabled);
		tool.setGmtModified(new Date());
		tool.setModifier(RequestContextHolder.getRequestContext().getAccountId());

		updateById(tool);
	}

	@Override
	public List<ToolEntity> getToolsByPluginId(String pluginId, String workspaceId) {
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, workspaceId);
		queryWrapper.eq(ToolEntity::getPluginId, pluginId);
		queryWrapper.orderByDesc(ToolEntity::getGmtModified);
		return list(queryWrapper);
	}

}
