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

import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.ReferTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.refer.Refer;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.ReferService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.ReferEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.ReferMapper;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.AppComponentExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.ParallelExecuteProcessor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for managing references between different components in the
 * application. Handles CRUD operations for references and constructs reference
 * relationships between agents and workflows.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Service
public class ReferServiceImpl extends ServiceImpl<ReferMapper, ReferEntity> implements ReferService {

	/**
	 * Saves a single reference entity
	 */
	@Override
	public Boolean saveRefer(ReferEntity refer) {
		return this.save(refer);
	}

	/**
	 * Saves a list of reference entities in a transaction
	 */
	@Override
	@Transactional
	public Boolean saveReferList(List<ReferEntity> refers) {
		return this.saveBatch(refers);
	}

	/**
	 * Deletes a reference by its refer code and main code
	 */
	@Override
	public Boolean deleteRefer(String referCode, String mainCode) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<ReferEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ReferEntity::getReferCode, referCode);
		queryWrapper.eq(ReferEntity::getMainCode, mainCode);
		queryWrapper.eq(ReferEntity::getWorkspaceId, context.getWorkspaceId());

		return this.remove(queryWrapper);

	}

	/**
	 * Deletes multiple references by main code and optional refer type
	 */
	@Override
	@Transactional
	public Boolean deleteReferList(String mainCode, Integer referType) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<ReferEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ReferEntity::getMainCode, mainCode);
		if (referType != null) {
			queryWrapper.eq(ReferEntity::getReferType, referType);
		}
		List<ReferEntity> entities = this.list(queryWrapper);
		queryWrapper.eq(ReferEntity::getWorkspaceId, context.getWorkspaceId());
		if (CollectionUtils.isEmpty(entities)) {
			return true;
		}
		List<Long> ids = new ArrayList<>();
		for (ReferEntity entity : entities) {
			ids.add(entity.getId());
		}
		return this.removeBatchByIds(ids);
	}

	/**
	 * Retrieves references by main code
	 */
	@Override
	public List<Refer> getReferListByMainCode(String mainCode) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<ReferEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ReferEntity::getMainCode, mainCode);
		queryWrapper.eq(ReferEntity::getWorkspaceId, context.getWorkspaceId());
		return toRefer(this.list(queryWrapper));
	}

	/**
	 * Retrieves references by refer code
	 */
	@Override
	public List<Refer> getReferListByReferCode(String referCode) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<ReferEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ReferEntity::getReferCode, referCode);
		queryWrapper.eq(ReferEntity::getWorkspaceId, context.getWorkspaceId());
		return toRefer(this.list(queryWrapper));
	}

	/**
	 * Constructs reference entities based on application configuration Handles both basic
	 * agent and workflow type applications
	 */
	@Override
	public List<ReferEntity> constructRefers(Application app) {
		RequestContext context = RequestContextHolder.getRequestContext();
		List<ReferEntity> refers = new ArrayList<>();
		if (app.getType() == AppType.BASIC) {
			AgentConfig agentConfig = JsonUtils.fromJson(app.getPubConfigStr(), AgentConfig.class);
			List<String> agentComponents = agentConfig.getAgentComponents();
			if (!CollectionUtils.isEmpty(agentComponents)) {
				for (String agentComponent : agentComponents) {
					ReferEntity refer = new ReferEntity();
					refer.setMainCode(app.getAppId());
					refer.setMainType(ReferTypeEnum.MAIN_TYPE_AGENT.getType());
					refer.setReferCode(agentComponent);
					refer.setReferType(ReferTypeEnum.REFER_TYPE_COMPONENT_AGENT.getType());
					refer.setWorkspaceId(context.getWorkspaceId());
					refer.setGmtCreate(new Date());
					refer.setGmtModified(new Date());
					refers.add(refer);
				}
			}
			List<String> flowComponents = agentConfig.getWorkflowComponents();
			if (!CollectionUtils.isEmpty(flowComponents)) {
				for (String flowComponent : flowComponents) {
					ReferEntity refer = new ReferEntity();
					refer.setMainCode(app.getAppId());
					refer.setMainType(ReferTypeEnum.MAIN_TYPE_AGENT.getType());
					refer.setReferCode(flowComponent);
					refer.setReferType(ReferTypeEnum.REFER_TYPE_COMPONENT_WORKFLOW.getType());
					refer.setWorkspaceId(context.getWorkspaceId());
					refer.setGmtCreate(new Date());
					refer.setGmtModified(new Date());
					refers.add(refer);
				}
			}

		}
		else if (app.getType() == AppType.WORKFLOW) {
			WorkflowConfig workflowConfig = JsonUtils.fromJson(app.getPubConfigStr(), WorkflowConfig.class);
			refers.addAll(fetchReferFromWorkflowConfig(workflowConfig, app));

		}
		return refers;
	}

	/**
	 * Extracts references from workflow configuration Processes nodes recursively to
	 * build reference relationships
	 */
	List<ReferEntity> fetchReferFromWorkflowConfig(WorkflowConfig workflowConfig, Application app) {
		List<ReferEntity> refers = new ArrayList<>();
		if (workflowConfig != null) {
			List<Node> nodes = workflowConfig.getNodes();
			if (!CollectionUtils.isEmpty(nodes)) {
				for (Node node : nodes) {
					if (node.getType().equals(NodeTypeEnum.ITERATOR.getCode())
							|| node.getType().equals(NodeTypeEnum.PARALLEL.getCode())) {
						// IteratorNode or ParallelNode
						ParallelExecuteProcessor.NodeParam nodeParam = JsonUtils
							.fromMap(node.getConfig().getNodeParam(), ParallelExecuteProcessor.NodeParam.class);
						refers.addAll(fetchReferFromWorkflowConfig(nodeParam.getBlock(), app));

					}
					else if (node.getType().equals(NodeTypeEnum.COMPONENT.getCode())) {
						// ComponentNode
						ReferEntity refer = new ReferEntity();
						refer.setMainCode(app.getAppId());
						if (app.getType() == AppType.WORKFLOW) {
							refer.setMainType(ReferTypeEnum.MAIN_TYPE_FLOW.getType());
						}
						else {
							refer.setMainType(ReferTypeEnum.MAIN_TYPE_AGENT.getType());
						}
						AppComponentExecuteProcessor.NodeParam config = JsonUtils
							.fromMap(node.getConfig().getNodeParam(), AppComponentExecuteProcessor.NodeParam.class);
						refer.setReferCode(config.getCode());
						refer.setReferType(Objects.equals(config.getType(), AppComponentTypeEnum.Agent.getValue())
								? ReferTypeEnum.REFER_TYPE_COMPONENT_AGENT.getType()
								: ReferTypeEnum.REFER_TYPE_COMPONENT_WORKFLOW.getType());
						refer.setGmtCreate(new Date());
						refer.setGmtModified(new Date());
						refers.add(refer);

					}
				}

			}
		}

		return refers;
	}

	/**
	 * Converts a list of ReferEntity to Refer objects
	 */
	private List<Refer> toRefer(List<ReferEntity> entities) {
		return entities.stream().map(this::toRefer).collect(Collectors.toList());
	}

	/**
	 * Converts a single ReferEntity to Refer object
	 */
	private Refer toRefer(ReferEntity entity) {
		Refer refer = new Refer();
		refer.setReferCode(entity.getReferCode());
		refer.setMainCode(entity.getMainCode());
		refer.setReferType(entity.getReferType());
		refer.setMainType(entity.getMainType());
		return refer;
	}

}
