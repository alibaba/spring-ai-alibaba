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
import com.alibaba.cloud.ai.graph.observation.node.DefaultGraphNodeObservationConvention;
import com.alibaba.cloud.ai.graph.observation.node.GraphNodeObservationContext;
import com.alibaba.cloud.ai.graph.observation.node.GraphNodeObservationDocumentation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lifecycle listener for graph observation operations. Implements GraphLifecycleListener
 * to create observations for different graph lifecycle events. Records complete node
 * execution information including input and output states.
 */
public class GraphObservationLifecycleListener implements GraphLifecycleListener {

	private static final Logger log = LoggerFactory.getLogger(GraphObservationLifecycleListener.class);

	private static final DefaultGraphObservationConvention DEFAULT_GRAPH_OBSERVATION_CONVENTION = new DefaultGraphObservationConvention();

	private static final DefaultGraphNodeObservationConvention DEFAULT_GRAPH_NODE_OBSERVATION_CONVENTION = new DefaultGraphNodeObservationConvention();

	private final ObservationRegistry observationRegistry;

	private volatile Observation graphObservation;

	private volatile Observation.Scope graphScope;

	private final Map<String, Observation> nodeObservations = new ConcurrentHashMap<>();

	private final Map<String, Observation.Scope> nodeScopes = new ConcurrentHashMap<>();

	/**
	 * Constructs a new GraphObservationLifecycleListener with the specified observation
	 * registry.
	 * @param observationRegistry the registry for managing observations
	 */
	public GraphObservationLifecycleListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Handles the start of a graph execution. Creates a graph-level observation.
	 * @param nodeId the identifier of the node being started
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
		log.debug("Starting graph execution observation");

		graphObservation = Observation.createNotStarted(DEFAULT_GRAPH_OBSERVATION_CONVENTION,
				() -> new GraphObservationContext("graph-execution", state, null), observationRegistry);

		Observation currentObservation = observationRegistry.getCurrentObservation();
		if (currentObservation != null) {
			graphObservation.parentObservation(currentObservation);
		}

		graphObservation.start();
		graphScope = graphObservation.openScope();
	}

	/**
	 * Handles the before execution phase of a graph node. Creates node observation and
	 * records input state.
	 * @param nodeId the identifier of the node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 * @param curTime the current timestamp
	 */
	@Override
	public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		log.debug("Starting observation for node: {}", nodeId);

		// Create minimal context for the observation
		GraphNodeObservationContext context = new GraphNodeObservationContext(nodeId, "execution");

		Observation nodeObservation = Observation.createNotStarted(DEFAULT_GRAPH_NODE_OBSERVATION_CONVENTION,
				() -> context, observationRegistry);

		if (graphObservation != null) {
			nodeObservation.parentObservation(graphObservation);
		}

		// Add input state using Documentation constant
		nodeObservation.highCardinalityKeyValue(
				GraphNodeObservationDocumentation.HighCardinalityKeyNames.GEN_AI_PROMPT.asString(),
				state != null ? state.toString() : "");

		nodeObservation.start();
		nodeObservations.put(nodeId, nodeObservation);

		Observation.Scope scope = nodeObservation.openScope();
		nodeScopes.put(nodeId, scope);
	}

	/**
	 * Handles the after execution phase of a graph node. Adds output state and stops
	 * observation.
	 * @param nodeId the identifier of the node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 * @param curTime the current timestamp
	 */
	@Override
	public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		log.debug("Stopping observation for node: {}", nodeId);

		Observation.Scope scope = nodeScopes.remove(nodeId);
		if (scope != null) {
			scope.close();
		}

		Observation nodeObservation = nodeObservations.remove(nodeId);

		if (nodeObservation != null) {
			// Add output state using Documentation constant
			nodeObservation.highCardinalityKeyValue(
					GraphNodeObservationDocumentation.HighCardinalityKeyNames.GEN_AI_COMPLETION.asString(),
					state != null ? state.toString() : "");

			nodeObservation.stop();
		}
		else {
			log.warn("No observation found for node: {}", nodeId);
		}
	}

	/**
	 * Handles errors during graph node execution. Records the error and stops
	 * observation.
	 * @param nodeId the identifier of the node that encountered an error
	 * @param state the current state of the graph execution
	 * @param ex the exception that occurred
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
		log.error("Error occurred in node: {}", nodeId, ex);

		Observation.Scope scope = nodeScopes.remove(nodeId);
		if (scope != null) {
			scope.close();
		}

		Observation nodeObservation = nodeObservations.remove(nodeId);

		if (nodeObservation != null) {
			// Add error state using Documentation constant
			nodeObservation.highCardinalityKeyValue(
					GraphNodeObservationDocumentation.HighCardinalityKeyNames.GEN_AI_COMPLETION.asString(),
					state != null ? state.toString() : "");

			nodeObservation.error(ex).stop();
		}

		if (graphObservation != null) {
			graphObservation.error(ex);
		}
	}

	/**
	 * Handles the completion of graph execution. Cleans up all observations.
	 * @param nodeId the identifier of the completed node
	 * @param state the current state of the graph execution
	 * @param config the runnable configuration for the node
	 */
	@Override
	public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
		log.debug("Graph execution completed");

		nodeScopes.values().forEach(scope -> {
			try {
				scope.close();
			}
			catch (Exception e) {
				log.debug("Error closing node scope: {}", e.getMessage());
			}
		});
		nodeScopes.clear();

		nodeObservations.values().forEach(observation -> {
			try {
				observation.stop();
			}
			catch (Exception e) {
				log.debug("Error stopping node observation: {}", e.getMessage());
			}
		});
		nodeObservations.clear();

		if (graphScope != null) {
			try {
				graphScope.close();
			}
			catch (Exception e) {
				log.debug("Error closing graph scope: {}", e.getMessage());
			}
			graphScope = null;
		}

		if (graphObservation != null) {
			try {
				graphObservation.stop();
			}
			catch (Exception e) {
				log.debug("Error stopping graph observation: {}", e.getMessage());
			}
			graphObservation = null;
		}
	}

}
