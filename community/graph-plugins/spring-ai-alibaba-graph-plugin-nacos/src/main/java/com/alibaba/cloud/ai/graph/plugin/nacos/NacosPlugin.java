/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.plugin.nacos;

import com.alibaba.cloud.ai.graph.plugin.GraphPlugin;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Nacos configuration management plugin for Spring AI Alibaba Graph. Provides
 * functionality to read, write, and monitor Nacos configurations.
 */
public class NacosPlugin implements GraphPlugin {

	private static final Logger logger = LoggerFactory.getLogger(NacosPlugin.class);

	private static final String DEFAULT_SERVER_ADDR = "127.0.0.1:8848";

	private static final String DEFAULT_NAMESPACE = "public";

	private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

	private final ConfigService configService;

	private final ObjectMapper objectMapper;

	public NacosPlugin() {
		this(createDefaultProperties());
	}

	public NacosPlugin(Properties properties) {
		try {
			this.configService = NacosFactory.createConfigService(properties);
			this.objectMapper = new ObjectMapper();
			logger.info("NacosPlugin initialized with properties: {}", properties);
		}
		catch (NacosException e) {
			logger.error("Failed to initialize NacosPlugin:", e);
			throw new RuntimeException("Failed to initialize NacosPlugin: " + e.getMessage(), e);
		}
	}

	private static Properties createDefaultProperties() {
		Properties properties = new Properties();
		properties.put("serverAddr",
				System.getenv("NACOS_SERVER_ADDR") != null ? System.getenv("NACOS_SERVER_ADDR") : DEFAULT_SERVER_ADDR);
		properties.put("namespace",
				System.getenv("NACOS_NAMESPACE") != null ? System.getenv("NACOS_NAMESPACE") : DEFAULT_NAMESPACE);

		// Optional authentication
		String username = System.getenv("NACOS_USERNAME");
		String password = System.getenv("NACOS_PASSWORD");
		if (username != null && password != null) {
			properties.put("username", username);
			properties.put("password", password);
		}

		return properties;
	}

	@Override
	public String getId() {
		return "nacos";
	}

	@Override
	public String getName() {
		return "Nacos Configuration Manager";
	}

	@Override
	public String getDescription() {
		return "Manage configurations in Nacos registry, including reading, writing, and monitoring configuration changes";
	}

	@Override
	public Map<String, Object> getInputSchema() {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");
		schema.put("required", new String[] { "operation", "dataId" });

		Map<String, Object> properties = new HashMap<>();

		// Operation type
		Map<String, Object> operation = new HashMap<>();
		operation.put("type", "string");
		operation.put("enum", new String[] { "get", "publish", "remove", "listen" });
		operation.put("description",
				"Operation type: get (read config), publish (write config), remove (delete config), listen (monitor config changes)");
		properties.put("operation", operation);

		// Data ID
		Map<String, Object> dataId = new HashMap<>();
		dataId.put("type", "string");
		dataId.put("description", "Configuration data ID");
		properties.put("dataId", dataId);

		// Group (optional)
		Map<String, Object> group = new HashMap<>();
		group.put("type", "string");
		group.put("description", "Configuration group (default: DEFAULT_GROUP)");
		properties.put("group", group);

		// Content (for publish operation)
		Map<String, Object> content = new HashMap<>();
		content.put("type", "string");
		content.put("description", "Configuration content (required for publish operation)");
		properties.put("content", content);

		// Type (for publish operation)
		Map<String, Object> type = new HashMap<>();
		type.put("type", "string");
		type.put("description", "Configuration type: text, json, yaml, properties, xml");
		properties.put("type", type);

		schema.put("properties", properties);
		return schema;
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> params) throws Exception {
		String operation = (String) params.get("operation");
		String dataId = (String) params.get("dataId");
		String group = params.get("group") != null ? (String) params.get("group") : DEFAULT_GROUP;

		if (operation == null || dataId == null) {
			throw new IllegalArgumentException("Operation and dataId parameters are required");
		}

		switch (operation.toLowerCase()) {
			case "get":
				return handleGetConfig(dataId, group);
			case "publish":
				return handlePublishConfig(dataId, group, params);
			case "remove":
				return handleRemoveConfig(dataId, group);
			case "listen":
				return handleListenConfig(dataId, group);
			default:
				throw new IllegalArgumentException(
						"Unsupported operation: " + operation + ". Supported operations: get, publish, remove, listen");
		}
	}

