/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.autoconfigure.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.observation.GraphObservationLifecycleListener;
import com.alibaba.cloud.ai.graph.observation.edge.GraphEdgeObservationHandler;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationHandler;
import com.alibaba.cloud.ai.graph.observation.node.GraphNodeObservationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Graph observation functionality.
 *
 * Provides automatic setup of: - GraphObservationLifecycleListener for lifecycle events -
 * ObservationHandlers for different graph components (when dependencies are available) -
 * Default CompileConfig with observation support
 *
 * @author sixiyida
 * @since 2025/7/3
 */
@AutoConfiguration
@ConditionalOnClass({ StateGraph.class, ObservationRegistry.class })
@EnableConfigurationProperties(GraphObservationProperties.class)
@ConditionalOnProperty(prefix = GraphObservationProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class GraphObservationAutoConfiguration {

	/**
	 * Creates a GraphObservationLifecycleListener that monitors graph lifecycle events.
	 * @param observationRegistry the observation registry for creating observations
	 * @return configured GraphObservationLifecycleListener
	 */
	@Bean
	@ConditionalOnMissingBean
	public GraphObservationLifecycleListener graphObservationLifecycleListener(
			ObjectProvider<ObservationRegistry> observationRegistry) {
		return new GraphObservationLifecycleListener(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));
	}

	/**
	 * Creates a default CompileConfig with observation support.
	 * @param observationRegistry the observation registry
	 * @param graphObservationLifecycleListeners the graph observation lifecycle listener
	 * @return configured CompileConfig with observation support
	 */
	@Bean
	@ConditionalOnMissingBean
	public CompileConfig observationGraphCompileConfig(ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<GraphObservationLifecycleListener> graphObservationLifecycleListeners) {

		CompileConfig.Builder builder = CompileConfig.builder()
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

		graphObservationLifecycleListeners.ifUnique(builder::withLifecycleListener);

		return builder.build();
	}

	/**
	 * Configuration for observation handlers. Only enabled when MeterRegistry is
	 * available on the classpath and as a bean.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class ObservationHandlersConfiguration {

		/**
		 * Creates GraphObservationHandler for graph-level observations.
		 * @param meterRegistry the meter registry for metrics
		 * @return configured GraphObservationHandler
		 */
		@Bean
		@ConditionalOnMissingBean
		public GraphObservationHandler graphObservationHandler(MeterRegistry meterRegistry) {
			return new GraphObservationHandler(meterRegistry);
		}

		/**
		 * Creates GraphNodeObservationHandler for node-level observations.
		 * @param meterRegistry the meter registry for metrics
		 * @return configured GraphNodeObservationHandler
		 */
		@Bean
		@ConditionalOnMissingBean
		public GraphNodeObservationHandler graphNodeObservationHandler(MeterRegistry meterRegistry) {
			return new GraphNodeObservationHandler(meterRegistry);
		}

		/**
		 * Creates GraphEdgeObservationHandler for edge-level observations.
		 * @param meterRegistry the meter registry for metrics
		 * @return configured GraphEdgeObservationHandler
		 */
		@Bean
		@ConditionalOnMissingBean
		public GraphEdgeObservationHandler graphEdgeObservationHandler(MeterRegistry meterRegistry) {
			return new GraphEdgeObservationHandler(meterRegistry);
		}

	}

}
