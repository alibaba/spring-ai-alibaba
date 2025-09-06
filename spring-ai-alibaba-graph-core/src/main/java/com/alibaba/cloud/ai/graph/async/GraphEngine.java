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
package com.alibaba.cloud.ai.graph.async;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.node.CommandNode;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static java.lang.String.format;

/**
 * A reactive graph execution engine based on Project Reactor. This completely replaces
 * the traditional Iterable-based AsyncGenerator approach.
 */
public class GraphEngine {

	private static final Logger log = LoggerFactory.getLogger(GraphEngine.class);

	private static final String INTERRUPT_AFTER = "__INTERRUPTED__";

	private final CompiledGraph compiledGraph;

	private final OverAllState initialState;

	private final RunnableConfig config;

	private final AtomicReference<Object> resultValue = new AtomicReference<>();

	public GraphEngine(CompiledGraph compiledGraph, OverAllState initialState, RunnableConfig config) {
		this.compiledGraph = compiledGraph;
		this.initialState = initialState;
		this.config = config;
	}

	public Flux<NodeOutput> asFlux() {
		return Flux.create(sink -> {
			try {
				GeneratorContext context = new GeneratorContext(initialState, config, compiledGraph);
				processGraphExecution(sink, context);
			}
			catch (Exception e) {
				sink.error(e);
			}
		}, FluxSink.OverflowStrategy.BUFFER);
	}

	public Optional<Object> resultValue() {
		return Optional.ofNullable(resultValue.get());
	}