	private Map<String, Object> handleGetConfig(String dataId, String group) throws Exception {
		try {
			String content = configService.getConfig(dataId, group, 5000);

			Map<String, Object> result = new HashMap<>();
			result.put("operation", "get");
			result.put("dataId", dataId);
			result.put("group", group);
			result.put("content", content);
			result.put("exists", content != null);

			if (content != null) {
				result.put("length", content.length());
				result.put("type", detectConfigType(content));
			}

			logger.info("Successfully retrieved config: dataId={}, group={}, exists={}", dataId, group,
					content != null);

			return result;
		}
		catch (NacosException e) {
			logger.error("Failed to get config: dataId={}, group={}, error={}", dataId, group, e.getMessage());
			throw new RuntimeException("Failed to retrieve configuration: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> handlePublishConfig(String dataId, String group, Map<String, Object> params)
			throws Exception {
		String content = (String) params.get("content");
		String type = (String) params.get("type");

		if (content == null) {
			throw new IllegalArgumentException("Content parameter is required for publish operation");
		}

		try {
			boolean success = configService.publishConfig(dataId, group, content, type);

			Map<String, Object> result = new HashMap<>();
			result.put("operation", "publish");
			result.put("dataId", dataId);
			result.put("group", group);
			result.put("content", content);
			result.put("type", type);
			result.put("success", success);
			result.put("length", content.length());

			if (success) {
				logger.info("Successfully published config: dataId={}, group={}, type={}, length={}", dataId, group,
						type, content.length());
			}
			else {
				logger.warn("Failed to publish config: dataId={}, group={}", dataId, group);
			}

			return result;
		}
		catch (NacosException e) {
			logger.error("Failed to publish config: dataId={}, group={}, error={}", dataId, group, e.getMessage());
			throw new RuntimeException("Failed to publish configuration: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> handleRemoveConfig(String dataId, String group) throws Exception {
		try {
			boolean success = configService.removeConfig(dataId, group);

			Map<String, Object> result = new HashMap<>();
			result.put("operation", "remove");
			result.put("dataId", dataId);
			result.put("group", group);
			result.put("success", success);

			if (success) {
				logger.info("Successfully removed config: dataId={}, group={}", dataId, group);
			}
			else {
				logger.warn("Failed to remove config: dataId={}, group={}", dataId, group);
			}

			return result;
		}
		catch (NacosException e) {
			logger.error("Failed to remove config: dataId={}, group={}, error={}", dataId, group, e.getMessage());
			throw new RuntimeException("Failed to remove configuration: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> handleListenConfig(String dataId, String group) throws Exception {
		try {
			// Add a listener for configuration changes
			configService.addListener(dataId, group, new Listener() {
				@Override
				public Executor getExecutor() {
					return null; // Use default executor
				}

				@Override
				public void receiveConfigInfo(String configInfo) {
					logger.info("Configuration changed: dataId={}, group={}, content={}", dataId, group, configInfo);
				}
			});

			Map<String, Object> result = new HashMap<>();
			result.put("operation", "listen");
			result.put("dataId", dataId);
			result.put("group", group);
			result.put("success", true);
			result.put("message", "Listener added successfully. Will monitor configuration changes.");

			logger.info("Successfully added listener for config: dataId={}, group={}", dataId, group);

			return result;
		}
		catch (NacosException e) {
			logger.error("Failed to add listener: dataId={}, group={}, error={}", dataId, group, e.getMessage());
			throw new RuntimeException("Failed to add configuration listener: " + e.getMessage(), e);
		}
	}

	private String detectConfigType(String content) {
		if (content == null || content.trim().isEmpty()) {
			return "text";
		}

		String trimmed = content.trim();
		if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
			return "json";
		}
		else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
			return "json";
		}
		else if (trimmed.contains("---") || trimmed.matches(".*:\\s*.*")) {
			return "yaml";
		}
		else if (trimmed.contains("=") && trimmed.matches(".*=.*")) {
			return "properties";
		}
		else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
			return "xml";
		}
		else {
			return "text";
		}
	}

}