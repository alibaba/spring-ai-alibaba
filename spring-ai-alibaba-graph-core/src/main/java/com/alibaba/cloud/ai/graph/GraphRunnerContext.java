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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNodeAction;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Context class to manage the state during graph execution.
 */
public class GraphRunnerContext {

	public static final String INTERRUPT_AFTER = "__INTERRUPTED__";

	private static final Logger log = LoggerFactory.getLogger(GraphRunnerContext.class);

	// Core execution state
	final CompiledGraph compiledGraph;

	final AtomicInteger iteration = new AtomicInteger(0);

	// Execution context
	OverAllState overallState;

	RunnableConfig config;

	String currentNodeId;

	String nextNodeId;

	Map<String, Object> currentState;

	String resumeFrom;

	ReturnFromEmbed returnFromEmbed;

	public GraphRunnerContext(OverAllState initialState, RunnableConfig config, CompiledGraph compiledGraph)
			throws Exception {
		this.compiledGraph = compiledGraph;
		this.config = config;

		if (initialState.isResume()) {
			initializeFromResume(initialState, config);
		}
		else {
			initializeFromStart(initialState, config);
		}
	}

	// ================================================================================================================
	// Initialization Methods
	// ================================================================================================================

	private void initializeFromResume(OverAllState initialState, RunnableConfig config) throws Exception {
		log.trace("RESUME REQUEST");

		var saver = compiledGraph.compileConfig.checkpointSaver()
			.orElseThrow(() -> new IllegalStateException("Resume request without a configured checkpoint saver!"));
		var checkpoint = saver.get(config)
			.orElseThrow(() -> new IllegalStateException("Resume request without a valid checkpoint!"));

		var startCheckpointNextNodeAction = compiledGraph.getNodeAction(checkpoint.getNextNodeId());
		if (startCheckpointNextNodeAction instanceof SubCompiledGraphNodeAction action) {
			// RESUME FORM SUBGRAPH DETECTED
			this.config = RunnableConfig.builder(config)
				.checkPointId(null) // Reset checkpoint id
				.addMetadata(action.resumeSubGraphId(), true) // add metadata for
				// sub graph
				.build();
		}
		else {
			// Reset checkpoint id
			this.config = config.withCheckPointId(null);
		}

		setCurrentState(checkpoint.getState());
		setCurrentNodeId(null);
		setNextNodeId(checkpoint.getNextNodeId());
		setOverallState(initialState.input(getCurrentState()));
		setResumeFrom(checkpoint.getNodeId());

		log.trace("RESUME FROM {}", checkpoint.getNodeId());
	}

	private void initializeFromStart(OverAllState initialState, RunnableConfig config) throws Exception {
		log.trace("START");

		org.springframework.util.CollectionUtils.isEmpty(initialState.data()); // Simple
																				// validation

		// Use CompiledGraph's getInitialState method
		Map<String, Object> inputs = initialState.data();
		setCurrentState(compiledGraph.getInitialState(inputs != null ? inputs : new HashMap<>(), config));
		setOverallState(initialState.input(getCurrentState()));
		setCurrentNodeId(START);
		setNextNodeId(null);
	}

	// ================================================================================================================
	// Execution Control Methods
	// ================================================================================================================

	public boolean shouldStop() {
		return nextNodeId == null && currentNodeId == null;
	}

	public boolean isMaxIterationsReached() {
		return iteration.incrementAndGet() > compiledGraph.getMaxIterations();
	}

	public boolean isStartNode() {
		return START.equals(currentNodeId);
	}

	public boolean isEndNode() {
		return END.equals(nextNodeId);
	}

	public boolean shouldInterrupt() {
		return shouldInterruptBefore(getNextNodeId(), getCurrentNodeId())
				|| shouldInterruptAfter(getCurrentNodeId(), getNextNodeId());
	}

	private boolean shouldInterruptBefore(String nodeId, String previousNodeId) {
		if (previousNodeId == null)
			return false;
		return compiledGraph.compileConfig.interruptsBefore().contains(nodeId);
	}

	private boolean shouldInterruptAfter(String nodeId, String previousNodeId) {
		if (nodeId == null || Objects.equals(nodeId, previousNodeId))
			return false;
		return (compiledGraph.compileConfig.interruptBeforeEdge() && Objects.equals(nodeId, INTERRUPT_AFTER))
				|| compiledGraph.compileConfig.interruptsAfter().contains(nodeId);
	}

	// ================================================================================================================
	// Node Action Methods
	// ================================================================================================================

	public AsyncNodeActionWithConfig getNodeAction(String nodeId) {
		return compiledGraph.getNodeAction(nodeId);
	}

	public Command getEntryPoint() throws Exception {
		var entryPoint = compiledGraph.getEdge(START);
		return nextNodeId(entryPoint, getCurrentState(), "entryPoint");
	}

	public Command nextNodeId(String nodeId, Map<String, Object> state) throws Exception {
		return nextNodeId(compiledGraph.getEdge(nodeId), state, nodeId);
	}

