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

import com.alibaba.cloud.ai.studio.core.base.entity.AgentSchemaEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.AgentSchemaMapper;
import com.alibaba.cloud.ai.studio.core.base.service.AgentSchemaService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.enums.agent.AgentStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Implementation of AgentSchemaService for CRUD operations.
 *
 * @since 1.0.0.3
 */
@Service
public class AgentSchemaServiceImpl extends ServiceImpl<AgentSchemaMapper, AgentSchemaEntity>
		implements AgentSchemaService {

	@Override
	public AgentSchemaEntity createAgentSchema(AgentSchemaEntity agentSchemaEntity) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();

		// Check for duplicate name in the same workspace
		String workspaceId = requestContext != null ? requestContext.getWorkspaceId() : null;
		if (StringUtils.isBlank(workspaceId)) {
			workspaceId = "1"; // Use default workspace ID that matches database
		}

		LambdaQueryWrapper<AgentSchemaEntity> duplicateCheck = new LambdaQueryWrapper<>();
		duplicateCheck.eq(AgentSchemaEntity::getWorkspaceId, workspaceId);
		duplicateCheck.eq(AgentSchemaEntity::getName, agentSchemaEntity.getName());
		List<AgentSchemaEntity> existingAgents = list(duplicateCheck);

		if (!existingAgents.isEmpty()) {
			throw new IllegalArgumentException("智能体名称已存在，请使用其他名称: " + agentSchemaEntity.getName());
		}

		// Set default values
		if (StringUtils.isBlank(agentSchemaEntity.getAgentId())) {
			agentSchemaEntity.setAgentId(IdGenerator.generateAgentId());
		}

		agentSchemaEntity.setWorkspaceId(workspaceId);
		agentSchemaEntity.setStatus(AgentStatus.ACTIVE);
		agentSchemaEntity.setEnabled(true);
		agentSchemaEntity.setGmtCreate(new Date());
		agentSchemaEntity.setGmtModified(new Date());

		String accountId = requestContext != null ? requestContext.getAccountId() : "10000";
		agentSchemaEntity.setCreator(accountId);
		agentSchemaEntity.setModifier(accountId);

		save(agentSchemaEntity);
		return agentSchemaEntity;
	}

	@Override
	public AgentSchemaEntity updateAgentSchema(AgentSchemaEntity agentSchemaEntity) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();

		AgentSchemaEntity existing = getById(agentSchemaEntity.getId());
		if (existing == null) {
			throw new IllegalArgumentException("Agent schema not found with id: " + agentSchemaEntity.getId());
		}

		// Check for duplicate name in the same workspace (excluding current agent)
		String workspaceId = requestContext != null ? requestContext.getWorkspaceId() : null;
		if (StringUtils.isBlank(workspaceId)) {
			workspaceId = "1"; // Use default workspace ID that matches database
		}

		LambdaQueryWrapper<AgentSchemaEntity> duplicateCheck = new LambdaQueryWrapper<>();
		duplicateCheck.eq(AgentSchemaEntity::getWorkspaceId, workspaceId);
		duplicateCheck.eq(AgentSchemaEntity::getName, agentSchemaEntity.getName());
		duplicateCheck.ne(AgentSchemaEntity::getId, agentSchemaEntity.getId()); // Exclude
																				// current
																				// agent
		List<AgentSchemaEntity> existingAgents = list(duplicateCheck);

		if (!existingAgents.isEmpty()) {
			throw new IllegalArgumentException("智能体名称已存在，请使用其他名称: " + agentSchemaEntity.getName());
		}

		// Update fields
		agentSchemaEntity.setGmtModified(new Date());
		agentSchemaEntity.setModifier(requestContext.getAccountId());

		updateById(agentSchemaEntity);
		return agentSchemaEntity;
	}

	@Override
	public void deleteAgentSchema(Long id) {
		removeById(id);
	}

	@Override
	public AgentSchemaEntity getAgentSchemaById(Long id) {
		return getById(id);
	}

	@Override
	public List<AgentSchemaEntity> getAgentSchemasByWorkspaceId(String workspaceId) {
		LambdaQueryWrapper<AgentSchemaEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AgentSchemaEntity::getWorkspaceId, workspaceId);
		queryWrapper.orderByDesc(AgentSchemaEntity::getGmtModified);
		return list(queryWrapper);
	}

	@Override
	public PagingList<AgentSchemaEntity> getAgentSchemasByWorkspaceId(String workspaceId,
			Page<AgentSchemaEntity> page) {
		LambdaQueryWrapper<AgentSchemaEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AgentSchemaEntity::getWorkspaceId, workspaceId);
		queryWrapper.orderByDesc(AgentSchemaEntity::getGmtModified);

		Page<AgentSchemaEntity> result = page(page, queryWrapper);
		return PagingList.<AgentSchemaEntity>builder()
			.current((int) result.getCurrent())
			.size((int) result.getSize())
			.total(result.getTotal())
			.records(result.getRecords())
			.build();
	}

	@Override
	public List<AgentSchemaEntity> getAgentSchemasByName(String name, String workspaceId) {
		LambdaQueryWrapper<AgentSchemaEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AgentSchemaEntity::getWorkspaceId, workspaceId);
		queryWrapper.like(AgentSchemaEntity::getName, name);
		queryWrapper.orderByDesc(AgentSchemaEntity::getGmtModified);
		return list(queryWrapper);
	}

	@Override
	public void setAgentSchemaEnabled(Long id, Boolean enabled) {
		AgentSchemaEntity agent = getById(id);
		if (agent == null) {
			throw new IllegalArgumentException("Agent schema not found with id: " + id);
		}

		agent.setEnabled(enabled);
		agent.setGmtModified(new Date());
		agent.setModifier(RequestContextHolder.getRequestContext().getAccountId());

		updateById(agent);
	}

}
