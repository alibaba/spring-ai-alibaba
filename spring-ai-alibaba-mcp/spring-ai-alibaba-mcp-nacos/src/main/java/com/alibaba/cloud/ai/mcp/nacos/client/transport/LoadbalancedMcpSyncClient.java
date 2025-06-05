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

package com.alibaba.cloud.ai.mcp.nacos.client.transport;

import com.alibaba.cloud.ai.mcp.nacos.client.utils.NacosMcpClientUtils;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.nacos.service.model.NacosMcpServerEndpoint;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yingzi
 * @since 2025/4/29:13:00
 */
public class LoadbalancedMcpSyncClient {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serverName;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final McpClientCommonProperties commonProperties;

	private final WebClient.Builder webClientBuilderTemplate;

	private final McpSyncClientConfigurer mcpSyncClientConfigurer;

	private final ObjectMapper objectMapper;

	private final ApplicationContext applicationContext;

	private final AtomicInteger index = new AtomicInteger(0);

	private Map<String, McpSyncClient> keyToClientMap;

	private NacosMcpServerEndpoint serverEndpoint;

	public LoadbalancedMcpSyncClient(String serverName, String version,
			NacosMcpOperationService nacosMcpOperationService, ApplicationContext applicationContext) {
		Assert.notNull(serverName, "serviceName cannot be null");
		Assert.notNull(version, "version cannot be null");
		Assert.notNull(nacosMcpOperationService, "nacosMcpOperationService cannot be null");
		Assert.notNull(applicationContext, "applicationContext cannot be null");

		this.serverName = serverName;
		this.nacosMcpOperationService = nacosMcpOperationService;
		this.applicationContext = applicationContext;

		try {
			this.serverEndpoint = this.nacosMcpOperationService.getServerEndpoint(this.serverName, version);
			if (this.serverEndpoint == null) {
				throw new NacosException(NacosException.NOT_FOUND,
						String.format("Can not find mcp server from nacos: %s", serverName));
			}
			if (!StringUtils.equals(serverEndpoint.getProtocol(), AiConstants.Mcp.MCP_PROTOCOL_SSE)) {
				throw new Exception("mcp server protocol must be sse");
			}
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Failed to get instances for service: %s", serverName));
		}
		commonProperties = this.applicationContext.getBean(McpClientCommonProperties.class);
		mcpSyncClientConfigurer = this.applicationContext.getBean(McpSyncClientConfigurer.class);
		objectMapper = this.applicationContext.getBean(ObjectMapper.class);
		webClientBuilderTemplate = this.applicationContext.getBean(WebClient.Builder.class);
	}

	public void init() {
		keyToClientMap = new ConcurrentHashMap<>();

		for (McpEndpointInfo mcpEndpointInfo : serverEndpoint.getMcpEndpointInfoList()) {
			updateByAddEndpoint(mcpEndpointInfo, serverEndpoint.getExportPath());
		}
	}

	public void subscribe() {
		this.nacosMcpOperationService.subscribeNacosMcpServer(this.serverName, mcpServerDetailInfo -> {
			List<McpEndpointInfo> mcpEndpointInfoList = mcpServerDetailInfo.getBackendEndpoints() == null
					? new ArrayList<>() : mcpServerDetailInfo.getBackendEndpoints();
			String exportPath = mcpServerDetailInfo.getRemoteServerConfig().getExportPath();
			String protocol = mcpServerDetailInfo.getProtocol();
			String realVersion = mcpServerDetailInfo.getVersionDetail().getVersion();
			NacosMcpServerEndpoint nacosMcpServerEndpoint = new NacosMcpServerEndpoint(mcpEndpointInfoList, exportPath,
					protocol, realVersion);
			updateClientList(nacosMcpServerEndpoint);
		});
	}

	public McpSyncClient getMcpSyncClient() {
		List<McpSyncClient> syncClients = getMcpSyncClientList();
		if (syncClients.isEmpty()) {
			throw new IllegalStateException("No McpAsyncClient available");
		}
		int currentIndex = index.getAndUpdate(index -> (index + 1) % syncClients.size());

		return syncClients.get(currentIndex);
	}

	public List<McpSyncClient> getMcpSyncClientList() {
		return keyToClientMap.values().stream().toList();
	}

	public String getServerName() {
		return serverName;
	}

	public NacosMcpServerEndpoint getNacosMcpServerEndpoint() {
		return this.serverEndpoint;
	}

	public McpSchema.ServerCapabilities getServerCapabilities() {
		return getMcpSyncClientList().get(0).getServerCapabilities();
	}

	public McpSchema.Implementation getServerInfo() {
		return getMcpSyncClientList().get(0).getServerInfo();
	}

	public McpSchema.ClientCapabilities getClientCapabilities() {
		return getMcpSyncClientList().get(0).getClientCapabilities();
	}

