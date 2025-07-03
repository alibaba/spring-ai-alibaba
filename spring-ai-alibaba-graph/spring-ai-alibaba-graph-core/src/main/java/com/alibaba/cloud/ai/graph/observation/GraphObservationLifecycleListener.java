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
package com.alibaba.cloud.ai.graph.observation;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.observation.graph.DefaultGraphObservationConvention;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.Map;

/**
 * Lifecycle listener for graph observation operations. Implements GraphLifecycleListener
 * to create observations for different graph lifecycle events. Provides observation
 * tracking for start, before, after, error, and complete events.
 */
public class GraphObservationLifecycleListener implements GraphLifecycleListener {

	private static final DefaultGraphObservationConvention DEFAULT_GRAPH_OBSERVATION_CONVENTION = new DefaultGraphObservationConvention();

	private final ObservationRegistry observationRegistry;

	/**
	 * Constructs a new GraphObservationLifecycleListener with the specified observation
	 * registry.
	 * @param observationRegistry the registry for managing observations
	 */
	public GraphObservationLifecycleListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Handles the start of a graph node execution. Creates an observation for the node
	 * start event.
	 * @param nodeId the identifier of the node being started
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
		Observation
			.start(DEFAULT_GRAPH_OBSERVATION_CONVENTION, () -> new GraphObservationContext(nodeId, state, null),
					observationRegistry)
			.stop();
	}

	/**
	 * Handles the before execution phase of a graph node. Creates an observation for the
	 * node before event.
	 * @param nodeId the identifier of the node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 * @param curTime the current timestamp
	 */
	@Override
	public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		Observation
			.start(DEFAULT_GRAPH_OBSERVATION_CONVENTION, () -> new GraphObservationContext(nodeId, state, null),
					observationRegistry)
			.stop();
	}

	/**
	 * Handles the after execution phase of a graph node. Creates an observation for the
	 * node after event with output data.
	 * @param nodeId the identifier of the node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 * @param curTime the current timestamp
	 */
	@Override
	public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		Observation
			.start(DEFAULT_GRAPH_OBSERVATION_CONVENTION, () -> new GraphObservationContext(nodeId, state, state),
					observationRegistry)
			.stop();
	}

	/**
	 * Handles errors during graph node execution. Creates an observation for the node
	 * error event.
	 * @param nodeId the identifier of the node that encountered an error
	 * @param state the current state of the graph execution
	 * @param ex the exception that occurred
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
		Observation
			.start(DEFAULT_GRAPH_OBSERVATION_CONVENTION, () -> new GraphObservationContext(nodeId, state, null),
					observationRegistry)
			.stop();
	}

	/**
	 * Handles the completion of a graph node execution. Creates an observation for the
	 * node complete event with output data.
	 * @param nodeId the identifier of the completed node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
		Observation
			.start(DEFAULT_GRAPH_OBSERVATION_CONVENTION, () -> new GraphObservationContext(nodeId, state, state),
					observationRegistry)
			.stop();
	}

}
