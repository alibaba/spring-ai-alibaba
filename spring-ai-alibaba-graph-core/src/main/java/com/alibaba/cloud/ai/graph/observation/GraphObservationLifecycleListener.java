/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.alibaba.cloud.ai.graph.observation.metric.SpringAiAlibabaObservationMetricAttributes;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graph Lifecycle Listener for Observability
 *
 * This listener integrates with Micrometer Observation API to provide
 * distributed tracing
 * for StateGraph execution. It tracks individual node executions linked to a
 * parent
 * Graph Observation.
 *
 * It captures state changes as input and output for each node, and records
 * standardized
 * metric attributes.
 *
 * @author sixiyida
 */
public class GraphObservationLifecycleListener implements GraphLifecycleListener {

	private static final Logger log = LoggerFactory.getLogger(GraphObservationLifecycleListener.class);

	private static final Map<String, GraphObservationContext> CONTEXTS = new ConcurrentHashMap<>();

	private final ObservationRegistry observationRegistry;

	public GraphObservationLifecycleListener(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	public static class GraphObservationContext {
		final Observation graphObservation;
		final Map<String, Observation> nodeObservations = new ConcurrentHashMap<>();
		final Map<String, Observation.Scope> nodeScopes = new ConcurrentHashMap<>();

		public GraphObservationContext(Observation graphObservation) {
			this.graphObservation = graphObservation;
		}
	}

	public static void register(String executionId, Observation graphObservation) {
		if (executionId != null && graphObservation != null) {
			CONTEXTS.put(executionId, new GraphObservationContext(graphObservation));
		}
	}

	public static void unregister(String executionId) {
		if (executionId != null) {
			GraphObservationContext ctx = CONTEXTS.remove(executionId);
			if (ctx != null) {
				for (Observation.Scope scope : ctx.nodeScopes.values()) {
					try {
						scope.close();
					} catch (Exception e) {
						log.warn("Failed to close scope during unregister", e);
					}
				}
				ctx.nodeScopes.clear();
				ctx.nodeObservations.clear();
			}
		}
	}

	@Override
	public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
		if (com.alibaba.cloud.ai.graph.StateGraph.START.equals(nodeId)) {
			startGraphObservation(state);
		}
	}

