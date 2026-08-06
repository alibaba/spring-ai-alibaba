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
import com.alibaba.cloud.ai.a2a.core.constants.A2aConstants;
import com.alibaba.cloud.ai.graph.agent.Agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A2aServerAgentCardAutoConfigurationTest {

	@Test
	void defaultsMissingAdditionalInterfaceVersionAndPreservesExplicitVersion() {
		Agent agent = mock(Agent.class);
		when(agent.name()).thenReturn("test-agent");
		A2aServerProperties serverProperties = serverProperties();
		A2aServerAgentCardProperties cardProperties = new A2aServerAgentCardProperties();
		cardProperties.setAdditionalInterfaces(List.of(
				new AgentInterface("HTTP+JSON", "http://localhost:8080/http-json", "tenant-a", null),
				new AgentInterface("JSONRPC", "http://localhost:8081/a2a", "tenant-b", "0.3")));

		AgentCard card = new A2aServerAgentCardAutoConfiguration().agentCard(agent, serverProperties, cardProperties);

		assertThat(card.supportedInterfaces())
			.contains(new AgentInterface("HTTP+JSON", "http://localhost:8080/http-json", "tenant-a",
					A2aConstants.DEFAULT_A2A_PROTOCOL_VERSION))
			.contains(new AgentInterface("JSONRPC", "http://localhost:8081/a2a", "tenant-b", "0.3"));
	}

	private A2aServerProperties serverProperties() {
		A2aServerProperties properties = new A2aServerProperties();
		properties.setAddress("localhost");
		properties.setPort(8080);
		return properties;
	}

}
