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

import com.alibaba.cloud.ai.mcp.nacos.client.utils.ApplicationContextHolder;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.JacksonUtils;
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
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @date 2025/4/29:13:00
 */
public class LoadbalancedMcpSyncClient implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serviceName;

	private final List<McpSyncClient> mcpSyncClientList;

	private final AtomicInteger currentIndex = new AtomicInteger(0);

	private final NamingService namingService;

	private List<Instance> instances;

	public LoadbalancedMcpSyncClient(String serviceName, List<McpSyncClient> mcpSyncClientList,
			NamingService namingService) {
		Assert.notNull(serviceName, "Service name must not be null");
		Assert.notNull(mcpSyncClientList, "McpSyncClient list must not be null");
		Assert.notNull(namingService, "NamingService must not be null");

		this.serviceName = serviceName;
		this.mcpSyncClientList = mcpSyncClientList;

		try {
			this.namingService = namingService;
			this.instances = namingService.selectInstances(serviceName, true);
		}
		catch (NacosException e) {
			throw new RuntimeException(String.format("Failed to get instances for service: %s", serviceName));
		}
	}

	public void subscribe() {
		try {
			this.namingService.subscribe(this.serviceName, this);
		}
		catch (NacosException e) {
			throw new RuntimeException(String.format("Failed to subscribe to service: %s", this.serviceName));
		}
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public List<McpSyncClient> getMcpSyncClientList() {
		return this.mcpSyncClientList;
	}

	public NamingService getNamingService() {
		return this.namingService;
	}

	public List<Instance> getInstances() {
		return this.instances;
	}

	public McpSyncClient getMcpSyncClient() {
		if (mcpSyncClientList.isEmpty()) {
			throw new IllegalStateException("No McpAsyncClient available");
		}
		int index = currentIndex.getAndIncrement() % mcpSyncClientList.size();
		return mcpSyncClientList.get(index);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------
	public McpSchema.ServerCapabilities getServerCapabilities() {
		return getMcpSyncClient().getServerCapabilities();
	}

	public McpSchema.Implementation getServerInfo() {
		return getMcpSyncClient().getServerInfo();
	}

	public McpSchema.ClientCapabilities getClientCapabilities() {
		return getMcpSyncClient().getClientCapabilities();
	}

	public McpSchema.Implementation getClientInfo() {
		return getMcpSyncClient().getClientInfo();
	}

	public void close() {
		Iterator<McpSyncClient> iterator = mcpSyncClientList.iterator();
		while (iterator.hasNext()) {
			McpSyncClient mcpSyncClient = iterator.next();
			mcpSyncClient.close();
			iterator.remove();
			logger.info("Closed and removed McpSyncClient: {}", mcpSyncClient.getClientInfo().name());
		}
	}

	public boolean closeGracefully() {
		List<Boolean> flagList = new ArrayList<>();
		Iterator<McpSyncClient> iterator = mcpSyncClientList.iterator();
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
		for (McpSyncClient mcpSyncClient : mcpSyncClientList) {
			mcpSyncClient.addRoot(root);
		}
	}

	public void removeRoot(String rootUri) {
		for (McpSyncClient mcpSyncClient : mcpSyncClientList) {
			mcpSyncClient.removeRoot(rootUri);
		}
	}

	public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
		return getMcpSyncClient().callTool(callToolRequest);
	}

	public McpSchema.ListToolsResult listTools() {
		return getMcpSyncClient().listTools();
	}

	public McpSchema.ListToolsResult listTools(String cursor) {
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
		for (McpSyncClient mcpSyncClient : mcpSyncClientList) {
			mcpSyncClient.subscribeResource(subscribeRequest);
		}
	}

	public void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
		for (McpSyncClient mcpSyncClient : mcpSyncClientList) {
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
		for (McpSyncClient mcpSyncClient : mcpSyncClientList) {
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

	private void updateClientList(List<Instance> currentInstances) {
		McpClientCommonProperties commonProperties = ApplicationContextHolder.getBean(McpClientCommonProperties.class);
		McpSyncClientConfigurer mcpSyncClientConfigurer = ApplicationContextHolder
			.getBean(McpSyncClientConfigurer.class);
		ObjectMapper objectMapper = ApplicationContextHolder.getBean(ObjectMapper.class);
		WebClient.Builder webClientBuilderTemplate = ApplicationContextHolder.getBean(WebClient.Builder.class);

		// 移除的实例列表
		List<Instance> removeInstances = instances.stream()
			.filter(instance -> !currentInstances.contains(instance))
			.collect(Collectors.toList());

		// 新增的实例列表
		List<Instance> addInstances = currentInstances.stream()
			.filter(instance -> !instances.contains(instance))
			.collect(Collectors.toList());

		// 删除McpSyncClient实例
		List<String> clientInfoNames = removeInstances.stream()
			.map(instance -> connectedClientName(commonProperties.getName(),
					this.serviceName + "-" + instance.getInstanceId()))
			.toList();
		Iterator<McpSyncClient> iterator = mcpSyncClientList.iterator();
		while (iterator.hasNext()) {
			McpSyncClient mcpSyncClient = iterator.next();
			McpSchema.Implementation clientInfo = mcpSyncClient.getClientInfo();
			if (clientInfoNames.contains(clientInfo.name())) {
				logger.info("Removing McpsyncClient: {}", clientInfo.name());
				if (mcpSyncClient.closeGracefully()) {
					iterator.remove();
				}
				else {
					logger.warn("Failed to remove mcpSyncClient: {}", clientInfo.name());
				}
			}
		}

		// 新增McpSyncClient实例
		McpSyncClient syncClient;
		for (Instance instance : addInstances) {
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

			logger.info("Added McpAsyncClient: {}", clientInfo.name());
			mcpSyncClientList.add(syncClient);
		}

		this.instances = currentInstances;
	}

	private String connectedClientName(String clientName, String serverConnectionName) {
		return clientName + " - " + serverConnectionName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String serviceName;

		private List<McpSyncClient> mcpSyncClientList;

		private NamingService namingService;

		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public Builder mcpSyncClientList(List<McpSyncClient> mcpSyncClientList) {
			this.mcpSyncClientList = mcpSyncClientList;
			return this;
		}

		public Builder namingService(NamingService namingService) {
			this.namingService = namingService;
			return this;
		}

		public LoadbalancedMcpSyncClient build() {
			return new LoadbalancedMcpSyncClient(this.serviceName, this.mcpSyncClientList, this.namingService);
		}

	}

}
