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

package com.alibaba.cloud.ai.mcp.nacos.registry;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.registry.utils.JsonSchemaUtils;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointSpec;
import com.alibaba.nacos.api.ai.model.mcp.McpServerBasicInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpServiceRef;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.ai.model.mcp.registry.ServerVersionDetail;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.utils.StringUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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

	private Map<String, McpToolMeta> toolsMeta;

	private McpSchema.ServerCapabilities serverCapabilities;

	private McpServerProperties mcpServerProperties;

	private NacosMcpOperationService nacosMcpOperationService;

	public NacosMcpRegister(NacosMcpOperationService nacosMcpOperationService, McpAsyncServer mcpAsyncServer,
			NacosMcpProperties nacosMcpProperties, NacosMcpRegistryProperties nacosMcpRegistryProperties,
			McpServerProperties mcpServerProperties, String type) {
		this.mcpAsyncServer = mcpAsyncServer;
		log.info("Mcp server type: {}", type);
		this.type = type;
		this.nacosMcpProperties = nacosMcpProperties;
		this.nacosMcpRegistryProperties = nacosMcpRegistryProperties;
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.mcpServerProperties = mcpServerProperties;

		try {
			if (StringUtils.isBlank(this.mcpServerProperties.getVersion())) {
				throw new IllegalArgumentException("mcp server version is blank");
			}

			this.serverInfo = mcpAsyncServer.getServerInfo();
			this.serverCapabilities = mcpAsyncServer.getServerCapabilities();

			Field toolsField = McpAsyncServer.class.getDeclaredField("tools");
			toolsField.setAccessible(true);
			this.tools = (CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification>) toolsField
				.get(mcpAsyncServer);
			this.toolsMeta = new HashMap<>();

			McpServerDetailInfo serverDetailInfo = null;
			try {
				serverDetailInfo = this.nacosMcpOperationService.getServerDetail(this.serverInfo.name(),
						this.serverInfo.version());
			}
			catch (NacosException e) {
				log.info("can not found McpServer info from nacos,{}", this.serverInfo.name());
			}
			if (serverDetailInfo != null) {
				try {
					if (!checkCompatible(serverDetailInfo)) {
						throw new Exception("check mcp server compatible false");
					}
				}
				catch (Exception e) {
					log.error("check Tools compatible false", e);
					throw e;
				}
				if (this.serverCapabilities.tools() != null) {
					updateTools(serverDetailInfo);
				}
				subscribe();
				return;
			}

			McpToolSpecification mcpToolSpec = new McpToolSpecification();
			if (this.serverCapabilities.tools() != null) {
				List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream()
					.map(McpServerFeatures.AsyncToolSpecification::tool)
					.toList();
				String toolsStr = JacksonUtils.toJson(toolsNeedtoRegister);
				List<McpTool> toolsToNacosList = JacksonUtils.toObj(toolsStr, new TypeReference<>() {
				});
				mcpToolSpec.setTools(toolsToNacosList);
			}
			ServerVersionDetail serverVersionDetail = new ServerVersionDetail();
			serverVersionDetail.setVersion(this.serverInfo.version());
			McpServerBasicInfo serverBasicInfo = new McpServerBasicInfo();
			serverBasicInfo.setName(this.serverInfo.name());
			serverBasicInfo.setVersionDetail(serverVersionDetail);
			serverBasicInfo.setDescription(this.serverInfo.name());

			McpEndpointSpec endpointSpec = new McpEndpointSpec();
			if (StringUtils.equals(this.type, AiConstants.Mcp.MCP_PROTOCOL_STDIO)) {
				serverBasicInfo.setProtocol(AiConstants.Mcp.MCP_PROTOCOL_STDIO);
				serverBasicInfo.setFrontProtocol(AiConstants.Mcp.MCP_PROTOCOL_STDIO);
			}
			else {
				endpointSpec.setType(AiConstants.Mcp.MCP_ENDPOINT_TYPE_REF);
				Map<String, String> endpointSpecData = new HashMap<>();
				endpointSpecData.put("serviceName", getRegisterServiceName());
				endpointSpecData.put("groupName", this.nacosMcpRegistryProperties.getServiceGroup());
				endpointSpec.setData(endpointSpecData);

				McpServerRemoteServiceConfig remoteServerConfigInfo = new McpServerRemoteServiceConfig();
				String contextPath = this.nacosMcpRegistryProperties.getSseExportContextPath();
				if (StringUtils.isBlank(contextPath)) {
					contextPath = "";
				}
				remoteServerConfigInfo.setExportPath(contextPath + this.mcpServerProperties.getSseEndpoint());
				serverBasicInfo.setRemoteServerConfig(remoteServerConfigInfo);
				serverBasicInfo.setProtocol(AiConstants.Mcp.MCP_PROTOCOL_SSE);
				serverBasicInfo.setFrontProtocol(AiConstants.Mcp.MCP_PROTOCOL_SSE);
			}
			this.nacosMcpOperationService.createMcpServer(this.serverInfo.name(), serverBasicInfo, mcpToolSpec,
					endpointSpec);
		}
		catch (Exception e) {
			log.error("Failed to register mcp server to nacos", e);
		}
	}

	private void subscribe() {
		nacosMcpOperationService.subscribeNacosMcpServer(this.serverInfo.name() + "::" + this.serverInfo.version(),
				(mcpServerDetailInfo) -> {
					if (this.serverCapabilities.tools() != null) {
						updateTools(mcpServerDetailInfo);
					}
				});
	}

	private void updateToolDescription(McpServerFeatures.AsyncToolSpecification localToolRegistration,
			McpSchema.Tool toolInNacos, List<McpServerFeatures.AsyncToolSpecification> toolsRegistrationNeedToUpdate)
			throws JsonProcessingException {
		Boolean changed = false;
		if (localToolRegistration.tool().description() != null
				&& !localToolRegistration.tool().description().equals(toolInNacos.description())) {
			changed = true;
		}
		String localInputSchemaString = JacksonUtils.toJson(localToolRegistration.tool().inputSchema());
		Map<String, Object> localInputSchemaMap = JacksonUtils.toObj(localInputSchemaString, new TypeReference<>() {
		});
		Map<String, Object> localProperties = (Map<String, Object>) localInputSchemaMap.get("properties");

		String nacosInputSchemaString = JacksonUtils.toJson(toolInNacos.inputSchema());
		Map<Object, Object> nacosInputSchemaMap = JacksonUtils.toObj(nacosInputSchemaString, new TypeReference<>() {
		});
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
					toolInNacos.description(), JacksonUtils.toJson(localInputSchemaMap));
			toolsRegistrationNeedToUpdate
				.add(new McpServerFeatures.AsyncToolSpecification(toolNeededUpdate, localToolRegistration.call()));
		}

	}

	private void updateTools(McpServerDetailInfo mcpServerDetailInfo) {
		try {
			boolean changed = false;
			McpToolSpecification toolSpec = mcpServerDetailInfo.getToolSpec();
			if (toolSpec == null) {
				log.info("get nacos mcp server tools is null,skip tools update");
				return;
			}
			String toolsInNacosStr = JacksonUtils.toJson(toolSpec.getTools());
			List<McpSchema.Tool> toolsInNacos = JacksonUtils.toObj(toolsInNacosStr, new TypeReference<>() {
			});
			changed = compareToolsMeta(toolSpec.getToolsMeta());
			this.toolsMeta = toolSpec.getToolsMeta();
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
			Instance instance = new Instance();
			instance.setIp(this.nacosMcpProperties.getIp());
			instance.setPort(port);
			instance.setEphemeral(this.nacosMcpRegistryProperties.isServiceEphemeral());
			nacosMcpOperationService.registerService(this.getRegisterServiceName(),
					this.nacosMcpRegistryProperties.getServiceGroup(), instance);
			log.info("Register mcp server service to nacos successfully");
		}
		catch (NacosException e) {
			log.error("Failed to register mcp server service to nacos", e);
		}

	}

	private boolean checkToolsCompatible(McpServerDetailInfo serverDetailInfo) {
		if (serverDetailInfo.getToolSpec() == null || serverDetailInfo.getToolSpec().getTools() == null
				|| serverDetailInfo.getToolSpec().getTools().isEmpty()) {
			return true;
		}
		McpToolSpecification toolSpec = serverDetailInfo.getToolSpec();
		Map<String, McpTool> toolsInNacos = toolSpec.getTools()
			.stream()
			.collect(Collectors.toMap(McpTool::getName, tool -> tool, (existing, replacement) -> replacement));
		Map<String, McpSchema.Tool> toolsInLocal = this.tools.stream()
			.collect(Collectors.toMap(tool -> tool.tool().name(), McpServerFeatures.AsyncToolSpecification::tool,
					(existing, replacement) -> replacement));
		if (!toolsInNacos.keySet().equals(toolsInLocal.keySet())) {
			return false;
		}
		for (String toolName : toolsInNacos.keySet()) {
			String jsonSchemaStringInNacos = JacksonUtils.toJson(toolsInNacos.get(toolName).getInputSchema());
			String jsonSchemaStringInLocal = JacksonUtils.toJson(toolsInLocal.get(toolName).inputSchema());
			if (!JsonSchemaUtils.compare(jsonSchemaStringInNacos, jsonSchemaStringInLocal)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkCompatible(McpServerDetailInfo serverDetailInfo) {
		if (!StringUtils.equals(this.serverInfo.version(), serverDetailInfo.getVersionDetail().getVersion())) {
			return false;
		}
		if (!StringUtils.equals(this.type, serverDetailInfo.getProtocol())) {
			return false;
		}
		if (StringUtils.equals(this.type, AiConstants.Mcp.MCP_PROTOCOL_STDIO)) {
			return true;
		}
		McpServiceRef mcpServiceRef = serverDetailInfo.getRemoteServerConfig().getServiceRef();
		if (!isServiceRefSame(mcpServiceRef)) {
			return false;
		}
		if (this.serverCapabilities.tools() != null) {
			boolean checkToolsResult = checkToolsCompatible(serverDetailInfo);
			if (!checkToolsResult) {
				return checkToolsResult;
			}
		}
		return true;
	}

	private boolean isServiceRefSame(McpServiceRef serviceRef) {
		String serviceName = getRegisterServiceName();
		if (!StringUtils.equals(serviceRef.getServiceName(), serviceName)) {
			return false;
		}
		if (!StringUtils.equals(serviceRef.getGroupName(), this.nacosMcpRegistryProperties.getServiceGroup())) {
			return false;
		}
		if (!StringUtils.equals(serviceRef.getNamespaceId(), this.nacosMcpProperties.getnamespace())) {
			return false;
		}
		return true;
	}

	private String getRegisterServiceName() {
		return StringUtils.isBlank(this.nacosMcpRegistryProperties.getServiceName())
				? this.serverInfo.name() + "::" + this.serverInfo.version()
				: this.nacosMcpRegistryProperties.getServiceName();
	}

	private boolean compareToolsMeta(Map<String, McpToolMeta> toolsMeta) {
		boolean changed = false;
		if (this.toolsMeta == null && toolsMeta != null || this.toolsMeta != null && toolsMeta == null) {
			return true;
		}
		else if (this.toolsMeta == null) {
			return false;
		}
		if (!this.toolsMeta.keySet().equals(toolsMeta.keySet())) {
			return false;
		}
		for (String toolName : toolsMeta.keySet()) {
			if (this.toolsMeta.get(toolName).isEnabled() != toolsMeta.get(toolName).isEnabled()) {
				changed = true;
				break;
			}
		}
		return changed;
	}

}
