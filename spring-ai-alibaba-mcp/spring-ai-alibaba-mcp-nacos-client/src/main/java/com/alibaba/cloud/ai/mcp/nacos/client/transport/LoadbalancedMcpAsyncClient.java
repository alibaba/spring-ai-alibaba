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
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpAsyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @date 2025/4/29:10:05
 */
public class LoadbalancedMcpAsyncClient implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serviceName;

	private final List<McpAsyncClient> mcpAsyncClientList;

	private final AtomicInteger currentIndex = new AtomicInteger(0);

	private final NamingService namingService;

	private List<Instance> instances;

	public LoadbalancedMcpAsyncClient(String serviceName, List<McpAsyncClient> mcpAsyncClientList,
			NamingService namingService) {
		Assert.notNull(serviceName, "serviceName cannot be null");
		Assert.notNull(mcpAsyncClientList, "mcpAsyncClientList cannot be null");
		Assert.notNull(namingService, "namingService cannot be null");

		this.serviceName = serviceName;
		this.mcpAsyncClientList = mcpAsyncClientList;

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
		return serviceName;
	}

	public List<McpAsyncClient> getMcpAsyncClientList() {
		return mcpAsyncClientList;
	}

	public NamingService getNamingService() {
		return this.namingService;
	}

	public List<Instance> getInstances() {
		return this.instances;
	}

	private McpAsyncClient getMcpAsyncClient() {
		if (mcpAsyncClientList.isEmpty()) {
			throw new IllegalStateException("No McpAsyncClient available");
		}
		int index = currentIndex.getAndIncrement() % mcpAsyncClientList.size();
		return mcpAsyncClientList.get(index);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------

	public McpSchema.ServerCapabilities getServerCapabilities() {
		return getMcpAsyncClient().getServerCapabilities();
	}

	public McpSchema.Implementation getServerInfo() {
		return getMcpAsyncClient().getServerInfo();
	}

	public boolean isInitialized() {
		return getMcpAsyncClient().isInitialized();
	}

	public McpSchema.ClientCapabilities getClientCapabilities() {
		return getMcpAsyncClient().getClientCapabilities();
	}

	public McpSchema.Implementation getClientInfo() {
		return getMcpAsyncClient().getClientInfo();
	}

	public void close() {
		Iterator<McpAsyncClient> iterator = mcpAsyncClientList.iterator();
		while (iterator.hasNext()) {
			McpAsyncClient mcpAsyncClient = iterator.next();
			mcpAsyncClient.close();
			iterator.remove();
			logger.info("Closed and removed McpSyncClient: {}", mcpAsyncClient.getClientInfo().name());
		}
	}

	public Mono<Void> closeGracefully() {
		Iterator<McpAsyncClient> iterator = mcpAsyncClientList.iterator();
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
		return Mono.when(mcpAsyncClientList.stream()
			.map(mcpAsyncClient -> mcpAsyncClient.addRoot(root))
			.collect(Collectors.toList()));
	}

	public Mono<Void> removeRoot(String rootUri) {
		return Mono.when(mcpAsyncClientList.stream()
			.map(mcpAsyncClient -> mcpAsyncClient.removeRoot(rootUri))
			.collect(Collectors.toList()));
	}

	public Mono<Void> rootsListChangedNotification() {
		return Mono.when(mcpAsyncClientList.stream()
			.map(McpAsyncClient::rootsListChangedNotification)
			.collect(Collectors.toList()));
	}

	public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
		return getMcpAsyncClient().callTool(callToolRequest);
	}

	public Mono<McpSchema.ListToolsResult> listTools() {
		return getMcpAsyncClient().listTools();
	}

	public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
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
		return Mono.when(mcpAsyncClientList.stream()
			.map(mcpAsyncClient -> mcpAsyncClient.subscribeResource(subscribeRequest))
			.collect(Collectors.toList()));
	}

	public Mono<Void> unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
		return Mono.when(mcpAsyncClientList.stream()
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
		return Mono.when(mcpAsyncClientList.stream()
			.map(mcpAsyncClient -> mcpAsyncClient.setLoggingLevel(loggingLevel))
			.collect(Collectors.toList()));
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
		McpAsyncClientConfigurer mcpSyncClientConfigurer = ApplicationContextHolder
			.getBean(McpAsyncClientConfigurer.class);
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

		// 删除McpAsyncClient实例
		List<String> clientInfoNames = removeInstances.stream()
			.map(instance -> connectedClientName(commonProperties.getName(),
					this.serviceName + "-" + instance.getInstanceId()))
			.toList();
		Iterator<McpAsyncClient> iterator = mcpAsyncClientList.iterator();
		while (iterator.hasNext()) {
			McpAsyncClient mcpAsyncClient = iterator.next();
			McpSchema.Implementation clientInfo = mcpAsyncClient.getClientInfo();
			if (clientInfoNames.contains(clientInfo.name())) {
				logger.info("Removing McpAsyncClient: {}", clientInfo.name());
				mcpAsyncClient.closeGracefully().subscribe(v -> {
					iterator.remove();
				}, e -> logger.error("Failed to remove McpAsyncClient: {}", clientInfo.name(), e));
			}
		}

		// 新增McpAsyncClient实例
		McpAsyncClient asyncClient;
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
			McpClient.AsyncSpec asyncSpec = McpClient.async(namedTransport.transport())
				.clientInfo(clientInfo)
				.requestTimeout(commonProperties.getRequestTimeout());
			asyncSpec = mcpSyncClientConfigurer.configure(namedTransport.name(), asyncSpec);
			asyncClient = asyncSpec.build();
			if (commonProperties.isInitialized()) {
				asyncClient.initialize().block();
			}
			logger.info("Added McpAsyncClient: {}", clientInfo.name());
			mcpAsyncClientList.add(asyncClient);
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

		private List<McpAsyncClient> mcpAsyncClientList;

		private NamingService namingService;

		public Builder serviceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public Builder mcpAsyncClientList(List<McpAsyncClient> mcpAsyncClientList) {
			this.mcpAsyncClientList = mcpAsyncClientList;
			return this;
		}

		public Builder namingService(NamingService namingService) {
			this.namingService = namingService;
			return this;
		}

		public LoadbalancedMcpAsyncClient build() {
			return new LoadbalancedMcpAsyncClient(this.serviceName, this.mcpAsyncClientList, this.namingService);
		}

	}

}
