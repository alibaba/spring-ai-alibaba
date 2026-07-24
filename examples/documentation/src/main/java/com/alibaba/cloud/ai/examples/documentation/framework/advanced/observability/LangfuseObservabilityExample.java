/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.advanced.observability;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

/**
 * Sends ReactAgent and model observations to Langfuse through OTLP.
 *
 * <p>Run this application with the {@code langfuse} Maven profile and the
 * OpenTelemetry environment variables documented in this package's README.
 */
@SpringBootApplication
public class LangfuseObservabilityExample {

	public static void main(String[] args) {
		SpringApplication.run(LangfuseObservabilityExample.class, args);
	}

	@Bean
	ReactAgent langfuseReactAgent(ChatModel chatModel, ObservationRegistry observationRegistry)
			throws GraphRunnerException {
		return ReactAgent.builder()
				.name("langfuse_observability_agent")
				.description("A ReactAgent instrumented with Micrometer Observation")
				.systemPrompt("You are a concise and helpful assistant.")
				.model(chatModel)
				.observationRegistry(observationRegistry)
				.build();
	}

	@Bean
	CommandLineRunner runObservedAgent(ReactAgent agent, ObservationRegistry observationRegistry) {
		return args -> {
			String prompt = "Explain in one sentence why observability is useful for AI agents.";
			String sessionId = UUID.randomUUID().toString();
			Observation observation = Observation.createNotStarted("react-agent.invoke", observationRegistry)
				.contextualName("langfuse-observability-agent")
				.highCardinalityKeyValue("langfuse.trace.name", "react-agent-demo")
				.highCardinalityKeyValue("langfuse.session.id", sessionId)
				.highCardinalityKeyValue("langfuse.user.id", "documentation-user")
				.highCardinalityKeyValue("langfuse.observation.input", prompt)
				.start();

			try (Observation.Scope ignored = observation.openScope()) {
				AssistantMessage response = agent.call(prompt);
				observation.highCardinalityKeyValue("langfuse.observation.output", response.getText());
				System.out.println(response.getText());
			}
			catch (Exception ex) {
				observation.error(ex);
				throw ex;
			}
			finally {
				observation.stop();
			}
		};
	}

}