	public McpSchema.Implementation getClientInfo() {
		return getMcpSyncClientList().get(0).getClientInfo();
	}

	public void close() {
		Iterator<McpSyncClient> iterator = getMcpSyncClientList().iterator();
		while (iterator.hasNext()) {
			McpSyncClient mcpSyncClient = iterator.next();
			mcpSyncClient.close();
			iterator.remove();
			logger.info("Closed and removed McpSyncClient: {}", mcpSyncClient.getClientInfo().name());
		}
	}

	public boolean closeGracefully() {
		List<Boolean> flagList = new ArrayList<>();
		Iterator<McpSyncClient> iterator = getMcpSyncClientList().iterator();
		while (iterator.hasNext()) {
			McpSyncClient mcpSyncClient = iterator.next();
			boolean flag = mcpSyncClient.closeGracefully();
			flagList.add(flag);
			if (flag) {
				iterator.remove();
				logger.info("Closed and removed McpSyncClient: {}", mcpSyncClient.getClientInfo().name());
			}
		}
		return !flagList.stream().allMatch(flag -> flag);
	}

	public Object ping() {
		return getMcpSyncClient().ping();
	}

	public void addRoot(McpSchema.Root root) {
		for (McpSyncClient mcpSyncClient : getMcpSyncClientList()) {
			mcpSyncClient.addRoot(root);
		}
	}

	public void removeRoot(String rootUri) {
		for (McpSyncClient mcpSyncClient : getMcpSyncClientList()) {
			mcpSyncClient.removeRoot(rootUri);
		}
	}

