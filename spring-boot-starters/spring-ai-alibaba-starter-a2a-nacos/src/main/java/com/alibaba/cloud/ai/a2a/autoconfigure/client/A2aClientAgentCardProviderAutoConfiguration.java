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

package com.alibaba.cloud.ai.a2a.autoconfigure.client;

import com.alibaba.cloud.ai.a2a.autoconfigure.A2aClientAgentCardProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.client.condition.A2aClientAgentCardWellKnownCondition;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider;
import com.alibaba.cloud.ai.a2a.core.constants.A2aConstants;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.SecurityRequirement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * Autoconfiguration for A2A client agent card provider.
 *
 * @author xiweng.yy
 */
@AutoConfiguration
@EnableConfigurationProperties({ A2aClientAgentCardProperties.class })
public class A2aClientAgentCardProviderAutoConfiguration {

	@Bean
	@ConditionalOnClass({ AgentCardProvider.class })
	@Conditional(A2aClientAgentCardWellKnownCondition.class)
	public AgentCardProvider remoteAgentCardProvider(A2aClientAgentCardProperties a2aClientAgentCardProperties) {
		return RemoteAgentCardProvider.newProvider(a2aClientAgentCardProperties.getWellKnownUrl());
	}

	@Bean
	@ConditionalOnClass({ AgentCardProvider.class })
	@ConditionalOnProperty(prefix = A2aClientAgentCardProperties.CONFIG_PREFIX, value = "name")
	public AgentCardProvider localAgentCardProvider(A2aClientAgentCardProperties a2aClientAgentCardProperties) {
		AgentCapabilities configuredCapabilities = a2aClientAgentCardProperties.getCapabilities() == null
				? AgentCapabilities.builder().build() : a2aClientAgentCardProperties.getCapabilities();
		AgentCapabilities capabilities = new AgentCapabilities(configuredCapabilities.streaming(),
				configuredCapabilities.pushNotifications(), configuredCapabilities.extendedAgentCard()
						|| a2aClientAgentCardProperties.isSupportsAuthenticatedExtendedCard(),
				configuredCapabilities.extensions());
		AgentCard agentCard = AgentCard.builder().name(a2aClientAgentCardProperties.getName())
			.description(a2aClientAgentCardProperties.getDescription())
			.provider(a2aClientAgentCardProperties.getProvider())
			.documentationUrl(a2aClientAgentCardProperties.getDocumentationUrl())
			.capabilities(capabilities)
			.defaultInputModes(a2aClientAgentCardProperties.getDefaultInputModes() == null ? List.of()
					: a2aClientAgentCardProperties.getDefaultInputModes())
			.defaultOutputModes(a2aClientAgentCardProperties.getDefaultOutputModes() == null ? List.of()
					: a2aClientAgentCardProperties.getDefaultOutputModes())
			.skills(a2aClientAgentCardProperties.getSkills() == null ? List.of()
					: a2aClientAgentCardProperties.getSkills())
			.securitySchemes(a2aClientAgentCardProperties.getSecuritySchemes())
			.securityRequirements(a2aClientAgentCardProperties.getSecurity() == null ? null
					: a2aClientAgentCardProperties.getSecurity().stream().map(SecurityRequirement::new).toList())
			.iconUrl(a2aClientAgentCardProperties.getIconUrl())
			.supportedInterfaces(getSupportedInterfaces(a2aClientAgentCardProperties))
			.version(a2aClientAgentCardProperties.getVersion())
			.build();
		return () -> new AgentCardWrapper(agentCard);
	}

	private List<AgentInterface> getSupportedInterfaces(A2aClientAgentCardProperties properties) {
		List<AgentInterface> interfaces = new ArrayList<>();
		if (properties.getUrl() != null) {
			String transport = properties.getPreferredTransport() == null ? "JSONRPC"
					: properties.getPreferredTransport();
			String protocolVersion = properties.getProtocolVersion();
			if (protocolVersion == null || protocolVersion.isBlank()) {
				protocolVersion = A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION;
			}
			interfaces.add(new AgentInterface(transport, properties.getUrl(), null, protocolVersion));
		}
		if (properties.getAdditionalInterfaces() != null) {
			properties.getAdditionalInterfaces()
				.stream()
				.map(this::withDefaultProtocolVersion)
				.filter(agentInterface -> !interfaces.contains(agentInterface))
				.forEach(interfaces::add);
		}
		return List.copyOf(interfaces);
	}

	private AgentInterface withDefaultProtocolVersion(AgentInterface agentInterface) {
		if (agentInterface.protocolVersion() == null || agentInterface.protocolVersion().isBlank()) {
			return new AgentInterface(agentInterface.protocolBinding(), agentInterface.url(), agentInterface.tenant(),
					A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION);
		}
		return agentInterface;
	}

}
