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
import com.alibaba.cloud.ai.a2a.core.server.A2aServerExecutorProvider;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
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
			RequestHandler requestHandler = context.getBean(RequestHandler.class);
			A2aServerExecutorProvider executorProvider = context.getBean(A2aServerExecutorProvider.class);
			assertThat(requestHandler).isInstanceOf(DefaultRequestHandler.class);
			assertThat(ReflectionTestUtils.getField(requestHandler, "agentCompletionTimeoutSeconds")).isEqualTo(30);
			assertThat(ReflectionTestUtils.getField(requestHandler, "consumptionCompletionTimeoutSeconds")).isEqualTo(5);
			assertThat(ReflectionTestUtils.getField(requestHandler, "executor"))
				.isSameAs(executorProvider.getA2aServerExecutor());
			assertThat(ReflectionTestUtils.getField(requestHandler, "eventConsumerExecutor"))
				.isSameAs(executorProvider.getEventConsumerExecutor());
			assertThat(executorProvider.getEventConsumerExecutor())
				.isNotSameAs(executorProvider.getA2aServerExecutor());
		});
	}

	@Test
	void customConfigProviderControlsBlockingTimeouts() {
		A2AConfigProvider configProvider = mock(A2AConfigProvider.class);
		org.mockito.Mockito.when(configProvider.getValue("a2a.blocking.agent.timeout.seconds")).thenReturn("11");
		org.mockito.Mockito.when(configProvider.getValue("a2a.blocking.consumption.timeout.seconds")).thenReturn("7");

		this.contextRunner.withBean(A2AConfigProvider.class, () -> configProvider).run(context -> {
			assertThat(context).hasNotFailed();
			RequestHandler requestHandler = context.getBean(RequestHandler.class);
			assertThat(ReflectionTestUtils.getField(requestHandler, "configProvider")).isSameAs(configProvider);
			assertThat(ReflectionTestUtils.getField(requestHandler, "agentCompletionTimeoutSeconds")).isEqualTo(11);
			assertThat(ReflectionTestUtils.getField(requestHandler, "consumptionCompletionTimeoutSeconds")).isEqualTo(7);
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
