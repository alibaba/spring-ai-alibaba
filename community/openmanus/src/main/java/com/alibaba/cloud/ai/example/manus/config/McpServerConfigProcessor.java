package com.alibaba.cloud.ai.example.manus.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(value = "spring.ai.mcp.enabled", havingValue = "true", matchIfMissing = true)
public class McpServerConfigProcessor implements BeanPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfigProcessor.class);

	private static final String WINDOWS_NPX = "npx.cmd";

	private static final String UNIX_NPX = "npx";

	private static final String CONFIG_FILE = "mcp-servers-config.json";

	private final ObjectMapper objectMapper;

	private final String osName;

	private final Path configPath;

	public McpServerConfigProcessor() {
		this.objectMapper = new ObjectMapper();
		this.osName = System.getProperty("os.name").toLowerCase();
		this.configPath = Paths.get("src", "main", "resources", CONFIG_FILE);
		processConfig();
	}

	private void processConfig() {
		try {
			// 读取配置文件
			ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
			JsonNode rootNode = objectMapper.readTree(resource.getInputStream());

			if (rootNode.has("mcpServers")) {
				ObjectNode mcpServers = (ObjectNode) rootNode.get("mcpServers");
				mcpServers.fields().forEachRemaining(entry -> {
					ObjectNode serverConfig = (ObjectNode) entry.getValue();
					if (serverConfig.has("command") && "npx".equals(serverConfig.get("command").asText())) {
						// 根据操作系统调整 npx 命令
						String npxCommand = isWindows() ? WINDOWS_NPX : UNIX_NPX;
						serverConfig.put("command", npxCommand);
					}
				});

				// 保存修改后的配置
				objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), rootNode);

				logger.info("MCP server configuration updated for {} platform", getOsType());
			}
		}
		catch (IOException e) {
			logger.error("Error processing MCP server configuration", e);
		}
	}

	private boolean isWindows() {
		return osName.contains("win");
	}

	private String getOsType() {
		if (isWindows()) {
			return "Windows";
		}
		else if (osName.contains("mac")) {
			return "MacOS";
		}
		else {
			return "Linux";
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