	private Command nextNodeId(EdgeValue route, Map<String, Object> state, String nodeId) throws Exception {
		if (route == null) {
			throw RunnableErrors.missingEdge.exception(nodeId);
		}
		if (route.id() != null) {
			return new Command(route.id(), state);
		}
		if (route.value() != null) {
			var command = route.value().action().apply(getOverallState(), getConfig()).get();
			var newRoute = command.gotoNode();
			String result = route.value().mappings().get(newRoute);
			if (result == null) {
				throw RunnableErrors.missingNodeInEdgeMapping.exception(nodeId, newRoute);
			}
			var updatedState = OverAllState.updateState(state, command.update(), getKeyStrategyMap());
			getOverallState().updateState(command.update());
			return new Command(result, updatedState);
		}
		throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
	}

	// ================================================================================================================
	// Checkpoint Methods
	// ================================================================================================================

	public Optional<Checkpoint> addCheckpoint(String nodeId, String nextNodeId) throws Exception {
		if (compiledGraph.compileConfig.checkpointSaver().isPresent()) {
			var cp = Checkpoint.builder()
				.nodeId(nodeId)
				.state(cloneState(getCurrentState()))
				.nextNodeId(nextNodeId)
				.build();
			compiledGraph.compileConfig.checkpointSaver().get().put(config, cp);
			return Optional.of(cp);
		}
		return Optional.empty();
	}

	// ================================================================================================================
	// Output Building Methods
	// ================================================================================================================

	public NodeOutput buildOutput(String nodeId, Optional<Checkpoint> checkpoint) throws Exception {
		if (checkpoint.isPresent() && config.streamMode() == CompiledGraph.StreamMode.SNAPSHOTS) {
			return StateSnapshot.of(getKeyStrategyMap(), checkpoint.get(), config,
					compiledGraph.stateGraph.getStateSerializer().stateFactory());
		}
		return buildNodeOutput(nodeId);
	}

	public NodeOutput buildCurrentNodeOutput() throws Exception {
		Optional<Checkpoint> cp = addCheckpoint(currentNodeId, nextNodeId);
		return buildOutput(currentNodeId, cp);
	}

	public NodeOutput buildNodeOutput(String nodeId) throws Exception {
		return NodeOutput.of(nodeId, cloneState(getCurrentState()));
	}

	public OverAllState cloneState(Map<String, Object> data) throws Exception {
		return compiledGraph.cloneState(data);
	}

	// ================================================================================================================
	// Lifecycle Methods
	// ================================================================================================================

	public void doListeners(String scene, Exception e) {
		for (GraphLifecycleListener listener : compiledGraph.compileConfig.lifecycleListeners()) {
			try {
				switch (scene) {
					case START:
						listener.onStart(getCurrentNodeId(), getCurrentState(), config);
						break;
					case END:
						listener.onComplete(getCurrentNodeId(), getCurrentState(), config);
						break;
					case NODE_BEFORE:
						listener.onStart(getCurrentNodeId(), getCurrentState(), config);
						break;
					case NODE_AFTER:
						listener.onComplete(getCurrentNodeId(), getCurrentState(), config);
						break;
					case ERROR:
						listener.onError(getCurrentNodeId(), getCurrentState(), e, config);
						break;
				}
			}
			catch (Exception ex) {
				log.error("Error in listener", ex);
			}
		}
	}

	// ================================================================================================================
	// State Management Methods
	// ================================================================================================================

	public void updateCurrentState(Map<String, Object> state) {
		this.currentState = OverAllState.updateState(this.currentState, state, getKeyStrategyMap());
	}

	// ================================================================================================================
	// Getter and Setter Methods
	// ================================================================================================================

	public String getCurrentNodeId() {
		return currentNodeId;
	}

	public void setCurrentNodeId(String nodeId) {
		this.currentNodeId = nodeId;
	}

	public String getNextNodeId() {
		return nextNodeId;
	}

	public void setNextNodeId(String nodeId) {
		this.nextNodeId = nodeId;
	}

	public Map<String, Object> getCurrentState() {
		return currentState;
	}

	public void setCurrentState(Map<String, Object> state) {
		this.currentState = state;
	}

	public OverAllState getOverallState() {
		return overallState;
	}

	public void setOverallState(OverAllState state) {
		this.overallState = state;
	}

	public Map<String, KeyStrategy> getKeyStrategyMap() {
		return compiledGraph.getKeyStrategyMap();
	}

	public CompiledGraph getCompiledGraph() {
		return compiledGraph;
	}

	public RunnableConfig getConfig() {
		return config;
	}

	public void setConfig(RunnableConfig config) {
		this.config = config;
	}

	public String getResumeFrom() {
		return resumeFrom;
	}

	public void setResumeFrom(String resumeFrom) {
		this.resumeFrom = resumeFrom;
	}

	public Optional<String> getResumeFromAndReset() {
		final var result = ofNullable(resumeFrom);
		resumeFrom = null;
		return result;
	}

	public Optional<ReturnFromEmbed> getReturnFromEmbedAndReset() {
		var result = ofNullable(returnFromEmbed);
		returnFromEmbed = null;
		return result;
	}

	public void setReturnFromEmbedWithValue(Object value) {
		returnFromEmbed = new ReturnFromEmbed(value);
	}

	public record ReturnFromEmbed(Object value) {
		public <T> Optional<T> value(TypeRef<T> ref) {
			return ofNullable(value).flatMap(ref::cast);
		}
	}

}