	private void processGraphExecution(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			if (context.shouldStop() || context.isMaxIterationsReached()) {
				handleCompletion(sink, context);
				return;
			}

			if (context.isStartNode()) {
				handleStartNode(sink, context);
				return;
			}

			if (context.isEndNode()) {
				handleEndNode(sink, context);
				return;
			}

			if (context.shouldInterrupt()) {
				handleInterruption(sink, context);
				return;
			}



			// Execute current node
			executeCurrentNode(sink, context);

		}
		catch (Exception e) {
			context.doListeners(ERROR, e);
			log.error("Error during graph execution", e);
			sink.error(e);
		}
	}

	private void handleStartNode(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			context.doListeners(START, null);
			Command nextCommand = context.getEntryPoint();
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			Optional<Checkpoint> cp = context.addCheckpoint(START, context.getNextNodeId());
			NodeOutput output = context.buildOutput(START, cp);

			context.setCurrentNodeId(context.getNextNodeId());
			sink.next(output);

			// Continue to next node
			processGraphExecution(sink, context);
		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleEndNode(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			context.doListeners(END, null);
			NodeOutput output = context.buildNodeOutput(END);
			sink.next(output);
			handleCompletion(sink, context);
		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleCompletion(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			if (compiledGraph.compileConfig.releaseThread()
					&& compiledGraph.compileConfig.checkpointSaver().isPresent()) {
				BaseCheckpointSaver.Tag tag = compiledGraph.compileConfig.checkpointSaver()
					.get()
					.release(context.config);
				resultValue.set(tag);
			}
			else {
				resultValue.set(context.getCurrentState());
			}
			sink.complete();
		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleInterruption(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			InterruptionMetadata metadata = InterruptionMetadata
				.builder(context.getCurrentNodeId(), context.cloneState(context.getCurrentState()))
				.build();
			resultValue.set(metadata);
			sink.complete();
		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void executeCurrentNode(FluxSink<NodeOutput> sink, GeneratorContext context) {
		try {
			context.setCurrentNodeId(context.getNextNodeId());
			String currentNodeId = context.getCurrentNodeId();
			AsyncNodeActionWithConfig action = context.getNodeAction(currentNodeId);

			if (action == null) {
				throw RunnableErrors.missingNode.exception(currentNodeId);
			}

			// Check for interruptable action
			if (action instanceof InterruptableAction) {
				Optional<InterruptionMetadata> interruptMetadata = ((InterruptableAction) action)
					.interrupt(currentNodeId, context.cloneState(context.getCurrentState()));
				if (interruptMetadata.isPresent()) {
					resultValue.set(interruptMetadata.get());
					sink.complete();
					return;
				}
			}

			// Execute action
			context.doListeners(NODE_BEFORE, null);

			// Convert CompletableFuture to Mono for reactive processing
			Mono.fromFuture(action.apply(context.getOverallState(), context.config))
				.subscribe(updateState -> handleActionResult(sink, context, action, updateState), error -> {
					context.doListeners(NODE_AFTER, null);
					sink.error(error);
				});

		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleActionResult(FluxSink<NodeOutput> sink, GeneratorContext context,
			AsyncNodeActionWithConfig action, Map<String, Object> updateState) {
		try {
			context.doListeners(NODE_AFTER, null);

			if (action instanceof CommandNode.AsyncCommandNodeActionWithConfig) {
				handleCommandAction(sink, context, updateState);
				return;
			}

			// Check for embedded Flux generators
			Optional<Flux<NodeOutput>> embedFlux = getEmbedFlux(updateState);
			if (embedFlux.isPresent()) {
				handleEmbeddedFlux(sink, context, embedFlux.get(), updateState);
				return;
			}

			// Regular state update
			context.updateCurrentState(
					OverAllState.updateState(context.getCurrentState(), updateState, context.getKeyStrategyMap()));
			context.getOverallState().updateState(updateState);

			if (compiledGraph.compileConfig.interruptBeforeEdge()
					&& compiledGraph.compileConfig.interruptsAfter().contains(context.getCurrentNodeId())) {
				context.setNextNodeId(INTERRUPT_AFTER);
			}
			else {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());
			}

			NodeOutput output = context.buildCurrentNodeOutput();
			sink.next(output);

			// Continue to next node
			processGraphExecution(sink, context);

		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleCommandAction(FluxSink<NodeOutput> sink, GeneratorContext context,
			Map<String, Object> updateState) {
		try {
			AsyncCommandAction commandAction = (AsyncCommandAction) updateState.get("command");
			Mono.fromFuture(commandAction.apply(context.getOverallState(), context.config)).subscribe(command -> {
				try {
					context.updateCurrentState(OverAllState.updateState(context.getCurrentState(), command.update(),
							context.getKeyStrategyMap()));
					context.getOverallState().updateState(command.update());
					context.setNextNodeId(command.gotoNode());

					NodeOutput output = context.buildCurrentNodeOutput();
					sink.next(output);
					processGraphExecution(sink, context);
				}
				catch (Exception e) {
					sink.error(e);
				}
			}, sink::error);
		}
		catch (Exception e) {
			sink.error(e);
		}
	}

	private void handleEmbeddedFlux(FluxSink<NodeOutput> sink, GeneratorContext context, Flux<NodeOutput> embedFlux,
			Map<String, Object> partialState) {
		// Remove Flux from partial state
		Map<String, Object> cleanPartialState = partialState.entrySet()
			.stream()
			.filter(e -> !(e.getValue() instanceof Flux))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// Apply partial state update first
		if (!cleanPartialState.isEmpty()) {
			Map<String, Object> intermediateState = OverAllState.updateState(context.getCurrentState(),
					cleanPartialState, context.getKeyStrategyMap());
			context.updateCurrentState(intermediateState);
			context.getOverallState().updateState(intermediateState);
		}

		// Process embedded Flux
		embedFlux.doOnNext(output -> {
			output.setSubGraph(true);
			sink.next(output);
		}).doOnError(sink::error).doOnComplete(() -> {
			try {
				// After embedded flux completes, continue with main flow
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());

				processGraphExecution(sink, context);
			}
			catch (Exception e) {
				sink.error(e);
			}
		}).subscribe();
	}

	private Optional<Flux<NodeOutput>> getEmbedFlux(Map<String, Object> partialState) {
		return partialState.entrySet()
			.stream()
			.filter(e -> e.getValue() instanceof Flux)
			.findFirst()
			.map(e -> (Flux<NodeOutput>) e.getValue());
	}

	/**
	 * Context class to manage the state during graph execution
	 */
	private static class GeneratorContext {

		private final CompiledGraph compiledGraph;

		private OverAllState overallState;

		private final RunnableConfig config;

		private final AtomicInteger iteration = new AtomicInteger(0);

		private String currentNodeId;

		private String nextNodeId;

		private Map<String, Object> currentState;

		public GeneratorContext(OverAllState initialState, RunnableConfig config, CompiledGraph compiledGraph)
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

			this.currentState = checkpoint.getState();
			this.currentNodeId = null;
			this.nextNodeId = checkpoint.getNextNodeId();
			this.overallState = initialState.input(this.currentState);

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
			this.currentState = compiledGraph.getInitialState(inputs != null ? inputs : new HashMap<>(), config);
			this.overallState = initialState.input(currentState);
			this.currentNodeId = START;
			this.nextNodeId = null;
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

		public AsyncNodeActionWithConfig getNodeAction(String nodeId) {
			return compiledGraph.getNodeAction(nodeId);
		}

		public Command getEntryPoint() throws Exception {
			var entryPoint = compiledGraph.getEdge(START);
			return nextNodeId(entryPoint, currentState, "entryPoint");
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
				var updatedState = OverAllState.updateState(state, command.update(), getKeyStrategyMap());
				this.overallState.updateState(command.update());
				return new Command(result, updatedState);
			}
			throw RunnableErrors.executionError.exception(format("invalid edge value for nodeId: [%s] !", nodeId));
		}

		public Optional<Checkpoint> addCheckpoint(String nodeId, String nextNodeId) throws Exception {
			if (compiledGraph.compileConfig.checkpointSaver().isPresent()) {
				var cp = Checkpoint.builder()
					.nodeId(nodeId)
					.state(cloneState(currentState))
					.nextNodeId(nextNodeId)
					.build();
				compiledGraph.compileConfig.checkpointSaver().get().put(config, cp);
				return Optional.of(cp);
			}
			return Optional.empty();
		}

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
			return NodeOutput.of(nodeId, cloneState(currentState));
		}

		public OverAllState cloneState(Map<String, Object> data) throws Exception {
			return compiledGraph.cloneState(data);
		}

		public void doListeners(String scene, Exception e) {
			// TODO: Implementation for lifecycle listeners would go here
			log.debug("Listener event: {} with exception: {}", scene, e != null ? e.getMessage() : "none");
		}

		// Getters and setters
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

		public void updateCurrentState(Map<String, Object> state) {
			this.currentState = state;
		}

		public OverAllState getOverallState() {
			return overallState;
		}

		public Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> getKeyStrategyMap() {
			return compiledGraph.getKeyStrategyMap();
		}

	}

}
