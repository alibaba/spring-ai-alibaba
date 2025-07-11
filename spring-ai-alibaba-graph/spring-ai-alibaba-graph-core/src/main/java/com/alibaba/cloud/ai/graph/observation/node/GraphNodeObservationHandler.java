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

import com.alibaba.cloud.ai.graph.observation.GraphMetricsGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for graph node observation events. Processes node observation lifecycle events
 * and generates metrics for node operations. Logs node execution details and delegates
 * metric generation to GraphMetricsGenerator.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public class GraphNodeObservationHandler implements ObservationHandler<GraphNodeObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(GraphNodeObservationHandler.class);

	private final MeterRegistry meterRegistry;

	/**
	 * Constructs a new GraphNodeObservationHandler with the specified meter registry.
	 * @param meterRegistry the meter registry for metric collection
	 */
	public GraphNodeObservationHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Handles the completion of a graph node observation. Logs successful node execution
	 * details and generates success metrics.
	 * @param context the node observation context containing execution details
	 */
	@Override
	public void onStop(GraphNodeObservationContext context) {
		GraphMetricsGenerator.generate(context, meterRegistry, true);
	}

	/**
	 * Handles errors during graph node observation. Logs error details and generates
	 * failure metrics.
	 * @param context the node observation context containing error details
	 */
	@Override
	public void onError(GraphNodeObservationContext context) {
		GraphMetricsGenerator.generate(context, meterRegistry, false);
	}

	/**
	 * Determines if this handler supports the given observation context. Returns true if
	 * the context is an instance of GraphNodeObservationContext.
	 * @param context the observation context to check
	 * @return true if this handler supports the context, false otherwise
	 */
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof GraphNodeObservationContext;
	}

}
