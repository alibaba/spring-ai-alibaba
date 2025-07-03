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
		// Check if reset is needed
		ConfigEntity resetConfig = configService.getConfig("manus.resetAgents")
			.orElseThrow(() -> new IllegalStateException("Cannot find reset configuration item"));

		// Create scanner
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicAgentDefinition.class));
		Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

		if (Boolean.parseBoolean(resetConfig.getConfigValue())) {
			log.info("Starting to reset all dynamic agents...");

			// Force update all dynamic agents scanned
			for (BeanDefinition beanDefinition : candidates) {
				try {
					Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
					DynamicAgentDefinition annotation = clazz.getAnnotation(DynamicAgentDefinition.class);
					if (annotation != null) {
						saveDynamicAgent(annotation, clazz);
					}
				}
				catch (ClassNotFoundException e) {
					log.error("Failed to load class: {}", beanDefinition.getBeanClassName(), e);
				}
			}

			// Scan and save StartupAgent loaded from configuration file
			scanAndSaveStartupAgents();

			// After reset, set the configuration to false
			configService.updateConfig("manus.resetAgents", "false");
			log.info("Dynamic agent reset completed");
		}
		else {
			log.info("Skipping dynamic agent reset");
		}
	}

	private void saveDynamicAgent(DynamicAgentDefinition annotation, Class<?> clazz) {
		// Check if there is a dynamic agent with the same name
		DynamicAgentEntity existingEntity = repository.findByAgentName(annotation.agentName());

		// Create or update dynamic agent entity
		DynamicAgentEntity entity = (existingEntity != null) ? existingEntity : new DynamicAgentEntity();

		// Update all fields
		entity.setAgentName(annotation.agentName());
		entity.setAgentDescription(annotation.agentDescription());
		entity.setNextStepPrompt(annotation.nextStepPrompt());
		entity.setAvailableToolKeys(Arrays.asList(annotation.availableToolKeys()));
		entity.setClassName(clazz.getName());

		// Save or update entity
		repository.save(entity);
		String action = (existingEntity != null) ? "Updated" : "Created";
		log.info("{} dynamic agent: {}", action, entity.getAgentName());
	}

	/**
	 * Scan and save StartupAgent loaded from configuration file
	 */
	private void scanAndSaveStartupAgents() {
		log.info("Starting to scan StartupAgent configuration file...");

		List<String> agentDirs = startupAgentConfigLoader.scanAvailableAgents();
		for (String agentDir : agentDirs) {
			try {
				StartupAgentConfigLoader.AgentConfig agentConfig = startupAgentConfigLoader.loadAgentConfig(agentDir);
				if (agentConfig != null) {
					saveStartupAgent(agentConfig);
				}
			}
			catch (Exception e) {
				log.error("Failed to load StartupAgent configuration: {}", agentDir, e);
			}
		}

		log.info("StartupAgent configuration file scanning completed, processed {} agents", agentDirs.size());
	}

	/**
	 * Save StartupAgent loaded from configuration file
	 */
	private void saveStartupAgent(StartupAgentConfigLoader.AgentConfig agentConfig) {
		// Check if there is a dynamic agent with the same name
		DynamicAgentEntity existingEntity = repository.findByAgentName(agentConfig.getAgentName());

		// Create or update dynamic agent entity
		DynamicAgentEntity entity = (existingEntity != null) ? existingEntity : new DynamicAgentEntity();

		// Update all fields
		entity.setAgentName(agentConfig.getAgentName());
		entity.setAgentDescription(agentConfig.getAgentDescription());
		entity.setNextStepPrompt(agentConfig.getNextStepPrompt());
		entity.setAvailableToolKeys(agentConfig.getAvailableToolKeys());
		entity.setClassName(""); // StartupAgent does not have a corresponding Java class

		// Save or update entity
		repository.save(entity);
		String action = (existingEntity != null) ? "Updated" : "Created";
		log.info("{} StartupAgent based on configuration file: {}", action, entity.getAgentName());
	}

}
