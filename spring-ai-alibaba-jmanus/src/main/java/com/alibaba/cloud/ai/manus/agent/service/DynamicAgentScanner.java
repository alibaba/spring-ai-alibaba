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
package com.alibaba.cloud.ai.manus.agent.service;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.manus.config.IConfigService;
import com.alibaba.cloud.ai.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.manus.agent.annotation.DynamicAgentDefinition;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.manus.agent.startupAgent.StartupAgentConfigLoader;

@Service
public class DynamicAgentScanner implements IDynamicAgentScanner {

	private static final Logger log = LoggerFactory.getLogger(DynamicAgentScanner.class);

	private final DynamicAgentRepository repository;

	private final String basePackage = "com.alibaba.cloud.ai.manus";

	@Autowired
	private IConfigService configService;

	@Autowired
	private StartupAgentConfigLoader startupAgentConfigLoader;

	@Value("${namespace.value}")
	private String namespace;

	@Value("${agent.init}")
	private Boolean agentInit = true;

	@Autowired
	public DynamicAgentScanner(DynamicAgentRepository repository) {
		this.repository = repository;
	}

	public void scanAndSaveAgents() {
		// Check configuration for YAML-based agent override behavior
		ConfigEntity overrideConfig = configService.getConfig("manus.agents.forceOverrideFromYaml")
			.orElseThrow(() -> new IllegalStateException("Cannot find agent override configuration item"));

		// First, check if there are any classes still using the old
		// @DynamicAgentDefinition annotation
		checkForDeprecatedAnnotationUsage();

		boolean shouldOverrideFromYaml = Boolean.parseBoolean(overrideConfig.getConfigValue());

		if (shouldOverrideFromYaml) {
			log.info(
					"✅ Force override from YAML enabled - Starting to scan and override agents from YAML configuration files...");

			// Scan and save/override StartupAgent loaded from configuration file
			scanAndSaveStartupAgents();

			log.info("✅ Dynamic agent override from YAML files completed");
		}
		else {
			log.info("⏭️ Force override from YAML disabled - Skipping agent override from YAML files");
		}
	}

