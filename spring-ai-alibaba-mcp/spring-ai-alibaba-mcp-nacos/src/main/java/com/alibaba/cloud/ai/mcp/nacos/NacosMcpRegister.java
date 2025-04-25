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

package com.alibaba.cloud.ai.mcp.nacos;

import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.McpToolsInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.RemoteServerConfigInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.ServiceRefInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.ToolMetaInfo;
import com.alibaba.cloud.ai.mcp.nacos.utils.JsonUtils;
import com.alibaba.nacos.api.PropertyKeyConst;
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
import io.modelcontextprotocol.spec.DefaultMcpSession;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Sunrisea
 */
public class NacosMcpRegister implements ApplicationListener<WebServerInitializedEvent> {

	private static final Logger log = LoggerFactory.getLogger(NacosMcpRegister.class);

	private final String toolsGroup = "mcp-tools";

	private final String toolsConfigSuffix = "-mcp-tools.json";

	private final String configNamespace = "nacos-default-mcp";

	private final String serverGroup = "mcp-server";

	private String type;

	private NacosMcpRegistryProperties nacosMcpProperties;

	private McpSchema.Implementation serverInfo;

	private McpAsyncServer mcpAsyncServer;

	private CopyOnWriteArrayList<McpServerFeatures.AsyncToolRegistration> tools;

	private Map<String, ToolMetaInfo> toolsMeta;

	private McpSchema.ServerCapabilities serverCapabilities;

	private ConfigService configService;

