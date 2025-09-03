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

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author xiweng.yy
 */
@AutoConfiguration
@ConditionalOnMissingBean(AgentCard.class)
@EnableConfigurationProperties({ A2aServerProperties.class })
public class A2aAgentCardAutoConfiguration {

	private static final String DEFAULT_PROTOCOL = "http://";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({ BaseAgent.class })
	public AgentCard agentCard(BaseAgent rootAgent, A2aServerProperties a2aServerProperties) {
		return new AgentCard.Builder().name(rootAgent.name())
			.description(rootAgent.description())
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.capabilities(new AgentCapabilities.Builder().streaming(true)
				.pushNotifications(false)
				.stateTransitionHistory(true)
				.build())
			.supportsAuthenticatedExtendedCard(false)
			.version("1.0.0")
			.protocolVersion("0.2.5")
			.preferredTransport("JSONRPC")
			.url(buildUrl(a2aServerProperties))
			.skills(List.of())
			.build();
	}

	private String buildUrl(A2aServerProperties a2aServerProperties) {
		return DEFAULT_PROTOCOL + a2aServerProperties.getAddress() + ":" + a2aServerProperties.getPort()
				+ a2aServerProperties.getMessageUrl();
	}

}
