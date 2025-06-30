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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StartupAgent configuration loader
 *
 * Responsible for loading the description and prompt content of startupAgent from the configuration file, supporting caching mechanism to improve performance
 */
@Component
public class StartupAgentConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(StartupAgentConfigLoader.class);

	private static final String CONFIG_BASE_PATH = "prompts/startup-agents/";

	// Cache configuration content
	private final Map<String, String> cache = new ConcurrentHashMap<>();

	/**
	 * Load configuration content from the specified path
	 * @param configPath Configuration file path
	 * @return Configuration content
	 */
	private String loadConfigContent(String configPath) {
		try {
			ClassPathResource resource = new ClassPathResource(configPath);
			if (!resource.exists()) {
				log.warn("Configuration file does not exist: {}", configPath);
				return "";
			}

			byte[] bytes = resource.getInputStream().readAllBytes();
			String content = new String(bytes, StandardCharsets.UTF_8);

			log.debug("Successfully loaded configuration file: {}", configPath);
			return content.trim();

		}
		catch (IOException e) {
			log.error("Failed to load configuration file: {}", configPath, e);
			return "";
		}
	}

	/**
	 * Clear cache
	 */
	public void clearCache() {
		cache.clear();
		log.info("StartupAgent configuration cache cleared");
	}

	/**
	 * Get cache size
	 * @return Cache entry count
	 */
	public int getCacheSize() {
		return cache.size();
	}

	/**
	 * Load agent configuration information
	 * @param agentName agent name
	 * @return agent configuration
	 */
	public AgentConfig loadAgentConfig(String agentName) {
		String configPath = CONFIG_BASE_PATH + agentName.toLowerCase() + "/agent-config.yml";
		String configContent = loadConfigContent(configPath);

		if (configContent.isEmpty()) {
			log.warn("Agent configuration file does not exist or is empty: {}", configPath);
			return null;
		}

		try {
			Yaml yaml = new Yaml();
			Map<String, Object> yamlData = yaml.load(configContent);

			if (yamlData == null) {
				log.warn("YAML configuration file parsing result is empty: {}", configPath);
				return null;
			}

			AgentConfig config = new AgentConfig();
			config.setAgentName((String) yamlData.getOrDefault("agentName", agentName));
			config.setAgentDescription((String) yamlData.getOrDefault("agentDescription", ""));
			config.setNextStepPrompt((String) yamlData.getOrDefault("nextStepPrompt", ""));

			// Process tool list
			Object toolKeysObj = yamlData.get("availableToolKeys");
			if (toolKeysObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> toolKeyList = (List<String>) toolKeysObj;
				config.setAvailableToolKeys(toolKeyList);
			}

			return config;
		}
		catch (Exception e) {
			log.error("Failed to parse Agent YAML configuration file: {}", configPath, e);
			return null;
		}
	}

	/**
	 * Scan all available startup agent configuration directories
	 * @return agent directory name list
	 */
	public List<String> scanAvailableAgents() {
		try {
			ClassPathResource baseResource = new ClassPathResource(CONFIG_BASE_PATH);
			if (!baseResource.exists()) {
				log.warn("StartupAgent configuration base directory does not exist: {}", CONFIG_BASE_PATH);
				return List.of();
			}

			// Here is a simplified implementation, directly returning the known agent list
			// In actual projects, it can be dynamically discovered by scanning the file system
			return Arrays.asList("default_agent", "text_file_agent", "browser_agent");
		}
		catch (Exception e) {
			log.error("Failed to scan Agent configuration directory", e);
			return List.of();
		}
	}

	/**
	 * Agent configuration class
	 */
	public static class AgentConfig {

		private String agentName;

		private String agentDescription;

		private String nextStepPrompt;

		private List<String> availableToolKeys;

		// Getters and Setters
		public String getAgentName() {
			return agentName;
		}

		public void setAgentName(String agentName) {
			this.agentName = agentName;
		}

		public String getAgentDescription() {
			return agentDescription;
		}

		public void setAgentDescription(String agentDescription) {
			this.agentDescription = agentDescription;
		}

		public String getNextStepPrompt() {
			return nextStepPrompt;
		}

		public void setNextStepPrompt(String nextStepPrompt) {
			this.nextStepPrompt = nextStepPrompt;
		}

		public List<String> getAvailableToolKeys() {
			return availableToolKeys;
		}

		public void setAvailableToolKeys(List<String> availableToolKeys) {
			this.availableToolKeys = availableToolKeys;
		}

	}

}