	@Override
	public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
		if (com.alibaba.cloud.ai.graph.StateGraph.END.equals(nodeId)) {
			stopGraphObservation(state, true, null);
		}
	}

	@Override
	public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
		log.error("Error in graph/node {}: {}", nodeId, ex.getMessage());
		// Handle Node Error
		handleError(nodeId, state, ex);
		// Handle Graph Error (Stop Graph Obs if needed, or if bubbling)
		// Usually onError is called for the node that failed.
		// If we want to fail the GRAPH too, we should do it.
		// But GraphRunner continues? No, GraphRunner returns Flux.error usually.
		stopGraphObservation(state, false, ex);
	}

	@Override
	public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		startNodeObservation(nodeId, state);
	}

	@Override
	public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
		stopNodeObservation(nodeId, state);
	}

	private void startGraphObservation(Map<String, Object> state) {
		String executionId = (String) state.get(GraphLifecycleListener.EXECUTION_ID_KEY);
		if (executionId == null) {
			return;
		}

		// Start Graph Observation
		Observation graphObs = Observation.createNotStarted("spring.ai.alibaba.graph.graph-execution",
				observationRegistry);

		// Set Standard Metric Attributes
		graphObs.lowCardinalityKeyValue(
				SpringAiAlibabaObservationMetricAttributes.GRAPH_NAME.value(),
				"graph-execution");

		// Set Input attributes
		String input = dumpState(state);
		graphObs.highCardinalityKeyValue(
				SpringAiAlibabaObservationMetricAttributes.LANGFUSE_INPUT.value(),
				input);
		graphObs.highCardinalityKeyValue(
				SpringAiAlibabaObservationMetricAttributes.GEN_AI_PROMPT.value(),
				input);

		graphObs.start();

		// Register valid observation context
		register(executionId, graphObs);
	}

	private void stopGraphObservation(Map<String, Object> state, boolean success, Throwable ex) {
		String executionId = (String) state.get(GraphLifecycleListener.EXECUTION_ID_KEY);
		if (executionId == null) {
			return;
		}

		GraphObservationContext ctx = CONTEXTS.get(executionId);
		if (ctx == null || ctx.graphObservation == null) {
			return;
		}

		Observation obs = ctx.graphObservation;

		if (success) {
			obs.lowCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.GRAPH_SUCCESS.value(),
					"true");

			String output = dumpState(state);
			obs.highCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.LANGFUSE_OUTPUT.value(),
					output);
			obs.highCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.GEN_AI_COMPLETION.value(),
					output);
		} else {
			obs.lowCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.GRAPH_SUCCESS.value(),
					"false");
			if (ex != null) {
				obs.error(ex);
			}
		}

		obs.stop();
		// Unregister context
		unregister(executionId);
	}

	private void startNodeObservation(String nodeId, Map<String, Object> state) {
		GraphObservationContext ctx = getContext(state);
		if (ctx == null) {
			// Log at debug level to avoid spam if context is not expected always
			log.debug("No observation context found for node execution {}", nodeId);
			return;
		}

		Observation nodeObservation = Observation.createNotStarted("spring.ai.alibaba.graph.node." + nodeId,
				observationRegistry)
				.parentObservation(ctx.graphObservation);

		// Record Standard Metric Attributes
		nodeObservation.lowCardinalityKeyValue(
				SpringAiAlibabaObservationMetricAttributes.GRAPH_NODE_NAME.value(),
				nodeId);

		// Set node input attributes (Dump valid state as High Cardinality)
		String nodeInput = dumpState(state);
		nodeObservation.highCardinalityKeyValue(SpringAiAlibabaObservationMetricAttributes.LANGFUSE_INPUT.value(),
				nodeInput);
		nodeObservation.highCardinalityKeyValue(SpringAiAlibabaObservationMetricAttributes.GEN_AI_PROMPT.value(),
				nodeInput);

		nodeObservation.start();
		// Open scope to propagate context
		Observation.Scope scope = nodeObservation.openScope();

		ctx.nodeObservations.put(nodeId, nodeObservation);
		ctx.nodeScopes.put(nodeId, scope);
	}

	private void stopNodeObservation(String nodeId, Map<String, Object> state) {
		GraphObservationContext ctx = getContext(state);
		if (ctx == null)
			return;

		Observation.Scope scope = ctx.nodeScopes.remove(nodeId);
		if (scope != null) {
			scope.close();
		}

		Observation nodeObservation = ctx.nodeObservations.remove(nodeId);
		if (nodeObservation != null) {
			// Record Success Status
			nodeObservation.lowCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.GRAPH_NODE_SUCCESS.value(),
					"true");

			// Set node output attributes (Dump valid state)
			String nodeOutput = dumpState(state);
			nodeObservation.highCardinalityKeyValue(SpringAiAlibabaObservationMetricAttributes.LANGFUSE_OUTPUT.value(),
					nodeOutput);
			nodeObservation.highCardinalityKeyValue(
					SpringAiAlibabaObservationMetricAttributes.GEN_AI_COMPLETION.value(), nodeOutput);

			nodeObservation.stop();
			log.debug("Node {} observation stopped", nodeId);
		}
	}

	private void handleError(String nodeId, Map<String, Object> state, Throwable ex) {
		GraphObservationContext ctx = getContext(state);
		if (ctx != null) {
			Observation.Scope scope = ctx.nodeScopes.remove(nodeId);
			if (scope != null) {
				scope.close();
			}

			Observation nodeObservation = ctx.nodeObservations.remove(nodeId);
			if (nodeObservation != null) {
				// Record Failure Status
				nodeObservation.lowCardinalityKeyValue(
						SpringAiAlibabaObservationMetricAttributes.GRAPH_NODE_SUCCESS.value(),
						"false");

				nodeObservation.error(ex);
				nodeObservation.stop();
			}
		}
	}

	private GraphObservationContext getContext(Map<String, Object> state) {
		if (state == null)
			return null;
		Object id = state.get(GraphLifecycleListener.EXECUTION_ID_KEY);
		if (id instanceof String) {
			return CONTEXTS.get((String) id);
		}
		return null;
	}

	private String dumpState(Map<String, Object> state) {
		if (state == null || state.isEmpty()) {
			return "empty state";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> entry : state.entrySet()) {
			String key = entry.getKey();
			// Skip internal keys and large logs
			if (key.startsWith("_") || "logs".equals(key)) {
				continue;
			}
			Object value = entry.getValue();
			String valStr = String.valueOf(value);
			if (valStr.length() > 1000) {
				valStr = valStr.substring(0, 1000) + "... (truncated)";
			}
			sb.append(key).append("=").append(valStr).append("; ");
		}
		return sb.length() > 0 ? sb.toString() : "empty visible state";
	}
}