	public NacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpRegistryProperties nacosMcpProperties, String type) {
		this.mcpAsyncServer = mcpAsyncServer;
		log.info("Mcp server type: " + type);
		this.type = type;
		this.nacosMcpProperties = nacosMcpProperties;

		try {
			Class clazz = McpAsyncServer.class;

			Field serverInfoField = clazz.getDeclaredField("serverInfo");
			serverInfoField.setAccessible(true);
			this.serverInfo = (McpSchema.Implementation) serverInfoField.get(mcpAsyncServer);

			Field serverCapabilitiesField = clazz.getDeclaredField("serverCapabilities");
			serverCapabilitiesField.setAccessible(true);
			this.serverCapabilities = (McpSchema.ServerCapabilities) serverCapabilitiesField.get(mcpAsyncServer);

			Field toolsField = clazz.getDeclaredField("tools");
			toolsField.setAccessible(true);
			this.tools = (CopyOnWriteArrayList<McpServerFeatures.AsyncToolRegistration>) toolsField.get(mcpAsyncServer);

			this.toolsMeta = new HashMap<>();
			this.tools.forEach(toolRegistration -> {
				ToolMetaInfo toolMetaInfo = new ToolMetaInfo();
				this.toolsMeta.put(toolRegistration.tool().name(), toolMetaInfo);
			});

			Properties configProperties = nacosMcpProperties.getNacosProperties();
			configProperties.put(PropertyKeyConst.NAMESPACE, configNamespace);
			this.configService = new NacosConfigService(configProperties);
			if (this.serverCapabilities.tools() != null) {
				Field mcpSessionField = clazz.getDeclaredField("mcpSession");
				mcpSessionField.setAccessible(true);
				DefaultMcpSession mcpSession = (DefaultMcpSession) mcpSessionField.get(mcpAsyncServer);
				Field requestHandlersField = DefaultMcpSession.class.getDeclaredField("requestHandlers");
				requestHandlersField.setAccessible(true);
				ConcurrentHashMap<String, DefaultMcpSession.RequestHandler<?>> requestHandlers = (ConcurrentHashMap<String, DefaultMcpSession.RequestHandler<?>>) requestHandlersField
					.get(mcpSession);
				requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());

				String toolsInNacosContent = this.configService.getConfig(this.serverInfo.name() + toolsConfigSuffix,
						toolsGroup, 3000);
				if (toolsInNacosContent != null) {
					updateTools(toolsInNacosContent);
				}
				List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream()
					.map(McpServerFeatures.AsyncToolRegistration::tool)
					.toList();
				McpToolsInfo mcpToolsInfo = new McpToolsInfo();
				mcpToolsInfo.setTools(toolsNeedtoRegister);
				mcpToolsInfo.setToolsMeta(this.toolsMeta);
				String toolsConfigContent = JsonUtils.serialize(mcpToolsInfo);
				boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + toolsConfigSuffix,
						toolsGroup, toolsConfigContent);
				if (!isPublishSuccess) {
					log.error("Publish tools config to nacos failed.");
					throw new Exception("Publish tools config to nacos failed.");
				}
				this.configService.addListener(this.serverInfo.name() + toolsConfigSuffix, toolsGroup, new Listener() {
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

			McpServerInfo mcpServerInfo = new McpServerInfo();
			mcpServerInfo.setName(this.serverInfo.name());
			mcpServerInfo.setVersion(this.serverInfo.version());
			mcpServerInfo.setEnabled(true);
			if ("stdio".equals(this.type)) {
				mcpServerInfo.setProtocol("local");
			}
			else {
				ServiceRefInfo serviceRefInfo = new ServiceRefInfo();
				serviceRefInfo.setNamespaceId(nacosMcpProperties.getServiceNamespace());
				serviceRefInfo.setServiceName(this.serverInfo.name() + "-mcp-service");
				serviceRefInfo.setGroupName(nacosMcpProperties.getServiceGroup());
				RemoteServerConfigInfo remoteServerConfigInfo = new RemoteServerConfigInfo();
				remoteServerConfigInfo.setServiceRef(serviceRefInfo);
				String contextPath = nacosMcpProperties.getSseExportContextPath();
				if (StringUtils.isBlank(contextPath)) {
					contextPath = "";
				}
				remoteServerConfigInfo.setExportPath(contextPath + "/sse");
				mcpServerInfo.setRemoteServerConfig(remoteServerConfigInfo);
				mcpServerInfo.setProtocol("mcp-sse");
			}
			if (this.serverCapabilities.tools() != null) {
				mcpServerInfo.setToolsDescriptionRef(this.serverInfo.name() + toolsConfigSuffix);
			}
			boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + "-mcp-server.json",
					serverGroup, JsonUtils.serialize(mcpServerInfo));
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

	private void updateToolDescription(McpServerFeatures.AsyncToolRegistration localToolRegistration,
			McpSchema.Tool toolInNacos, List<McpServerFeatures.AsyncToolRegistration> toolsRegistrationNeedToUpdate)
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
				.add(new McpServerFeatures.AsyncToolRegistration(toolNeededUpdate, localToolRegistration.call()));
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
			List<McpServerFeatures.AsyncToolRegistration> toolsRegistrationNeedToUpdate = new ArrayList<>();
			Map<String, McpSchema.Tool> toolsInNacosMap = toolsInNacos.stream()
				.collect(Collectors.toMap(McpSchema.Tool::name, tool -> tool));
			for (McpServerFeatures.AsyncToolRegistration toolRegistration : this.tools) {
				String name = toolRegistration.tool().name();
				if (!toolsInNacosMap.containsKey(name)) {
					continue;
				}
				McpSchema.Tool toolInNacos = toolsInNacosMap.get(name);
				updateToolDescription(toolRegistration, toolInNacos, toolsRegistrationNeedToUpdate);
				break;
			}
			for (McpServerFeatures.AsyncToolRegistration toolRegistration : toolsRegistrationNeedToUpdate) {
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
		if ("stdio".equals(this.type) || !nacosMcpProperties.isServiceRegister()) {
			log.info("No need to register mcp server service to nacos");
			return;
		}
		try {
			int port = event.getWebServer().getPort();
			NamingService namingService = new NacosNamingService(nacosMcpProperties.getNacosProperties());
			Instance instance = new Instance();
			instance.setIp(nacosMcpProperties.getIp());
			instance.setPort(port);
			instance.setEphemeral(nacosMcpProperties.isServiceEphemeral());
			namingService.registerInstance(this.serverInfo.name() + "-mcp-service",
					nacosMcpProperties.getServiceGroup(), instance);
			log.info("Register mcp server service to nacos successfully");
		}
		catch (NacosException e) {
			log.error("Failed to register mcp server service to nacos", e);
		}
	}

	private DefaultMcpSession.RequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
		return params -> {
			List<McpSchema.Tool> toolsAll = this.tools.stream()
				.map(McpServerFeatures.AsyncToolRegistration::tool)
				.toList();
			List<McpSchema.Tool> toolsEnable = new ArrayList<>();
			for (McpSchema.Tool tool : toolsAll) {
				if (this.toolsMeta.containsKey(tool.name()) && this.toolsMeta.get(tool.name()).getEnabled()) {
					toolsEnable.add(tool);
				}
			}
			return Mono.just(new McpSchema.ListToolsResult(toolsEnable, null));
		};
	}

}
