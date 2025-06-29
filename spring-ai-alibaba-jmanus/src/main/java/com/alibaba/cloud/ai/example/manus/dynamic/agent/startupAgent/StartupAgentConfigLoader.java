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
 * StartupAgent配置加载器
 *
 * 负责从配置文件中加载startupAgent的描述和提示内容 支持缓存机制，提高性能
 */
@Component
public class StartupAgentConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(StartupAgentConfigLoader.class);

	private static final String CONFIG_BASE_PATH = "prompts/startup-agents/";

	// 缓存配置内容
	private final Map<String, String> cache = new ConcurrentHashMap<>();

	/**
	 * 从指定路径加载配置内容
	 * @param configPath 配置文件路径
	 * @return 配置内容
	 */
	private String loadConfigContent(String configPath) {
		try {
			ClassPathResource resource = new ClassPathResource(configPath);
			if (!resource.exists()) {
				log.warn("配置文件不存在: {}", configPath);
				return "";
			}

			byte[] bytes = resource.getInputStream().readAllBytes();
			String content = new String(bytes, StandardCharsets.UTF_8);

			log.debug("成功加载配置文件: {}", configPath);
			return content.trim();

		}
		catch (IOException e) {
			log.error("加载配置文件失败: {}", configPath, e);
			return "";
		}
	}

	/**
	 * 清空缓存
	 */
	public void clearCache() {
		cache.clear();
		log.info("StartupAgent配置缓存已清空");
	}

	/**
	 * 获取缓存大小
	 * @return 缓存条目数量
	 */
	public int getCacheSize() {
		return cache.size();
	}

	/**
	 * 加载agent配置信息
	 * @param agentName agent名称
	 * @return agent配置
	 */
	public AgentConfig loadAgentConfig(String agentName) {
		String configPath = CONFIG_BASE_PATH + agentName.toLowerCase() + "/agent-config.yml";
		String configContent = loadConfigContent(configPath);

		if (configContent.isEmpty()) {
			log.warn("Agent配置文件不存在或为空: {}", configPath);
			return null;
		}

		try {
			Yaml yaml = new Yaml();
			Map<String, Object> yamlData = yaml.load(configContent);

			if (yamlData == null) {
				log.warn("YAML配置文件解析结果为空: {}", configPath);
				return null;
			}

			AgentConfig config = new AgentConfig();
			config.setAgentName((String) yamlData.getOrDefault("agentName", agentName));
			config.setAgentDescription((String) yamlData.getOrDefault("agentDescription", ""));
			config.setNextStepPrompt((String) yamlData.getOrDefault("nextStepPrompt", ""));

			// 处理工具列表
			Object toolKeysObj = yamlData.get("availableToolKeys");
			if (toolKeysObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> toolKeyList = (List<String>) toolKeysObj;
				config.setAvailableToolKeys(toolKeyList);
			}

			return config;
		}
		catch (Exception e) {
			log.error("解析Agent YAML配置文件失败: {}", configPath, e);
			return null;
		}
	}

	/**
	 * 扫描所有可用的startup agent配置目录
	 * @return agent目录名称列表
	 */
	public List<String> scanAvailableAgents() {
		try {
			ClassPathResource baseResource = new ClassPathResource(CONFIG_BASE_PATH);
			if (!baseResource.exists()) {
				log.warn("StartupAgent配置基础目录不存在: {}", CONFIG_BASE_PATH);
				return List.of();
			}

			// 这里简化实现，直接返回已知的agent列表
			// 实际项目中可以通过扫描文件系统来动态发现
			return Arrays.asList("default_agent", "text_file_agent", "browser_agent");
		}
		catch (Exception e) {
			log.error("扫描Agent配置目录失败", e);
			return List.of();
		}
	}

	/**
	 * Agent配置类
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
