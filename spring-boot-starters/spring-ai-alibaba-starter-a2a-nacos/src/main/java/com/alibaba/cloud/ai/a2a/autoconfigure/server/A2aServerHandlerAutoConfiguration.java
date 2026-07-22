/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.autoconfigure.server;

import com.alibaba.cloud.ai.a2a.autoconfigure.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentRequestRouter;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.a2a.core.server.A2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.core.server.DefaultA2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.core.server.GraphAgentExecutor;
import com.alibaba.cloud.ai.a2a.core.server.JsonRpcA2aRequestHandler;
import com.alibaba.cloud.ai.a2a.core.server.ServerTypeEnum;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;

/**
 * A2A server handler auto-configuration for single-agent mode.
 * <p>
 * This configuration is skipped when multi-agent mode is active
 * (when {@link MultiAgentRequestRouter} bean exists).
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = { A2aServerAgentCardAutoConfiguration.class, A2aServerMultiAgentAutoConfiguration.class })
@EnableConfigurationProperties({ A2aServerProperties.class })
@ConditionalOnBean({ AgentCard.class, Agent.class })
@ConditionalOnMissingBean(MultiAgentRequestRouter.class)
public class A2aServerHandlerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public A2aServerExecutorProvider a2aServerExecutorProvider() {
		return new DefaultA2aServerExecutorProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentExecutor agentExecutor(Agent rootAgent) {
		// FIXME: currently only ReactAgent and A2aRemoteAgent are supported as the root
		if (!(rootAgent instanceof ReactAgent) && !(rootAgent instanceof A2aRemoteAgent)) {
			throw new IllegalArgumentException(
					"The root agent must be an instance of ReactAgent or A2aRemoteAgent, other type will be supported later.");
		}
		return new GraphAgentExecutor(rootAgent);
	}

	@Bean
	@ConditionalOnMissingBean
	public A2AConfigProvider a2aConfigProvider() {
		return new DefaultValuesConfigProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskStore taskStore() {
		return new InMemoryTaskStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public MainEventBus mainEventBus() {
		return new MainEventBus();
	}

	@Bean
	@ConditionalOnMissingBean
	public QueueManager queueManager(TaskStore taskStore, MainEventBus mainEventBus) {
		return new InMemoryQueueManager(TaskStateProviderAdapter.from(taskStore), mainEventBus);
	}

	@Bean
	@ConditionalOnMissingBean
	public PushNotificationConfigStore pushConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public PushNotificationSender pushSender(PushNotificationConfigStore pushConfigStore) {
		return new BasePushNotificationSender(pushConfigStore);
	}

	@Bean
	@ConditionalOnMissingBean
	public MainEventBusProcessor mainEventBusProcessor(MainEventBus mainEventBus, TaskStore taskStore,
			PushNotificationSender pushSender, QueueManager queueManager) {
		return new MainEventBusProcessor(mainEventBus, taskStore, pushSender, queueManager);
	}

	@Bean
	@ConditionalOnMissingBean
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, MainEventBusProcessor mainEventBusProcessor,
			A2aServerExecutorProvider a2aServerExecutorProvider) {
		return DefaultRequestHandler.create(agentExecutor, taskStore, queueManager, pushConfigStore,
				mainEventBusProcessor, a2aServerExecutorProvider.getA2aServerExecutor(),
				a2aServerExecutorProvider.getEventConsumerExecutor());
	}

	@Bean
	@ConditionalOnProperty(prefix = A2aServerProperties.CONFIG_PREFIX, value = "type",
			havingValue = ServerTypeEnum.JSON_RPC_TYPE, matchIfMissing = true)
	public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler,
			A2aServerExecutorProvider a2aServerExecutorProvider) {
		return new JSONRPCHandler(agentCard, requestHandler, a2aServerExecutorProvider.getA2aServerExecutor());
	}

	@Bean
	@ConditionalOnProperty(prefix = A2aServerProperties.CONFIG_PREFIX, value = "type",
			havingValue = ServerTypeEnum.JSON_RPC_TYPE, matchIfMissing = true)
	public JsonRpcA2aRequestHandler jsonRpcA2aRequestHandler(JSONRPCHandler jsonrpcHandler) {
		return new JsonRpcA2aRequestHandler(jsonrpcHandler);
	}

}
