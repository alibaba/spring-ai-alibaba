/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.config;

import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manually configure ToolCallingManager to provide necessary beans after removing OpenAI
 * autoconfiguration
 *
 * @author Assistant
 */
@Configuration
public class ToolCallingManagerConfiguration {

	/**
	 * Configure ToolCallbackResolver Bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public ToolCallbackResolver toolCallbackResolver() {
		// Use default delegating resolver, supporting tool callback resolution from
		// multiple sources
		return new DelegatingToolCallbackResolver(List.of());
	}

	/**
	 * Configure ToolExecutionExceptionProcessor Bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
		// Use default tool execution exception processor
		return DefaultToolExecutionExceptionProcessor.builder().build();
	}

	/**
	 * Configure ToolCallingManager Bean (standard version)
	 *
	 * This Bean is used to handle tool call execution and management, using Spring AI
	 * default implementation
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "manus.tool-calling.use-observable", havingValue = "false", matchIfMissing = true)
	public ToolCallingManager defaultToolCallingManager(ToolCallbackResolver toolCallbackResolver,
			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
			ObjectProvider<ObservationRegistry> observationRegistry) {

		// Get ObservationRegistry, use NOOP implementation if not configured
		ObservationRegistry registry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);

		// Use Spring AI default ToolCallingManager implementation
		return new DefaultToolCallingManager(registry, toolCallbackResolver, toolExecutionExceptionProcessor);
	}

	/**
	 * Configure ObservableToolCallingManager Bean (enhanced version with monitoring
	 * support)
	 *
	 * Set manus.tool-calling.use-observable=true to enable. This version includes ARMS
	 * monitoring and observation capabilities
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "manus.tool-calling.use-observable", havingValue = "true")
	public ToolCallingManager observableToolCallingManager(ToolCallbackResolver toolCallbackResolver,
			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
			ObjectProvider<ObservationRegistry> observationRegistry) {

		// Get ObservationRegistry, use NOOP implementation if not configured
		ObservationRegistry registry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);

		// Use ToolCallingManager implementation with observation capabilities
		// Note: This requires spring-ai-alibaba-core dependency
		try {
			Class<?> observableClass = Class.forName("com.alibaba.cloud.ai.tool.ObservableToolCallingManager");
			return (ToolCallingManager) observableClass
				.getDeclaredConstructor(ObservationRegistry.class, ToolCallbackResolver.class,
						ToolExecutionExceptionProcessor.class)
				.newInstance(registry, toolCallbackResolver, toolExecutionExceptionProcessor);
		}
		catch (Exception e) {
			// If ObservableToolCallingManager is not available, fallback to default
			// implementation
			return new DefaultToolCallingManager(registry, toolCallbackResolver, toolExecutionExceptionProcessor);
		}
	}

}
