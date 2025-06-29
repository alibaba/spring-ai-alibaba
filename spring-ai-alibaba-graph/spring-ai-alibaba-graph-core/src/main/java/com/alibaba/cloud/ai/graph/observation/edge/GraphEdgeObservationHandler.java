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
package com.alibaba.cloud.ai.graph.observation.edge;

import com.alibaba.cloud.ai.graph.observation.GraphMetricsGenerator;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for graph edge observation events. Processes edge observation lifecycle events
 * and generates metrics for edge operations. Logs edge execution details and delegates
 * metric generation to GraphMetricsGenerator.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public class GraphEdgeObservationHandler implements ObservationHandler<GraphEdgeObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(GraphObservationHandler.class);

	private final MeterRegistry meterRegistry;

	/**
	 * Constructs a new GraphEdgeObservationHandler with the specified meter registry.
	 * @param meterRegistry the meter registry for metric collection
	 */
	public GraphEdgeObservationHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Handles the completion of a graph edge observation. Logs successful edge execution
	 * details and generates success metrics.
	 * @param context the edge observation context containing execution details
	 */
	@Override
	public void onStop(GraphEdgeObservationContext context) {
		logger.info("Graph graphName: {} state: {} nextNode : {}", context.getGraphEdgeName(),
				context.getState().toString(), context.getNextNode());
		GraphMetricsGenerator.generate(context, meterRegistry, true);
	}

	/**
	 * Handles errors during graph edge observation. Logs error details and generates
	 * failure metrics.
	 * @param context the edge observation context containing error details
	 */
	@Override
	public void onError(GraphEdgeObservationContext context) {
		logger.info("Graph graphName: {} state: {} nextNode : {}", context.getGraphEdgeName(),
				context.getState().toString(), context.getNextNode());
		GraphMetricsGenerator.generate(context, meterRegistry, false);
	}

	/**
	 * Determines if this handler supports the given observation context. Returns true if
	 * the context is an instance of GraphEdgeObservationContext.
	 * @param context the observation context to check
	 * @return true if this handler supports the context, false otherwise
	 */
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof GraphEdgeObservationContext;
	}

}
