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

import com.alibaba.cloud.ai.graph.observation.GraphMetricsGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for graph observation events. Processes graph observation contexts and
 * generates metrics for monitoring.
 *
 * @author XiaoYunTao
 * @since 2025/6/29
 */
public class GraphObservationHandler implements ObservationHandler<GraphObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(GraphObservationHandler.class);

	private final MeterRegistry meterRegistry;

	/**
	 * Constructs a new GraphObservationHandler with the specified meter registry.
	 * @param meterRegistry the meter registry for metrics collection
	 */
	public GraphObservationHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Handles successful graph observation completion. Logs graph information and
	 * generates success metrics.
	 */
	@Override
	public void onStop(GraphObservationContext context) {
		logger.info("Graph graphName: {} state: {} output : {}", context.getGraphName(), context.getState().toString(),
				context.getOutput().toString());
		GraphMetricsGenerator.generate(context, meterRegistry, true);
	}

	/**
	 * Handles graph observation errors. Logs graph information and generates error
	 * metrics.
	 */
	@Override
	public void onError(GraphObservationContext context) {
		logger.info("Graph graphName: {} state: {} output : {}", context.getGraphName(), context.getState().toString(),
				context.getOutput().toString());
		GraphMetricsGenerator.generate(context, meterRegistry, false);
	}

	/**
	 * Checks if this handler supports the given observation context.
	 * @param context the observation context to check
	 * @return true if the context is a GraphObservationContext
	 */
	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof GraphObservationContext;
	}

}
