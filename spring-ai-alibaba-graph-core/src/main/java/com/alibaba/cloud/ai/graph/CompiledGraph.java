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
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.ConditionalParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		}

		// EVALUATE EDGES
		for (var e : processedData.edges().elements) {
			var targets = e.targets();
			if (targets.size() == 1) {
				var target = targets.get(0);
				// Check if this is a conditional edge
				if (target.value() != null) {
					var edgeCondition = target.value();
					// Check if this is a multi-command action (returns multiple nodes)
					if (edgeCondition.isMultiCommand()) {
						// Multi-command action - create ConditionalParallelNode for parallel execution
						var conditionalParallelNode = new ConditionalParallelNode(
								e.sourceId(),
								edgeCondition,
								nodeFactories,
								keyStrategyMap,
								compileConfig);

						nodeFactories.put(conditionalParallelNode.id(), conditionalParallelNode.actionFactory());
						edges.put(e.sourceId(), new EdgeValue(conditionalParallelNode.id()));

						// Find parallel node targets from mappings
						var mappedNodeIds = edgeCondition.mappings().values().stream()
								.collect(Collectors.toSet());

						// Validate that all mapped nodes exist in the graph
						var missingNodeIds = mappedNodeIds.stream()
								.filter(nodeId -> !nodeFactories.containsKey(nodeId))
								.collect(Collectors.toSet());
						if (!missingNodeIds.isEmpty()) {
							throw new GraphStateException("Conditional multi-command mapping from node '"
									+ e.sourceId() + "' references unknown target nodes: " + missingNodeIds);
						}

						var parallelNodeTargets = findParallelNodeTargets(mappedNodeIds);
						if (!parallelNodeTargets.isEmpty()) {
							// Set edge from ConditionalParallelNode to the next node
							// All parallel nodes point to the same target, use that target
							edges.put(conditionalParallelNode.id(), new EdgeValue(parallelNodeTargets.iterator().next()));
						} else {
							throw Errors.illegalMultipleTargetsOnParallelNode.exception(e.sourceId(), 0);
						}
						// The ConditionalParallelNode will handle parallel execution internally
					} else {
						// Single Command action - same as regular single target edge
						edges.put(e.sourceId(), target);
					}
				} else {
					// Regular single target edge (no condition)
					edges.put(e.sourceId(), target);
				}
			} else {
				Supplier<Stream<EdgeValue>> parallelNodeStream = () -> targets.stream()
						.filter(target -> nodeFactories.containsKey(target.id()));

				var parallelNodeEdges = parallelNodeStream.get()
						.map(target -> new Edge(target.id()))
						.filter(ee -> processedData.edges().elements.contains(ee))
						.map(ee -> processedData.edges().elements.indexOf(ee))
						.map(index -> processedData.edges().elements.get(index))
						.toList();

				var parallelNodeTargets = parallelNodeEdges.stream()
						.map(ee -> ee.target().id())
						.collect(Collectors.toSet());

				if (parallelNodeTargets.size() > 1) {

					// find the first defer node

					var conditionalEdges = parallelNodeEdges.stream()
							.filter(ee -> ee.target().value() != null)
							.toList();
					if (!conditionalEdges.isEmpty()) {
						throw Errors.unsupportedConditionalEdgeOnParallelNode.exception(e.sourceId(),
								conditionalEdges.stream().map(Edge::sourceId).toList());
					}
					throw Errors.illegalMultipleTargetsOnParallelNode.exception(e.sourceId(), parallelNodeTargets);
				}

				var targetList = parallelNodeStream.get().toList();

				var actions = targetList.stream()
						.map(target -> {
							try {
								return nodeFactories.get(target.id()).apply(compileConfig);
							} catch (GraphStateException ex) {
								throw new RuntimeException("Failed to create parallel node action for target: "
										+ target.id() + ". Cause: " + ex.getMessage(), ex);
							}
						})
						.toList();

				var actionNodeIds = targetList.stream().map(EdgeValue::id).toList();

				var targetNodeId = parallelNodeTargets.iterator().next();
				var parallelNode = new ParallelNode(e.sourceId(), targetNodeId, actions, actionNodeIds, keyStrategyMap,
						compileConfig);

				nodeFactories.put(parallelNode.id(), parallelNode.actionFactory());

				edges.put(e.sourceId(), new EdgeValue(parallelNode.id()));

				edges.put(parallelNode.id(), new EdgeValue(targetNodeId));

			}
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
		BaseCheckpointSaver saver = compileConfig.checkpointSaver()
				.orElseThrow(() -> (new IllegalStateException("Missing CheckpointSaver!")));

		// merge values with checkpoint values
		Checkpoint branchCheckpoint = saver.get(config)
				.map(Checkpoint::copyOf)
				.map(cp -> cp.updateState(values, keyStrategyMap))
				.orElseThrow(() -> (new IllegalStateException("Missing Checkpoint!")));

		String nextNodeId = null;
		if (asNode != null) {
			var nextNodeCommand = nextNodeId(asNode, branchCheckpoint.getState(), config);

			nextNodeId = nextNodeCommand.gotoNode();
			branchCheckpoint = branchCheckpoint.updateState(nextNodeCommand.update(), keyStrategyMap);

		}
		// update checkpoint in saver
		RunnableConfig newConfig = saver.put(config, branchCheckpoint);

		return RunnableConfig.builder(newConfig).checkPointId(branchCheckpoint.getId()).nextNode(nextNodeId).build();
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

	/**
	 * Finds the target nodes for a set of source node IDs by looking up their edges.
	 * This is used to determine where parallel nodes should route after execution.
	 * Similar to the logic used for ParallelNode.
	 * 
	 * @param sourceNodeIds the set of source node IDs to find targets for
	 * @return a set of target node IDs that the source nodes point to
	 */
	private Set<String> findParallelNodeTargets(Set<String> sourceNodeIds) {
		var parallelNodeEdges = sourceNodeIds.stream()
				.map(nodeId -> new Edge(nodeId))  // Create Edge object with nodeId as source
				.filter(ee -> processedData.edges().elements.contains(ee))
				.map(ee -> processedData.edges().elements.indexOf(ee))
				.map(index -> processedData.edges().elements.get(index))
				.toList();

		return parallelNodeEdges.stream()
				.map(ee -> ee.target().id())
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
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
			return runner.run(overAllState).flatMap(data -> {
				if (data.isDone()) {
					if (data.resultValue().isPresent() && data.resultValue().get() instanceof NodeOutput) {
						return Flux.just((NodeOutput) data.resultValue().get());
					} else {
						return Flux.empty();
					}
				}
				if (data.isError()) {
					return Mono.fromFuture(data.getOutput()).onErrorMap(throwable -> throwable).flux();
				}

				return Mono.fromFuture(data.getOutput()).flux();
			});
		} catch (Exception e) {
			return Flux.error(e);
		}
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

