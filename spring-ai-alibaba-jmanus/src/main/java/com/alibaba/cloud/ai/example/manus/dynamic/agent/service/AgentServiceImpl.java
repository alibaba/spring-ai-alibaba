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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.IMcpService;
import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.namespace.vo.NamespaceConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.service.NamespaceService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.DynamicAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.ToolCallbackProvider;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.example.manus.planning.IPlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;

@Service
public class AgentServiceImpl implements AgentService {

	private static final String DEFAULT_AGENT_NAME = "DEFAULT_AGENT";

	// MapReduce protected agent names - cannot be deleted by users
	private static final String[] PROTECTED_MAPREDUCE_AGENTS = { "MAPREDUCE_DATA_PREPARE_AGENT", "MAPREDUCE_FIN_AGENT",
			"MAPREDUCE_MAP_TASK_AGENT", "MAPREDUCE_REDUCE_TASK_AGENT" };

	private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

	private final IDynamicAgentLoader dynamicAgentLoader;

	private final DynamicAgentRepository repository;

	private final IPlanningFactory planningFactory;

	private final IMcpService mcpService;

	private final NamespaceService namespaceService;

	@Autowired
	@Lazy
	private ILlmService llmService;

	@Autowired
	@Lazy
	private ToolCallingManager toolCallingManager;

	@Autowired
	public AgentServiceImpl(@Lazy IDynamicAgentLoader dynamicAgentLoader, DynamicAgentRepository repository,
			@Lazy IPlanningFactory planningFactory, @Lazy IMcpService mcpService, NamespaceService namespaceService) {
		this.dynamicAgentLoader = dynamicAgentLoader;
		this.repository = repository;
		this.planningFactory = planningFactory;
		this.mcpService = mcpService;
		this.namespaceService = namespaceService;
	}

	@Override
	public List<AgentConfig> getAllAgents() {
		return repository.findAll().stream().map(this::mapToAgentConfig).collect(Collectors.toList());
	}

