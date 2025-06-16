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

package com.alibaba.cloud.ai.mcp.nacos2.client.transport;

import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpNacosConstant;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpToolsInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @since 2025/4/29:13:00
 */
public class LoadbalancedMcpSyncClient implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serviceName;

	private final NamingService namingService;

	private final NacosConfigService nacosConfigService;

	private final Long TIME_OUT_MS = 3000L;

	private final McpClientCommonProperties commonProperties;

	private final WebClient.Builder webClientBuilderTemplate;

	private final McpSyncClientConfigurer mcpSyncClientConfigurer;

	private final ObjectMapper objectMapper;

	private final ApplicationContext applicationContext;

	private final AtomicInteger index = new AtomicInteger(0);

	private Map<String, List<String>> md5ToToolsMap;

	private Map<String, List<McpSyncClient>> md5ToClientMap;

	private List<Instance> instances;

	public LoadbalancedMcpSyncClient(String serviceName, String serviceGroup, NamingService namingService,
			NacosConfigService nacosConfigService, ApplicationContext applicationContext) {
		Assert.notNull(serviceName, "serviceName cannot be null");
		Assert.notNull(serviceGroup, "serviceGroup cannot be null");
		Assert.notNull(namingService, "namingService cannot be null");
		Assert.notNull(nacosConfigService, "nacosConfigService cannot be null");

		this.serviceName = serviceName;
		this.nacosConfigService = nacosConfigService;
		this.applicationContext = applicationContext;

		try {
			this.namingService = namingService;
			this.instances = namingService.selectInstances(this.serviceName + McpNacosConstant.SERVER_NAME_SUFFIX,
					serviceGroup, true);
		}
		catch (NacosException e) {
			throw new RuntimeException(String.format("Failed to get instances for service: %s", serviceName));
		}
		commonProperties = this.applicationContext.getBean(McpClientCommonProperties.class);
		mcpSyncClientConfigurer = this.applicationContext.getBean(McpSyncClientConfigurer.class);
		objectMapper = this.applicationContext.getBean(ObjectMapper.class);
		webClientBuilderTemplate = this.applicationContext.getBean(WebClient.Builder.class);
	}

	public void init() {
		md5ToToolsMap = new ConcurrentHashMap<>();
		md5ToClientMap = new ConcurrentHashMap<>();

		for (Instance instance : instances) {
			updateByAddInstance(instance);
		}
	}

	public void subscribe() {
		try {
			this.namingService.subscribe(this.serviceName + McpNacosConstant.SERVER_NAME_SUFFIX,
					McpNacosConstant.SERVER_GROUP, this);
		}
		catch (NacosException e) {
			throw new RuntimeException(String.format("Failed to subscribe to service: %s", this.serviceName));
		}
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
		return md5ToClientMap.values().stream().flatMap(List::stream).toList();
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public NamingService getNamingService() {
		return this.namingService;
	}

	public List<Instance> getInstances() {
		return this.instances;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------
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
		String toolName = callToolRequest.name();
		List<McpSyncClient> syncClients = new ArrayList<>();
		md5ToToolsMap.forEach((md5, tools) -> {
			if (tools.contains(toolName)) {
				syncClients.addAll(md5ToClientMap.get(md5));
			}
		});
		int currentIndex = index.getAndUpdate(index -> (index + 1) % syncClients.size());

		return syncClients.get(currentIndex).callTool(callToolRequest);
	}

	public McpSchema.ListToolsResult listTools() {
		return listToolsInternal(null);
	}

	public McpSchema.ListToolsResult listTools(String cursor) {
		return listToolsInternal(cursor);
	}

	private McpSchema.ListToolsResult listToolsInternal(String cursor) {
		return parseConfig(loadConfig(), cursor);
	}

	private String loadConfig() {
		String content = null;
		try {
			content = nacosConfigService.getConfig(this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX,
					McpNacosConstant.TOOLS_GROUP, TIME_OUT_MS);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
		if (content == null || content.isEmpty()) {
			throw new RuntimeException(String.format("Empty tool config content for dataId: %s, group: %s",
					this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP));
		}
		return content;
	}

	private McpSchema.ListToolsResult parseConfig(String content, String cursor) {
		try {
			McpToolsInfo mcpToolsInfo = objectMapper.readValue(content, McpToolsInfo.class);
			return new McpSchema.ListToolsResult(mcpToolsInfo.getTools(), cursor);
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to parse config for dataId: {}, group: {}",
					this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP, e);
			throw new RuntimeException(String.format("Failed to parse tool list, dataId: %s, group: %s\"",
					this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP), e);
		}
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

	@Override
	public void onEvent(Event event) {
		if (event instanceof NamingEvent namingEvent) {
			if (this.serviceName.equals(namingEvent.getServiceName())) {
				logger.info("Received service instance change event for service: {}", namingEvent.getServiceName());
				List<Instance> instances = namingEvent.getInstances();
				logger.info("Updated instances count: {}", instances.size());
				// 打印每个实例的详细信息
				instances.forEach(instance -> {
					logger.info("Instance: {}:{} (Healthy: {}, Enabled: {}, Metadata: {})", instance.getIp(),
							instance.getPort(), instance.isHealthy(), instance.isEnabled(),
							JacksonUtils.toJson(instance.getMetadata()));
				});
				updateClientList(instances);
			}
		}
	}

	private McpSyncClient clientByInstance(Instance instance) {
		McpSyncClient syncClient;

		String baseUrl = instance.getMetadata().getOrDefault("scheme", "http") + "://" + instance.getIp() + ":"
				+ instance.getPort();
		WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(baseUrl);
		WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);
		NamedClientMcpTransport namedTransport = new NamedClientMcpTransport(
				serviceName + "-" + instance.getInstanceId(), transport);

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

	private void updateByAddInstance(Instance instance) {
		Map<String, String> metadata = instance.getMetadata();
		String serverMd5 = metadata.get("server.md5");
		assert serverMd5 != null;
		McpSyncClient mcpSyncClient = clientByInstance(instance);
		md5ToClientMap.computeIfAbsent(serverMd5, k -> new ArrayList<>()).add(mcpSyncClient);

		if (!md5ToToolsMap.containsKey(serverMd5)) {
			String tools = metadata.get("tools.names");
			md5ToToolsMap.put(serverMd5, List.of(tools.split(",")));
		}
	}

	private void updateClientList(List<Instance> currentInstances) {
		// 新增的实例
		List<Instance> addInstances = currentInstances.stream()
			.filter(instance -> !instances.contains(instance))
			.collect(Collectors.toList());
		for (Instance addInstance : addInstances) {
			updateByAddInstance(addInstance);
		}
		// 移除的实例
		List<Instance> removeInstances = instances.stream()
			.filter(instance -> !currentInstances.contains(instance))
			.collect(Collectors.toList());
		for (Instance removeInstance : removeInstances) {
			updateByRemoveInstance(removeInstance);
		}
		this.instances = currentInstances;
	}

	private void updateByRemoveInstance(Instance instance) {
		String clientInfoName = connectedClientName(commonProperties.getName(),
				this.serviceName + "-" + instance.getInstanceId());
		String serverMd5 = instance.getMetadata().get("server.md5");

		List<McpSyncClient> clientList = md5ToClientMap.getOrDefault(serverMd5, Collections.emptyList());
		McpSyncClient syncClient;
		for (McpSyncClient mcpSyncClient : clientList) {
			McpSchema.Implementation clientInfo = mcpSyncClient.getClientInfo();
			String clientName = clientInfo.name();
			if (clientInfoName.equals(clientName)) {
				logger.info("Removing McpSyncClient: {}", clientName);
				syncClient = mcpSyncClient;
				syncClient.closeGracefully();
				// 安全地移除
				md5ToClientMap.get(serverMd5).remove(syncClient);

				if (md5ToClientMap.get(serverMd5).isEmpty()) {
					md5ToClientMap.remove(serverMd5);
					md5ToToolsMap.remove(serverMd5);
				}
				logger.info("Removed McpSyncClient: {} Success", syncClient.getClientInfo().name());
				break;
			}
		}
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String serviceName;

		private String serviceGroup;

		private NamingService namingService;

		private NacosConfigService nacosConfigService;

		private ApplicationContext applicationContext;

		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public Builder serviceGroup(String serviceGroup) {
			this.serviceGroup = serviceGroup;
			return this;
		}

		public Builder namingService(NamingService namingService) {
			this.namingService = namingService;
			return this;
		}

		public Builder nacosConfigService(NacosConfigService nacosConfigService) {
			this.nacosConfigService = nacosConfigService;
			return this;
		}

		public Builder applicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			return this;
		}

		public LoadbalancedMcpSyncClient build() {
			return new LoadbalancedMcpSyncClient(this.serviceName, this.serviceGroup, this.namingService,
					this.nacosConfigService, this.applicationContext);
		}

	}

}
