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

import com.alibaba.cloud.ai.a2a.A2aAgentCardProperties;
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
@EnableConfigurationProperties({A2aServerProperties.class, A2aAgentCardProperties.class})
public class A2aAgentCardAutoConfiguration {

	private static final String DEFAULT_PROTOCOL = "http://";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({BaseAgent.class})
	public AgentCard agentCard(BaseAgent rootAgent, A2aServerProperties a2aServerProperties, A2aAgentCardProperties a2aAgentCardProperties) {
		return new AgentCard.Builder().name(getName(rootAgent, a2aAgentCardProperties))
				.description(getDescription(rootAgent, a2aAgentCardProperties))
				.defaultInputModes(getDefaultInputModes(rootAgent, a2aAgentCardProperties))
				.defaultOutputModes(getDefaultOutputModes(rootAgent, a2aAgentCardProperties))
				.capabilities(getCapabilities(rootAgent, a2aAgentCardProperties))
				.version(a2aServerProperties.getVersion())
				.protocolVersion(A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION)
				.preferredTransport(a2aServerProperties.getType())
				.url(getUrl(a2aServerProperties, a2aAgentCardProperties))
				.supportsAuthenticatedExtendedCard(a2aAgentCardProperties.isSupportsAuthenticatedExtendedCard())
				.skills(getAgentSkills(rootAgent, a2aAgentCardProperties))
				.provider(a2aAgentCardProperties.getProvider())
				.documentationUrl(a2aAgentCardProperties.getDocumentationUrl())
				.security(a2aAgentCardProperties.getSecurity())
				.securitySchemes(a2aAgentCardProperties.getSecuritySchemes())
				.iconUrl(a2aAgentCardProperties.getIconUrl())
				.additionalInterfaces(getAdditionalInterfaces(a2aAgentCardProperties, a2aServerProperties))
				.build();
	}

	private String getName(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return StringUtils.hasLength(a2aAgentCardProperties.getName()) ? a2aAgentCardProperties.getName() : rootAgent.name();
	}

	private String getDescription(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return StringUtils.hasLength(a2aAgentCardProperties.getDescription()) ? a2aAgentCardProperties.getDescription() : rootAgent.name();
	}

	private List<String> getDefaultInputModes(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return null != a2aAgentCardProperties.getDefaultInputModes() ? a2aAgentCardProperties.getDefaultInputModes() : List.of("text/plain");
	}

	private AgentCapabilities getCapabilities(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return null != a2aAgentCardProperties.getCapabilities() ? a2aAgentCardProperties.getCapabilities() : new AgentCapabilities.Builder().streaming(true)
				.build();
	}

	private List<String> getDefaultOutputModes(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return null != a2aAgentCardProperties.getDefaultOutputModes() ? a2aAgentCardProperties.getDefaultOutputModes() : List.of("text/plain");
	}

	private String getUrl(A2aServerProperties a2aServerProperties, A2aAgentCardProperties a2aAgentCardProperties) {
		return StringUtils.hasLength(a2aAgentCardProperties.getUrl()) ? a2aAgentCardProperties.getUrl() : buildUrl(a2aServerProperties);
	}

	private List<AgentSkill> getAgentSkills(BaseAgent rootAgent, A2aAgentCardProperties a2aAgentCardProperties) {
		return null != a2aAgentCardProperties.getSkills() ? a2aAgentCardProperties.getSkills() : List.of();
	}

	private List<AgentInterface> getAdditionalInterfaces(A2aAgentCardProperties a2aAgentCardProperties, A2aServerProperties a2aServerProperties) {
		if (null != a2aAgentCardProperties.getAdditionalInterfaces()) {
			return a2aAgentCardProperties.getAdditionalInterfaces();
		}
		return List.of(new AgentInterface(a2aServerProperties.getType(), buildUrl(a2aServerProperties)));
	}

	private String buildUrl(A2aServerProperties a2aServerProperties) {
		return DEFAULT_PROTOCOL + a2aServerProperties.getAddress() + ":" + a2aServerProperties.getPort()
				+ a2aServerProperties.getMessageUrl();
	}

}
