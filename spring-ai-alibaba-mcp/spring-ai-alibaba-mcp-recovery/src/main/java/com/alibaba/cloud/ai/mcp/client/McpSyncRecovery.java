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

package com.alibaba.cloud.ai.mcp.client;

import com.alibaba.cloud.ai.mcp.client.component.CommonUtil;
import com.alibaba.cloud.ai.mcp.client.component.McpReconnectTask;
import com.alibaba.cloud.ai.mcp.client.component.McpSyncClientWrapper;
import com.alibaba.cloud.ai.mcp.client.config.McpRecoveryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.autoconfigure.configurer.McpSyncClientConfigurer;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author yingzi
 * @since 2025/7/14
 */

public class McpSyncRecovery {

	private static final Logger logger = LoggerFactory.getLogger(McpSyncRecovery.class);

	private final McpRecoveryProperties mcpRecoveryProperties;

	private final McpSseClientProperties mcpSseClientProperties;

	private final McpClientCommonProperties commonProperties;

	private final McpSyncClientConfigurer mcpSyncClientConfigurer;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final WebClient.Builder webClientBuilderTemplate = WebClient.builder();

	private final CommonUtil commonUtil;

	private volatile boolean isRunning = true;

	private final Map<String, McpSyncClientWrapper> mcpClientWrapperMap = new ConcurrentHashMap<>();

	private final DelayQueue<McpReconnectTask> reconnectTaskQueue = new DelayQueue<>();

	public McpSyncRecovery(McpRecoveryProperties mcpRecoveryProperties, McpSseClientProperties mcpSseClientProperties,
			McpClientCommonProperties mcpClientCommonProperties, McpSyncClientConfigurer mcpSyncClientConfigurer) {
		this.mcpRecoveryProperties = mcpRecoveryProperties;

		this.mcpSseClientProperties = mcpSseClientProperties;
		this.commonProperties = mcpClientCommonProperties;
		this.mcpSyncClientConfigurer = mcpSyncClientConfigurer;

		this.commonUtil = new CommonUtil(mcpRecoveryProperties);
	}

	public void init() {
		Map<String, McpSseClientProperties.SseParameters> connections = mcpSseClientProperties.getConnections();
		if (CollectionUtils.isEmpty(connections)) {
			logger.warn("No MCP connection config found.");
			return;
		}
		connections.forEach((serviceName, params) -> {
			boolean clientCreated = createClient(serviceName, params);
			if (!clientCreated) {
				// 如果创建失败，将任务重新放回队列
				reconnectTaskQueue.offer(new McpReconnectTask(serviceName,
						mcpRecoveryProperties.getDelay().getSeconds(), TimeUnit.SECONDS));
				logger.warn("Failed to create client for serviceName: {}, will retry.", serviceName);
			}
		});
	}

	public void startReconnectTask() {
		commonUtil.getReconnectExecutor().submit(this::processReconnectQueue);
	}

	private void processReconnectQueue() {
		while (isRunning) {
			try {
				McpReconnectTask task = reconnectTaskQueue.take(); // 从队列中取出任务
				String serviceName = task.getServerName();
				logger.debug("Processing reconnect task for serviceName: {}", serviceName);
				// 尝试创建客户端
				boolean clientCreated = createClient(serviceName,
						mcpSseClientProperties.getConnections().get(serviceName));
				if (!clientCreated) {
					// 如果创建失败，将任务重新放回队列
					reconnectTaskQueue.offer(task);
					logger.warn("Failed to create client for service: {}, will retry.", serviceName);
				}
			}
			catch (InterruptedException e) {
				logger.debug("Reconnect thread interrupted", e);
				Thread.currentThread().interrupt();
			}
			catch (Exception e) {
				logger.error("Error in reconnect thread", e);
			}
		}
	}

