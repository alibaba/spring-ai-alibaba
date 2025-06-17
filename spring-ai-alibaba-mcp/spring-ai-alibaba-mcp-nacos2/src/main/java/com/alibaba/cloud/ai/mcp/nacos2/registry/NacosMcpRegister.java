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

package com.alibaba.cloud.ai.mcp.nacos2.registry;

import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpToolsInfo;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.RemoteServerConfigInfo;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.ServiceRefInfo;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.ToolMetaInfo;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpNacosConstant;
import com.alibaba.cloud.ai.mcp.nacos2.registry.utils.JsonUtils;
import com.alibaba.cloud.ai.mcp.nacos2.registry.utils.MD5Utils;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.utils.StringUtils;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Sunrisea
 */
public class NacosMcpRegister implements ApplicationListener<WebServerInitializedEvent> {

	private static final Logger log = LoggerFactory.getLogger(NacosMcpRegister.class);

	private String type;

	private NacosMcpRegistryProperties nacosMcpRegistryProperties;

	private NacosMcpProperties nacosMcpProperties;

	private McpSchema.Implementation serverInfo;

	private McpAsyncServer mcpAsyncServer;

	private CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools;

	private Map<String, ToolMetaInfo> toolsMeta;

	private McpSchema.ServerCapabilities serverCapabilities;

	private ConfigService configService;

	private final Long TIME_OUT_MS = 3000L;

