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
import com.alibaba.cloud.ai.a2a.core.constants.A2aConstants;
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
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
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
	public QueueManager multiAgentQueueManager() {
		return new InMemoryQueueManager();
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
	public MultiAgentRequestRouter multiAgentRequestRouter() {
		return new MultiAgentRequestRouter();
	}

	@Bean
	public List<AgentCard> multiAgentCards(A2aServerProperties serverProperties,
			A2aMultiAgentProperties multiAgentProperties, ObjectProvider<Map<String, Agent>> agentMapProvider,
			MultiAgentRequestRouter router, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender,
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
			RequestHandler requestHandler = new DefaultRequestHandler(agentExecutor, taskStore, queueManager,
					pushConfigStore, pushSender, executorProvider.getA2aServerExecutor());
			JSONRPCHandler jsonRpcHandler = new JSONRPCHandler(agentCard, requestHandler);
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
				: new AgentCapabilities.Builder().streaming(true).build();

		String url = StringUtils.hasLength(cardProps.getUrl()) ? cardProps.getUrl()
				: buildMultiAgentUrl(serverProperties, agentKey);

		return new AgentCard.Builder().name(name)
			.description(description)
			.defaultInputModes(inputModes)
			.defaultOutputModes(outputModes)
			.capabilities(capabilities)
			.version(serverProperties.getVersion())
			.protocolVersion(A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION)
			.preferredTransport(serverProperties.getType())
			.url(url)
			.supportsAuthenticatedExtendedCard(cardProps.isSupportsAuthenticatedExtendedCard())
			.skills(cardProps.getSkills() != null ? cardProps.getSkills() : List.of())
			.provider(cardProps.getProvider())
			.documentationUrl(cardProps.getDocumentationUrl())
			.security(cardProps.getSecurity())
			.securitySchemes(cardProps.getSecuritySchemes())
			.iconUrl(cardProps.getIconUrl())
			.additionalInterfaces(buildAdditionalInterfaces(cardProps, serverProperties, agentKey))
			.build();
	}

	private String buildMultiAgentUrl(A2aServerProperties serverProperties, String agentKey) {
		return DEFAULT_PROTOCOL + serverProperties.getAddress() + ":" + serverProperties.getPort() + "/a2a/"
				+ agentKey;
	}

	private List<AgentInterface> buildAdditionalInterfaces(A2aAgentCardProperties cardProps,
			A2aServerProperties serverProperties, String agentKey) {
		if (cardProps.getAdditionalInterfaces() != null) {
			return cardProps.getAdditionalInterfaces();
		}
		String url = StringUtils.hasLength(cardProps.getUrl()) ? cardProps.getUrl()
				: buildMultiAgentUrl(serverProperties, agentKey);
		return List.of(new AgentInterface(serverProperties.getType(), url));
	}

}
