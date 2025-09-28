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
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNodeAction;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Context class to manage the state during graph execution
 */
public class GraphRunnerContext {

	public static final String INTERRUPT_AFTER = "__INTERRUPTED__";

	private static final Logger log = LoggerFactory.getLogger(GraphRunner.class);

	final CompiledGraph compiledGraph;

	final AtomicInteger iteration = new AtomicInteger(0);

	OverAllState overallState;

	RunnableConfig config;

	String currentNodeId;

	String nextNodeId;

	Map<String, Object> currentStateData;

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

	private void initializeFromResume(OverAllState initialState, RunnableConfig config) {
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

		this.currentStateData = checkpoint.getState();
		this.currentNodeId = null;
		this.nextNodeId = checkpoint.getNextNodeId();
		this.overallState = initialState.input(this.currentStateData);
		this.resumeFrom = checkpoint.getNodeId();

		log.trace("RESUME FROM {}", checkpoint.getNodeId());
	}

	private void initializeFromStart(OverAllState initialState, RunnableConfig config) {
		log.trace("START");

		Map<String, Object> inputs = initialState.data();
		if (!CollectionUtils.isEmpty(inputs)) {
			// Simple validation without accessing protected method
			log.debug("Initializing with inputs: {}", inputs.keySet());
		}

		// Use CompiledGraph's getInitialState method
		this.currentStateData = compiledGraph.getInitialState(inputs, config);
		// fixme
		this.overallState = stateCreate(currentStateData, initialState);
		this.currentNodeId = START;
		this.nextNodeId = null;
	}

	// FIXME, duplicated method with CompiledGraph.stateCreate, need to have a unified way of when and how to do OverallState creation.
	// This temporary fix is to make sure the message provided by user is always the last element in the messages list.
	private OverAllState stateCreate(Map<String, Object> inputs, OverAllState initialState) {
		// Creates a new OverAllState instance using key strategies from the graph and provided input data.
		return OverAllStateBuilder.builder()
				.withKeyStrategies(initialState.keyStrategies())
				.withData(inputs)
				.withStore(initialState.getStore())
				.build();
	}

	// Helper methods
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
		return shouldInterruptBefore(nextNodeId, currentNodeId) || shouldInterruptAfter(currentNodeId, nextNodeId);
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
		return nextNodeId(entryPoint, currentStateData, "entryPoint");
	}

	public Command nextNodeId(String nodeId, Map<String, Object> state) throws Exception {
		return nextNodeId(compiledGraph.getEdge(nodeId), state, nodeId);
	}

	private Command nextNodeId(com.alibaba.cloud.ai.graph.internal.edge.EdgeValue route, Map<String, Object> state,
			String nodeId) throws Exception {
		if (route == null) {
			throw RunnableErrors.missingEdge.exception(nodeId);
		}
		if (route.id() != null) {
			return new Command(route.id(), state);
		}
		if (route.value() != null) {
			var command = route.value().action().apply(this.overallState, config).get();
			var newRoute = command.gotoNode();
			String result = route.value().mappings().get(newRoute);
			if (result == null) {
				throw RunnableErrors.missingNodeInEdgeMapping.exception(nodeId, newRoute);
			}
			this.mergeIntoCurrentState(command.update());
			return new Command(result, this.currentStateData);
		}
		throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
	}

	// ================================================================================================================
	// Checkpoint Methods
	// ================================================================================================================

	public Optional<Checkpoint> addCheckpoint(String nodeId, String nextNodeId) throws Exception {
		if (compiledGraph.compileConfig.checkpointSaver().isPresent()) {
			var cp = Checkpoint.builder().nodeId(nodeId).state(cloneState(currentStateData)).nextNodeId(nextNodeId).build();
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
		return NodeOutput.of(nodeId, cloneState(currentStateData));
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
						listener.onStart(getCurrentNodeId(), getCurrentStateData(), config);
						break;
					case END:
						listener.onComplete(getCurrentNodeId(), getCurrentStateData(), config);
						break;
					case NODE_BEFORE:
						listener.onStart(getCurrentNodeId(), getCurrentStateData(), config);
						break;
					case NODE_AFTER:
						listener.onComplete(getCurrentNodeId(), getCurrentStateData(), config);
						break;
					case ERROR:
						listener.onError(getCurrentNodeId(), getCurrentStateData(), e, config);
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

	public void setCurrentStatData(Map<String, Object> state) {
		this.currentStateData = state;
	}

	/**
	 * This method updates both the current state data and the overall state.
	 *
	 * @param updateState the state updates to apply
	 */
	public void mergeIntoCurrentState(Map<String , Object> updateState) {
		this.currentStateData = OverAllState.updateState(this.currentStateData,
				updateState, getKeyStrategyMap());
		this.overallState.updateState(updateState);
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

	public Map<String, Object> getCurrentStateData() {
		return currentStateData;
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
