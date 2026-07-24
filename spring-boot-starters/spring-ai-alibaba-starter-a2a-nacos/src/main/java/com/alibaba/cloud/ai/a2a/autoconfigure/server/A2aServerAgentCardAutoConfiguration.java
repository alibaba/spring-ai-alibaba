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

import com.alibaba.cloud.ai.a2a.autoconfigure.A2aServerAgentCardProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentRequestRouter;
import com.alibaba.cloud.ai.graph.agent.Agent;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.SecurityRequirement;

/**
 * Auto-configuration for single-agent A2A server.
 * <p>
 * This configuration is used when only a single agent is registered (traditional mode).
 * When multi-agent mode is enabled (agents map is configured), this configuration
 * is skipped in favor of {@link A2aServerMultiAgentAutoConfiguration}.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = A2aServerMultiAgentAutoConfiguration.class)
@ConditionalOnMissingBean({ AgentCard.class, MultiAgentRequestRouter.class })
@EnableConfigurationProperties({ A2aServerProperties.class, A2aServerAgentCardProperties.class })
public class A2aServerAgentCardAutoConfiguration {

	private static final String DEFAULT_PROTOCOL = "http://";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({ Agent.class })
	public AgentCard agentCard(Agent rootAgent, A2aServerProperties a2aServerProperties,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return AgentCard.builder().name(getName(rootAgent, a2AServerAgentCardProperties))
			.description(getDescription(rootAgent, a2AServerAgentCardProperties))
			.defaultInputModes(getDefaultInputModes(rootAgent, a2AServerAgentCardProperties))
			.defaultOutputModes(getDefaultOutputModes(rootAgent, a2AServerAgentCardProperties))
			.capabilities(getCapabilities(rootAgent, a2AServerAgentCardProperties))
			.version(a2aServerProperties.getVersion())
			.supportedInterfaces(getSupportedInterfaces(a2AServerAgentCardProperties, a2aServerProperties))
			.skills(getAgentSkills(rootAgent, a2AServerAgentCardProperties))
			.provider(a2AServerAgentCardProperties.getProvider())
			.documentationUrl(a2AServerAgentCardProperties.getDocumentationUrl())
			.securityRequirements(getSecurityRequirements(a2AServerAgentCardProperties))
			.securitySchemes(a2AServerAgentCardProperties.getSecuritySchemes())
			.iconUrl(a2AServerAgentCardProperties.getIconUrl())
			.build();
	}

	private String getName(Agent rootAgent, A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getName()) ? a2AServerAgentCardProperties.getName()
				: rootAgent.name();
	}

	private String getDescription(Agent rootAgent, A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getDescription())
				? a2AServerAgentCardProperties.getDescription() : rootAgent.name();
	}

	private List<String> getDefaultInputModes(Agent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getDefaultInputModes()
				? a2AServerAgentCardProperties.getDefaultInputModes() : List.of("text/plain");
	}

	private AgentCapabilities getCapabilities(Agent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		AgentCapabilities capabilities = null != a2AServerAgentCardProperties.getCapabilities()
				? a2AServerAgentCardProperties.getCapabilities()
				: AgentCapabilities.builder().streaming(true).build();
		return new AgentCapabilities(capabilities.streaming(), capabilities.pushNotifications(),
				capabilities.extendedAgentCard() || a2AServerAgentCardProperties.isSupportsAuthenticatedExtendedCard(),
				capabilities.extensions());
	}

	private List<String> getDefaultOutputModes(Agent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getDefaultOutputModes()
				? a2AServerAgentCardProperties.getDefaultOutputModes() : List.of("text/plain");
	}

	private String getUrl(A2aServerProperties a2aServerProperties,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getUrl()) ? a2AServerAgentCardProperties.getUrl()
				: buildUrl(a2aServerProperties);
	}

	private List<AgentSkill> getAgentSkills(Agent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getSkills() ? a2AServerAgentCardProperties.getSkills() : List.of();
	}

	private List<AgentInterface> getSupportedInterfaces(A2aServerAgentCardProperties a2AServerAgentCardProperties,
			A2aServerProperties a2aServerProperties) {
		List<AgentInterface> interfaces = new ArrayList<>();
		interfaces.add(new AgentInterface(a2aServerProperties.getType(),
				getUrl(a2aServerProperties, a2AServerAgentCardProperties)));
		if (a2AServerAgentCardProperties.getAdditionalInterfaces() != null) {
			a2AServerAgentCardProperties.getAdditionalInterfaces()
				.stream()
				.map(A2aAgentInterfaceNormalizer::withDefaultProtocolVersion)
				.filter(agentInterface -> !interfaces.contains(agentInterface))
				.forEach(interfaces::add);
		}
		return List.copyOf(interfaces);
	}

	private List<SecurityRequirement> getSecurityRequirements(
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return a2AServerAgentCardProperties.getSecurity() == null ? null
				: a2AServerAgentCardProperties.getSecurity().stream().map(SecurityRequirement::new).toList();
	}

	private String buildUrl(A2aServerProperties a2aServerProperties) {
		return DEFAULT_PROTOCOL + a2aServerProperties.getAddress() + ":" + a2aServerProperties.getPort()
				+ a2aServerProperties.getMessageUrl();
	}

}