	public NacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, String type) {
		this.mcpAsyncServer = mcpAsyncServer;
		log.info("Mcp server type: {}", type);
		this.type = type;
		this.nacosMcpProperties = nacosMcpProperties;
		this.nacosMcpRegistryProperties = nacosMcpRegistryProperties;

		try {
			this.serverInfo = mcpAsyncServer.getServerInfo();
			this.serverCapabilities = mcpAsyncServer.getServerCapabilities();

			Field toolsField = McpAsyncServer.class.getDeclaredField("tools");
			toolsField.setAccessible(true);
			this.tools = (CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification>) toolsField
				.get(mcpAsyncServer);

			this.toolsMeta = new HashMap<>();
			this.tools.forEach(toolRegistration -> {
				ToolMetaInfo toolMetaInfo = new ToolMetaInfo();
				this.toolsMeta.put(toolRegistration.tool().name(), toolMetaInfo);
			});

			Properties configProperties = nacosMcpProperties.getNacosProperties();
			this.configService = new NacosConfigService(configProperties);
			if (this.serverCapabilities.tools() != null) {
				String toolsInNacosContent = this.configService.getConfig(
						this.serverInfo.name() + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP,
						TIME_OUT_MS);
				if (toolsInNacosContent != null) {
					updateTools(toolsInNacosContent);
				}
				List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream()
					.map(McpServerFeatures.AsyncToolSpecification::tool)
					.toList();
				McpToolsInfo mcpToolsInfo = new McpToolsInfo();
				mcpToolsInfo.setTools(toolsNeedtoRegister);
				mcpToolsInfo.setToolsMeta(this.toolsMeta);
				String toolsConfigContent = JsonUtils.serialize(mcpToolsInfo);
				boolean isPublishSuccess = this.configService.publishConfig(
						this.serverInfo.name() + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP,
						toolsConfigContent);
				if (!isPublishSuccess) {
					log.error("Publish tools config to nacos failed.");
					throw new Exception("Publish tools config to nacos failed.");
				}
				this.configService.addListener(this.serverInfo.name() + McpNacosConstant.TOOLS_CONFIG_SUFFIX,
						McpNacosConstant.TOOLS_GROUP, new Listener() {
							@Override
							public void receiveConfigInfo(String configInfo) {
								updateTools(configInfo);
							}

							@Override
							public Executor getExecutor() {
								return null;
							}
						});
			}

			String serverInfoContent = this.configService.getConfig(
					this.serverInfo.name() + McpNacosConstant.SERVER_CONFIG_SUFFIX, McpNacosConstant.SERVER_GROUP,
					3000);
			String serverDescription = this.serverInfo.name();
			if (serverInfoContent != null) {
				Map<String, Object> serverInfoMap = JsonUtils.deserialize(serverInfoContent, Map.class);
				if (serverInfoMap.containsKey("description")) {
					serverDescription = (String) serverInfoMap.get("description");
				}
			}

			McpServerInfo mcpServerInfo = new McpServerInfo();
			mcpServerInfo.setName(this.serverInfo.name());
			mcpServerInfo.setVersion(this.serverInfo.version());
			mcpServerInfo.setDescription(serverDescription);
			mcpServerInfo.setEnabled(true);
			if ("stdio".equals(this.type)) {
				mcpServerInfo.setProtocol("local");
			}
			else {
				ServiceRefInfo serviceRefInfo = new ServiceRefInfo();
				serviceRefInfo.setNamespaceId(nacosMcpProperties.getNamespace());
				serviceRefInfo.setServiceName(this.serverInfo.name() + McpNacosConstant.SERVER_NAME_SUFFIX);
				serviceRefInfo.setGroupName(nacosMcpRegistryProperties.getServiceGroup());
				RemoteServerConfigInfo remoteServerConfigInfo = new RemoteServerConfigInfo();
				remoteServerConfigInfo.setServiceRef(serviceRefInfo);
				String contextPath = nacosMcpRegistryProperties.getSseExportContextPath();
				if (StringUtils.isBlank(contextPath)) {
					contextPath = "";
				}
				remoteServerConfigInfo.setExportPath(contextPath + "/sse");
				mcpServerInfo.setRemoteServerConfig(remoteServerConfigInfo);
				mcpServerInfo.setProtocol("mcp-sse");
			}
			if (this.serverCapabilities.tools() != null) {
				mcpServerInfo.setToolsDescriptionRef(this.serverInfo.name() + McpNacosConstant.TOOLS_CONFIG_SUFFIX);
			}
			boolean isPublishSuccess = this.configService.publishConfig(
					this.serverInfo.name() + McpNacosConstant.SERVER_CONFIG_SUFFIX, McpNacosConstant.SERVER_GROUP,
					JsonUtils.serialize(mcpServerInfo));
			if (!isPublishSuccess) {
				log.error("Publish mcp server info to nacos failed.");
				throw new Exception("Publish mcp server info to nacos failed.");
			}
			log.info("Register mcp server info to nacos successfully");
		}
		catch (Exception e) {
			log.error("Failed to register mcp server to nacos", e);
		}
	}

	private void updateToolDescription(McpServerFeatures.AsyncToolSpecification localToolRegistration,
			McpSchema.Tool toolInNacos, List<McpServerFeatures.AsyncToolSpecification> toolsRegistrationNeedToUpdate)
			throws JsonProcessingException {
		Boolean changed = false;
		if (localToolRegistration.tool().description() != null
				&& !localToolRegistration.tool().description().equals(toolInNacos.description())) {
			changed = true;
		}
		String localInputSchemaString = JsonUtils.serialize(localToolRegistration.tool().inputSchema());
		Map<String, Object> localInputSchemaMap = JsonUtils.deserialize(localInputSchemaString, Map.class);
		Map<String, Object> localProperties = (Map<String, Object>) localInputSchemaMap.get("properties");

		String nacosInputSchemaString = JsonUtils.serialize(toolInNacos.inputSchema());
		Map<Object, Object> nacosInputSchemaMap = JsonUtils.deserialize(nacosInputSchemaString, Map.class);
		Map<String, Object> nacosProperties = (Map<String, Object>) nacosInputSchemaMap.get("properties");

		for (String key : localProperties.keySet()) {
			if (nacosProperties.containsKey(key)) {
				Map<String, Object> localProperty = (Map<String, Object>) localProperties.get(key);
				Map<String, Object> nacosProperty = (Map<String, Object>) nacosProperties.get(key);
				String localDescription = (String) localProperty.get("description");
				String nacosDescription = (String) nacosProperty.get("description");
				if (nacosDescription != null && !nacosDescription.equals(localDescription)) {
					localProperty.put("description", nacosDescription);
					changed = true;
				}
			}
		}

		if (changed) {
			McpSchema.Tool toolNeededUpdate = new McpSchema.Tool(localToolRegistration.tool().name(),
					toolInNacos.description(), JsonUtils.serialize(localInputSchemaMap));
			toolsRegistrationNeedToUpdate
				.add(new McpServerFeatures.AsyncToolSpecification(toolNeededUpdate, localToolRegistration.call()));
		}

	}

	private void updateTools(String toolsInNacosContent) {
		try {
			boolean changed = false;
			McpToolsInfo toolsInfo = JsonUtils.deserialize(toolsInNacosContent, McpToolsInfo.class);
			List<McpSchema.Tool> toolsInNacos = toolsInfo.getTools();
			if (!this.toolsMeta.equals(toolsInfo.getToolsMeta())) {
				changed = true;
				this.toolsMeta = toolsInfo.getToolsMeta();
			}
			List<McpServerFeatures.AsyncToolSpecification> toolsRegistrationNeedToUpdate = new ArrayList<>();
			Map<String, McpSchema.Tool> toolsInNacosMap = toolsInNacos.stream()
				.collect(Collectors.toMap(McpSchema.Tool::name, tool -> tool));
			for (McpServerFeatures.AsyncToolSpecification toolRegistration : this.tools) {
				String name = toolRegistration.tool().name();
				if (!toolsInNacosMap.containsKey(name)) {
					continue;
				}
				McpSchema.Tool toolInNacos = toolsInNacosMap.get(name);
				updateToolDescription(toolRegistration, toolInNacos, toolsRegistrationNeedToUpdate);
				break;
			}
			for (McpServerFeatures.AsyncToolSpecification toolRegistration : toolsRegistrationNeedToUpdate) {
				for (int i = 0; i < this.tools.size(); i++) {
					if (this.tools.get(i).tool().name().equals(toolRegistration.tool().name())) {
						this.tools.set(i, toolRegistration);
						changed = true;
						break;
					}
				}
			}
			if (changed) {
				log.info("tools description updated");
			}
			if (changed && this.serverCapabilities.tools().listChanged()) {
				this.mcpAsyncServer.notifyToolsListChanged().block();
			}
		}
		catch (Exception e) {
			log.error("Failed to update tools according to nacos", e);
		}
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if ("stdio".equals(this.type) || !nacosMcpRegistryProperties.isServiceRegister()) {
			log.info("No need to register mcp server service to nacos");
			return;
		}
		try {
			int port = event.getWebServer().getPort();
			Properties nacosProperties = nacosMcpProperties.getNacosProperties();
			NamingService namingService = new NacosNamingService(nacosProperties);
			Instance instance = new Instance();

			Map<String, String> metadata = new HashMap();

			// 配置Mcp Server信息的MD5
			String content = configService.getConfig(this.serverInfo.name() + McpNacosConstant.SERVER_CONFIG_SUFFIX,
					McpNacosConstant.SERVER_GROUP, TIME_OUT_MS);
			if (content == null || content.isEmpty()) {
				throw new RuntimeException("Config content is empty for dataId: " + this.serverInfo.name()
						+ McpNacosConstant.SERVER_CONFIG_SUFFIX);
			}

			// 计算 MD5
			MD5Utils md5Utils = new MD5Utils();
			metadata.put("server.md5", md5Utils.getMd5(content));
			// 配置对应的工具信息
			String toolConfig = configService.getConfig(this.serverInfo.name() + McpNacosConstant.TOOLS_CONFIG_SUFFIX,
					McpNacosConstant.TOOLS_GROUP, TIME_OUT_MS);
			McpToolsInfo toolsInfo = null;
			toolsInfo = JsonUtils.deserialize(toolConfig, McpToolsInfo.class);

			List<String> toolNames = toolsInfo.getTools()
				.stream()
				.map(McpSchema.Tool::name)
				.collect(Collectors.toList());
			metadata.put("tools.names", String.join(",", toolNames));

			instance.setIp(nacosMcpProperties.getIp());
			instance.setPort(port);
			instance.setEphemeral(nacosMcpRegistryProperties.isServiceEphemeral());
			instance.setMetadata(metadata);
			namingService.registerInstance(this.serverInfo.name() + McpNacosConstant.SERVER_NAME_SUFFIX,
					nacosMcpRegistryProperties.getServiceGroup(), instance);
			log.info("Register mcp server service to nacos successfully");
		}
		catch (NacosException e) {
			log.error("Failed to register mcp server service to nacos", e);
		}
		catch (JsonProcessingException e) {
			log.error("parse tools failed", e);
		}
	}

}
