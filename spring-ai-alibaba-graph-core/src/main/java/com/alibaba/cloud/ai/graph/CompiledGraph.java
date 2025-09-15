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
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.Errors;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.Edges;
import static com.alibaba.cloud.ai.graph.StateGraph.Nodes;
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
	 * The Nodes.
	 */
	final Map<String, AsyncNodeActionWithConfig> nodes = new LinkedHashMap<>();

	/**
	 * The Edges.
	 */
	final Map<String, EdgeValue> edges = new LinkedHashMap<>();

	private final Map<String, KeyStrategy> keyStrategyMap;

	private final ProcessedNodesEdgesAndConfig processedData;

	private int maxIterations = 25;

	/**
	 * Constructs a CompiledGraph with the given StateGraph.
	 * @param stateGraph the StateGraph to be used in this CompiledGraph
	 * @param compileConfig the compile config
	 * @throws GraphStateException the graph state exception
	 */
	protected CompiledGraph(StateGraph stateGraph, CompileConfig compileConfig) throws GraphStateException {
		this.stateGraph = stateGraph;
		this.keyStrategyMap = stateGraph.getKeyStrategyFactory()
			.apply()
			.entrySet()
			.stream()
			.map(e -> Map.entry(e.getKey(), e.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		this.processedData = ProcessedNodesEdgesAndConfig.process(stateGraph, compileConfig);

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

		// EVALUATES NODES
		for (var n : processedData.nodes().elements) {
			var factory = n.actionFactory();
			Objects.requireNonNull(factory, format("action factory for node id '%s' is null!", n.id()));
			nodes.put(n.id(), factory.apply(compileConfig));
		}

		// EVALUATE EDGES
		for (var e : processedData.edges().elements) {
			var targets = e.targets();
			if (targets.size() == 1) {
				edges.put(e.sourceId(), targets.get(0));
			}
			else {
				Supplier<Stream<EdgeValue>> parallelNodeStream = () -> targets.stream()
					.filter(target -> nodes.containsKey(target.id()));

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

				var actions = parallelNodeStream.get()
					// .map( target -> nodes.remove(target.id()) )
					.map(target -> nodes.get(target.id()))
					.toList();

				var parallelNode = new ParallelNode(e.sourceId(), actions, keyStrategyMap, compileConfig);

				nodes.put(parallelNode.id(), parallelNode.actionFactory().apply(compileConfig));

				edges.put(e.sourceId(), new EdgeValue(parallelNode.id()));

				edges.put(parallelNode.id(), new EdgeValue(parallelNodeTargets.iterator().next()));

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
	 * Same of {@link #stateOf(RunnableConfig)} but throws an IllegalStateException if
	 * checkpoint is not found.
	 * @param config the RunnableConfig
	 * @return the StateSnapshot of the given RunnableConfig
	 * @throws IllegalStateException if the saver is not defined, or no checkpoint is
	 * found
	 */
	public StateSnapshot getState(RunnableConfig config) {
		return stateOf(config).orElseThrow(() -> (new IllegalStateException("Missing Checkpoint!")));
	}

	/**
	 * Get the StateSnapshot of the given RunnableConfig.
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
	 * Update the state of the graph with the given values. If asNode is given, it will be
	 * used to determine the next node to run. If not given, the next node will be
	 * determined by the state graph.
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

			var command = route.value().action().apply(derefState, config).get();

			var newRoute = command.gotoNode();

			String result = route.value().mappings().get(newRoute);
			if (result == null) {
				throw RunnableErrors.missingNodeInEdgeMapping.exception(nodeId, newRoute);
			}

			var currentState = OverAllState.updateState(state, command.update(), keyStrategyMap);

			return new Command(result, currentState);
		}
		throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
	}

	/**
	 * Determines the next node ID based on the current node ID and state.
	 * @param nodeId the current node ID
	 * @param state the current state
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
	 * @param data the data
	 * @return the over all state
	 */
	OverAllState cloneState(Map<String, Object> data, OverAllState overAllState)
			throws IOException, ClassNotFoundException {
		return new OverAllState(stateGraph.getStateSerializer().cloneObject(data).data(), overAllState.keyStrategies(),
				overAllState.isResume(), overAllState.getStore());
	}

	/**
	 * Clone state over all state.
	 * @param data the data
	 * @return the over all state
	 */
	public OverAllState cloneState(Map<String, Object> data) throws IOException, ClassNotFoundException {
		return new OverAllState(stateGraph.getStateSerializer().cloneObject(data).data());
	}

	/**
	 * Package-private access to nodes for ReactiveNodeGenerator
	 */
	public AsyncNodeActionWithConfig getNodeAction(String nodeId) {
		return nodes.get(nodeId);
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
	 * @param maxIterations the maximum number of iterations
	 * @throws IllegalArgumentException if maxIterations is less than or equal to 0
	 */
	public void setMaxIterations(int maxIterations) {
		if (maxIterations <= 0) {
			throw new IllegalArgumentException("maxIterations must be > 0!");
		}
		this.maxIterations = maxIterations;
	}

	public Flux<GraphResponse<NodeOutput>> fluxDataStream(Map<String, Object> inputs, RunnableConfig config) {
		return fluxDataStream(stateCreate(inputs), config);
	}

	public Flux<GraphResponse<NodeOutput>> fluxDataStream(OverAllState state, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		try {
			GraphRunner runner = new GraphRunner(this, state, config);
			return runner.run();
		}
		catch (Exception e) {
			return Flux.error(e);
		}
	}

	/**
	 * Creates a Flux stream of NodeOutput based on the provided inputs. This is the
	 * modern reactive approach using Project Reactor.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> fluxStream(Map<String, Object> inputs, RunnableConfig config) {
		return fluxStreamFromInitialNode(stateCreate(inputs), config);
	}

	/**
	 * Creates a Flux stream from an initial state.
	 * @param overAllState the initial state
	 * @param config the configuration
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> fluxStreamFromInitialNode(OverAllState overAllState, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		try {
			GraphRunner runner = new GraphRunner(this, overAllState, config);
			return runner.run().flatMap(data -> {
				if (data.isDone()) {
					// TODO, collect data.resultValue if necessary.
					return Flux.empty();
				}
				if (data.isError()) {
					return Mono.fromFuture(data.getOutput()).onErrorMap(throwable -> throwable).flux();
				}
				return Mono.fromFuture(data.getOutput()).flux();
			});
		}
		catch (Exception e) {
			return Flux.error(e);
		}
	}

	/**
	 * Creates a Flux stream of NodeOutput based on the provided inputs.
	 * @param inputs the input map
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> fluxStream(Map<String, Object> inputs) {
		return fluxStream(inputs, RunnableConfig.builder().build());
	}

	/**
	 * Creates a Flux stream with empty inputs.
	 * @return a Flux stream of NodeOutput
	 */
	public Flux<NodeOutput> fluxStream() {
		return fluxStream(Map.of());
	}

	/**
	 * Creates a Flux stream for snapshots based on the provided inputs.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return a Flux stream of NodeOutput containing snapshots
	 */
	public Flux<NodeOutput> fluxStreamSnapshots(Map<String, Object> inputs, RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		return fluxStream(inputs, config.withStreamMode(StreamMode.SNAPSHOTS));
	}

	/**
	 * Calls the graph execution and returns the final state.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> call(Map<String, Object> inputs, RunnableConfig config) {
		return Optional.ofNullable(fluxStream(inputs, config).last().map(NodeOutput::state).block());
	}

	/**
	 * Calls the graph execution from initial state and returns the final state.
	 * @param overAllState the initial state
	 * @param config the configuration
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> call(OverAllState overAllState, RunnableConfig config) {
		return Optional
			.ofNullable(fluxStreamFromInitialNode(overAllState, config).last().map(NodeOutput::state).block()); // 阻塞等待结果
	}

	/**
	 * Calls the graph execution and returns the final state.
	 * @param inputs the input map
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> call(Map<String, Object> inputs) {
		return call(inputs, RunnableConfig.builder().build());
	}

	/**
	 * Resumes graph execution reactively.
	 * @param feedback the human feedback
	 * @param config the configuration
	 * @return an Optional containing the final state
	 */
	public Optional<OverAllState> resume(OverAllState.HumanFeedback feedback, RunnableConfig config) {
		try {
			StateSnapshot stateSnapshot = this.getState(config);
			OverAllState resumeState = stateCreate(stateSnapshot.state().data());
			resumeState.withResume();
			resumeState.withHumanFeedback(feedback);

			return Optional
				.ofNullable(fluxStreamFromInitialNode(resumeState, config).last().map(NodeOutput::state).block()); // 阻塞等待结果
		}
		catch (Exception e) {
			throw new RuntimeException("Resume execution failed", e);
		}
	}

	/**
	 * Schedule the graph execution with enhanced configuration options.
	 * @param scheduleConfig the schedule configuration
	 * @return a ScheduledGraphExecution instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) {
		return new ScheduledAgentTask(this, scheduleConfig).start();
	}

	private OverAllState stateCreate(Map<String, Object> inputs) {
		// Creates a new OverAllState instance using key strategies from the graph
		// and provided input data.
		return OverAllStateBuilder.builder()
			.withKeyStrategies(stateGraph.getKeyStrategyFactory().apply())
			.withData(inputs)
			.withStore(compileConfig.getStore())
			.build();
	}

	/**
	 * Get the last StateSnapshot of the given RunnableConfig.
	 * @param config - the RunnableConfig
	 * @return the last StateSnapshot of the given RunnableConfig if any
	 */
	Optional<StateSnapshot> lastStateOf(RunnableConfig config) {
		return getStateHistory(config).stream().findFirst();
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
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
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title) {

		String content = type.generator.generate(processedData.nodes(), processedData.edges(), title, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph with default title.
	 * @param type the type of graph representation to generate
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type) {
		return getGraph(type, "Graph Diagram", true);
	}

	/**
	 * Creates an AsyncGenerator stream of NodeOutput based on the provided inputs.
	 * @deprecated Use {@link #fluxStream(Map, RunnableConfig)} which returns Flux instead
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");
		// Convert Flux to AsyncGenerator for backward compatibility
		Flux<NodeOutput> flux = fluxStream(inputs, config);
		return AsyncGenerator.fromFlux(flux);
	}

	// ========================================
	// DEPRECATED METHODS FROM CompiledGraph2
	// ========================================

	/**
	 * Stream async generator from initial node.
	 * @deprecated Use {@link #fluxStreamFromInitialNode(OverAllState, RunnableConfig)}
	 * which returns Flux instead
	 * @param overAllState the over all state
	 * @param config the config
	 * @return the async generator
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public AsyncGenerator<NodeOutput> streamFromInitialNode(OverAllState overAllState, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");
		// Convert Flux to AsyncGenerator for backward compatibility
		Flux<NodeOutput> flux = fluxStreamFromInitialNode(overAllState, config);
		return AsyncGenerator.fromFlux(flux);
	}

	/**
	 * Creates an AsyncGenerator stream of NodeOutput based on the provided inputs.
	 * @deprecated Use {@link #fluxStream(Map)} which returns Flux instead
	 * @param inputs the input map
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> inputs) throws GraphRunnerException {
		// Convert Flux to AsyncGenerator for backward compatibility
		Flux<NodeOutput> flux = fluxStream(inputs);
		return AsyncGenerator.fromFlux(flux);
	}

	/**
	 * Stream async generator with empty inputs.
	 * @deprecated Use {@link #fluxStream()} which returns Flux instead
	 * @return the async generator
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public AsyncGenerator<NodeOutput> stream() throws GraphRunnerException {
		// Convert Flux to AsyncGenerator for backward compatibility
		Flux<NodeOutput> flux = fluxStream();
		return AsyncGenerator.fromFlux(flux);
	}

	/**
	 * Invokes the graph execution with the provided inputs and returns the final state.
	 * @deprecated Use {@link #call(Map, RunnableConfig)} which returns
	 * Optional&lt;OverAllState&gt; instead
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an Optional containing the final state if present, otherwise an empty
	 * Optional
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public Optional<OverAllState> invoke(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		try {
			return call(inputs, config);
		}
		catch (Exception e) {
			if (e instanceof GraphRunnerException) {
				throw e;
			}
			throw new GraphRunnerException("Invoke execution failed", e);
		}
	}

	/**
	 * Invoke with initial state.
	 * @deprecated Use {@link #call(OverAllState, RunnableConfig)} which returns
	 * Optional&lt;OverAllState&gt; instead
	 * @param overAllState the over all state
	 * @param config the config
	 * @return the optional
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public Optional<OverAllState> invoke(OverAllState overAllState, RunnableConfig config) throws GraphRunnerException {
		try {
			return call(overAllState, config);
		}
		catch (Exception e) {
			if (e instanceof GraphRunnerException) {
				throw e;
			}
			throw new GraphRunnerException("Invoke execution failed", e);
		}
	}

	/**
	 * Invokes the graph execution with the provided inputs and returns the final state.
	 * @deprecated Use {@link #call(Map)} which returns Optional&lt;OverAllState&gt;
	 * instead
	 * @param inputs the input map
	 * @return an Optional containing the final state if present, otherwise an empty
	 * Optional
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public Optional<OverAllState> invoke(Map<String, Object> inputs) throws GraphRunnerException {
		try {
			return call(inputs);
		}
		catch (Exception e) {
			if (e instanceof GraphRunnerException) {
				throw e;
			}
			throw new GraphRunnerException("Invoke execution failed", e);
		}
	}

	/**
	 * Creates an AsyncGenerator stream for snapshots based on the provided inputs.
	 * @deprecated Use {@link #fluxStreamSnapshots(Map, RunnableConfig)} which returns
	 * Flux instead
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	@Deprecated(since = "1.0.4", forRemoval = true)
	public AsyncGenerator<NodeOutput> streamSnapshots(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");
		// Convert Flux to AsyncGenerator for backward compatibility
		Flux<NodeOutput> flux = fluxStreamSnapshots(inputs, config);
		return AsyncGenerator.fromFlux(flux);
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

/**
 * The type Processed nodes edges and config.
 */
record ProcessedNodesEdgesAndConfig(Nodes nodes, Edges edges, Set<String> interruptsBefore,
		Set<String> interruptsAfter) {

	/**
	 * Instantiates a new Processed nodes edges and config.
	 * @param stateGraph the state graph
	 * @param config the config
	 */
	ProcessedNodesEdgesAndConfig(StateGraph stateGraph, CompileConfig config) {
		this(stateGraph.nodes, stateGraph.edges, config.interruptsBefore(), config.interruptsAfter());
	}

	/**
	 * Process processed nodes edges and config.
	 * @param stateGraph the state graph
	 * @param config the config
	 * @return the processed nodes edges and config
	 * @throws GraphStateException the graph state exception
	 */
	static ProcessedNodesEdgesAndConfig process(StateGraph stateGraph, CompileConfig config)
			throws GraphStateException {

		var subgraphNodes = stateGraph.nodes.onlySubStateGraphNodes();

		if (subgraphNodes.isEmpty()) {
			return new ProcessedNodesEdgesAndConfig(stateGraph, config);
		}

		var interruptsBefore = config.interruptsBefore();
		var interruptsAfter = config.interruptsAfter();
		var nodes = new Nodes(stateGraph.nodes.exceptSubStateGraphNodes());
		var edges = new Edges(stateGraph.edges.elements);

		for (var subgraphNode : subgraphNodes) {

			var sgWorkflow = subgraphNode.subGraph();

			ProcessedNodesEdgesAndConfig processedSubGraph = process(sgWorkflow, config);
			Nodes processedSubGraphNodes = processedSubGraph.nodes;
			Edges processedSubGraphEdges = processedSubGraph.edges;

			//
			// Process START Node
			//
			var sgEdgeStart = processedSubGraphEdges.edgeBySourceId(START).orElseThrow();

			if (sgEdgeStart.isParallel()) {
				throw new GraphStateException("subgraph not support start with parallel branches yet!");
			}

			var sgEdgeStartTarget = sgEdgeStart.target();

			if (sgEdgeStartTarget.id() == null) {
				throw new GraphStateException(format("the target for node '%s' is null!", subgraphNode.id()));
			}

			var sgEdgeStartRealTargetId = subgraphNode.formatId(sgEdgeStartTarget.id());

			// Process Interruption (Before) Subgraph(s)
			interruptsBefore = interruptsBefore.stream()
				.map(interrupt -> Objects.equals(subgraphNode.id(), interrupt) ? sgEdgeStartRealTargetId : interrupt)
				.collect(Collectors.toUnmodifiableSet());

			var edgesWithSubgraphTargetId = edges.edgesByTargetId(subgraphNode.id());

			if (edgesWithSubgraphTargetId.isEmpty()) {
				throw new GraphStateException(
						format("the node '%s' is not present as target in graph!", subgraphNode.id()));
			}

			for (var edgeWithSubgraphTargetId : edgesWithSubgraphTargetId) {

				var newEdge = edgeWithSubgraphTargetId.withSourceAndTargetIdsUpdated(subgraphNode, Function.identity(),
						id -> new EdgeValue((Objects.equals(id, subgraphNode.id())
								? subgraphNode.formatId(sgEdgeStartTarget.id()) : id)));
				edges.elements.remove(edgeWithSubgraphTargetId);
				edges.elements.add(newEdge);
			}
			//
			// Process END Nodes
			//
			var sgEdgesEnd = processedSubGraphEdges.edgesByTargetId(END);

			var edgeWithSubgraphSourceId = edges.edgeBySourceId(subgraphNode.id()).orElseThrow();

			if (edgeWithSubgraphSourceId.isParallel()) {
				throw new GraphStateException("subgraph not support routes to parallel branches yet!");
			}

			// Process Interruption (After) Subgraph(s)
			if (interruptsAfter.contains(subgraphNode.id())) {

				var exceptionMessage = (edgeWithSubgraphSourceId.target()
					.id() == null) ? "'interruption after' on subgraph is not supported yet!" : format(
							"'interruption after' on subgraph is not supported yet! consider to use 'interruption before' node: '%s'",
							edgeWithSubgraphSourceId.target().id());
				throw new GraphStateException(exceptionMessage);
			}

			sgEdgesEnd.stream()
				.map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode, subgraphNode::formatId,
						id -> (Objects.equals(id, END) ? edgeWithSubgraphSourceId.target()
								: new EdgeValue(subgraphNode.formatId(id)))))
				.forEach(edges.elements::add);
			edges.elements.remove(edgeWithSubgraphSourceId);

			//
			// Process edges
			//
			processedSubGraphEdges.elements.stream()
				.filter(e -> !Objects.equals(e.sourceId(), START))
				.filter(e -> !e.anyMatchByTargetId(END))
				.map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode, subgraphNode::formatId,
						id -> new EdgeValue(subgraphNode.formatId(id))))
				.forEach(edges.elements::add);

			//
			// Process nodes
			//
			processedSubGraphNodes.elements.stream().map(n -> {
				return n.withIdUpdated(subgraphNode::formatId);
			}).forEach(nodes.elements::add);
		}

		return new ProcessedNodesEdgesAndConfig(nodes, edges, interruptsBefore, interruptsAfter);
	}
}
