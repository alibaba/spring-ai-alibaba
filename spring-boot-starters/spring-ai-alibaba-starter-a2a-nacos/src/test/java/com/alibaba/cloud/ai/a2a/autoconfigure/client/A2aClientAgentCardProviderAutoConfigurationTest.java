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
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class A2aClientAgentCardProviderAutoConfigurationTest {

	private static final String AGENT_URL = "http://localhost:8080/a2a";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(A2aClientAgentCardProviderAutoConfiguration.class))
		.withPropertyValues("spring.ai.alibaba.a2a.client.card.name=test-agent",
				"spring.ai.alibaba.a2a.client.card.description=Test agent",
				"spring.ai.alibaba.a2a.client.card.version=1.0.0",
				"spring.ai.alibaba.a2a.client.card.url=" + AGENT_URL);

	@Test
	void shouldUseSdkProtocolVersionWhenNotConfigured() {
		this.contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			AgentCardProvider provider = context.getBean(AgentCardProvider.class);

			assertThat(provider.getAgentCard().protocolVersion())
				.isEqualTo(new AgentInterface("JSONRPC", AGENT_URL).protocolVersion());
		});
	}

	@Test
	void shouldUseSdkProtocolVersionForAdditionalInterfaceWhenNotConfigured() {
		A2aClientAgentCardProperties properties = new A2aClientAgentCardProperties();
		properties.setName("test-agent");
		properties.setDescription("Test agent");
		properties.setVersion("1.0.0");
		properties.setAdditionalInterfaces(List.of(new AgentInterface("JSONRPC", AGENT_URL, null, null)));

		AgentCardProvider provider = new A2aClientAgentCardProviderAutoConfiguration()
			.localAgentCardProvider(properties);

		assertThat(provider.getAgentCard().protocolVersion())
			.isEqualTo(new AgentInterface("JSONRPC", AGENT_URL).protocolVersion());
	}

}