	private boolean createClient(String key, McpSseClientProperties.SseParameters params) {
		try {
			WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(params.url());
			String sseEndpoint = params.sseEndpoint() != null ? params.sseEndpoint() : "/sse";
			WebFluxSseClientTransport transport = WebFluxSseClientTransport.builder(webClientBuilder)
				.sseEndpoint(sseEndpoint)
				.objectMapper(objectMapper)
				.build();
			NamedClientMcpTransport namedTransport = new NamedClientMcpTransport(key, transport);

			McpSchema.Implementation clientInfo = new McpSchema.Implementation(
					CommonUtil.connectedClientName(commonProperties.getName(), namedTransport.name()),
					commonProperties.getVersion());
			McpClient.SyncSpec syncSpec = McpClient.sync(namedTransport.transport())
				.clientInfo(clientInfo)
				.requestTimeout(commonProperties.getRequestTimeout());
			syncSpec = mcpSyncClientConfigurer.configure(namedTransport.name(), syncSpec);
			McpSyncClient syncClient = syncSpec.build();
			if (commonProperties.isInitialized()) {
				// Access the delegate field using a helper method
				McpAsyncClient mcpAsyncClient = getAsyncClientFromSyncClient(syncClient);

				mcpAsyncClient.initialize().doOnError(WebClientRequestException.class, ex -> {
					logger.error("WebClientRequestException occurred during initialization: {}", ex.getMessage());
					isRunning = false;
				}).subscribe(result -> {
					if (result != null) {
						logger.info("Sync client initialization successful");
					}
				});
			}
			if (isRunning) {
				logger.info("Initialized server name: {} with server URL: {}", key, params.url());
				List<ToolCallback> callbacks = Arrays
					.asList(new SyncMcpToolCallbackProvider(syncClient).getToolCallbacks());
				mcpClientWrapperMap.put(key, new McpSyncClientWrapper(syncClient, callbacks));
			}
			return isRunning;
		}
		catch (Exception e) {
			isRunning = false;
			logger.error("Unexpected error occurred during reconnection process", e);
			return isRunning;
		}
	}

	public void startScheduledPolling() {
		commonUtil.getPingScheduler()
			.scheduleAtFixedRate(this::checkMcpClients, mcpRecoveryProperties.getPing().getSeconds(),
					mcpRecoveryProperties.getPing().getSeconds(), TimeUnit.SECONDS);
	}

	private void checkMcpClients() {
		logger.debug("Checking MCP clients...");
		checkAndRestartTask();

		mcpClientWrapperMap.forEach((serviceName, wrapperClient) -> {
			McpSyncClient syncClient = wrapperClient.getClient();
			// Access the delegate field using a helper method
			McpAsyncClient mcpAsyncClient = getAsyncClientFromSyncClient(syncClient);
			mcpAsyncClient.ping().doOnError(WebClientResponseException.class, ex -> {
				logger.error("Ping failed for {}", serviceName);
				mcpClientWrapperMap.remove(serviceName);
				reconnectTaskQueue.offer(new McpReconnectTask(serviceName,
						mcpRecoveryProperties.getDelay().getSeconds(), TimeUnit.SECONDS));
				logger.info("need reconnect: {}", serviceName);
			}).subscribe();
		});
	}

	private McpAsyncClient getAsyncClientFromSyncClient(McpSyncClient syncClient) {
		Field delegateField;
		try {
			delegateField = McpSyncClient.class.getDeclaredField("delegate");
			delegateField.setAccessible(true);
			McpAsyncClient asyncClient;
			asyncClient = (McpAsyncClient) delegateField.get(syncClient);
			return asyncClient;
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			logger.error("Failed to access delegate field in McpSyncClient", e);
			throw new RuntimeException(e);
		}
	}

	private void checkAndRestartTask() {
		if (!isRunning) {
			logger.info("Restarting task...");
			isRunning = true;
			startReconnectTask();
		}
	}

	public List<ToolCallback> getToolCallback() {
		return mcpClientWrapperMap.values()
			.stream()
			.map(McpSyncClientWrapper::getToolCallbacks)
			.flatMap(List::stream)
			.toList();
	}

	public void stop() {
		isRunning = false;
		commonUtil.stop();
	}

}
