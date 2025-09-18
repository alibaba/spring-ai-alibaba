
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
package com.alibaba.cloud.ai.manus.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class McpServerConfigurationLoader implements EnvironmentPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfigurationLoader.class);

	private static final String WINDOWS_NPX = "npx.cmd";

	private static final String UNIX_NPX = "npx";

	private static final String CONFIG_FILE = "mcp-servers-config.json";

	private static final String TEMP_DIR_PREFIX = "mcp-config";

	private final ObjectMapper objectMapper;

	public McpServerConfigurationLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		logger.info("Starting to process MCP server configuration...");
		String enabled = environment.getProperty("spring.ai.mcp.enabled", "true");
		logger.info("MCP configuration status: enabled={}", enabled);

		if (!Boolean.parseBoolean(enabled)) {
			logger.info("MCP configuration is disabled, skipping processing");
			return;
		}

		try {
			// Read the original configuration file
			Resource resource = new ClassPathResource(CONFIG_FILE);
			logger.info("Loading configuration file: {}", resource.getURL());
			// Create a temporary directory to store the processed configuration
			Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
			tempDir.toFile().deleteOnExit();
			Path tempConfigPath = tempDir.resolve(CONFIG_FILE);
			logger.info("Created temporary configuration file: {}", tempConfigPath);

			// Read and process the configuration
			try (InputStream is = resource.getInputStream()) {
				JsonNode rootNode = objectMapper.readTree(is);
				logger.debug("Original configuration content: {}", objectMapper.writeValueAsString(rootNode));

				if (rootNode.has("mcpServers")) {
					ObjectNode mcpServers = (ObjectNode) rootNode.get("mcpServers");
					mcpServers.fields().forEachRemaining(entry -> {
						String serverName = entry.getKey();
						ObjectNode serverConfig = (ObjectNode) entry.getValue();
						logger.info("Processing server configuration: {}", serverName);

						if (serverConfig.has("command") && "npx".equals(serverConfig.get("command").asText())) {
							String npxCommand = isWindows() ? WINDOWS_NPX : UNIX_NPX;
							serverConfig.put("command", npxCommand);
							logger.info("Updated server {} command: {} -> {}", serverName, "npx", npxCommand);
						}
					});

					// Write the processed configuration to the temporary file
					objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempConfigPath.toFile(), rootNode);
					logger.debug("Processed configuration content: {}", objectMapper.writeValueAsString(rootNode));

					// Set the system property to point to the new configuration file
					// location
					String newConfigPath = "file:" + tempConfigPath.toAbsolutePath();
					System.setProperty("spring.ai.mcp.client.stdio.servers-configuration", newConfigPath);
					logger.info("Updated configuration file path: {}", newConfigPath);

					logger.info("MCP server configuration processing completed, current operating system: {}",
							getOsType());
				}
				else {
					logger.warn("mcpServers node not found in configuration file");
				}
			}
		}
		catch (IOException e) {
			logger.error("Error processing MCP server configuration", e);
			logger.error("Error details: ", e);
		}
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		logger.debug("Current operating system: {}", os);
		return os.contains("win");
	}

	private String getOsType() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			return "Windows";
		}
		else if (osName.contains("mac")) {
			return "MacOS";
		}
		else {
			return "Linux";
		}
	}

}
