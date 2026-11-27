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
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.Errors;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.ParallelBranchPath;
import com.alibaba.cloud.ai.graph.internal.ParallelEdgeProcessor;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.ConditionalParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * The type Compiled graph.
 */
public class CompiledGraph {

	private static final Logger log = LoggerFactory.getLogger(CompiledGraph.class);

	private static String INTERRUPT_AFTER = "__INTERRUPTED__";

	/**
	 * The State graph.
	 */
	public final StateGraph stateGraph;

	/**
	 * The Compile config.
	 */
	public final CompileConfig compileConfig;

	/**
	 * The Node Factories - stores factory functions instead of instances to ensure
	 * thread safety.
	 */
	final Map<String, Node.ActionFactory> nodeFactories = new LinkedHashMap<>();

	private final Set<String> compiledSubgraphs = new LinkedHashSet<>();

	/**
	 * The Edges.
	 */
	final Map<String, EdgeValue> edges = new LinkedHashMap<>();

	private final Map<String, KeyStrategy> keyStrategyMap;

	private final ProcessedNodesEdgesAndConfig processedData;

	private int maxIterations = 25;

	/**
	 * Constructs a CompiledGraph with the given StateGraph.
	 * 
	 * @param stateGraph    the StateGraph to be used in this CompiledGraph
	 * @param compileConfig the compile config
	 * @throws GraphStateException the graph state exception
	 */
	protected CompiledGraph(StateGraph stateGraph, CompileConfig compileConfig) throws GraphStateException {
		this.maxIterations = compileConfig.recursionLimit();
		this.stateGraph = stateGraph;

		this.keyStrategyMap = stateGraph.getKeyStrategyFactory()
				.apply()
				.entrySet()
				.stream()
				.map(e -> Map.entry(e.getKey(), e.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		this.processedData = ProcessedNodesEdgesAndConfig.process(stateGraph, compileConfig);

		// set extra Key and KeyStrategy defined from sub Graphs (StateGraph)
		for (var entry : processedData.keyStrategyMap().entrySet()) {
			if (!this.keyStrategyMap.containsKey(entry.getKey())) {
				this.keyStrategyMap.put(entry.getKey(), entry.getValue());
			}
		}

		// CHECK INTERRUPTIONS
		for (String interruption : processedData.interruptsBefore()) {
			if (!processedData.nodes().anyMatchById(interruption)) {
				throw Errors.interruptionNodeNotExist.exception(interruption);
			}
		}
		for (String interruption : processedData.interruptsAfter()) {
			if (!processedData.nodes().anyMatchById(interruption)) {
				throw Errors.interruptionNodeNotExist.exception(interruption);
			}
		}

		// RE-CREATE THE EVENTUALLY UPDATED COMPILE CONFIG
		this.compileConfig = CompileConfig.builder(compileConfig)
				.interruptsBefore(processedData.interruptsBefore())
				.interruptsAfter(processedData.interruptsAfter())
				.build();

		// STORE NODE FACTORIES - for thread safety, we store factories instead of
		// instances
		for (var n : processedData.nodes().elements) {
			var factory = n.actionFactory();
			Objects.requireNonNull(factory, format("action factory for node id '%s' is null!", n.id()));
			nodeFactories.put(n.id(), factory);
			if (n instanceof SubCompiledGraphNode) {
				compiledSubgraphs.add(n.id());
			}
		}

		var parallelEdges = new ParallelEdgeProcessor(processedData.edges());
		for (var edge : processedData.edges().elements) {
			String source = edge.sourceId();
			String declaredJoin = processedData.parallelJoins().get(source);
			if (edge.isParallel()) {
				var targets = edge.targets().stream().map(EdgeValue::id).toList();
				var plan = parallelEdges.plan(source, targets, declaredJoin);
				var actions = new ArrayList<AsyncNodeActionWithConfig>();
				for (String target : targets) {
					actions.add(branchFactory(source, target, plan).apply(this.compileConfig));
				}
				registerParallel(source, plan.join(), new ParallelNode(source, plan.join(), actions, targets));
			}
			else if (edge.target().value() != null && edge.target().value().isMultiCommand()) {
				var condition = edge.target().value();
				var targets = new LinkedHashSet<>(condition.mappings().values());
				if (declaredJoin == null) {
					declaredJoin = immediateConditionalJoin(source, targets);
				}
				var plan = parallelEdges.plan(source, targets, declaredJoin);
				var branches = new LinkedHashMap<String, Node.ActionFactory>();
				targets.forEach(target -> branches.put(target, branchFactory(source, target, plan)));
				registerParallel(source, plan.join(), new ConditionalParallelNode(source, plan.join(), condition, branches));
			}
			else {
				edges.put(source, edge.target());
			}
		}
	}

	private void registerParallel(String source, String join, Node fork) {
		nodeFactories.put(fork.id(), fork.actionFactory());
		edges.put(source, new EdgeValue(fork.id()));
		edges.put(fork.id(), new EdgeValue(join));
	}

	private String immediateConditionalJoin(String source, Set<String> branches) throws GraphStateException {
		var targets = new LinkedHashSet<String>();
		for (String branch : branches) {
			var next = processedData.edges().edgeBySourceId(branch);
			if (next.isEmpty() || next.get().isParallel() || next.get().target().id() == null) {
				throw new GraphStateException(
						"Parallel route from '%s' through '%s' requires an explicit join".formatted(source, branch));
			}
			targets.add(next.get().target().id());
		}
		if (targets.size() != 1) {
			throw Errors.illegalMultipleTargetsOnParallelNode.exception(source, targets.stream().sorted().toList());
		}
		return targets.iterator().next();
	}

	private Node.ActionFactory branchFactory(String fork, String target, ParallelEdgeProcessor.Plan plan) {
		if (target.equals(plan.join())) {
			return ignored -> (state, config) -> CompletableFuture.completedFuture(Map.of());
		}
		if (plan.directBranches().contains(target)) {
			return nodeFactories.get(target);
		}
		return ignored -> new PathAction(this, fork, new ParallelBranchPath(target, plan.join()));
	}

	private record PathAction(CompiledGraph graph, String fork, ParallelBranchPath path)
			implements ParallelBranchPath.Action {

		private String namespace() {
			return ParallelNode.formatNodeId(fork) + "/" + path.entry();
		}

		@Override
		public String getResumeSubGraphId() {
			return ResumableSubGraphAction.resumeSubGraphId(namespace());
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig parent) {
			var config = ResumableSubGraphAction.childConfig(namespace(), parent, graph.compileConfig,
					graph.compileConfig).build();
			boolean resume = ResumableSubGraphAction.shouldResume(namespace(), parent);
			if (!resume && graph.compileConfig.taskCheckpoints()) {
				var childConfig = config;
				resume = graph.compileConfig.checkpointSaver().flatMap(saver -> saver.get(childConfig)).isPresent();
				if (resume) {
					config = config.withResume();
				}
			}
			var runner = new GraphRunner(graph, config, path);
			var inputs = resume ? graph.stateCreate(Map.of()) : state;
			var result = runner.run(inputs);
			return CompletableFuture.completedFuture(Map.of(ResumableSubGraphAction.outputKeyToParent(namespace()),
					result));
		}

	}

	public Collection<StateSnapshot> getStateHistory(RunnableConfig config) {
		BaseCheckpointSaver saver = compileConfig.checkpointSaver()
				.orElseThrow(() -> (new IllegalStateException("Missing CheckpointSaver!")));

		return saver.list(config)
				.stream()
				.map(checkpoint -> StateSnapshot.of(keyStrategyMap, checkpoint, config, stateGraph.getStateFactory()))
				.collect(toList());
	}

	/**
	 * Same of {@link #stateOf(RunnableConfig)} but throws an IllegalStateException
	 * if
	 * checkpoint is not found.
	 * 
	 * @param config the RunnableConfig
	 * @return the StateSnapshot of the given RunnableConfig
	 * @throws IllegalStateException if the saver is not defined, or no checkpoint
	 *                               is
	 *                               found
	 */
	public StateSnapshot getState(RunnableConfig config) {
		return stateOf(config).orElseThrow(() -> (new IllegalStateException("Missing Checkpoint!")));
	}

	/**
	 * Get the StateSnapshot of the given RunnableConfig.
	 * 
	 * @param config the RunnableConfig
	 * @return an Optional of StateSnapshot of the given RunnableConfig
	 * @throws IllegalStateException if the saver is not defined
	 */
	public Optional<StateSnapshot> stateOf(RunnableConfig config) {
		BaseCheckpointSaver saver = compileConfig.checkpointSaver()
				.orElseThrow(() -> (new IllegalStateException("Missing CheckpointSaver!")));

		return saver.get(config)
				.map(checkpoint -> StateSnapshot.of(keyStrategyMap, checkpoint, config, stateGraph.getStateFactory()));

	}

	/**
	 * Update the state of the graph with the given values. If asNode is given, it
	 * will be
	 * used to determine the next node to run. If not given, the next node will be
	 * determined by the state graph.
	 * 
	 * @param config the RunnableConfig containing the graph state
	 * @param values the values to be updated
	 * @param asNode the node id to be used for the next node. can be null
	 * @return the updated RunnableConfig
	 * @throws Exception when something goes wrong
	 */
	public RunnableConfig updateState(RunnableConfig config, Map<String, Object> values, String asNode)
			throws Exception {
		return writeUpdatedCheckpoint(config, values, asNode, false);
	}

	/**
	 * Appends a new checkpoint derived from the selected checkpoint. The source
	 * remains available subject to the saver's history retention. Resume using the
	 * returned configuration to run the new branch.
	 * @param config the source thread and checkpoint
	 * @param values state changes for the new checkpoint
	 * @param asNode the node whose outgoing edges should be evaluated, or null to keep the saved destination
	 * @return the runnable configuration for the new checkpoint
	 * @throws Exception if routing or checkpoint storage fails
	 */
	public RunnableConfig forkState(RunnableConfig config, Map<String, Object> values, String asNode)
			throws Exception {
		return writeUpdatedCheckpoint(config, values, asNode, true);
	}

	private RunnableConfig writeUpdatedCheckpoint(RunnableConfig config, Map<String, Object> values, String asNode,
			boolean fork) throws Exception {
		BaseCheckpointSaver saver = compileConfig.checkpointSaver()
				.orElseThrow(() -> new IllegalStateException("Missing CheckpointSaver!"));
		try (var claim = compileConfig.taskCheckpoints() ? GraphRunner.claim(saver, config) : null) {
			var run = claim == null ? config : claim.lease().configure(config);
			var checkpoint = updatedCheckpoint(saver, run, values, asNode);
			var stored = saver.put(fork ? run.withCheckPointId(null) : run, checkpoint);
			var result = RunnableConfig.builder(claim == null ? stored : config).checkPointId(checkpoint.getId());
			if (!fork) {
				result.nextNode(asNode == null ? null : checkpoint.getNextNodeId());
			}
			return result.build();
		}
	}

	private Checkpoint updatedCheckpoint(BaseCheckpointSaver saver, RunnableConfig config, Map<String, Object> values,
			String asNode) throws Exception {
		Checkpoint source = saver.get(config)
				.orElseThrow(() -> new IllegalStateException("Missing Checkpoint!"));
		Checkpoint checkpoint = Checkpoint.copyOf(source).updateState(values, keyStrategyMap);
		if (asNode == null) {
			Object subgraphPaused = source.runtimeData().get(GraphRunnerContext.SUBGRAPH_PAUSED);
			// A running child receives explicit parent edits through the shared-state resume path.
			if (Boolean.FALSE.equals(subgraphPaused) || (subgraphPaused != null
					&& (values == null || values.isEmpty()))) {
				return checkpoint.withRuntimeData(Map.of(GraphRunnerContext.SUBGRAPH_PAUSED, subgraphPaused));
			}
			return checkpoint;
		}
		var route = nextNodeId(asNode, checkpoint.getState(), config);
		var routed = Checkpoint.builder().id(checkpoint.getId()).state(route.update())
				.nodeId(asNode).nextNodeId(route.gotoNode()).build();
		return isCompiledSubgraph(route.gotoNode())
				? routed.withRuntimeData(Map.of(GraphRunnerContext.SUBGRAPH_PAUSED, false)) : routed;
	}

	boolean isCompiledSubgraph(String nodeId) {
		return compiledSubgraphs.contains(nodeId);
	}

	/***
	 * Update the state of the graph with the given values.
	 * 
	 * @param config the RunnableConfig containing the graph state
	 * @param values the values to be updated
	 * @return the updated RunnableConfig
	 * @throws Exception when something goes wrong
	 */
	public RunnableConfig updateState(RunnableConfig config, Map<String, Object> values) throws Exception {
		return updateState(config, values, null);
	}



	private Command nextNodeId(EdgeValue route, Map<String, Object> state, String nodeId, RunnableConfig config)
			throws Exception {

		if (route == null) {
			throw RunnableErrors.missingEdge.exception(nodeId);
		}
		if (route.id() != null) {
			return new Command(route.id(), state);
		}
		if (route.value() != null) {
			OverAllState derefState = stateGraph.getStateFactory().apply(state);
			var edgeCondition = route.value();

			// Check if this is a multi-command action
			if (edgeCondition.isMultiCommand()) {
				// Multi-command action - route to ConditionalParallelNode
				String conditionalParallelNodeId = ParallelNode.formatNodeId(nodeId);
				return new Command(conditionalParallelNodeId, state);
			} else {
				// Single Command action
				var singleAction = edgeCondition.singleAction();
				var command = singleAction.apply(derefState, config).get();

				var newRoute = command.gotoNode();
				String result = route.value().mappings().get(newRoute);
				if (result == null) {
					throw RunnableErrors.missingNodeInEdgeMapping.exception(nodeId, newRoute);
				}

				var currentState = OverAllState.updateState(state, command.update(), keyStrategyMap);
				return new Command(result, currentState);
			}
		}
		throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
	}

	/**
	 * Determines the next node ID based on the current node ID and state.
	 * 
	 * @param nodeId the current node ID
	 * @param state  the current state
	 * @return the next node command
	 * @throws Exception if there is an error determining the next node ID
	 */
	private Command nextNodeId(String nodeId, Map<String, Object> state, RunnableConfig config) throws Exception {
		return nextNodeId(edges.get(nodeId), state, nodeId, config);

	}

	private Command getEntryPoint(Map<String, Object> state, RunnableConfig config) throws Exception {
		var entryPoint = this.edges.get(START);
		return nextNodeId(entryPoint, state, "entryPoint", config);
	}

	private boolean shouldInterruptBefore(String nodeId, String previousNodeId) {
		if (previousNodeId == null) { // FIX RESUME ERROR
			return false;
		}
		return compileConfig.interruptsBefore().contains(nodeId);
	}

	private boolean shouldInterruptAfter(String nodeId, String previousNodeId) {
		if (nodeId == null || Objects.equals(nodeId, previousNodeId)) { // FIX RESUME
			// ERROR
			return false;
		}
		return (compileConfig.interruptBeforeEdge() && Objects.equals(nodeId, INTERRUPT_AFTER))
				|| compileConfig.interruptsAfter().contains(nodeId);
	}

	private Optional<Checkpoint> addCheckpoint(RunnableConfig config, String nodeId, Map<String, Object> state,
			String nextNodeId, OverAllState overAllState) throws Exception {
		if (compileConfig.checkpointSaver().isPresent()) {
			var cp = Checkpoint.builder()
					.nodeId(nodeId)
					.state(cloneState(state, overAllState))
					.nextNodeId(nextNodeId)
					.build();
			compileConfig.checkpointSaver().get().put(config, cp);
			return Optional.of(cp);
		}
		return Optional.empty();

	}

	/**
	 * Gets initial state.
	 * 
	 * @param inputs the inputs
	 * @param config the config
	 * @return the initial state
	 */
	public Map<String, Object> getInitialState(Map<String, Object> inputs, RunnableConfig config) {

		return compileConfig.checkpointSaver()
				.flatMap(saver -> saver.get(config))
				.map(cp -> OverAllState.updateState(cp.getState(), inputs, keyStrategyMap))
				.orElseGet(() -> OverAllState.updateState(new HashMap<>(), inputs, keyStrategyMap));
	}

	/**
	 * Clone state over all state.
	 * 
	 * @param data the data
	 * @return the over all state
	 */
	OverAllState cloneState(Map<String, Object> data, OverAllState overAllState)
			throws IOException, ClassNotFoundException {
		return new OverAllState(stateGraph.getStateSerializer().cloneObject(data).data(), overAllState.keyStrategies(),
				overAllState.getStore());
	}

	/**
	 * Clone state over all state.
	 * 
	 * @param data the data
	 * @return the over all state
	 */
	public OverAllState cloneState(Map<String, Object> data) throws IOException, ClassNotFoundException {
		return new OverAllState(stateGraph.getStateSerializer().cloneObject(data).data(), getKeyStrategyMap());
	}

	/**
	 * Package-private access to nodes for ReactiveNodeGenerator.
	 */
	public AsyncNodeActionWithConfig getNodeAction(String nodeId) {
		Node.ActionFactory factory = nodeFactories.get(nodeId);
		try {
			return factory != null ? factory.apply(compileConfig) : null;
		} catch (GraphStateException e) {
			throw new RuntimeException(
					"Failed to create node action for nodeId: " + nodeId + ". Cause: " + e.getMessage(), e);
		}
	}

	/**
	 * Package-private access to edges for ReactiveNodeGenerator
	 */
	public EdgeValue getEdge(String nodeId) {
		return edges.get(nodeId);
	}

	/**
	 * Package-private access to keyStrategyMap for ReactiveNodeGenerator
	 */
	public Map<String, KeyStrategy> getKeyStrategyMap() {
		return keyStrategyMap;
	}

	/**
	 * Package-private access to maxIterations for ReactiveNodeGenerator
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * Sets the maximum number of iterations for the graph execution.
	 *
	 * @param maxIterations the maximum number of iterations
	 * @throws IllegalArgumentException if maxIterations is less than or equal to 0
	 * @deprecated use CompileConfig.recursionLimit() instead
	 */
	@Deprecated(forRemoval = true)
	public void setMaxIterations(int maxIterations) {
		if (maxIterations <= 0) {
			throw new IllegalArgumentException("maxIterations must be > 0!");
		}
		this.maxIterations = maxIterations;
	}

	public GraphResponse<NodeOutput> invokeAndGetResponse(Map<String, Object> inputs, RunnableConfig config) {
		return graphResponseStream(inputs, config).last().block();
	}

	public GraphResponse<NodeOutput> invokeAndGetResponse(OverAllState state, RunnableConfig config) {
		return graphResponseStream(state, config).last().block();
	}

	public Flux<GraphResponse<NodeOutput>> graphResponseStream(Map<String, Object> inputs, RunnableConfig config) {
		return graphResponseStream(stateCreate(inputs), config);
	}

	public Flux<GraphResponse<NodeOutput>> graphResponseStream(OverAllState state, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		try {
			GraphRunner runner = new GraphRunner(this, config);
			return runner.run(state);
		} catch (Exception e) {
			return Flux.error(e);
		}
	}

	/**
	 * Creates a Flux stream of NodeOutput based on the provided inputs. This is the
	 * modern reactive approach using Project Reactor.
	 * 
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> stream(Map<String, Object> inputs, RunnableConfig config) {
		return streamFromInitialNode(stateCreate(inputs), config);
	}

	/**
	 * Creates a Flux stream from an initial state.
	 * 
	 * @param overAllState the initial state
	 * @param config       the configuration
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> streamFromInitialNode(OverAllState overAllState, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		try {
			GraphRunner runner = new GraphRunner(this, config);
			return flattenGraphResponsesPreservingOrder(runner.run(overAllState));
		} catch (Exception e) {
			return Flux.error(e);
		}
	}

	static Flux<NodeOutput> flattenGraphResponsesPreservingOrder(Flux<GraphResponse<NodeOutput>> responses) {
		return responses.flatMapSequential(data -> {
			if (data.isDone()) {
				if (data.resultValue().isPresent() && data.resultValue().get() instanceof NodeOutput) {
					return Flux.just((NodeOutput) data.resultValue().get());
				}
				else {
					return Flux.empty();
				}
			}
			if (data.isError()) {
				return Mono.fromFuture(data.getOutput()).onErrorMap(throwable -> throwable).flux();
			}

			return Mono.fromFuture(data.getOutput()).flux();
		});
	}

	/**
	 * Creates a Flux stream of NodeOutput based on the provided inputs.
	 * 
	 * @param inputs the input map
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> stream(Map<String, Object> inputs) {
		return stream(inputs, RunnableConfig.builder().build());
	}

	/**
	 * Creates a Flux stream with empty inputs.
	 * 
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> stream() {
		return stream(Map.of());
	}

	/**
	 * Creates a Flux stream for snapshots based on the provided inputs.
	 * 
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return a Flux stream of NodeOutput containing snapshots
	 */
	public Flux<NodeOutput> streamSnapshots(Map<String, Object> inputs, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		return stream(inputs, config.withStreamMode(StreamMode.SNAPSHOTS));
	}

	/**
	 * Calls the graph execution and returns the final state.
	 * <p>
	 * Checkpoint semantics: when a checkpoint saver is configured, this method resumes from
	 * a persisted checkpoint only when {@code config} explicitly requests it, either by
	 * carrying a concrete checkpoint id ({@link RunnableConfig#withCheckPointId(String)}) or
	 * by carrying resume metadata ({@link RunnableConfig#withResume()}). Calling
	 * {@code invoke(Map.of(), config)} with neither reuses the persisted state but restarts
	 * execution from {@code START}; use {@code invoke(Map.of(), config.withResume())} to
	 * continue from the latest checkpoint's next node without knowing its id.
	 *
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> invoke(Map<String, Object> inputs, RunnableConfig config) {
		return Optional.ofNullable(stream(inputs, config).last().map(NodeOutput::state).block());
	}

	/**
	 * Calls the graph execution from initial state and returns the final state.
	 * 
	 * @param overAllState the initial state
	 * @param config       the configuration
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> invoke(OverAllState overAllState, RunnableConfig config) {
		return Optional
				.ofNullable(streamFromInitialNode(overAllState, config).last().map(NodeOutput::state).block());
	}

	/**
	 * Calls the graph execution and returns the final state.
	 * 
	 * @param inputs the input map
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> invoke(Map<String, Object> inputs) {
		return invoke(inputs, RunnableConfig.builder().build());
	}

	public Optional<NodeOutput> invokeAndGetOutput(OverAllState overAllState, RunnableConfig config) {
		return Optional.ofNullable(streamFromInitialNode(overAllState, config).last().block());
	}

	public Optional<NodeOutput> invokeAndGetOutput(Map<String, Object> inputs, RunnableConfig config) {
		return Optional.ofNullable(stream(inputs, config).last().block());
	}

	public Optional<NodeOutput> invokeAndGetOutput(Map<String, Object> inputs) {
		return invokeAndGetOutput(inputs, RunnableConfig.builder().build());
	}

	/**
	 * Schedule the graph execution with enhanced configuration options.
	 * 
	 * @param scheduleConfig the schedule configuration
	 * @return a ScheduledGraphExecution instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) {
		return new ScheduledAgentTask(this, scheduleConfig).start();
	}

	private OverAllState stateCreate(Map<String, Object> inputs) {
		// Handle null inputs (resume scenarios)
		if (inputs == null) {
			inputs = new HashMap<>();
		}

		// Enforce Execution ID availability
		if (!inputs.containsKey(GraphLifecycleListener.EXECUTION_ID_KEY)) {
			Map<String, Object> newInputs = new HashMap<>(inputs);
			newInputs.put(GraphLifecycleListener.EXECUTION_ID_KEY, java.util.UUID.randomUUID().toString());
			inputs = newInputs;
		}

		// Creates a new OverAllState instance using key strategies from the graph
		// and provided input data.
		return OverAllStateBuilder.builder()
				.withKeyStrategies(getKeyStrategyMap())
				.withData(inputs)
				.withStore(compileConfig.getStore())
				.build();
	}

	/**
	 * Get the last StateSnapshot of the given RunnableConfig.
	 * 
	 * @param config - the RunnableConfig
	 * @return the last StateSnapshot of the given RunnableConfig if any
	 */
	public Optional<StateSnapshot> lastStateOf(RunnableConfig config) {
		return getStateHistory(config).stream().findFirst();
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * 
	 * @param type                  the type of graph representation to generate
	 * @param title                 the title of the graph
	 * @param printConditionalEdges whether to print conditional edges
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title, boolean printConditionalEdges) {

		String content = type.generator.generate(processedData.nodes(), processedData.edges(), title,
				printConditionalEdges);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * 
	 * @param type  the type of graph representation to generate
	 * @param title the title of the graph
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title) {

		String content = type.generator.generate(processedData.nodes(), processedData.edges(), title, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph with default
	 * title.
	 * 
	 * @param type the type of graph representation to generate
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type) {
		return getGraph(type, "Graph Diagram", true);
	}

	/**
	 * The enum Stream mode.
	 */
	public enum StreamMode {

		/**
		 * Values stream mode.
		 */
		VALUES,
		/**
		 * Snapshots stream mode.
		 */
		SNAPSHOTS

	}

}
