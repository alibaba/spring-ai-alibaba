/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.ParallelBranchPath;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.SystemClock;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;

import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

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

	public static final String BRANCH_WRITES = "_graph_branch_writes_";

	private static final Logger log = LoggerFactory.getLogger(GraphRunner.class);

	final CompiledGraph compiledGraph;

	final AtomicInteger iteration = new AtomicInteger(0);

	private final ParallelBranchPath branchPath;

	private final List<Map<String, Object>> branchWrites = new ArrayList<>();

	private boolean captureWrites;

	private final UUID executionId = UUID.randomUUID();

	private volatile Checkpoint currentCheckpoint;

	private boolean replayingHistoricalCheckpoint;

	OverAllState overallState;

	RunnableConfig config;

	String currentNodeId;

	String nextNodeId;

	Usage tokenUsage;

	String resumeFrom;

	ReturnFromEmbed returnFromEmbed;

	public GraphRunnerContext(OverAllState initialState, RunnableConfig config, CompiledGraph compiledGraph)
			throws Exception {
		this(initialState, config, compiledGraph, null);
	}

	GraphRunnerContext(OverAllState initialState, RunnableConfig config, CompiledGraph compiledGraph,
			ParallelBranchPath branchPath) throws Exception {
		this.compiledGraph = compiledGraph;
		this.config = config;
		this.branchPath = branchPath;
		this.captureWrites = branchPath != null || isEmbeddedRun();

		// Decide whether this run continues from a persisted checkpoint or starts over
		// from START. A checkpoint is only resumed when the config explicitly asks for it,
		// either by carrying a concrete checkpoint id or by carrying resume metadata
		// (added via RunnableConfig.withResume() / RunnableConfig.builder(config).resume()).
		//
		// Note: passing a config that merely has a checkpoint saver / thread id (e.g.
		// invoke(Map.of(), config) without a checkpoint id and without resume metadata)
		// intentionally starts from START and only reuses the persisted state; it does NOT
		// continue from the latest checkpoint's next node. To resume from the latest
		// checkpoint without knowing its id, use config.withResume(). See RunnableConfig#withResume().
		if (config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isPresent() || config.checkPointId().isPresent()) {
			initializeFromResume(initialState, config);
		} else {
			initializeFromStart(initialState, config);
		}
	}

	private void initializeFromResume(OverAllState initialState, RunnableConfig config) {
		log.trace("RESUME REQUEST");

		var saver = compiledGraph.compileConfig.checkpointSaver()
				.orElseThrow(() -> new IllegalStateException("Resume request without a configured checkpoint saver!"));
		var checkpoint = saver.get(config)
				.orElseThrow(() -> new IllegalStateException("Resume request without a valid checkpoint!"));
		this.currentCheckpoint = checkpoint;
		if (captureWrites && checkpoint.runtimeData().containsKey(BRANCH_WRITES)) {
			CheckpointWrite.decodeAll(checkpoint.runtimeData().get(BRANCH_WRITES)).stream()
					.map(CheckpointWrite::encode).forEach(branchWrites::add);
		}
		else if (branchPath == null) {
			// Earlier child checkpoints contain only a state snapshot.
			captureWrites = false;
		}
		this.replayingHistoricalCheckpoint = compiledGraph.compileConfig.taskCheckpoints()
				&& config.checkPointId().isPresent()
				&& saver.get(config.withCheckPointId(null))
						.map(latest -> !latest.getId().equals(checkpoint.getId()))
						.orElse(false);

		var startCheckpointNextNodeAction = compiledGraph.getNodeAction(checkpoint.getNextNodeId());
		if (startCheckpointNextNodeAction instanceof ResumableSubGraphAction resumableAction) {
			// RESUME FORM SUBGRAPH DETECTED
			this.config = RunnableConfig.builder(config)
					.checkPointId(null) // Reset checkpoint id
					.addMetadata(resumableAction.getResumeSubGraphId(), true) // add metadata for
					// sub graph
					.build();
			this.config.clearContext();
		} else {
			// Reset checkpoint id
			this.config = config.withCheckPointId(null);
		}

		this.currentNodeId = null;
		this.nextNodeId = checkpoint.getNextNodeId();
		this.overallState = stateCreate(checkpoint.getState(), initialState);
		this.overallState.input(initialState.data());
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

		// A child starts from the parent state already materialized by its reducers.
		this.overallState = stateCreate(isParallelBranch() || isEmbeddedRun()
				? inputs : compiledGraph.getInitialState(inputs, config), initialState);
		this.currentNodeId = START;
		this.nextNodeId = null;
	}

	// FIXME, duplicated method with CompiledGraph.stateCreate, need to have a
	// unified way of when and how to do OverallState creation.
	// This temporary fix is to make sure the message provided by user is always the
	// last element in the messages list.
	private OverAllState stateCreate(Map<String, Object> inputs, OverAllState initialState) {
		// Creates a new OverAllState instance using key strategies from the graph and
		// provided input data.
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
		return END.equals(nextNodeId) || (branchPath != null && branchPath.join().equals(nextNodeId));
	}

	public boolean isParallelBranch() {
		return branchPath != null;
	}

	public boolean isEmbeddedRun() {
		return config.metadata(ResumableSubGraphAction.CAPTURE_WRITES).filter(Boolean.TRUE::equals).isPresent();
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
		return resolveEntryPoint().toFuture().get();
	}

	public Command nextNodeId(String nodeId, Map<String, Object> state) throws Exception {
		return resolveNextNode(nodeId, state).toFuture().get();
	}

	public Mono<Command> resolveEntryPoint() {
		if (branchPath != null) {
			return Mono.just(new Command(branchPath.entry(), overallState.data()));
		}
		return resolveNextNode(compiledGraph.getEdge(START), overallState.data(), START);
	}

	public Mono<Command> resolveNextNode(String nodeId, Map<String, Object> state) {
		return resolveNextNode(compiledGraph.getEdge(nodeId), state, nodeId);
	}

	private Mono<Command> resolveNextNode(EdgeValue route, Map<String, Object> state, String nodeId) {
		return Mono.defer(() -> {
			if (route == null) {
				return Mono.error(RunnableErrors.missingEdge.exception(nodeId));
			}
			if (route.id() != null) {
				return Mono.just(new Command(route.id(), state));
			}
			var condition = route.value();
			if (condition == null) {
				return Mono.error(RunnableErrors.executionError.exception(format("Invalid edge value for node '%s'", nodeId)));
			}
			if (condition.isMultiCommand()) {
				return Mono.just(new Command(ParallelNode.formatNodeId(nodeId), state));
			}
			return Mono.fromFuture(condition.singleAction().apply(overallState, config))
					.switchIfEmpty(Mono.error(RunnableErrors.executionError.exception(
							format("Conditional edge from node '%s' returned no command", nodeId))))
					.handle((command, sink) -> {
						String target = condition.mappings().get(command.gotoNode());
						if (target == null) {
							sink.error(RunnableErrors.missingNodeInEdgeMapping.exception(command.gotoNode(), nodeId));
						}
						else {
							mergeIntoCurrentState(command.update());
							sink.next(new Command(target, state));
						}
					});
		});
	}

	// ================================================================================================================
	// Checkpoint Methods
	// ================================================================================================================

	public Optional<Checkpoint> addCheckpoint(String nodeId, String nextNodeId) throws Exception {
		if (branchPath != null && !compiledGraph.compileConfig.taskCheckpoints()) {
			return Optional.empty();
		}
		if (compiledGraph.compileConfig.checkpointSaver().isPresent()) {
			var cp = Checkpoint.builder().nodeId(nodeId).state(cloneState(overallState.data())).nextNodeId(nextNodeId)
					.build();
			if (captureWrites) {
				cp = cp.withRuntimeData(branchCompletionMetadata());
			}
			// Force checkPointId to null to ensure we append a new checkpoint instead of
			// replacing the current one
			RunnableConfig appendConfig = RunnableConfig.builder(config).checkPointId(null).build();
			this.config = compiledGraph.compileConfig.checkpointSaver().get().put(appendConfig, cp);
			this.currentCheckpoint = cp;
			this.replayingHistoricalCheckpoint = false;
			return Optional.of(cp);
		}
		return Optional.empty();
	}

	public Map<String, Object> branchCompletionMetadata() {
		return captureWrites ? Map.of(BRANCH_WRITES, List.copyOf(branchWrites)) : Map.of();
	}

	public void checkpointPausedNode() throws Exception {
		var saver = compiledGraph.compileConfig.checkpointSaver();
		if (saver.isEmpty()) {
			return;
		}
		if (currentCheckpoint == null || replayingHistoricalCheckpoint
				|| !currentNodeId.equals(currentCheckpoint.getNextNodeId())) {
			addCheckpoint(currentNodeId, currentNodeId);
			return;
		}
		var runtime = new HashMap<>(currentCheckpoint.runtimeData());
		runtime.putAll(branchCompletionMetadata());
		var checkpoint = Checkpoint.builder().id(currentCheckpoint.getId()).nodeId(currentCheckpoint.getNodeId())
				.nextNodeId(currentNodeId).state(cloneState(overallState.data())).build().withRuntimeData(runtime);
		saver.get().put(config.withCheckPointId(checkpoint.getId()), checkpoint);
		currentCheckpoint = checkpoint;
	}

	/**
	 * Builds a stable task identity within a persisted step or a single in-memory run.
	 */
	public String branchTaskId(int index, String nodeId) {
		String scope = currentCheckpoint == null || replayingHistoricalCheckpoint
				? executionId + ":" + iteration.get() : currentCheckpoint.getId();
		String identity = scope + ":" + index + ":" + nodeId;
		return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
	}

	public synchronized Map<String, Object> stepCheckpointData() {
		return canWriteTaskProgress() ? currentCheckpoint.runtimeData() : Map.of();
	}

	public synchronized void updateStepCheckpoint(UnaryOperator<Map<String, Object>> update) {
		if (!canWriteTaskProgress()) {
			return;
		}
		try {
			var checkpoint = currentCheckpoint.withRuntimeData(update.apply(currentCheckpoint.runtimeData()));
			compiledGraph.compileConfig.checkpointSaver().orElseThrow()
					.put(config.withCheckPointId(checkpoint.getId()), checkpoint);
			this.currentCheckpoint = checkpoint;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot checkpoint tasks for node '" + currentNodeId + "'", ex);
		}
	}

	private boolean canWriteTaskProgress() {
		return compiledGraph.compileConfig.taskCheckpoints() && currentCheckpoint != null
				&& !replayingHistoricalCheckpoint && currentCheckpoint.getNextNodeId().equals(currentNodeId);
	}

	public void clearHumanFeedback() {
		this.config = RunnableConfig.builder(config)
				.removeMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY)
				.removeMetadata(RunnableConfig.STATE_UPDATE_METADATA_KEY)
				.build();
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

	public StreamingOutput<?> buildStreamingOutput(Message message, Object originData, String nodeId, boolean streaming) {
		// Create StreamingOutput with chunk and originData
		OutputType outputType = OutputType.from(streaming, nodeId);
		StreamingOutput<?> output = new StreamingOutput<>(message, originData, nodeId,
				(String) config.metadata("_AGENT_").orElse(""), this.overallState, outputType);
		output.setSubGraph(true);
		return output;
	}

	public StreamingOutput<?> buildStreamingOutput(Object originData, String nodeId, boolean streaming) {
		// Create StreamingOutput with chunk only
		OutputType outputType = OutputType.from(streaming, nodeId);
		StreamingOutput<?> output = new StreamingOutput<>(originData, nodeId, (String) config.metadata("_AGENT_").orElse(""),
				this.overallState, outputType);
		output.setSubGraph(true);
		return output;
	}

	// Normal NodeOutput builders for nodes with normal message output.

	public NodeOutput buildNodeOutput(String nodeId) throws Exception {
		return NodeOutput.of(
				nodeId,
				(String) config.metadata("_AGENT_").orElse(""),
				cloneState(this.overallState.data()),
				this.tokenUsage);
	}

	public OverAllState cloneState(Map<String, Object> data) throws Exception {
		return compiledGraph.cloneState(data);
	}

	// ================================================================================================================
	// Lifecycle Methods
	// ================================================================================================================

	public void doListeners(String scene, Exception e) {
		doListeners(scene, getCurrentNodeId(), e);
	}

	/**
	 * Notifies lifecycle listeners using an explicit node ID.
	 * @param scene the lifecycle scene
	 * @param nodeId the node associated with the lifecycle event
	 * @param e the error when the scene is {@link StateGraph#ERROR}
	 */
	public void doListeners(String scene, String nodeId, Exception e) {
		for (GraphLifecycleListener listener : compiledGraph.compileConfig.lifecycleListeners()) {
			try {
				switch (scene) {
					case START:
						listener.onStart(nodeId, getCurrentStateData(), config);
						break;
					case END:
						listener.onComplete(END, getCurrentStateData(), config);
						break;
					case NODE_BEFORE:
						listener.before(nodeId, getCurrentStateData(), config, SystemClock.now());
						break;
					case NODE_AFTER:
						listener.after(nodeId, getCurrentStateData(), config, SystemClock.now());
						break;
					case ERROR:
						listener.onError(nodeId, getCurrentStateData(), e, config);
						break;
				}
			} catch (Exception ex) {
				log.error("Error in listener", ex);
			}
		}
	}

	/**
	 * This method updates both the current state data and the overall state.
	 *
	 * @param updateState the state updates to apply
	 */
	public void mergeIntoCurrentState(Map<String, Object> updateState) {
		// Create a new map and filter out ChatResponse entries
		Map<String, Object> filteredState = findTokenUsageInDeltaState(updateState);
		var write = captureWrites && !filteredState.isEmpty() ? CheckpointWrite.encode(filteredState) : null;
		this.overallState.updateState(filteredState);
		if (write != null) {
			branchWrites.add(write);
		}
	}

	/**
	 * FIXME, this method is a temporary fix to separate Usage from state updates.
	 * works together with AgentLlmNode non-stream node.
	 */
	private Map<String, Object> findTokenUsageInDeltaState(Map<String, Object> updateState) {
		Map<String, Object> filteredState = new HashMap<>();
		for (Map.Entry<String, Object> entry : updateState.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Usage && entry.getKey().equals("_TOKEN_USAGE_")) {
				// Assign ChatResponse to this.chatResponse
				this.tokenUsage = (Usage) value;
			} else {
				// Add non-ChatResponse entries to the filtered map
				filteredState.put(entry.getKey(), value);
			}
		}
		return filteredState;
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
		return overallState.data();
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

	/**
	 * FIXME
	 * Below are duplicated methods. Need to have a unified way of streaming output
	 * to end user.
	 */
	public NodeOutput buildNodeOutputAndAddCheckpoint(Map<String, Object> updateStates) throws Exception {
		Optional<Checkpoint> cp = addCheckpoint(currentNodeId, nextNodeId);
		return buildOutput(currentNodeId, updateStates, cp, false);
	}

	public NodeOutput buildOutput(String nodeId, Map<String, Object> updateStates, Optional<Checkpoint> checkpoint, boolean streaming)
			throws Exception {
		if (checkpoint.isPresent() && config.streamMode() == CompiledGraph.StreamMode.SNAPSHOTS) {
			return StateSnapshot.of(getKeyStrategyMap(), checkpoint.get(), config,
					compiledGraph.stateGraph.getStateSerializer().stateFactory());
		}
		return buildNodeOutput(nodeId, updateStates, streaming);
	}

	public NodeOutput buildNodeOutput(String nodeId, Map<String, Object> updateStates, boolean streaming) throws Exception {
		Message message = null;

		// Check if updateStates is not empty
		if (updateStates != null && !updateStates.isEmpty()) {
			// Check if "messages" key exists and is a List
			Object messagesObj = updateStates.get("messages");
			if (messagesObj instanceof List<?> messagesList && !messagesList.isEmpty()) {
				// Iterate backwards to find the last Message, skipping non-Message
				// markers such as RemoveByHash that may be appended after the actual
				// message (e.g. in HITL partial tool-response handling).
				for (int i = messagesList.size() - 1; i >= 0; i--) {
					Object element = messagesList.get(i);
					if (element instanceof Message msg) {
						message = msg;
						break;
					}
				}
			} else if (messagesObj instanceof Message singleMessage) {
				// If it's a single Message instance
				message = singleMessage;
			}
		}

		OutputType outputType = OutputType.from(streaming, nodeId);

		if (message != null) {
			return new StreamingOutput<>(message, nodeId, (String) config.metadata("_AGENT_").orElse(""),
					cloneState(this.overallState.data()), tokenUsage, outputType);
		} else {
			return new StreamingOutput<>(nodeId, (String) config.metadata("_AGENT_").orElse(""),
					cloneState(this.overallState.data()), tokenUsage, outputType);
		}
	}

}