	/**
	 * Check if there are any classes still using the deprecated @DynamicAgentDefinition
	 * annotation and throw runtime exception to prevent system startup
	 */
	private void checkForDeprecatedAnnotationUsage() {
		log.info("Checking for deprecated @DynamicAgentDefinition annotation usage...");

		// Create scanner to detect old annotation usage
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicAgentDefinition.class));
		Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

		if (!candidates.isEmpty()) {
			StringBuilder errorMessage = new StringBuilder();
			errorMessage.append("\n\n========================================\n");
			errorMessage.append("❌ DEPRECATED ANNOTATION DETECTED!\n");
			errorMessage.append("========================================\n\n");
			errorMessage
				.append("The following classes are still using the deprecated @DynamicAgentDefinition annotation:\n\n");

			for (BeanDefinition beanDefinition : candidates) {
				errorMessage.append("  ❌ ").append(beanDefinition.getBeanClassName()).append("\n");
			}

			errorMessage.append("\n📋 MIGRATION GUIDE:\n");
			errorMessage.append("----------------------------------------\n");
			errorMessage.append("Please migrate these agents to YAML configuration format:\n\n");

			errorMessage.append("1️⃣ Remove @DynamicAgentDefinition annotation from Java classes\n");
			errorMessage.append("2️⃣ Create YAML config file for each agent:\n");
			errorMessage.append("   📁 src/main/resources/prompts/startup-agents/{agent_name}/agent-config.yml\n\n");

			errorMessage.append("3️⃣ YAML file format example:\n");
			errorMessage.append("   ```yaml\n");
			errorMessage.append("   # Agent Configuration\n");
			errorMessage.append("   agentName: YOUR_AGENT_NAME\n");
			errorMessage.append("   agentDescription: Your agent description here\n");
			errorMessage.append("   availableToolKeys:\n");
			errorMessage.append("     - tool1\n");
			errorMessage.append("     - tool2\n");
			errorMessage.append("   \n");
			errorMessage.append("   # Next Step Prompt Configuration\n");
			errorMessage.append("   nextStepPrompt: |\n");
			errorMessage.append("     Your multi-line prompt content here...\n");
			errorMessage.append("   ```\n\n");

			errorMessage.append("4️⃣ Directory naming convention:\n");
			errorMessage.append("   - Use lowercase with underscores: agent_name\n");
			errorMessage.append("   - Examples: map_task_agent, reduce_task_agent\n\n");

			errorMessage.append("5️⃣ Example migration for existing agents:\n");
			for (BeanDefinition beanDefinition : candidates) {
				try {
					Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
					DynamicAgentDefinition annotation = clazz.getAnnotation(DynamicAgentDefinition.class);
					if (annotation != null) {
						String agentName = annotation.agentName().toLowerCase().replace("_", "_");
						errorMessage.append("   📂 ")
							.append(clazz.getSimpleName())
							.append(" → ")
							.append("src/main/resources/prompts/startup-agents/")
							.append(agentName.toLowerCase())
							.append("/agent-config.yml\n");
					}
				}
				catch (ClassNotFoundException e) {
					log.warn("Could not load class for migration example: {}", beanDefinition.getBeanClassName());
				}
			}

			errorMessage.append("\n6️⃣ After migration:\n");
			errorMessage.append("   - Delete or empty the Java class (remove annotation and content)\n");
			errorMessage.append("   - The YAML configuration will be automatically loaded\n");
			errorMessage.append("   - Test your agents to ensure they work correctly\n\n");

			errorMessage.append("📚 Reference examples:\n");
			errorMessage.append("   - Check existing YAML configs in: src/main/resources/prompts/startup-agents/\n");
			errorMessage.append("   - Examples: text_file_agent, browser_agent, default_agent\n\n");

			errorMessage.append("========================================\n");
			errorMessage.append("System startup will be blocked until migration is complete!\n");
			errorMessage.append("========================================\n");

			throw new RuntimeException(errorMessage.toString());
		}

		log.info("✅ No deprecated @DynamicAgentDefinition annotations found. System can start normally.");
	}

	/**
	 * Scan and save/override StartupAgent loaded from configuration file
	 */
	private void scanAndSaveStartupAgents() {
		log.info("🔍 Starting to scan YAML agent configuration files...");

		List<String> agentDirs = startupAgentConfigLoader.scanAvailableAgents();
		int processedCount = 0;
		int overriddenCount = 0;
		int createdCount = 0;

		for (String agentDir : agentDirs) {
			try {
				StartupAgentConfigLoader.AgentConfig agentConfig = startupAgentConfigLoader.loadAgentConfig(agentDir);
				if (agentConfig != null) {
					// Check if this is an override or new creation
					DynamicAgentEntity existingEntity = repository.findByAgentName(agentConfig.getAgentName());
					if (existingEntity != null) {
						overriddenCount++;
					}
					else {
						createdCount++;
					}

					if (agentInit) {
						saveStartupAgent(agentConfig);
					}
					processedCount++;
				}
			}
			catch (Exception e) {
				log.error("❌ Failed to load YAML agent configuration: {}", agentDir, e);
			}
		}

		log.info("✅ YAML agent configuration scanning completed - Total: {}, Created: {}, Overridden: {}",
				processedCount, createdCount, overriddenCount);
	}

	/**
	 * Save/Override StartupAgent loaded from configuration file
	 */
	private void saveStartupAgent(StartupAgentConfigLoader.AgentConfig agentConfig) {
		// Check if there is a dynamic agent with the same name
		DynamicAgentEntity existingEntity = repository.findByAgentName(agentConfig.getAgentName());

		// Create or update dynamic agent entity
		DynamicAgentEntity entity = (existingEntity != null) ? existingEntity : new DynamicAgentEntity();

		// Update all fields (force override if exists)
		entity.setAgentName(agentConfig.getAgentName());
		entity.setNamespace(namespace);
		entity.setAgentDescription(agentConfig.getAgentDescription());
		entity.setNextStepPrompt(agentConfig.getNextStepPrompt());
		entity.setAvailableToolKeys(agentConfig.getAvailableToolKeys());
		entity.setClassName(""); // YAML-based agents do not have corresponding Java
									// classes

		// Save or update entity
		repository.save(entity);
		String action = (existingEntity != null) ? "🔄 Overridden" : "✨ Created";
		log.info("{} agent from YAML config: {}", action, entity.getAgentName());
	}

}