	public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
		return getMcpSyncClient().callTool(callToolRequest);
	}

	public McpSchema.ListToolsResult listTools() {
		return listToolsInternal(null);
	}

	public McpSchema.ListToolsResult listTools(String cursor) {
		return listToolsInternal(cursor);
	}

	private McpSchema.ListToolsResult listToolsInternal(String cursor) {
		return getMcpSyncClient().listTools(cursor);
	}

	public McpSchema.ListResourcesResult listResources(String cursor) {
		return getMcpSyncClient().listResources(cursor);
	}

	public McpSchema.ListResourcesResult listResources() {
		return getMcpSyncClient().listResources();
	}

	public McpSchema.ReadResourceResult readResource(McpSchema.Resource resource) {
		return getMcpSyncClient().readResource(resource);
	}

	public McpSchema.ReadResourceResult readResource(McpSchema.ReadResourceRequest readResourceRequest) {
		return getMcpSyncClient().readResource(readResourceRequest);
	}

	public McpSchema.ListResourceTemplatesResult listResourceTemplates(String cursor) {
		return getMcpSyncClient().listResourceTemplates(cursor);
	}

	public McpSchema.ListResourceTemplatesResult listResourceTemplates() {
		return getMcpSyncClient().listResourceTemplates();
	}

	public void subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
		for (McpSyncClient mcpSyncClient : getMcpSyncClientList()) {
			mcpSyncClient.subscribeResource(subscribeRequest);
		}
	}

	public void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
		for (McpSyncClient mcpSyncClient : getMcpSyncClientList()) {
			mcpSyncClient.unsubscribeResource(unsubscribeRequest);
		}
	}

	public McpSchema.ListPromptsResult listPrompts(String cursor) {
		return getMcpSyncClient().listPrompts(cursor);
	}

	public McpSchema.ListPromptsResult listPrompts() {
		return getMcpSyncClient().listPrompts();
	}

	public McpSchema.GetPromptResult getPrompt(McpSchema.GetPromptRequest getPromptRequest) {
		return getMcpSyncClient().getPrompt(getPromptRequest);
	}

	public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
		for (McpSyncClient mcpSyncClient : getMcpSyncClientList()) {
			mcpSyncClient.setLoggingLevel(loggingLevel);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------

	private McpSyncClient clientByEndpoint(McpEndpointInfo mcpEndpointInfo, String exportPath) {
		McpSyncClient syncClient;
		String baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
		WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(baseUrl);
		WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper, exportPath);
		NamedClientMcpTransport namedTransport = new NamedClientMcpTransport(
				serverName + "-" + NacosMcpClientUtils.getMcpEndpointInfoId(mcpEndpointInfo, exportPath), transport);
		McpSchema.Implementation clientInfo = new McpSchema.Implementation(
				this.connectedClientName(commonProperties.getName(), namedTransport.name()),
				commonProperties.getVersion());
		McpClient.SyncSpec syncSpec = McpClient.sync(namedTransport.transport())
			.clientInfo(clientInfo)
			.requestTimeout(commonProperties.getRequestTimeout());
		syncSpec = mcpSyncClientConfigurer.configure(namedTransport.name(), syncSpec);
		syncClient = syncSpec.build();
		if (commonProperties.isInitialized()) {
			syncClient.initialize();
		}
		logger.info("Added McpSyncClient: {}", clientInfo.name());
		return syncClient;
	}

	private void updateByAddEndpoint(McpEndpointInfo serverEndpoint, String exportPath) {
		McpSyncClient mcpSyncClient = clientByEndpoint(serverEndpoint, exportPath);
		String key = NacosMcpClientUtils.getMcpEndpointInfoId(serverEndpoint, exportPath);
		keyToClientMap.putIfAbsent(key, mcpSyncClient);
	}

	private void updateClientList(NacosMcpServerEndpoint newServerEndpoint) {
		if (!StringUtils.equals(this.serverEndpoint.getExportPath(), newServerEndpoint.getExportPath())
				|| !StringUtils.equals(this.serverEndpoint.getVersion(), newServerEndpoint.getVersion())) {
			updateAll(newServerEndpoint);
		}
		else {
			List<McpEndpointInfo> currentMcpEndpointInfoList = this.serverEndpoint.getMcpEndpointInfoList();
			List<McpEndpointInfo> newMcpEndpointInfoList = newServerEndpoint.getMcpEndpointInfoList();
			List<McpEndpointInfo> addEndpointInfoList = newMcpEndpointInfoList.stream()
				.filter(newEndpoint -> currentMcpEndpointInfoList.stream()
					.noneMatch(currentEndpoint -> currentEndpoint.getAddress().equals(newEndpoint.getAddress())
							&& currentEndpoint.getPort() == newEndpoint.getPort()))
				.toList();
			List<McpEndpointInfo> removeEndpointInfoList = currentMcpEndpointInfoList.stream()
				.filter(currentEndpoint -> newMcpEndpointInfoList.stream()
					.noneMatch(newEndpoint -> newEndpoint.getAddress().equals(currentEndpoint.getAddress())
							&& newEndpoint.getPort() == currentEndpoint.getPort()))
				.toList();
			for (McpEndpointInfo addEndpointInfo : addEndpointInfoList) {
				updateByAddEndpoint(addEndpointInfo, newServerEndpoint.getExportPath());
			}
			for (McpEndpointInfo removeEndpointInfo : removeEndpointInfoList) {
				updateByRemoveEndpoint(removeEndpointInfo, newServerEndpoint.getExportPath());
			}
		}
		this.serverEndpoint = newServerEndpoint;
	}

	private void updateAll(NacosMcpServerEndpoint newServerEndpoint) {
		Map<String, McpSyncClient> newKeyToClientMap = new ConcurrentHashMap<>();
		Map<String, McpSyncClient> oldKeyToClientMap = this.keyToClientMap;
		Map<String, Integer> newKeyToCountMap = new ConcurrentHashMap<>();
		for (McpEndpointInfo mcpEndpointInfo : newServerEndpoint.getMcpEndpointInfoList()) {
			McpSyncClient syncClient = clientByEndpoint(mcpEndpointInfo, newServerEndpoint.getExportPath());
			String key = NacosMcpClientUtils.getMcpEndpointInfoId(mcpEndpointInfo, newServerEndpoint.getExportPath());
			newKeyToClientMap.putIfAbsent(key, syncClient);
			newKeyToCountMap.putIfAbsent(key, 0);
		}
		this.keyToClientMap = newKeyToClientMap;
		for (Map.Entry<String, McpSyncClient> entry : oldKeyToClientMap.entrySet()) {
			McpSyncClient syncClient = entry.getValue();
			logger.info("Removing McpSyncClient: {}", syncClient.getClientInfo().name());
			syncClient.closeGracefully();
			logger.info("Removed McpSyncClient: {} Success", syncClient.getClientInfo().name());
		}
	}

	private void updateByRemoveEndpoint(McpEndpointInfo serverEndpoint, String exportPath) {
		String key = NacosMcpClientUtils.getMcpEndpointInfoId(serverEndpoint, exportPath);
		if (keyToClientMap.containsKey(key)) {
			McpSyncClient syncClient = keyToClientMap.remove(key);
			logger.info("Removing McpSyncClient: {}", syncClient.getClientInfo().name());
			syncClient.closeGracefully();
			logger.info("Removed McpSyncClient: {} Success", syncClient.getClientInfo().name());
		}
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String serverName;

		private String version;

		private NacosMcpOperationService nacosMcpOperationService;

		private ApplicationContext applicationContext;

		public Builder serverName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		public Builder version(String version) {
			this.version = version;
			return this;
		}

		public Builder nacosMcpOperationService(NacosMcpOperationService nacosMcpOperationService) {
			this.nacosMcpOperationService = nacosMcpOperationService;
			return this;
		}

		public Builder applicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			return this;
		}

		public LoadbalancedMcpSyncClient build() {
			return new LoadbalancedMcpSyncClient(this.serverName, this.version, this.nacosMcpOperationService,
					this.applicationContext);
		}

	}

}
