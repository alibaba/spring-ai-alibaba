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

package com.alibaba.cloud.ai.autoconfigure.a2a.client;

import com.alibaba.cloud.ai.a2a.A2aClientAgentCardProperties;
import com.alibaba.cloud.ai.autoconfigure.a2a.client.condition.A2aClientAgentCardWellKnownCondition;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider;
import io.a2a.spec.AgentCard;

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
		AgentCard agentCard = new AgentCard.Builder().name(a2aClientAgentCardProperties.getName())
			.description(a2aClientAgentCardProperties.getDescription())
			.url(a2aClientAgentCardProperties.getUrl())
			.provider(a2aClientAgentCardProperties.getProvider())
			.documentationUrl(a2aClientAgentCardProperties.getDocumentationUrl())
			.capabilities(a2aClientAgentCardProperties.getCapabilities())
			.defaultInputModes(a2aClientAgentCardProperties.getDefaultInputModes())
			.defaultOutputModes(a2aClientAgentCardProperties.getDefaultOutputModes())
			.skills(a2aClientAgentCardProperties.getSkills())
			.supportsAuthenticatedExtendedCard(a2aClientAgentCardProperties.isSupportsAuthenticatedExtendedCard())
			.securitySchemes(a2aClientAgentCardProperties.getSecuritySchemes())
			.security(a2aClientAgentCardProperties.getSecurity())
			.iconUrl(a2aClientAgentCardProperties.getIconUrl())
			.additionalInterfaces(a2aClientAgentCardProperties.getAdditionalInterfaces())
			.version(a2aClientAgentCardProperties.getVersion())
			.protocolVersion(a2aClientAgentCardProperties.getProtocolVersion())
			.preferredTransport(a2aClientAgentCardProperties.getPreferredTransport())
			.build();
		return () -> agentCard;
	}

}
