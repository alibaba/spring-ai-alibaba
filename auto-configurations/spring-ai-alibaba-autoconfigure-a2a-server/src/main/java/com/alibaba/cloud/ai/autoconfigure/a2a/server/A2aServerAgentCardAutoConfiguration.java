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

import java.util.List;

import com.alibaba.cloud.ai.a2a.A2aServerAgentCardProperties;
import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.constants.A2aConstants;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * @author xiweng.yy
 */
@AutoConfiguration
@ConditionalOnMissingBean(AgentCard.class)
@EnableConfigurationProperties({ A2aServerProperties.class, A2aServerAgentCardProperties.class })
public class A2aServerAgentCardAutoConfiguration {

	private static final String DEFAULT_PROTOCOL = "http://";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({ BaseAgent.class })
	public AgentCard agentCard(BaseAgent rootAgent, A2aServerProperties a2aServerProperties,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return new AgentCard.Builder().name(getName(rootAgent, a2AServerAgentCardProperties))
			.description(getDescription(rootAgent, a2AServerAgentCardProperties))
			.defaultInputModes(getDefaultInputModes(rootAgent, a2AServerAgentCardProperties))
			.defaultOutputModes(getDefaultOutputModes(rootAgent, a2AServerAgentCardProperties))
			.capabilities(getCapabilities(rootAgent, a2AServerAgentCardProperties))
			.version(a2aServerProperties.getVersion())
			.protocolVersion(A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION)
			.preferredTransport(a2aServerProperties.getType())
			.url(getUrl(a2aServerProperties, a2AServerAgentCardProperties))
			.supportsAuthenticatedExtendedCard(a2AServerAgentCardProperties.isSupportsAuthenticatedExtendedCard())
			.skills(getAgentSkills(rootAgent, a2AServerAgentCardProperties))
			.provider(a2AServerAgentCardProperties.getProvider())
			.documentationUrl(a2AServerAgentCardProperties.getDocumentationUrl())
			.security(a2AServerAgentCardProperties.getSecurity())
			.securitySchemes(a2AServerAgentCardProperties.getSecuritySchemes())
			.iconUrl(a2AServerAgentCardProperties.getIconUrl())
			.additionalInterfaces(getAdditionalInterfaces(a2AServerAgentCardProperties, a2aServerProperties))
			.build();
	}

	private String getName(BaseAgent rootAgent, A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getName()) ? a2AServerAgentCardProperties.getName()
				: rootAgent.name();
	}

	private String getDescription(BaseAgent rootAgent, A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getDescription())
				? a2AServerAgentCardProperties.getDescription() : rootAgent.name();
	}

	private List<String> getDefaultInputModes(BaseAgent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getDefaultInputModes()
				? a2AServerAgentCardProperties.getDefaultInputModes() : List.of("text/plain");
	}

	private AgentCapabilities getCapabilities(BaseAgent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getCapabilities() ? a2AServerAgentCardProperties.getCapabilities()
				: new AgentCapabilities.Builder().streaming(true).build();
	}

	private List<String> getDefaultOutputModes(BaseAgent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getDefaultOutputModes()
				? a2AServerAgentCardProperties.getDefaultOutputModes() : List.of("text/plain");
	}

	private String getUrl(A2aServerProperties a2aServerProperties,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return StringUtils.hasLength(a2AServerAgentCardProperties.getUrl()) ? a2AServerAgentCardProperties.getUrl()
				: buildUrl(a2aServerProperties);
	}

	private List<AgentSkill> getAgentSkills(BaseAgent rootAgent,
			A2aServerAgentCardProperties a2AServerAgentCardProperties) {
		return null != a2AServerAgentCardProperties.getSkills() ? a2AServerAgentCardProperties.getSkills() : List.of();
	}

	private List<AgentInterface> getAdditionalInterfaces(A2aServerAgentCardProperties a2AServerAgentCardProperties,
			A2aServerProperties a2aServerProperties) {
		if (null != a2AServerAgentCardProperties.getAdditionalInterfaces()) {
			return a2AServerAgentCardProperties.getAdditionalInterfaces();
		}
		return List.of(new AgentInterface(a2aServerProperties.getType(), buildUrl(a2aServerProperties)));
	}

	private String buildUrl(A2aServerProperties a2aServerProperties) {
		return DEFAULT_PROTOCOL + a2aServerProperties.getAddress() + ":" + a2aServerProperties.getPort()
				+ a2aServerProperties.getMessageUrl();
	}

}
