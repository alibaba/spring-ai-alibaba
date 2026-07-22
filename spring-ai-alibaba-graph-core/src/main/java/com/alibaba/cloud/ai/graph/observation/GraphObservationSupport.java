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
package com.alibaba.cloud.ai.graph.observation;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import io.micrometer.observation.ObservationRegistry;

import java.util.Objects;

/**
 * Utilities for attaching graph observation support to compile configurations.
 */
public final class GraphObservationSupport {

	private GraphObservationSupport() {
	}

	/**
	 * Enhances a compile configuration with graph observation support.
	 * @param compileConfig the base compile configuration
	 * @param observationRegistry the observation registry to apply
	 * @return the enhanced compile configuration
	 */
	public static CompileConfig enhance(CompileConfig compileConfig, ObservationRegistry observationRegistry) {
		CompileConfig baseConfig = compileConfig != null ? compileConfig : CompileConfig.builder().build();
		ObservationRegistry effectiveRegistry = observationRegistry != null ? observationRegistry
				: baseConfig.observationRegistry();

		if (effectiveRegistry == null || effectiveRegistry == ObservationRegistry.NOOP) {
			return baseConfig;
		}

		GraphObservationLifecycleListener existingListener = findGraphObservationLifecycleListener(baseConfig);
		boolean hasLifecycleListener = existingListener != null;
		boolean sameRegistry = baseConfig.observationRegistry() == effectiveRegistry;
		boolean listenerMatchesRegistry = existingListener == null
				|| Objects.equals(existingListener.observationRegistry(), effectiveRegistry);

		if (sameRegistry && hasLifecycleListener && listenerMatchesRegistry) {
			return baseConfig;
		}

		CompileConfig.Builder builder = CompileConfig.builder(baseConfig).observationRegistry(effectiveRegistry);
		if (hasLifecycleListener && !listenerMatchesRegistry) {
			builder = CompileConfig.builder(baseConfig)
					.observationRegistry(effectiveRegistry);

			baseConfig.lifecycleListeners()
					.stream()
					.filter(listener -> !(listener instanceof GraphObservationLifecycleListener))
					.forEach(builder::withLifecycleListener);
		}

		if (!hasLifecycleListener || !listenerMatchesRegistry) {
			builder.withLifecycleListener(new GraphObservationLifecycleListener(effectiveRegistry));
		}

		return builder.build();
	}

	/**
	 * Checks whether the compile configuration already contains a lifecycle listener
	 * assignable to the given type.
	 * @param compileConfig the compile configuration
	 * @param listenerType the listener type to search for
	 * @return {@code true} if a matching listener is present
	 */
	public static boolean hasLifecycleListener(CompileConfig compileConfig,
			Class<? extends GraphLifecycleListener> listenerType) {
		if (compileConfig == null || listenerType == null) {
			return false;
		}
		return compileConfig.lifecycleListeners()
				.stream()
				.anyMatch(listener -> listenerType.isAssignableFrom(listener.getClass()));
	}

	/**
	 * Returns the graph observation lifecycle listener from the compile
	 * configuration, if present.
	 * @param compileConfig the compile configuration
	 * @return the graph observation lifecycle listener, or {@code null} if absent
	 */
	public static GraphObservationLifecycleListener findGraphObservationLifecycleListener(CompileConfig compileConfig) {
		if (compileConfig == null) {
			return null;
		}
		return compileConfig.lifecycleListeners()
				.stream()
				.filter(GraphObservationLifecycleListener.class::isInstance)
				.map(GraphObservationLifecycleListener.class::cast)
				.findFirst()
				.orElse(null);
	}

}
