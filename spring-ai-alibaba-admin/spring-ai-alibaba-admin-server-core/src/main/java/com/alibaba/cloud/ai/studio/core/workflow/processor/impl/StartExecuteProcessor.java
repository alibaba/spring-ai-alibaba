/*
 * Copyright 2026 the original author or authors.
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

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ParamSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE;

/**
 * 应用编排执行接口�?
 *
 * @since 1.0.0.3
 */
@Component("StartExecuteProcessor")
public class StartExecuteProcessor extends AbstractExecuteProcessor {

	public StartExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.START.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.START.getDesc();
	}

	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		long start = System.currentTimeMillis();

		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		// 设置系统变量
		Map<String, Object> sysMap = context.getSysMap();
		if (sysMap != null) {
			sysMap.keySet().forEach(key -> {
				if (!context.getVariablesMap().containsKey("sys")) {
					Map<String, Object> sysObj = new HashMap<>();
					sysObj.put(key, sysMap.get(key));
					context.getVariablesMap().put("sys", sysObj);
				}
				else {
					Map<String, Object> sysObj = (Map<String, Object>) context.getVariablesMap().get("sys");
					sysObj.put(key, sysMap.get(key));
				}
			});
		}

		// 设置会话变量
		WorkflowConfig.GlobalConfig globalConfig = context.getWorkflowConfig().getGlobalConfig();
		List<CommonParam> sessionParamList = (globalConfig == null || globalConfig.getVariableConfig() == null)
				? Lists.newArrayList() : globalConfig.getVariableConfig().getConversationParams();
		if (!CollectionUtils.isEmpty(sessionParamList)) {
			sessionParamList.stream().forEach(sessionParam -> {
				if (!context.getVariablesMap().containsKey(ParamSourceEnum.conversation.name())) {
					Map<String, Object> sessionObj = new HashMap<>();
					Object value = redisManager.get(String.format(WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE,
							context.getAppId(), context.getConversationId(), sessionParam.getKey()));
					value = value == null ? sessionParam.getDefaultValue() : value;
					sessionObj.put(sessionParam.getKey(), value);
					context.getVariablesMap().put(ParamSourceEnum.conversation.name(), sessionObj);
				}
				else {
					Map<String, Object> sessionObj = (Map<String, Object>) context.getVariablesMap().get("session");
					Object value = redisManager.get(String.format(WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE,
							context.getAppId(), context.getConversationId(), sessionParam.getKey()));
					value = value == null ? sessionParam.getDefaultValue() : value;
					sessionObj.put(sessionParam.getKey(), value);
				}
			});
		}

		// 设置用户透传参数，放入Start节点下的变量关联
		Map<String, Object> userMap = context.getUserMap();
		if (userMap != null) {
			userMap.keySet().forEach(key -> {
				if (!context.getVariablesMap().containsKey(node.getId())) {
					Map<String, Object> userObj = new HashMap<>();
					userObj.put(key, userMap.get(key));
					context.getVariablesMap().put(node.getId(), userObj);
				}
				else {
					Map<String, Object> userObj = (Map<String, Object>) context.getVariablesMap().get(node.getId());
					userObj.put(key, userMap.get(key));
					context.getVariablesMap().put(node.getId(), userObj);
				}
			});
		}

		Map<String, Object> resultMap = Maps.newHashMap();
		resultMap.put("user", context.getUserMap());
		resultMap.put("sys", context.getSysMap());
		resultMap.put("session", context.getVariablesMap().get("session"));
		nodeResult.setInput(JsonUtils.toJson(resultMap));
		nodeResult.setOutput(JsonUtils.toJson(resultMap));
		nodeResult.setNodeExecTime((System.currentTimeMillis() - start) + "ms");

		return nodeResult;
	}

	@Override
	public void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		// 开始节点不需要处理variableMap，上面已处理
	}

}
