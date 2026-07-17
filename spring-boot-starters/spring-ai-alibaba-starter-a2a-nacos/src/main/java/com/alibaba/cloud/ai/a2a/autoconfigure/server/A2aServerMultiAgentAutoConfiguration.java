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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.a2a.autoconfigure.A2aAgentCardProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.A2aMultiAgentProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.server.condition.OnMultiAgentModeCondition;
import com.alibaba.cloud.ai.a2a.core.registry.AgentRegistry;
import com.alibaba.cloud.ai.a2a.core.registry.AgentRegistryService;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentJsonRpcRouterProvider;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentRequestRouter;
import com.alibaba.cloud.ai.a2a.core.server.A2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.core.server.DefaultA2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.core.server.GraphAgentExecutor;
import com.alibaba.cloud.ai.a2a.core.server.JsonRpcA2aRequestHandler;
import com.alibaba.cloud.ai.a2a.core.server.ServerTypeEnum;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
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
import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-configuration for multi-agent A2A server support.
 * <p>
 * This configuration is activated when multiple agents are defined in the configuration:
 * <pre>
 * spring:
 *   ai:
 *     alibaba:
 *       a2a:
 *         server:
 *           agents:
 *             agent1:
 *               name: "Agent 1"
 *             agent2:
 *               name: "Agent 2"
 * </pre>
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = { A2aServerAgentCardAutoConfiguration.class, A2aServerHandlerAutoConfiguration.class })
@EnableConfigurationProperties({ A2aServerProperties.class, A2aMultiAgentProperties.class })
@Conditional(OnMultiAgentModeCondition.class)
public class A2aServerMultiAgentAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(A2aServerMultiAgentAutoConfiguration.class);

	private static final String DEFAULT_PROTOCOL = "http://";

	@Bean
	@ConditionalOnMissingBean
	public A2aServerExecutorProvider multiAgentA2aServerExecutorProvider() {
		return new DefaultA2aServerExecutorProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public TaskStore multiAgentTaskStore() {
		return new InMemoryTaskStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public MainEventBus multiAgentMainEventBus() {
		return new MainEventBus();
	}

	@Bean
	@ConditionalOnMissingBean
	public QueueManager multiAgentQueueManager(TaskStore taskStore, MainEventBus mainEventBus) {
		return new InMemoryQueueManager(asTaskStateProvider(taskStore), mainEventBus);
	}

	@Bean
	@ConditionalOnMissingBean
	public PushNotificationConfigStore multiAgentPushConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public PushNotificationSender multiAgentPushSender(PushNotificationConfigStore pushConfigStore) {
		return new BasePushNotificationSender(pushConfigStore);
	}

	@Bean
	@ConditionalOnMissingBean
	public MainEventBusProcessor multiAgentMainEventBusProcessor(MainEventBus mainEventBus, TaskStore taskStore,
			PushNotificationSender pushSender, QueueManager queueManager) {
		return new MainEventBusProcessor(mainEventBus, taskStore, pushSender, queueManager);
	}

	@Bean
	public MultiAgentRequestRouter multiAgentRequestRouter() {
		return new MultiAgentRequestRouter();
	}

	@Bean
	public List<AgentCard> multiAgentCards(A2aServerProperties serverProperties,
			A2aMultiAgentProperties multiAgentProperties, ObjectProvider<Map<String, Agent>> agentMapProvider,
			MultiAgentRequestRouter router, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, MainEventBusProcessor mainEventBusProcessor,
			A2aServerExecutorProvider executorProvider) {

		Map<String, Agent> agents = agentMapProvider.getIfAvailable();
		Map<String, A2aAgentCardProperties> agentConfigs = multiAgentProperties.getAgents();

		List<AgentCard> agentCards = new ArrayList<>();

		for (Map.Entry<String, A2aAgentCardProperties> entry : agentConfigs.entrySet()) {
			String agentKey = entry.getKey();
			A2aAgentCardProperties cardProps = entry.getValue();

			// Find the corresponding agent bean
			Agent agent = findAgentByName(agents, agentKey, cardProps);
			if (agent == null) {
				LOGGER.warn("No agent bean found for configuration key: {}. Skipping.", agentKey);
				continue;
			}

			// Validate agent type
			if (!(agent instanceof ReactAgent) && !(agent instanceof A2aRemoteAgent)) {
				LOGGER.warn("Agent {} is not a ReactAgent or A2aRemoteAgent. Skipping.", agentKey);
				continue;
			}

			// Build AgentCard
			AgentCard agentCard = buildAgentCard(agentKey, agent, serverProperties, cardProps);
			agentCards.add(agentCard);

			// Create and register handler for this agent
			AgentExecutor agentExecutor = new GraphAgentExecutor(agent);
			RequestHandler requestHandler = DefaultRequestHandler.create(agentExecutor, taskStore, queueManager,
					pushConfigStore, mainEventBusProcessor, executorProvider.getA2aServerExecutor(),
					executorProvider.getA2aServerExecutor());
			JSONRPCHandler jsonRpcHandler = new JSONRPCHandler(agentCard, requestHandler,
					executorProvider.getA2aServerExecutor());
			JsonRpcA2aRequestHandler a2aHandler = new JsonRpcA2aRequestHandler(jsonRpcHandler);

			router.registerHandler(agentKey, a2aHandler);
			LOGGER.info("Registered multi-agent A2A handler for: {} (bean: {})", agentKey, agent.name());
		}

		if (agentCards.isEmpty()) {
			LOGGER.warn("No valid agents found for multi-agent configuration. "
					+ "Make sure agent beans are registered and match configuration keys.");
		}

		return agentCards;
	}

	@Bean
	@ConditionalOnBean(AgentRegistry.class)
	public AgentRegistryService multiAgentRegistryService(List<AgentCard> multiAgentCards,
			AgentRegistry agentRegistry) {
		return new AgentRegistryService(agentRegistry, multiAgentCards);
	}

	@Bean
	@ConditionalOnProperty(prefix = A2aServerProperties.CONFIG_PREFIX, value = "type",
			havingValue = ServerTypeEnum.JSON_RPC_TYPE, matchIfMissing = true)
	public RouterFunction<ServerResponse> multiAgentRouterFunction(MultiAgentRequestRouter router) {
		return new MultiAgentJsonRpcRouterProvider(router).getRouter();
	}

	private Agent findAgentByName(Map<String, Agent> agents, String agentKey, A2aAgentCardProperties cardProps) {
		if (agents == null || agents.isEmpty()) {
			return null;
		}

		// First, try to find by bean name matching the config key
		if (agents.containsKey(agentKey)) {
			return agents.get(agentKey);
		}

		// Second, try to find by agent name matching the config's name property
		String configName = cardProps.getName();
		if (StringUtils.hasLength(configName)) {
			for (Agent agent : agents.values()) {
				if (configName.equals(agent.name())) {
					return agent;
				}
			}
		}

		// Third, try to find by agent name matching the config key
		for (Agent agent : agents.values()) {
			if (agentKey.equals(agent.name())) {
				return agent;
			}
		}

		return null;
	}

	private AgentCard buildAgentCard(String agentKey, Agent agent, A2aServerProperties serverProperties,
			A2aAgentCardProperties cardProps) {
		String name = StringUtils.hasLength(cardProps.getName()) ? cardProps.getName() : agent.name();
		String description = StringUtils.hasLength(cardProps.getDescription()) ? cardProps.getDescription()
				: agent.description();
		List<String> inputModes = cardProps.getDefaultInputModes() != null ? cardProps.getDefaultInputModes()
				: List.of("text/plain");
		List<String> outputModes = cardProps.getDefaultOutputModes() != null ? cardProps.getDefaultOutputModes()
				: List.of("text/plain");
		AgentCapabilities capabilities = cardProps.getCapabilities() != null ? cardProps.getCapabilities()
				: AgentCapabilities.builder().streaming(true).build();
		capabilities = new AgentCapabilities(capabilities.streaming(), capabilities.pushNotifications(),
				capabilities.extendedAgentCard() || cardProps.isSupportsAuthenticatedExtendedCard(),
				capabilities.extensions());

		return AgentCard.builder().name(name)
			.description(description)
			.defaultInputModes(inputModes)
			.defaultOutputModes(outputModes)
			.capabilities(capabilities)
			.version(serverProperties.getVersion())
			.supportedInterfaces(buildSupportedInterfaces(cardProps, serverProperties, agentKey))
			.skills(cardProps.getSkills() != null ? cardProps.getSkills() : List.of())
			.provider(cardProps.getProvider())
			.documentationUrl(cardProps.getDocumentationUrl())
			.securityRequirements(cardProps.getSecurity() == null ? null
					: cardProps.getSecurity().stream().map(SecurityRequirement::new).toList())
			.securitySchemes(cardProps.getSecuritySchemes())
			.iconUrl(cardProps.getIconUrl())
			.build();
	}

	private String buildMultiAgentUrl(A2aServerProperties serverProperties, String agentKey) {
		return DEFAULT_PROTOCOL + serverProperties.getAddress() + ":" + serverProperties.getPort() + "/a2a/"
				+ agentKey;
	}

	private List<AgentInterface> buildSupportedInterfaces(A2aAgentCardProperties cardProps,
			A2aServerProperties serverProperties, String agentKey) {
		String url = StringUtils.hasLength(cardProps.getUrl()) ? cardProps.getUrl()
				: buildMultiAgentUrl(serverProperties, agentKey);
		List<AgentInterface> interfaces = new ArrayList<>();
		interfaces.add(new AgentInterface(serverProperties.getType(), url));
		if (cardProps.getAdditionalInterfaces() != null) {
			cardProps.getAdditionalInterfaces()
				.stream()
				.filter(agentInterface -> !interfaces.contains(agentInterface))
				.forEach(interfaces::add);
		}
		return List.copyOf(interfaces);
	}

	private static TaskStateProvider asTaskStateProvider(TaskStore taskStore) {
		if (taskStore instanceof TaskStateProvider taskStateProvider) {
			return taskStateProvider;
		}
		return new TaskStateProvider() {
			@Override
			public boolean isTaskActive(String taskId) {
				Task task = taskStore.get(taskId);
				return task == null || !task.status().state().isFinal();
			}

			@Override
			public boolean isTaskFinalized(String taskId) {
				Task task = taskStore.get(taskId);
				return task != null && task.status().state().isFinal();
			}
		};
	}

}