	@Override
	public List<AgentConfig> getAllAgentsByNamespace(String namespace) {
		List<DynamicAgentEntity> entities;
		if (namespace == null || namespace.trim().isEmpty()) {
			// If namespace is null or empty, use default namespace
			namespace = "default";
			log.info("Namespace not specified, using default namespace: {}", namespace);
		}
		if ("default".equalsIgnoreCase(namespace)) {
			entities = repository.findAll();
		}
		else {
			entities = repository.findAllByNamespace(namespace);
		}
		return entities.stream().map(this::mapToAgentConfig).collect(Collectors.toList());
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
			// Set default namespace if namespace is null or empty
			if (config.getNamespace() == null || config.getNamespace().trim().isEmpty()) {
				String defaultNamespace = getDefaultNamespace();
				config.setNamespace(defaultNamespace);
				log.info("Namespace not specified for Agent: {}, using default namespace: {}", config.getName(),
						defaultNamespace);
			}

			// Check if an Agent with the same name already exists
			DynamicAgentEntity existingAgent = repository.findByAgentName(config.getName());
			if (existingAgent != null) {
				log.info("Found Agent with same name: {}, updating Agent", config.getName());
				config.setId(existingAgent.getId().toString());
				return updateAgent(config);
			}

			DynamicAgentEntity entity = new DynamicAgentEntity();
			entity = mergePrompts(entity, config.getName());
			updateEntityFromConfig(entity, config);
			entity = repository.save(entity);
			log.info("Successfully created new Agent: {}", config.getName());
			return mapToAgentConfig(entity);
		}
		catch (Exception e) {
			log.warn("Exception occurred during Agent creation: {}, error message: {}", config.getName(),
					e.getMessage());
			// If it's a uniqueness constraint violation exception, try returning the
			// existing Agent
			if (e.getMessage() != null && e.getMessage().contains("Unique")) {
				DynamicAgentEntity existingAgent = repository.findByAgentName(config.getName());
				if (existingAgent != null) {
					log.info("Return existing Agent: {}", config.getName());
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

		// Protect default agent from deletion
		if (DEFAULT_AGENT_NAME.equals(entity.getAgentName())) {
			throw new IllegalArgumentException("Cannot delete default Agent");
		}

		// Protect MapReduce system agents from deletion
		if (Arrays.asList(PROTECTED_MAPREDUCE_AGENTS).contains(entity.getAgentName())) {
			throw new IllegalArgumentException("Cannot delete protected system Agent: " + entity.getAgentName());
		}

		repository.deleteById(Long.parseLong(id));
	}

	public List<Tool> getAvailableTools() {

		String uuid = UUID.randomUUID().toString();
		List<String> columns = Arrays.asList("dummyColumn1", "dummyColumn2");
		try {
			Map<String, ToolCallBackContext> toolcallContext = planningFactory.toolCallbackMap(uuid, uuid, columns);
			return toolcallContext.entrySet().stream().map(entry -> {
				Tool tool = new Tool();
				tool.setKey(entry.getKey());
				tool.setName(entry.getKey()); // You might want to provide a more friendly
				// name
				tool.setDescription(entry.getValue().getFunctionInstance().getDescription());
				tool.setEnabled(true);
				tool.setServiceGroup(entry.getValue().getFunctionInstance().getServiceGroup());
				return tool;
			}).collect(Collectors.toList());
		}
		finally {
			mcpService.close(uuid);
		}
	}

	private AgentConfig mapToAgentConfig(DynamicAgentEntity entity) {
		AgentConfig config = new AgentConfig();
		entity = mergePrompts(entity, entity.getAgentName());
		config.setId(entity.getId().toString());
		config.setName(entity.getAgentName());
		config.setDescription(entity.getAgentDescription());
		config.setSystemPrompt(entity.getSystemPrompt());
		config.setNextStepPrompt(entity.getNextStepPrompt());
		config.setAvailableTools(entity.getAvailableToolKeys());
		config.setClassName(entity.getClassName());
		config.setNamespace(entity.getNamespace());
		DynamicModelEntity model = entity.getModel();
		config.setModel(model == null ? null : model.mapToModelConfig());
		return config;
	}

	/**
	 * Get default namespace code when no namespace is specified. Uses the first available
	 * namespace from getAllNamespaces(), or "default" if no namespaces exist.
	 * @return default namespace code
	 */
	private String getDefaultNamespace() {
		try {
			List<NamespaceConfig> namespaces = namespaceService.getAllNamespaces();
			if (!namespaces.isEmpty()) {
				// Find the namespace with code "default" first
				for (NamespaceConfig namespace : namespaces) {
					if ("default".equals(namespace.getCode())) {
						log.debug("Found default namespace with code: {}", namespace.getCode());
						return namespace.getCode();
					}
				}
				// If no "default" code namespace found, use the first one
				String firstNamespaceCode = namespaces.get(0).getCode();
				log.debug("Using first namespace as default: {}", firstNamespaceCode);
				return firstNamespaceCode;
			}
			else {
				// If no namespaces exist, return "default"
				log.warn("No namespaces found, using fallback default namespace code: default");
				return "default";
			}
		}
		catch (Exception e) {
			log.error("Error getting default namespace, using fallback: {}", e.getMessage());
			return "default";
		}
	}

	private void updateEntityFromConfig(DynamicAgentEntity entity, AgentConfig config) {
		// Set default namespace if namespace is null or empty
		if (config.getNamespace() == null || config.getNamespace().trim().isEmpty()) {
			String defaultNamespace = getDefaultNamespace();
			config.setNamespace(defaultNamespace);
			log.info("Namespace not specified for Agent: {}, using default namespace: {}", config.getName(),
					defaultNamespace);
		}

		entity.setAgentName(config.getName());
		entity.setAgentDescription(config.getDescription());
		String nextStepPrompt = config.getNextStepPrompt();
		entity = mergePrompts(entity, config.getName());
		entity.setNextStepPrompt(nextStepPrompt);

		// 1. Create new collection to ensure uniqueness and order
		java.util.Set<String> toolSet = new java.util.LinkedHashSet<>();
		List<String> availableTools = config.getAvailableTools();
		if (availableTools != null) {
			toolSet.addAll(availableTools);
		}
		// 2. Add TerminateTool (if not exists)
		if (!toolSet.contains(com.alibaba.cloud.ai.example.manus.tool.TerminateTool.name)) {
			log.info("Adding necessary tool for Agent[{}]: {}", config.getName(),
					com.alibaba.cloud.ai.example.manus.tool.TerminateTool.name);
			toolSet.add(com.alibaba.cloud.ai.example.manus.tool.TerminateTool.name);
		}
		// 3. Convert to List and set
		entity.setAvailableToolKeys(new java.util.ArrayList<>(toolSet));
		entity.setClassName(config.getName());
		ModelConfig model = config.getModel();
		if (model != null) {
			entity.setModel(new DynamicModelEntity(model.getId()));
		}

		// 4. Set the user-selected namespace
		entity.setNamespace(config.getNamespace());
	}

	private DynamicAgentEntity mergePrompts(DynamicAgentEntity entity, String agentName) {
		// The SystemPrompt property here is deprecated, use nextStepPrompt directly
		if (StringUtils.isNotBlank(entity.getSystemPrompt())) {
			String systemPrompt = entity.getSystemPrompt();
			String nextPrompt = entity.getNextStepPrompt();
			// The SystemPrompt property here is deprecated, use nextStepPrompt directly
			if (nextPrompt != null && !nextPrompt.trim().isEmpty()) {
				nextPrompt = systemPrompt + "\n" + nextPrompt;
			}
			log.warn(
					"Agent[{}] SystemPrompt is not empty, but the property is deprecated, only keep nextPrompt. This time merge the agent content. If you need this content to take effect in prompt, please directly update the unique prompt in the interface. Current specified value: {}",
					agentName, nextPrompt);
			entity.setSystemPrompt(" ");
		}
		return entity;
	}

	@Override
	public BaseAgent createDynamicBaseAgent(String name, String planId, String rootPlanId,
			Map<String, Object> initialAgentSetting, List<String> columns) {

		log.info("Create new BaseAgent: {}, planId: {}", name, planId);

		try {
			// Load existing Agent through dynamicAgentLoader
			DynamicAgent agent = dynamicAgentLoader.loadAgent(name, initialAgentSetting);

			// Set planId
			agent.setCurrentPlanId(planId);
			agent.setRootPlanId(rootPlanId);
			// Set tool callback mapping
			Map<String, ToolCallBackContext> toolCallbackMap = planningFactory.toolCallbackMap(planId, rootPlanId,
					columns);
			agent.setToolCallbackProvider(new ToolCallbackProvider() {

				@Override
				public Map<String, ToolCallBackContext> getToolCallBackContext() {
					return toolCallbackMap;
				}

			});

			log.info("Successfully loaded BaseAgent: {}, available tools count: {}", name,
					agent.getToolCallList().size());

			return agent;
		}
		catch (Exception e) {
			log.error("Exception occurred during BaseAgent loading: {}, error message: {}", name, e.getMessage(), e);
			throw new RuntimeException("Failed to load BaseAgent: " + e.getMessage(), e);
		}
	}

}
