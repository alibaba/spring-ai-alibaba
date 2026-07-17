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

package com.alibaba.cloud.ai.a2a.registry.nacos.discovery;

import java.util.List;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class NacosAgentCardWrapperTest {

	@Test
	void urlShouldFallbackToAgentCardUrlWhenPreferredTransportIsMissing() {
		AgentCard agentCard = createAgentCard(null, List.of());
		NacosAgentCardWrapper wrapper = new NacosAgentCardWrapper(agentCard);

		assertThatNoException().isThrownBy(wrapper::url);
		assertThat(wrapper.url()).isEqualTo("http://localhost:8080");
	}

	@Test
	void urlShouldUseAdditionalInterfaceWithPreferredTransport() {
		AgentCard agentCard = createAgentCard("JSONRPC",
				List.of(new AgentInterface("JSONRPC", "http://localhost:8081")));
		NacosAgentCardWrapper wrapper = new NacosAgentCardWrapper(agentCard);

		assertThat(wrapper.url()).isEqualTo("http://localhost:8081");
	}

	private AgentCard createAgentCard(String preferredTransport, List<AgentInterface> additionalInterfaces) {
		return AgentCard.builder().name("test-agent")
			.description("Test Agent")
			.url("http://localhost:8080")
			.version("1.0.0")
			.preferredTransport(preferredTransport)
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.capabilities(AgentCapabilities.builder().streaming(false).build())
			.supportedInterfaces(additionalInterfaces)
			.build();
	}

}
