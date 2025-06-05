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
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @since 2025/4/29:10:05
 */
public class LoadbalancedMcpAsyncClient {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serverName;

	private final NacosMcpOperationService nacosMcpOperationService;

	private final McpClientCommonProperties commonProperties;

	private final WebClient.Builder webClientBuilderTemplate;

	private final McpAsyncClientConfigurer mcpAsyncClientConfigurer;

	private final ObjectMapper objectMapper;

	private final ApplicationContext applicationContext;

	private final AtomicInteger index = new AtomicInteger(0);

	private Map<String, McpAsyncClient> keyToClientMap;

	private NacosMcpServerEndpoint serverEndpoint;

	public LoadbalancedMcpAsyncClient(String serverName, String version,
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
				throw new RuntimeException("mcp server protocol must be sse");
			}
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Failed to get instances for service: %s", serverName), e);
		}
		commonProperties = this.applicationContext.getBean(McpClientCommonProperties.class);
		mcpAsyncClientConfigurer = this.applicationContext.getBean(McpAsyncClientConfigurer.class);
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
			if (!StringUtils.equals(protocol, AiConstants.Mcp.MCP_PROTOCOL_SSE)) {
				return;
			}
			updateClientList(nacosMcpServerEndpoint);
		});
	}

	public McpAsyncClient getMcpAsyncClient() {
		List<McpAsyncClient> asynClients = getMcpAsyncClientList();
		if (asynClients.isEmpty()) {
			throw new IllegalStateException("No McpAsyncClient available");
		}

		int currentIndex = index.getAndUpdate(index -> (index + 1) % asynClients.size());
		return asynClients.get(currentIndex);
	}

	public List<McpAsyncClient> getMcpAsyncClientList() {
		return keyToClientMap.values().stream().toList();
	}

	public String getServerName() {
		return serverName;
	}

	public NacosMcpServerEndpoint getNacosMcpServerEndpoint() {
		return this.serverEndpoint;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------

	public McpSchema.ServerCapabilities getServerCapabilities() {
		return getMcpAsyncClientList().get(0).getServerCapabilities();
	}

	public McpSchema.Implementation getServerInfo() {
		return getMcpAsyncClientList().get(0).getServerInfo();
	}

	public McpSchema.ClientCapabilities getClientCapabilities() {
		return getMcpAsyncClientList().get(0).getClientCapabilities();
	}

	public McpSchema.Implementation getClientInfo() {
		return getMcpAsyncClientList().get(0).getClientInfo();
	}

	public void close() {
		Iterator<McpAsyncClient> iterator = getMcpAsyncClientList().iterator();
		while (iterator.hasNext()) {
			McpAsyncClient mcpAsyncClient = iterator.next();
			mcpAsyncClient.close();
			iterator.remove();
			logger.info("Closed and removed McpAsyncClient: {}", mcpAsyncClient.getClientInfo().name());
		}
	}

	public Mono<Void> closeGracefully() {
		Iterator<McpAsyncClient> iterator = getMcpAsyncClientList().iterator();
		List<Mono<Void>> closeMonos = new ArrayList<>();
		while (iterator.hasNext()) {
			McpAsyncClient mcpAsyncClient = iterator.next();
			Mono<Void> voidMono = mcpAsyncClient.closeGracefully().doOnSuccess(v -> {
				iterator.remove();
				logger.info("Closed and removed McpAsyncClient: {}", mcpAsyncClient.getClientInfo().name());
			});
			closeMonos.add(voidMono);
		}
		return Mono.when(closeMonos);
	}

	public Mono<Object> ping() {
		return getMcpAsyncClient().ping();
	}

	public Mono<Void> addRoot(McpSchema.Root root) {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(mcpAsyncClient -> mcpAsyncClient.addRoot(root))
			.collect(Collectors.toList()));
	}

	public Mono<Void> removeRoot(String rootUri) {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(mcpAsyncClient -> mcpAsyncClient.removeRoot(rootUri))
			.collect(Collectors.toList()));
	}

	public Mono<Void> rootsListChangedNotification() {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(McpAsyncClient::rootsListChangedNotification)
			.collect(Collectors.toList()));
	}

	public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
		return getMcpAsyncClient().callTool(callToolRequest);
	}

	public Mono<McpSchema.ListToolsResult> listTools() {
		return listToolsInternal(null);
	}

	public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
		return listToolsInternal(cursor);
	}

	private Mono<McpSchema.ListToolsResult> listToolsInternal(String cursor) {
		return getMcpAsyncClient().listTools(cursor);
	}

	public Mono<McpSchema.ListResourcesResult> listResources() {
		return getMcpAsyncClient().listResources();
	}

	public Mono<McpSchema.ListResourcesResult> listResources(String cursor) {
		return getMcpAsyncClient().listResources(cursor);
	}

	public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource) {
		return getMcpAsyncClient().readResource(resource);
	}

	public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.ReadResourceRequest readResourceRequest) {
		return getMcpAsyncClient().readResource(readResourceRequest);
	}

	public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates() {
		return getMcpAsyncClient().listResourceTemplates();
	}

	public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates(String cursor) {
		return getMcpAsyncClient().listResourceTemplates(cursor);
	}

	public Mono<Void> subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(mcpAsyncClient -> mcpAsyncClient.subscribeResource(subscribeRequest))
			.collect(Collectors.toList()));
	}

	public Mono<Void> unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(mcpAsyncClient -> mcpAsyncClient.unsubscribeResource(unsubscribeRequest))
			.collect(Collectors.toList()));
	}

	public Mono<McpSchema.ListPromptsResult> listPrompts() {
		return getMcpAsyncClient().listPrompts();
	}

	public Mono<McpSchema.ListPromptsResult> listPrompts(String cursor) {
		return getMcpAsyncClient().listPrompts(cursor);
	}

	public Mono<McpSchema.GetPromptResult> getPrompt(McpSchema.GetPromptRequest getPromptRequest) {
		return getMcpAsyncClient().getPrompt(getPromptRequest);
	}

	public Mono<Void> setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
		return Mono.when(getMcpAsyncClientList().stream()
			.map(mcpAsyncClient -> mcpAsyncClient.setLoggingLevel(loggingLevel))
			.collect(Collectors.toList()));
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------

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
		Map<String, McpAsyncClient> newKeyToClientMap = new ConcurrentHashMap<>();
		Map<String, McpAsyncClient> oldKeyToClientMap = this.keyToClientMap;
		Map<String, Integer> newKeyToCountMap = new ConcurrentHashMap<>();
		for (McpEndpointInfo mcpEndpointInfo : newServerEndpoint.getMcpEndpointInfoList()) {
			McpAsyncClient syncClient = clientByEndpoint(mcpEndpointInfo, newServerEndpoint.getExportPath());
			String key = NacosMcpClientUtils.getMcpEndpointInfoId(mcpEndpointInfo, newServerEndpoint.getExportPath());
			newKeyToClientMap.putIfAbsent(key, syncClient);
			newKeyToCountMap.putIfAbsent(key, 0);
		}
		this.keyToClientMap = newKeyToClientMap;
		for (Map.Entry<String, McpAsyncClient> entry : oldKeyToClientMap.entrySet()) {
			McpAsyncClient asyncClient = entry.getValue();
			logger.info("Removing McpAsyncClient: {}", asyncClient.getClientInfo().name());
			asyncClient.closeGracefully().block();
			logger.info("Removed McpAsyncClient: {} Success", asyncClient.getClientInfo().name());
		}
	}

	private McpAsyncClient clientByEndpoint(McpEndpointInfo mcpEndpointInfo, String exportPath) {
		McpAsyncClient asyncClient;

		String baseUrl = "http://" + mcpEndpointInfo.getAddress() + ":" + mcpEndpointInfo.getPort();
		WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(baseUrl);
		WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper, exportPath);
		NamedClientMcpTransport namedTransport = new NamedClientMcpTransport(
				serverName + "-" + NacosMcpClientUtils.getMcpEndpointInfoId(mcpEndpointInfo, exportPath), transport);
		McpSchema.Implementation clientInfo = new McpSchema.Implementation(
				this.connectedClientName(commonProperties.getName(), namedTransport.name()),
				commonProperties.getVersion());
		McpClient.AsyncSpec asyncSpec = McpClient.async(namedTransport.transport())
			.clientInfo(clientInfo)
			.requestTimeout(commonProperties.getRequestTimeout());
		asyncSpec = mcpAsyncClientConfigurer.configure(namedTransport.name(), asyncSpec);
		asyncClient = asyncSpec.build();
		if (commonProperties.isInitialized()) {
			asyncClient.initialize().block();
		}
		logger.info("Added McpAsyncClient: {}", clientInfo.name());
		return asyncClient;
	}

	private void updateByAddEndpoint(McpEndpointInfo serverEndpoint, String exportPath) {
		McpAsyncClient mcpAsyncClient = clientByEndpoint(serverEndpoint, exportPath);
		String key = NacosMcpClientUtils.getMcpEndpointInfoId(serverEndpoint, exportPath);
		keyToClientMap.putIfAbsent(key, mcpAsyncClient);
	}

	private void updateByRemoveEndpoint(McpEndpointInfo serverEndpoint, String exportPath) {
		String key = NacosMcpClientUtils.getMcpEndpointInfoId(serverEndpoint, exportPath);
		if (keyToClientMap.containsKey(key)) {
			McpAsyncClient asyncClient = keyToClientMap.remove(key);
			logger.info("Removing McpAsyncClient: {}", asyncClient.getClientInfo().name());
			asyncClient.closeGracefully().block();
			logger.info("Removed McpAsyncClient: {} Success", asyncClient.getClientInfo().name());
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

		public LoadbalancedMcpAsyncClient build() {
			return new LoadbalancedMcpAsyncClient(this.serverName, this.version, this.nacosMcpOperationService,
					this.applicationContext);
		}

	}

}
