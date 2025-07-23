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
package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AgentFactory 负责动态获取和更新 ChatClient 实例。 支持优先从 agentClientMap 获取，找不到则从 Spring 容器查找。
 */
@Component
@DependsOn({ "agentsConfiguration", "agentModelsConfiguration" })
public class AgentFactory {

	private static final Logger logger = LoggerFactory.getLogger(AgentFactory.class);

	private final Map<String, ChatClient> agentClientMap;

	private final ApplicationContext context;

	private final AgentsConfiguration agentsConfiguration;

	/**
	 * 构造器注入，保证 agentClientMap 为线程安全实现。
	 */
	public AgentFactory(Map<String, ChatClient> agentClientMap, ApplicationContext context,
			AgentsConfiguration agentsConfiguration) {
		if (agentClientMap instanceof ConcurrentHashMap) {
			this.agentClientMap = agentClientMap;
		}
		else {
			this.agentClientMap = new ConcurrentHashMap<>(agentClientMap);
		}
		this.context = context;
		this.agentsConfiguration = agentsConfiguration;
	}

	/**
	 * 根据名字获取 ChatClient Bean（优先从 agentClientMap，找不到则从Spring容器获取）。
	 * @param agentName agent名称
	 * @return ChatClient 或 null
	 */
	public ChatClient getAgentByName(String agentName) {
		Objects.requireNonNull(agentName, "agentName must not be null");
		if (agentClientMap.containsKey(agentName)) {
			return agentClientMap.get(agentName);
		}
		try {
			return context.getBean(agentName, ChatClient.class);
		}
		catch (Exception e) {
			logger.warn("No ChatClient bean found for agentName: {}", agentName, e);
			return null;
		}
	}

	/**
	 * 根据名字更新/注册 ChatClient（仅更新 agentClientMap，不会影响Spring容器的Bean定义）。
	 * @param agentName agent名称
	 * @param modelName 模型名称
	 */
	public void updateAgentByName(String agentName, String modelName) {
		Objects.requireNonNull(agentName, "agentName must not be null");
		Objects.requireNonNull(modelName, "modelName must not be null");
		ChatClient newClient;
		try {
			newClient = agentsConfiguration.buildAgentByName(agentName, modelName);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Unknown agentName: {}", agentName);
			return;
		}
		if (newClient == null) {
			logger.warn("Failed to build ChatClient for modelName: {}", modelName);
			return;
		}
		agentClientMap.put(agentName, newClient);
	}

	/**
	 * 批量更新/注册 ChatClient。
	 * @param models 每个map需包含agentName和modelName
	 */
	public void batchUpdateAgents(List<ModelParamRepositoryImpl.AgentModel> models) {
		if (models == null)
			return;
		for (ModelParamRepositoryImpl.AgentModel model : models) {
			String agentName = model.agentName();
			String modelName = model.modelName();
			if (agentName != null && modelName != null) {
				updateAgentByName(agentName, modelName);
			}
			else {
				logger.warn("Invalid model config: {}", model);
			}
		}
	}

}
