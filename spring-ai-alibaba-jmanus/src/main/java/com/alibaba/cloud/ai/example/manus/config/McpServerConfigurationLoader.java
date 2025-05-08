
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
package com.alibaba.cloud.ai.example.manus.config;

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

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		logger.info("开始处理 MCP 服务器配置...");
		String enabled = environment.getProperty("spring.ai.mcp.enabled", "true");
		logger.info("MCP 配置状态: enabled={}", enabled);

		if (!Boolean.parseBoolean(enabled)) {
			logger.info("MCP 配置已禁用，跳过处理");
			return;
		}

		try {
			// 读取原始配置文件
			Resource resource = new ClassPathResource(CONFIG_FILE);
			logger.info("加载配置文件: {}", resource.getURL());
			ObjectMapper objectMapper = new ObjectMapper();

			// 创建临时目录存放处理后的配置
			Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
			tempDir.toFile().deleteOnExit();
			Path tempConfigPath = tempDir.resolve(CONFIG_FILE);
			logger.info("创建临时配置文件: {}", tempConfigPath);

			// 读取并处理配置
			try (InputStream is = resource.getInputStream()) {
				JsonNode rootNode = objectMapper.readTree(is);
				logger.debug("原始配置内容: {}", objectMapper.writeValueAsString(rootNode));

				if (rootNode.has("mcpServers")) {
					ObjectNode mcpServers = (ObjectNode) rootNode.get("mcpServers");
					mcpServers.fields().forEachRemaining(entry -> {
						String serverName = entry.getKey();
						ObjectNode serverConfig = (ObjectNode) entry.getValue();
						logger.info("处理服务器配置: {}", serverName);

						if (serverConfig.has("command") && "npx".equals(serverConfig.get("command").asText())) {
							String npxCommand = isWindows() ? WINDOWS_NPX : UNIX_NPX;
							serverConfig.put("command", npxCommand);
							logger.info("更新服务器 {} 的命令: {} -> {}", serverName, "npx", npxCommand);
						}
					});

					// 将处理后的配置写入临时文件
					objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempConfigPath.toFile(), rootNode);
					logger.debug("处理后的配置内容: {}", objectMapper.writeValueAsString(rootNode));

					// 设置系统属性，指向新的配置文件位置
					String newConfigPath = "file:" + tempConfigPath.toAbsolutePath();
					System.setProperty("spring.ai.mcp.client.stdio.servers-configuration", newConfigPath);
					logger.info("已更新配置文件路径: {}", newConfigPath);

					logger.info("MCP 服务器配置处理完成，当前操作系统: {}", getOsType());
				}
				else {
					logger.warn("配置文件中未找到 mcpServers 节点");
				}
			}
		}
		catch (IOException e) {
			logger.error("处理 MCP 服务器配置时发生错误", e);
			logger.error("错误详情: ", e);
		}
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		logger.debug("当前操作系统: {}", os);
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
