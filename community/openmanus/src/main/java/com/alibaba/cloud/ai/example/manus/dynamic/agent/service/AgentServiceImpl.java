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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration;
import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;

@Service
public class AgentServiceImpl implements AgentService {

	private static final String DEFAULT_AGENT_NAME = "DEFAULT_AGENT"; // 修改为通过 name 判断

	private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

	@Autowired
	private DynamicAgentLoader dynamicAgentLoader;

	@Autowired
	private DynamicAgentRepository repository;

	@Autowired
	private ManusConfiguration manusConfiguration;

	@Override
	public List<AgentConfig> getAllAgents() {
		return repository.findAll().stream().map(this::mapToAgentConfig).collect(Collectors.toList());
	}

	@Override
	public AgentConfig getAgentById(String id) {
		DynamicAgentEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
		return mapToAgentConfig(entity);
	}

	@Override
	public AgentConfig createAgent(AgentConfig config) {
		try {
			// 检查是否已存在同名Agent
			DynamicAgentEntity existingAgent = repository.findByAgentName(config.getName());
			if (existingAgent != null) {
				log.info("发现同名Agent: {}，返回已存在的Agent", config.getName());
				return mapToAgentConfig(existingAgent);
			}

			DynamicAgentEntity entity = new DynamicAgentEntity();
			updateEntityFromConfig(entity, config);
			entity = repository.save(entity);
			log.info("成功创建新Agent: {}", config.getName());
			return mapToAgentConfig(entity);
		}
		catch (Exception e) {
			log.warn("创建Agent过程中发生异常: {}，错误信息: {}", config.getName(), e.getMessage());
			// 如果是唯一性约束违反异常，尝试返回已存在的Agent
			if (e.getMessage() != null && e.getMessage().contains("Unique")) {
				DynamicAgentEntity existingAgent = repository.findByAgentName(config.getName());
				if (existingAgent != null) {
					log.info("返回已存在的Agent: {}", config.getName());
					return mapToAgentConfig(existingAgent);
				}
			}
			throw e;
		}
	}

	@Override
	public AgentConfig updateAgent(AgentConfig config) {
		DynamicAgentEntity entity = repository.findById(Long.parseLong(config.getId()))
			.orElseThrow(() -> new IllegalArgumentException("Agent not found: " + config.getId()));
		updateEntityFromConfig(entity, config);
		entity = repository.save(entity);
		return mapToAgentConfig(entity);
	}

	@Override
	public void deleteAgent(String id) {
		DynamicAgentEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

		if (DEFAULT_AGENT_NAME.equals(entity.getAgentName())) {
			throw new IllegalArgumentException("不能删除默认 Agent");
		}

		repository.deleteById(Long.parseLong(id));
	}

	@Override
	public List<Tool> getAvailableTools() {

		Map<String, ToolCallBackContext> toolcallContext = manusConfiguration.toolCallbackMap(null);
		return toolcallContext.entrySet().stream().map(entry -> {
			Tool tool = new Tool();
			tool.setKey(entry.getKey());
			tool.setName(entry.getKey()); // You might want to provide a more friendly
											// name
			tool.setDescription(entry.getValue().getFunctionInstance().getDescription());
			tool.setEnabled(true);
			return tool;
		}).collect(Collectors.toList());
	}

	private AgentConfig mapToAgentConfig(DynamicAgentEntity entity) {
		AgentConfig config = new AgentConfig();
		config.setId(entity.getId().toString());
		config.setName(entity.getAgentName());
		config.setDescription(entity.getAgentDescription());
		config.setSystemPrompt(entity.getSystemPrompt());
		config.setNextStepPrompt(entity.getNextStepPrompt());
		config.setAvailableTools(entity.getAvailableToolKeys());
		config.setClassName(entity.getClassName());
		return config;
	}

	private void updateEntityFromConfig(DynamicAgentEntity entity, AgentConfig config) {
		entity.setAgentName(config.getName());
		entity.setAgentDescription(config.getDescription());
		entity.setSystemPrompt(config.getSystemPrompt());
		entity.setNextStepPrompt(config.getNextStepPrompt());
		entity.setAvailableToolKeys(config.getAvailableTools());
		entity.setClassName(config.getName());
	}

}
