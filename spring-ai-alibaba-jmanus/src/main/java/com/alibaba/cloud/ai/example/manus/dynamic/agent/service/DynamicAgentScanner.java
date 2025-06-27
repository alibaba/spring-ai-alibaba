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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.config.ConfigService;
import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent.StartupAgentConfigLoader;

import jakarta.annotation.PostConstruct;

@Service
public class DynamicAgentScanner {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgentScanner.class);

	private final DynamicAgentRepository repository;

	private final String basePackage = "com.alibaba.cloud.ai.example.manus";

	@Autowired
	private ConfigService configService;

	@Autowired
	private StartupAgentConfigLoader startupAgentConfigLoader;

	@Autowired
	public DynamicAgentScanner(DynamicAgentRepository repository) {
		this.repository = repository;
	}

	@PostConstruct
	public void scanAndSaveAgents() {
		// 检查是否需要重置
		ConfigEntity resetConfig = configService.getConfig("manus.resetAgents")
			.orElseThrow(() -> new IllegalStateException("无法找到重置配置项"));

		// 创建扫描器
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicAgentDefinition.class));
		Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

		if (Boolean.parseBoolean(resetConfig.getConfigValue())) {
			log.info("开始重置所有动态代理...");

			// 强制更新所有扫描到的动态代理
			for (BeanDefinition beanDefinition : candidates) {
				try {
					Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
					DynamicAgentDefinition annotation = clazz.getAnnotation(DynamicAgentDefinition.class);
					if (annotation != null) {
						saveDynamicAgent(annotation, clazz);
					}
				}
				catch (ClassNotFoundException e) {
					log.error("加载类失败: {}", beanDefinition.getBeanClassName(), e);
				}
			}

			// 扫描并保存从配置文件加载的StartupAgent
			scanAndSaveStartupAgents();

			// 重置完成后，将配置改为 false
			configService.updateConfig("manus.resetAgents", "false");
			log.info("动态代理重置完成");
		}
		else {
			log.info("跳过动态代理重置");
		}
	}

	private void saveDynamicAgent(DynamicAgentDefinition annotation, Class<?> clazz) {
		// 检查是否存在同名的动态代理
		DynamicAgentEntity existingEntity = repository.findByAgentName(annotation.agentName());

		// 创建或更新动态代理实体
		DynamicAgentEntity entity = (existingEntity != null) ? existingEntity : new DynamicAgentEntity();

		// 更新所有字段
		entity.setAgentName(annotation.agentName());
		entity.setAgentDescription(annotation.agentDescription());
		entity.setNextStepPrompt(annotation.nextStepPrompt());
		entity.setAvailableToolKeys(Arrays.asList(annotation.availableToolKeys()));
		entity.setClassName(clazz.getName());

		// 保存或更新实体
		repository.save(entity);
		String action = (existingEntity != null) ? "更新" : "创建";
		log.info("已{}动态代理: {}", action, entity.getAgentName());
	}

	/**
	 * 扫描并保存从配置文件加载的StartupAgent
	 */
	private void scanAndSaveStartupAgents() {
		log.info("开始扫描StartupAgent配置文件...");

		List<String> agentDirs = startupAgentConfigLoader.scanAvailableAgents();
		for (String agentDir : agentDirs) {
			try {
				StartupAgentConfigLoader.AgentConfig agentConfig = startupAgentConfigLoader.loadAgentConfig(agentDir);
				if (agentConfig != null) {
					saveStartupAgent(agentConfig);
				}
			}
			catch (Exception e) {
				log.error("加载StartupAgent配置失败: {}", agentDir, e);
			}
		}

		log.info("StartupAgent配置文件扫描完成，共处理 {} 个agent", agentDirs.size());
	}

	/**
	 * 保存从配置文件加载的StartupAgent
	 */
	private void saveStartupAgent(StartupAgentConfigLoader.AgentConfig agentConfig) {
		// 检查是否存在同名的动态代理
		DynamicAgentEntity existingEntity = repository.findByAgentName(agentConfig.getAgentName());

		// 创建或更新动态代理实体
		DynamicAgentEntity entity = (existingEntity != null) ? existingEntity : new DynamicAgentEntity();

		// 更新所有字段
		entity.setAgentName(agentConfig.getAgentName());
		entity.setAgentDescription(agentConfig.getAgentDescription());
		entity.setNextStepPrompt(agentConfig.getNextStepPrompt());
		entity.setAvailableToolKeys(agentConfig.getAvailableToolKeys());
		entity.setClassName(""); // 基于配置文件的agent没有对应的Java类

		// 保存或更新实体
		repository.save(entity);
		String action = (existingEntity != null) ? "更新" : "创建";
		log.info("已{}基于配置文件的动态代理: {}", action, entity.getAgentName());
	}

}
