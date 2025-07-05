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
package com.alibaba.cloud.ai.graph.observation.graph;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for graph observation conventions. This interface extends
 * ObservationConvention to provide graph-specific observation conventions for monitoring
 * and metrics collection. It defines how graph observation contexts should be handled and
 * provides a default implementation for context support.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public interface GraphObservationConvention extends ObservationConvention<GraphObservationContext> {

	/**
	 * Determines whether this convention supports the given observation context. The
	 * default implementation checks if the context is an instance of
	 * GraphObservationContext.
	 * @param context the observation context to check
	 * @return true if this convention supports the context, false otherwise
	 */
	default boolean supportsContext(Observation.Context context) {
		return context instanceof GraphObservationContext;
	}

}
