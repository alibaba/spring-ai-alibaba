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
package com.alibaba.cloud.ai.graph.observation.node;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Convention interface for graph node observation operations. Defines the contract for
 * customizing node observation behavior and metadata. Extends ObservationConvention to
 * provide node-specific observation conventions.
 */
public interface GraphNodeObservationConvention extends ObservationConvention<GraphNodeObservationContext> {

	/**
	 * Determines if this convention supports the given observation context. Returns true
	 * if the context is an instance of GraphNodeObservationContext.
	 * @param context the observation context to check
	 * @return true if this convention supports the context, false otherwise
	 */
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof GraphNodeObservationContext;
	}

}
