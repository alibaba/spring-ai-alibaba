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
import java.util.Collections;
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
public class LoadbalancedMcpAsyncClient implements EventListener {

	private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);

	private final String serviceName;

	private final NamingService namingService;

	private final NacosConfigService nacosConfigService;

	private final Long TIME_OUT_MS = 3000L;

	private final McpClientCommonProperties commonProperties;

	private final WebClient.Builder webClientBuilderTemplate;

	private final McpAsyncClientConfigurer mcpAsyncClientConfigurer;

	private final ObjectMapper objectMapper;

	private final ApplicationContext applicationContext;

	private final AtomicInteger index = new AtomicInteger(0);

	private Map<String, List<String>> md5ToToolsMap;

	private Map<String, List<McpAsyncClient>> md5ToClientMap;

	private List<Instance> instances;

	public LoadbalancedMcpAsyncClient(String serviceName, String serviceGroup, NamingService namingService,
			NacosConfigService nacosConfigService, ApplicationContext applicationContext) {
		Assert.notNull(serviceName, "serviceName cannot be null");
		Assert.notNull(serviceGroup, "serviceGroup cannot be null");
		Assert.notNull(namingService, "namingService cannot be null");
		Assert.notNull(nacosConfigService, "nacosConfigService cannot be null");
		Assert.notNull(applicationContext, "applicationContext cannot be null");

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
		mcpAsyncClientConfigurer = this.applicationContext.getBean(McpAsyncClientConfigurer.class);
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

	public McpAsyncClient getMcpAsyncClient() {
		List<McpAsyncClient> asynClients = getMcpAsyncClientList();
		if (asynClients.isEmpty()) {
			throw new IllegalStateException("No McpAsyncClient available");
		}
		int currentIndex = index.getAndUpdate(index -> (index + 1) % asynClients.size());

		return asynClients.get(currentIndex);
	}

	public List<McpAsyncClient> getMcpAsyncClientList() {
		return md5ToClientMap.values().stream().flatMap(List::stream).toList();
	}

	public String getServiceName() {
		return serviceName;
	}

	public NamingService getNamingService() {
		return this.namingService;
	}

	public List<Instance> getInstances() {
		return this.instances;
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
			logger.info("Closed and removed McpSyncClient: {}", mcpAsyncClient.getClientInfo().name());
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
		String toolName = callToolRequest.name();
		List<McpAsyncClient> asyncClients = new ArrayList<>();
		md5ToToolsMap.forEach((md5, tools) -> {
			if (tools.contains(toolName)) {
				asyncClients.addAll(md5ToClientMap.get(md5));
			}
		});
		int currentIndex = index.getAndUpdate(index -> (index + 1) % asyncClients.size());

		return asyncClients.get(currentIndex).callTool(callToolRequest);
	}

	public Mono<McpSchema.ListToolsResult> listTools() {
		return listToolsInternal(null);
	}

	public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
		return listToolsInternal(cursor);
	}

	private Mono<McpSchema.ListToolsResult> listToolsInternal(String cursor) {
		return loadConfig().flatMap(content -> parseConfig(content, cursor));
	}

	private Mono<String> loadConfig() {
		return Mono.fromCallable(() -> {
			String content = nacosConfigService.getConfig(this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX,
					McpNacosConstant.TOOLS_GROUP, TIME_OUT_MS);
			if (content == null || content.isEmpty()) {
				throw new RuntimeException(String.format("Empty tool config content for dataId: %s, group: %s",
						this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP));
			}
			return content;
		}).onErrorMap(throwable -> {
			throw new RuntimeException(String.format("Empty tool config content for dataId: %s, group: %s",
					this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP));
		});
	}

	private Mono<McpSchema.ListToolsResult> parseConfig(String content, String cursor) {
		return Mono.fromCallable(() -> {
			try {
				McpToolsInfo mcpToolsInfo = objectMapper.readValue(content, McpToolsInfo.class);
				return new McpSchema.ListToolsResult(mcpToolsInfo.getTools(), cursor);
			}
			catch (JsonProcessingException e) {
				logger.error("Failed to parse config for dataId: {}, group: {}",
						this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP, e);
				throw new RuntimeException(
						String.format("Failed to parse tool list, dataId: %s, group: %s\"",
								this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP),
						e);
			}
		}).onErrorMap(throwable -> {
			// 已处理过 JsonProcessingException，这里防止其他异常穿透
			logger.error("Unexpected error during parsing tool config for dataId: {}, group: {}",
					this.serviceName + McpNacosConstant.TOOLS_CONFIG_SUFFIX, McpNacosConstant.TOOLS_GROUP, throwable);
			return new RuntimeException(throwable);
		});
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

	@Override
	public void onEvent(Event event) {
		if (event instanceof NamingEvent namingEvent) {
			if ((this.serviceName + McpNacosConstant.SERVER_NAME_SUFFIX).equals(namingEvent.getServiceName())) {
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

	private McpAsyncClient clientByInstance(Instance instance) {
		McpAsyncClient asyncClient;

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
		asyncSpec = mcpAsyncClientConfigurer.configure(namedTransport.name(), asyncSpec);
		asyncClient = asyncSpec.build();
		if (commonProperties.isInitialized()) {
			asyncClient.initialize().block();
		}
		logger.info("Added McpAsyncClient: {}", clientInfo.name());
		return asyncClient;
	}

	private void updateByAddInstance(Instance instance) {
		Map<String, String> metadata = instance.getMetadata();
		String serverMd5 = metadata.get("server.md5");
		assert serverMd5 != null;
		McpAsyncClient mcpAsyncClient = clientByInstance(instance);
		md5ToClientMap.computeIfAbsent(serverMd5, k -> new ArrayList<>()).add(mcpAsyncClient);

		if (!md5ToToolsMap.containsKey(serverMd5)) {
			String tools = metadata.get("tools.names");
			md5ToToolsMap.put(serverMd5, List.of(tools.split(",")));
		}
	}

	private void updateByRemoveInstance(Instance instance) {
		String clientInfoName = connectedClientName(commonProperties.getName(),
				this.serviceName + "-" + instance.getInstanceId());
		String serverMd5 = instance.getMetadata().get("server.md5");

		List<McpAsyncClient> clientList = md5ToClientMap.getOrDefault(serverMd5, Collections.emptyList());
		McpAsyncClient asyncClient;
		for (McpAsyncClient mcpAsyncClient : clientList) {
			McpSchema.Implementation clientInfo = mcpAsyncClient.getClientInfo();
			String clientName = clientInfo.name();
			if (clientInfoName.equals(clientName)) {
				logger.info("Removing McpAsyncClient: {}", clientName);
				asyncClient = mcpAsyncClient;
				asyncClient.closeGracefully().block();
				// 安全地移除
				md5ToClientMap.get(serverMd5).remove(asyncClient);

				if (md5ToClientMap.get(serverMd5).isEmpty()) {
					md5ToClientMap.remove(serverMd5);
					md5ToToolsMap.remove(serverMd5);
				}
				logger.info("Removed McpAsyncClient: {} Success", asyncClient.getClientInfo().name());
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

		public LoadbalancedMcpAsyncClient build() {
			return new LoadbalancedMcpAsyncClient(this.serviceName, this.serviceGroup, this.namingService,
					this.nacosConfigService, this.applicationContext);
		}

	}

}
