package com.alibaba.cloud.ai.example.manus.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class McpServerConfigurationLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfigurationLoader.class);

	private static final String WINDOWS_NPX = "npx.cmd";

	private static final String UNIX_NPX = "npx";

	private static final String CONFIG_FILE = "mcp-servers-config.json";

	private static final String TEMP_DIR_PREFIX = "mcp-config";

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		if (!Boolean.parseBoolean(environment.getProperty("spring.ai.mcp.enabled", "true"))) {
			return;
		}

		try {
			// 读取原始配置文件
			Resource resource = new ClassPathResource(CONFIG_FILE);
			ObjectMapper objectMapper = new ObjectMapper();

			// 创建临时目录存放处理后的配置
			Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
			tempDir.toFile().deleteOnExit();

			Path tempConfigPath = tempDir.resolve(CONFIG_FILE);

			// 读取并处理配置
			try (InputStream is = resource.getInputStream()) {
				JsonNode rootNode = objectMapper.readTree(is);

				if (rootNode.has("mcpServers")) {
					ObjectNode mcpServers = (ObjectNode) rootNode.get("mcpServers");
					mcpServers.fields().forEachRemaining(entry -> {
						ObjectNode serverConfig = (ObjectNode) entry.getValue();
						if (serverConfig.has("command") && "npx".equals(serverConfig.get("command").asText())) {
							String npxCommand = isWindows() ? WINDOWS_NPX : UNIX_NPX;
							serverConfig.put("command", npxCommand);
						}
					});

					// 将处理后的配置写入临时文件
					objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempConfigPath.toFile(), rootNode);

					// 设置系统属性，指向新的配置文件位置
					System.setProperty("spring.ai.mcp.client.stdio.servers-configuration",
							"file:" + tempConfigPath.toAbsolutePath());

					logger.info("MCP server configuration processed for {} platform", getOsType());
				}
			}
		}
		catch (IOException e) {
			logger.error("Error processing MCP server configuration", e);
		}
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
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
