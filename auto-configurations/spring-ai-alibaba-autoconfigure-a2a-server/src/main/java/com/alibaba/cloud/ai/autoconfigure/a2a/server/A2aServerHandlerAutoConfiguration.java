/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.autoconfigure.a2a.server;

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.server.A2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.server.DefaultA2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.server.GraphAgentExecutor;
import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;
import com.alibaba.cloud.ai.a2a.server.ServerTypeEnum;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * A2A server handler autoconfiguration.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = A2aServerAgentCardAutoConfiguration.class)
@EnableConfigurationProperties({ A2aServerProperties.class })
@ConditionalOnBean({ AgentCard.class, BaseAgent.class })
public class A2aServerHandlerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public A2aServerExecutorProvider a2aServerExecutorProvider() {
		return new DefaultA2aServerExecutorProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public AgentExecutor agentExecutor(BaseAgent rootAgent) {
		return new GraphAgentExecutor(rootAgent);
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskStore taskStore() {
		return new InMemoryTaskStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public QueueManager queueManager() {
		return new InMemoryQueueManager();
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
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender,
			A2aServerExecutorProvider a2aServerExecutorProvider) {
		return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender,
				a2aServerExecutorProvider.getA2aServerExecutor());
	}

	@Bean
	@ConditionalOnProperty(prefix = A2aServerProperties.CONFIG_PREFIX, value = "type",
			havingValue = ServerTypeEnum.JSON_RPC_TYPE, matchIfMissing = true)
	public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler) {
		return new JSONRPCHandler(agentCard, requestHandler);
	}

	@Bean
	@ConditionalOnProperty(prefix = A2aServerProperties.CONFIG_PREFIX, value = "type",
			havingValue = ServerTypeEnum.JSON_RPC_TYPE, matchIfMissing = true)
	public JsonRpcA2aRequestHandler jsonRpcA2aRequestHandler(JSONRPCHandler jsonrpcHandler) {
		return new JsonRpcA2aRequestHandler(jsonrpcHandler);
	}

}
