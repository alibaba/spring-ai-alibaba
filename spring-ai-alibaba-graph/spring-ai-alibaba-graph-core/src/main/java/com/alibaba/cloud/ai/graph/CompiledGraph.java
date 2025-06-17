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

import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
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
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.AsyncGeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

/**
 * The type Compiled graph.
 */
public class CompiledGraph {

	private static final Logger log = LoggerFactory.getLogger(CompiledGraph.class);

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

	/**
	 * The State graph.
	 */
	public final StateGraph stateGraph;

	private final Map<String, KeyStrategy> keyStrategyMap;

	/**
	 * The Nodes.
	 */
	final Map<String, AsyncNodeActionWithConfig> nodes = new LinkedHashMap<>();

	/**
	 * The Edges.
	 */
	final Map<String, EdgeValue> edges = new LinkedHashMap<>();

	private final ProcessedNodesEdgesAndConfig processedData;

	private int maxIterations = 25;

	/**
	 * The Compile config.
	 */
	public final CompileConfig compileConfig;

	/**
	 * Constructs a CompiledGraph with the given StateGraph.
	 * @param stateGraph the StateGraph to be used in this CompiledGraph
	 * @param compileConfig the compile config
	 * @throws GraphStateException the graph state exception
	 */
	protected CompiledGraph(StateGraph stateGraph, CompileConfig compileConfig) throws GraphStateException {
		this.stateGraph = stateGraph;
		this.keyStrategyMap = Objects.isNull(stateGraph.getOverAllStateFactory())
				? stateGraph.getKeyStrategyFactory().apply()
				: stateGraph.getOverAllStateFactory().create().keyStrategies();

		this.processedData = ProcessedNodesEdgesAndConfig.process(stateGraph, compileConfig);

		// CHECK INTERRUPTIONS
		for (String interruption : processedData.interruptsBefore()) {
			if (!processedData.nodes().anyMatchById(interruption)) {
				throw Errors.interruptionNodeNotExist.exception(interruption);
			}
		}
		for (String interruption : processedData.interruptsBefore()) {
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

				var parallelNode = new ParallelNode(e.sourceId(), actions, keyStrategyMap);

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
	 * @param config the RunnableConfig containg the graph state
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
			.map(Checkpoint::new)
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
	 * @param config the RunnableConfig containg the graph state
	 * @param values the values to be updated
	 * @return the updated RunnableConfig
	 * @throws Exception when something goes wrong
	 */
	public RunnableConfig updateState(RunnableConfig config, Map<String, Object> values) throws Exception {
		return updateState(config, values, null);
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
		if (nodeId == null) { // FIX RESUME ERROR
			return false;
		}
		return compileConfig.interruptsAfter().contains(nodeId);
	}

	private Optional<Checkpoint> addCheckpoint(RunnableConfig config, String nodeId, Map<String, Object> state,
			String nextNodeId) throws Exception {
		if (compileConfig.checkpointSaver().isPresent()) {
			var cp = Checkpoint.builder().nodeId(nodeId).state(cloneState(state)).nextNodeId(nextNodeId).build();
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
	Map<String, Object> getInitialState(Map<String, Object> inputs, RunnableConfig config) {

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
	OverAllState cloneState(Map<String, Object> data) throws IOException, ClassNotFoundException {
		return stateGraph.getStateSerializer().cloneObject(data);
	}

	/**
	 * Creates an AsyncGenerator stream of NodeOutput based on the provided inputs.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");
		final AsyncNodeGenerator<NodeOutput> generator = new AsyncNodeGenerator<>(stateCreate(inputs), config);

		return new AsyncGenerator.WithEmbed<>(generator);
	}

	/**
	 * Stream async generator.
	 * @param overAllState the over all state
	 * @param config the config
	 * @return the async generator
	 */
	public AsyncGenerator<NodeOutput> streamFromInitialNode(OverAllState overAllState, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");
		final AsyncNodeGenerator<NodeOutput> generator = new AsyncNodeGenerator<>(overAllState, config);

		return new AsyncGenerator.WithEmbed<>(generator);
	}

	/**
	 * Creates an AsyncGenerator stream of NodeOutput based on the provided inputs.
	 * @param inputs the input map
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> inputs) throws GraphRunnerException {
		return this.streamFromInitialNode(stateCreate(inputs), RunnableConfig.builder().build());
	}

	/**
	 * Stream async generator.
	 * @return the async generator
	 */
	public AsyncGenerator<NodeOutput> stream() throws GraphRunnerException {
		return this.stream(Map.of(), RunnableConfig.builder().build());
	}

	/**
	 * Invokes the graph execution with the provided inputs and returns the final state.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an Optional containing the final state if present, otherwise an empty
	 * Optional
	 */
	public Optional<OverAllState> invoke(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		return stream(inputs, config).stream().reduce((a, b) -> b).map(NodeOutput::state);
	}

	/**
	 * Invoke optional.
	 * @param overAllState the over all state
	 * @param config the config
	 * @return the optional
	 */
	public Optional<OverAllState> invoke(OverAllState overAllState, RunnableConfig config) throws GraphRunnerException {
		return streamFromInitialNode(overAllState, config).stream().reduce((a, b) -> b).map(NodeOutput::state);
	}

	/**
	 * Invokes the graph execution with the provided inputs and returns the final state.
	 * @param inputs the input map
	 * @return an Optional containing the final state if present, otherwise an empty
	 * Optional
	 */
	public Optional<OverAllState> invoke(Map<String, Object> inputs) throws GraphRunnerException {
		return this.invoke(stateCreate(inputs), RunnableConfig.builder().build());
	}

	private OverAllState stateCreate(Map<String, Object> inputs) {
		// Creates a new OverAllState instance based on the presence of an
		// OverAllStateFactory in the stateGraph.
		// If no factory is present, constructs a new state using key strategies from the
		// graph and provided input data.
		// If a factory exists, uses it to create the state and applies the input data.
		return Objects.isNull(stateGraph.getOverAllStateFactory()) ? OverAllStateBuilder.builder()
			.withKeyStrategies(stateGraph.getKeyStrategyFactory().apply())
			.withData(inputs)
			.build() : stateGraph.getOverAllStateFactory().create().input(inputs);
	}

	/**
	 * Experimental API
	 * @param feedback the feedback
	 * @param config the config
	 * @return the optional
	 */
	public Optional<OverAllState> resume(OverAllState.HumanFeedback feedback, RunnableConfig config)
			throws GraphRunnerException {
		StateSnapshot stateSnapshot = this.getState(config);
		OverAllState resumeState = stateCreate(stateSnapshot.state().data());
		resumeState.withResume();
		resumeState.withHumanFeedback(feedback);

		return this.invoke(resumeState, config);
	}

	/**
	 * Creates an AsyncGenerator stream of NodeOutput based on the provided inputs.
	 * @param inputs the input map
	 * @param config the invoke configuration
	 * @return an AsyncGenerator stream of NodeOutput
	 */
	public AsyncGenerator<NodeOutput> streamSnapshots(Map<String, Object> inputs, RunnableConfig config)
			throws GraphRunnerException {
		Objects.requireNonNull(config, "config cannot be null");

		final AsyncNodeGenerator<NodeOutput> generator = new AsyncNodeGenerator<>(stateCreate(inputs),
				config.withStreamMode(StreamMode.SNAPSHOTS));

		return new AsyncGenerator.WithEmbed<>(generator);
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
	 * Async Generator for streaming outputs.
	 *
	 * @param <Output> the type of the output
	 */
	public class AsyncNodeGenerator<Output extends NodeOutput> implements AsyncGenerator<Output> {

		/**
		 * The Current state.
		 */
		Map<String, Object> currentState;

		/**
		 * The Current node id.
		 */
		String currentNodeId;

		/**
		 * The Next node id.
		 */
		String nextNodeId;

		/**
		 * The Over all state.
		 */
		OverAllState overAllState;

		/**
		 * The Iteration.
		 */
		int iteration = 0;

		/**
		 * The Config.
		 */
		RunnableConfig config;

		/**
		 * The Resumed from embed.
		 */
		boolean resumedFromEmbed = false;

		/**
		 * Instantiates a new Async node generator.
		 * @param overAllState the over all state
		 * @param config the config
		 */
		protected AsyncNodeGenerator(OverAllState overAllState, RunnableConfig config) throws GraphRunnerException {

			if (overAllState.isResume()) {

				log.trace("RESUME REQUEST");

				BaseCheckpointSaver saver = compileConfig.checkpointSaver()
					.orElseThrow(() -> (new IllegalStateException(
							"inputs cannot be null (ie. resume request) if no checkpoint saver is configured")));
				Checkpoint startCheckpoint = saver.get(config)
					.orElseThrow(() -> (new IllegalStateException("Resume request without a saved checkpoint!")));

				this.currentState = startCheckpoint.getState();

				// Reset checkpoint id
				this.config = config.withCheckPointId(null);
				this.overAllState = overAllState.input(this.currentState);
				this.nextNodeId = startCheckpoint.getNextNodeId();
				this.currentNodeId = null;
				log.trace("RESUME FROM {}", startCheckpoint.getNodeId());
			}
			else {

				log.trace("START");
				Map<String, Object> inputs = overAllState.data();
				boolean verify = overAllState.keyVerify();
				if (!CollectionUtils.isEmpty(inputs) && !verify) {
					throw RunnableErrors.initializationError.exception(Arrays.toString(inputs.keySet().toArray()));
				}
				// patch for backward support of AppendableValue
				this.currentState = getInitialState(inputs, config);
				this.overAllState = overAllState.input(currentState);
				this.nextNodeId = null;
				this.currentNodeId = START;
				this.config = config;
			}
		}

		private Optional<BaseCheckpointSaver.Tag> releaseThread() throws Exception {
			if (compileConfig.releaseThread() && compileConfig.checkpointSaver().isPresent()) {
				return Optional.of(compileConfig.checkpointSaver().get().release(config));
			}
			return Optional.empty();
		}

		/**
		 * Build node output output.
		 * @param nodeId the node id
		 * @return the output
		 */
		@SuppressWarnings("unchecked")
		protected Output buildNodeOutput(String nodeId) {
			return (Output) NodeOutput.of(nodeId, cloneState(currentState));
		}

		/**
		 * Clone state over all state.
		 * @param data the data
		 * @return the over all state
		 */
		OverAllState cloneState(Map<String, Object> data) {
			return new OverAllState(data, keyStrategyMap, overAllState.isResume());
		}

		/**
		 * Build state snapshot output.
		 * @param checkpoint the checkpoint
		 * @return the output
		 */
		@SuppressWarnings("unchecked")
		protected Output buildStateSnapshot(Checkpoint checkpoint) {
			return (Output) StateSnapshot.of(keyStrategyMap, checkpoint, config,
					stateGraph.getStateSerializer().stateFactory());
		}

		/**
		 * Gets embed generator from partial state.
		 * @param partialState the partial state containing generator instances
		 * @return an Optional containing Data with the generator if found, empty
		 * otherwise
		 */
		private Optional<Data<Output>> getEmbedGenerator(Map<String, Object> partialState) {
			// Extract all AsyncGenerator instances
			List<AsyncGenerator<Output>> asyncNodeGenerators = new ArrayList<>();
			var generatorEntries = partialState.entrySet().stream().filter(e -> {
				// Fixed when parallel nodes return asynchronous generating the same key
				Object value = e.getValue();
				if (value instanceof AsyncGenerator) {
					return true;
				}
				if (value instanceof Collection collection) {
					collection.forEach(o -> {
						if (o instanceof AsyncGenerator<?>) {
							asyncNodeGenerators.add((AsyncGenerator<Output>) o);
						}
					});
				}
				return false;
			}).collect(Collectors.toList());

			if (generatorEntries.isEmpty() && asyncNodeGenerators.isEmpty()) {
				return Optional.empty();
			}

			// Log information about found generators
			if (generatorEntries.size() > 1) {
				log.debug("Multiple generators found: {} - keys: {}", generatorEntries.size(),
						generatorEntries.stream().map(Map.Entry::getKey).collect(Collectors.joining(", ")));
			}

			// Create appropriate generator (single or merged)
			AsyncGenerator<Output> generator = AsyncGeneratorUtils.createAppropriateGenerator(generatorEntries,
					asyncNodeGenerators, keyStrategyMap);

			// Create data processing logic for the generator
			return Optional.of(Data.composeWith(generator.map(n -> {
				n.setSubGraph(true);
				return n;
			}), data -> processGeneratorOutput(data, partialState, generatorEntries)));
		}

		/**
		 * Processes output data from generator.
		 * @param data output data from generator
		 * @param partialState partial state
		 * @param generatorEntries generator entries list
		 * @throws Exception if an error occurs during processing
		 */
		@SuppressWarnings("unchecked")
		private void processGeneratorOutput(Object data, Map<String, Object> partialState,
				List<Map.Entry<String, Object>> generatorEntries) throws Exception {
			// Remove all generators
			Map<String, Object> partialStateWithoutGenerators = new HashMap<>();
			for (Map.Entry<String, Object> entry : partialState.entrySet()) {
				if (entry.getValue() instanceof AsyncGenerator) {
					continue; // Skip top-level AsyncGenerator values
				}

				if (entry.getValue() instanceof Collection<?>) {
					Collection<?> collection = (Collection<?>) entry.getValue();
					ArrayList<Object> filteredCollection = new ArrayList<>();

					for (Object item : collection) {
						if (!(item instanceof AsyncGenerator)) {
							filteredCollection.add(item);
						}
					}

					if (!filteredCollection.isEmpty()) {
						partialStateWithoutGenerators.put(entry.getKey(), filteredCollection);
					}
				}
				else {
					// Keep the entry if it's not an AsyncGenerator and not a collection
					// containing it
					partialStateWithoutGenerators.put(entry.getKey(), entry.getValue());
				}
			}

			// Update state with partial state without generators
			var intermediateState = OverAllState.updateState(currentState, partialStateWithoutGenerators,
					keyStrategyMap);
			currentState = intermediateState;
			overAllState.updateState(partialStateWithoutGenerators);

			// If data is not null and is a Map, update state with it
			if (data != null) {
				if (data instanceof Map<?, ?>) {
					currentState = OverAllState.updateState(intermediateState, (Map<String, Object>) data,
							keyStrategyMap);
					overAllState.updateState((Map<String, Object>) data);

					if (log.isDebugEnabled() && generatorEntries.size() > 1) {
						log.debug("Updated state with data keys: {}",
								((Map<String, Object>) data).keySet().stream().collect(Collectors.joining(", ")));
					}
				}
				else {
					throw new IllegalArgumentException("Embedded generator must return a Map");
				}
			}

			// Get next node command
			var nextNodeCommand = nextNodeId(currentNodeId, overAllState, currentState, config);
			nextNodeId = nextNodeCommand.gotoNode();
			currentState = nextNodeCommand.update();
			resumedFromEmbed = true;
		}

		private CompletableFuture<Data<Output>> evaluateAction(AsyncNodeActionWithConfig action,
				OverAllState withState) {

			return action.apply(withState, config).thenApply(updateState -> {
				try {

					Optional<Data<Output>> embed = getEmbedGenerator(updateState);
					if (embed.isPresent()) {
						return embed.get();
					}

					this.currentState = OverAllState.updateState(currentState, updateState, keyStrategyMap);
					this.overAllState.updateState(updateState);
					var nextNodeCommand = nextNodeId(currentNodeId, overAllState, currentState, config);
					nextNodeId = nextNodeCommand.gotoNode();
					this.currentState = nextNodeCommand.update();

					return Data.of(getNodeOutput());
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}

			});
		}

		private Command nextNodeId(String nodeId, OverAllState overAllState, Map<String, Object> state,
				RunnableConfig config) throws Exception {
			EdgeValue route = edges.get(nodeId);

			if (route == null) {
				throw RunnableErrors.missingEdge.exception(nodeId);
			}
			if (route.id() != null) {
				return new Command(route.id(), state);
			}
			if (route.value() != null) {
				var command = route.value().action().apply(overAllState, config).get();

				var newRoute = command.gotoNode();

				String result = route.value().mappings().get(newRoute);
				if (result == null) {
					throw RunnableErrors.missingNodeInEdgeMapping.exception(nodeId, newRoute);
				}

				var currentState = OverAllState.updateState(state, command.update(), keyStrategyMap);

				overAllState.updateState(command.update());

				return new Command(result, currentState);
			}
			throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
		}

		/**
		 * evaluate Action without nested support
		 */
		private CompletableFuture<Output> evaluateActionWithoutNested(AsyncNodeAction action, OverAllState withState) {

			return action.apply(withState).thenApply(partialState -> {
				try {
					currentState = OverAllState.updateState(currentState, partialState, keyStrategyMap);

					var nextNodeCommand = nextNodeId(currentNodeId, overAllState, currentState, config);
					nextNodeId = nextNodeCommand.gotoNode();
					currentState = nextNodeCommand.update();

					Optional<Checkpoint> cp = addCheckpoint(config, currentNodeId, currentState, nextNodeId);
					return (cp.isPresent() && config.streamMode() == StreamMode.SNAPSHOTS)
							? buildStateSnapshot(cp.get()) : buildNodeOutput(currentNodeId);

				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			});

		}

		private CompletableFuture<Output> getNodeOutput() throws Exception {
			Optional<Checkpoint> cp = addCheckpoint(config, currentNodeId, currentState, nextNodeId);
			return completedFuture((cp.isPresent() && config.streamMode() == StreamMode.SNAPSHOTS)
					? buildStateSnapshot(cp.get()) : buildNodeOutput(currentNodeId));
		}

		@Override
		public Data<Output> next() {
			try {
				// GUARD: CHECK MAX ITERATION REACHED
				if (++iteration > maxIterations) {
					// log.warn( "Maximum number of iterations ({}) reached!",
					// maxIterations);
					return Data.error(new IllegalStateException(
							format("Maximum number of iterations (%d) reached!", maxIterations)));
				}

				// GUARD: CHECK IF IT IS END
				if (nextNodeId == null && currentNodeId == null) {
					return releaseThread().map(Data::<Output>done).orElseGet(() -> Data.done(currentState));
				}

				// IS IT A RESUME FROM EMBED ?
				if (resumedFromEmbed) {
					final CompletableFuture<Output> future = getNodeOutput();
					resumedFromEmbed = false;
					return Data.of(future);
				}

				if (START.equals(currentNodeId)) {
					doListeners(START, null);
					var nextNodeCommand = getEntryPoint(currentState, config);
					nextNodeId = nextNodeCommand.gotoNode();
					currentState = nextNodeCommand.update();

					var cp = addCheckpoint(config, START, currentState, nextNodeId);

					var output = (cp.isPresent() && config.streamMode() == StreamMode.SNAPSHOTS)
							? buildStateSnapshot(cp.get()) : buildNodeOutput(currentNodeId);

					currentNodeId = nextNodeId;

					return Data.of(output);
				}

				if (END.equals(nextNodeId)) {
					nextNodeId = null;
					currentNodeId = null;
					doListeners(END, null);
					return Data.of(buildNodeOutput(END));
				}

				// check on previous node
				if (shouldInterruptAfter(currentNodeId, nextNodeId)) {
					return Data.done(currentNodeId);
				}

				if (shouldInterruptBefore(nextNodeId, currentNodeId)) {
					return Data.done(currentNodeId);
				}

				currentNodeId = nextNodeId;

				var action = nodes.get(currentNodeId);

				if (action == null)
					throw RunnableErrors.missingNode.exception(currentNodeId);

				return evaluateAction(action, this.overAllState).get();
			}
			catch (Exception e) {
				doListeners(ERROR, e);
				log.error(e.getMessage(), e);
				return Data.error(e);
			}

		}

		private void doListeners(String scene, Exception e) {
			Deque<GraphLifecycleListener> listeners = new LinkedBlockingDeque<>(compileConfig.lifecycleListeners());

			processListenersLIFO(listeners, scene, e);
		}

		private void processListenersLIFO(Deque<GraphLifecycleListener> listeners, String scene, Exception e) {
			if (listeners.isEmpty()) {
				return;
			}

			GraphLifecycleListener listener = listeners.pollLast();

			try {
				if (START.equals(scene)) {
					listener.onStart(START, this.currentState);
				}
				else if (END.equals(scene)) {
					listener.onComplete(END, this.currentState);
				}
				else if (ERROR.equals(scene)) {
					listener.onError(this.currentNodeId, this.currentState, e);
				}

				processListenersLIFO(listeners, scene, e);
			}
			catch (Exception ex) {
				log.debug("Error occurred during listener processing: {}", ex.getMessage());
			}
		}

	}

}

/**
 * The type Processed nodes edges and config.
 */
record ProcessedNodesEdgesAndConfig(StateGraph.Nodes nodes, StateGraph.Edges edges, Set<String> interruptsBefore,
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
		var nodes = new StateGraph.Nodes(stateGraph.nodes.exceptSubStateGraphNodes());
		var edges = new StateGraph.Edges(stateGraph.edges.elements);

		for (var subgraphNode : subgraphNodes) {

			var sgWorkflow = subgraphNode.subGraph();

			//
			// Process START Node
			//
			var sgEdgeStart = sgWorkflow.edges.edgeBySourceId(START).orElseThrow();

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
			var sgEdgesEnd = sgWorkflow.edges.edgesByTargetId(END);

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
			sgWorkflow.edges.elements.stream()
				.filter(e -> !Objects.equals(e.sourceId(), START))
				.filter(e -> !e.anyMatchByTargetId(END))
				.map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode, subgraphNode::formatId,
						id -> new EdgeValue(subgraphNode.formatId(id))))
				.forEach(edges.elements::add);

			//
			// Process nodes
			//
			sgWorkflow.nodes.elements.stream()
				.map(n -> n.withIdUpdated(subgraphNode::formatId))
				.forEach(nodes.elements::add);

		}

		return new ProcessedNodesEdgesAndConfig(nodes, edges, interruptsBefore, interruptsAfter);

	}

}
