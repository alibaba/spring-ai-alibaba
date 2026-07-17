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

import java.util.List;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class A2aServerHandlerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(A2aServerHandlerAutoConfiguration.class))
		.withPropertyValues("spring.ai.alibaba.a2a.server.type=JSONRPC")
		.withBean(AgentCard.class, A2aServerHandlerAutoConfigurationTest::agentCard)
		.withBean(ReactAgent.class, () -> mock(ReactAgent.class));

	@Test
	void shouldCreateProductionRequestHandler() {
		this.contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(A2AConfigProvider.class);
			assertThat(context).hasSingleBean(RequestHandler.class);
		});
	}

	private static AgentCard agentCard() {
		return AgentCard.builder()
			.name("test-agent")
			.description("Test agent")
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(true).build())
			.defaultInputModes(List.of("text/plain"))
			.defaultOutputModes(List.of("text/plain"))
			.skills(List.of())
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
			.build();
	}

}
