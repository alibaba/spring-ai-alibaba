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

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNodeAction;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * A reactive graph execution engine based on Project Reactor. This completely replaces
 * the traditional Iterable-based AsyncGenerator approach.
 */
public class GraphRunner {

	private static final Logger log = LoggerFactory.getLogger(GraphRunner.class);

	private static final String INTERRUPT_AFTER = "__INTERRUPTED__";

	private final CompiledGraph compiledGraph;

	private final OverAllState initialState;

	private final RunnableConfig config;

	private final AtomicReference<Object> resultValue = new AtomicReference<>();

	public GraphRunner(CompiledGraph compiledGraph, OverAllState initialState, RunnableConfig config) {
		this.compiledGraph = compiledGraph;
		this.initialState = initialState;
		this.config = config;
	}

	public Flux<GraphResponse<NodeOutput>> run() {
		return Flux.create(sink -> {
			try {
				GeneratorContext context = new GeneratorContext(initialState, config, compiledGraph);
				processGraphExecution(sink, context);
			}
			catch (Exception e) {
				sink.next(GraphResponse.error(e));
				sink.complete();
			}
		}, FluxSink.OverflowStrategy.BUFFER);
	}

	public Optional<Object> resultValue() {
		return Optional.ofNullable(resultValue.get());
	}

	private void processGraphExecution(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
		try {
			if (context.shouldStop() || context.isMaxIterationsReached()) {
				handleCompletion(sink, context);
				return;
			}

			final var returnFromEmbed = context.getReturnFromEmbedAndReset();
			// Is it a resume from embed flux, can from a subgraph or normal node.
			if (returnFromEmbed.isPresent()) {
				var interruption = returnFromEmbed.get().value(new TypeRef<InterruptionMetadata>() {
				});
				if (interruption.isPresent()) {
					sink.next(GraphResponse.done(interruption.get()));
					sink.complete();
					return;
				}
				sink.next(GraphResponse.done(context.buildCurrentNodeOutput()));
				sink.complete();
				return;
			}

			// TODO, duplicate interruption mechanism with handleInterruption() below?
			// possibly needs to be unified.
			if (context.getCurrentNodeId() != null && config.isInterrupted(context.getCurrentNodeId())) {
				config.withNodeResumed(context.getCurrentNodeId());
				sink.next(GraphResponse.done(GraphResponse.done(context.getCurrentState())));
				sink.complete();
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

			final var resumeFrom = context.getResumeFromAndReset();
			if (resumeFrom.isPresent()) {
				if (compiledGraph.compileConfig.interruptBeforeEdge()
						&& Objects.equals(context.getNextNodeId(), INTERRUPT_AFTER)) {
					var nextNodeCommand = context.nextNodeId(resumeFrom.get(), context.getCurrentState());
					// nextNodeId = nextNodeCommand.gotoNode();
					context.setNextNodeId(nextNodeCommand.gotoNode());
					context.updateCurrentState(nextNodeCommand.update());
					context.setCurrentNodeId(null);
				}
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
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleStartNode(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
		try {
			context.doListeners(START, null);
			Command nextCommand = context.getEntryPoint();
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			Optional<Checkpoint> cp = context.addCheckpoint(START, context.getNextNodeId());
			NodeOutput output = context.buildOutput(START, cp);

			context.setCurrentNodeId(context.getNextNodeId());
			sink.next(GraphResponse.of(output));

			// Continue to next node
			processGraphExecution(sink, context);
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleEndNode(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
		try {
			context.doListeners(END, null);
			NodeOutput output = context.buildNodeOutput(END);
			sink.next(GraphResponse.of(output));
			handleCompletion(sink, context);
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleCompletion(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
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
			sink.next(GraphResponse.done(resultValue.get()));
			sink.complete();
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleInterruption(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
		try {
			InterruptionMetadata metadata = InterruptionMetadata
					.builder(context.getCurrentNodeId(), context.cloneState(context.getCurrentState()))
					.build();
			resultValue.set(metadata);
			sink.next(GraphResponse.done(metadata));
			sink.complete();
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void executeCurrentNode(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context) {
		try {
			context.setCurrentNodeId(context.getNextNodeId());
			String currentNodeId = context.getCurrentNodeId();
			AsyncNodeActionWithConfig action = context.getNodeAction(currentNodeId);

			if (action == null) {
				sink.next(GraphResponse.error(RunnableErrors.missingNode.exception(currentNodeId)));
				sink.complete();
				return;
			}

			// Check for interruptable action
			if (action instanceof InterruptableAction) {
				Optional<InterruptionMetadata> interruptMetadata = ((InterruptableAction) action)
						.interrupt(currentNodeId, context.cloneState(context.getCurrentState()));
				if (interruptMetadata.isPresent()) {
					resultValue.set(interruptMetadata.get());
					sink.next(GraphResponse.done(interruptMetadata.get()));
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
						sink.next(GraphResponse.error(error));
						sink.complete();
					});

		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleActionResult(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context,
			AsyncNodeActionWithConfig action, Map<String, Object> updateState) {
		try {
			context.doListeners(NODE_AFTER, null);

			// if (action instanceof CommandNode.AsyncCommandNodeActionWithConfig) {
			// handleCommandAction(sink, context, updateState);
			// return;
			// }

			// Check for embedded flux stream
			Optional<Flux<GraphResponse<NodeOutput>>> embedFlux = getEmbedFlux(context, updateState);
			if (embedFlux.isPresent()) {
				handleEmbeddedFlux(sink, context, embedFlux.get(), updateState);
				return;
			}

			// FIXME, remove this this deprecated embedded generator support
			Optional<AsyncGenerator<NodeOutput>> embedGenerator = getEmbedGenerator(updateState);
			if (embedGenerator.isPresent()) {
				handleEmbeddedGenerator(sink, context, embedGenerator.get(), updateState);
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
			sink.next(GraphResponse.of(output));

			// Continue to next node
			processGraphExecution(sink, context);

		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleCommandAction(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context,
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
					sink.next(GraphResponse.of(output));
					processGraphExecution(sink, context);
				}
				catch (Exception e) {
					sink.next(GraphResponse.error(e));
					sink.complete();
				}
			}, error -> {
				sink.next(GraphResponse.error(error));
				sink.complete();
			});
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	private void handleEmbeddedFlux(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState) {

		var result = new AtomicReference<Object>(null);

		// Process embedded Flux
		embedFlux.doOnNext(data -> {
			if (data.getOutput() != null) {
				var output = data.getOutput().join();
				output.setSubGraph(true);
				sink.next(GraphResponse.of(output));
			}
			result.set(data);
		}).doOnError(error -> {
			sink.next(GraphResponse.error(error));
			sink.complete();
		}).doOnComplete(() -> {
			try {
				var data = (GraphResponse) result.get();
				var nodeResultValue = data.resultValue;

				if (nodeResultValue instanceof InterruptionMetadata) {
					context.setReturnFromEmbedWithValue(nodeResultValue);
					sink.complete();
					return;
				}

				if (nodeResultValue != null) {
					if (nodeResultValue instanceof Map<?, ?>) {
						// Remove Flux from partial state
						Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
								.stream()
								.filter(e -> !(e.getValue() instanceof Flux))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

						// Apply partial state update first
						Map<String, Object> intermediateState = OverAllState.updateState(context.getCurrentState(),
								partialStateWithoutFlux, context.getKeyStrategyMap());
						var currentState = OverAllState.updateState(intermediateState,
								(Map<String, Object>) nodeResultValue, context.getKeyStrategyMap());
						context.updateCurrentState(currentState);
						context.getOverallState().updateState(currentState);
					}
					else {
						throw new IllegalArgumentException("Node stream must return Map result using Data.done(),");
					}
				}

				// After embedded flux completes, continue with main flow
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());

				processGraphExecution(sink, context);
			}
			catch (Exception e) {
				sink.next(GraphResponse.error(e));
				sink.complete();
			}
		}).subscribe();
	}

	private Optional<Flux<GraphResponse<NodeOutput>>> getEmbedFlux(GeneratorContext context,
			Map<String, Object> partialState) {
		return partialState.entrySet().stream().filter(e -> e.getValue() instanceof Flux<?>).findFirst().map(e -> {
			var chatFlux = (Flux<?>) e.getValue();
			var lastChatResponseRef = new AtomicReference<ChatResponse>(null);
			var lastGraphResponseRef = new AtomicReference<GraphResponse<NodeOutput>>(null);

			// Handle different element types in the flux
			return chatFlux.map(element -> {
				if (element instanceof ChatResponse response) {
					ChatResponse lastResponse = lastChatResponseRef.get();
					if (lastResponse == null) {
						GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
								.of(new StreamingOutput(response, context.getCurrentNodeId(), context.getOverallState()));
						lastChatResponseRef.set(response);
						lastGraphResponseRef.set(lastGraphResponse);
						return lastGraphResponse;
					}

					final var currentMessage = response.getResult().getOutput();

					if (currentMessage.hasToolCalls()) {
						GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
								.of(new StreamingOutput(response, context.getCurrentNodeId(), context.getOverallState()));
						lastGraphResponseRef.set(lastGraphResponse);
						return lastGraphResponse;
					}

					final var lastMessageText = requireNonNull(lastResponse.getResult().getOutput().getText(),
							"lastResponse text cannot be null");

					final var currentMessageText = currentMessage.getText();

					var newMessage = new AssistantMessage(
							currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText,
							currentMessage.getMetadata(), currentMessage.getToolCalls(), currentMessage.getMedia());

					var newGeneration = new Generation(newMessage, response.getResult().getMetadata());

					ChatResponse newResponse = new ChatResponse(List.of(newGeneration), response.getMetadata());
					lastChatResponseRef.set(newResponse);
					GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
							.of(new StreamingOutput(newResponse, context.getCurrentNodeId(), context.getOverallState()));
					lastGraphResponseRef.set(lastGraphResponse);
					return lastGraphResponse;
				}
				else if (element instanceof GraphResponse) {
					GraphResponse<NodeOutput> graphResponse = (GraphResponse<NodeOutput>) element;
					lastGraphResponseRef.set(graphResponse);
					return graphResponse;
				}
				else {
					// Handle unexpected types by creating an error response
					String errorMsg = "Unsupported flux element type: "
							+ (element != null ? element.getClass().getSimpleName() : "null");
					return GraphResponse.<NodeOutput>error(new IllegalArgumentException(errorMsg));
				}
			}).concatWith(Mono.defer(() -> {
				if (lastChatResponseRef.get() == null) {
					GraphResponse<?> lastGraphResponse = lastGraphResponseRef.get();
					if (lastGraphResponse != null && lastGraphResponse.resultValue().isPresent()) {
						Object result = lastGraphResponse.resultValue().get();
						if (result instanceof Map resultMap) {
							if (!resultMap.containsKey(e.getKey()) && resultMap.containsKey("messages")) {
								List<Object> messages = (List<Object>)resultMap.get("messages");
								resultMap.put(e.getKey(), ((AssistantMessage)messages.get(messages.size() - 1)).getText());
							}
						}
						return Mono.just(lastGraphResponseRef.get());
					}
					return Mono.empty();
				}
				else {
					return Mono.fromCallable(() -> {
						Map<String, Object> completionResult = Map.of(e.getKey(),
								lastChatResponseRef.get().getResult().getOutput());
						return GraphResponse.done(completionResult);
					});
				}
			}));
		});
	}

	/**
	 * Gets embed generator from partial state.
	 * @param partialState the partial state containing generator instances
	 * @return an Optional containing Data with the generator if found, empty otherwise
	 */
	@Deprecated
	private Optional<AsyncGenerator<NodeOutput>> getEmbedGenerator(Map<String, Object> partialState) {
		return partialState.entrySet()
				.stream()
				.filter(e -> e.getValue() instanceof AsyncGenerator)
				.findFirst()
				.map(generatorEntry -> (AsyncGenerator<NodeOutput>) generatorEntry.getValue());
	}

	@Deprecated
	private void handleEmbeddedGenerator(FluxSink<GraphResponse<NodeOutput>> sink, GeneratorContext context,
			AsyncGenerator<NodeOutput> generator, Map<String, Object> partialState) {

		generator.stream().peek(output -> {
			output.setSubGraph(true);
			sink.next(GraphResponse.of(output));
		});

		try {
			var iteratorResult = AsyncGenerator.resultValue(generator);
			if (iteratorResult.isPresent()) {
				var nodeResultValue = iteratorResult.get();

				if (nodeResultValue instanceof InterruptionMetadata) {
					context.setReturnFromEmbedWithValue(nodeResultValue);
					sink.complete();
					return;
				}

				if (nodeResultValue != null) {
					if (nodeResultValue instanceof Map<?, ?>) {
						// Remove Flux from partial state
						Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
								.stream()
								.filter(e -> !(e.getValue() instanceof Flux))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

						// Apply partial state update first
						Map<String, Object> intermediateState = OverAllState.updateState(context.getCurrentState(),
								partialStateWithoutFlux, context.getKeyStrategyMap());
						var currentState = OverAllState.updateState(intermediateState,
								(Map<String, Object>) nodeResultValue, context.getKeyStrategyMap());
						context.updateCurrentState(currentState);
						context.getOverallState().updateState(currentState);
					}
					else {
						throw new IllegalArgumentException("Node stream must return Map result using Data.done(),");
					}
				}
			}

			// After embedded flux completes, continue with main flow
			Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			processGraphExecution(sink, context);
		}
		catch (Exception e) {
			sink.next(GraphResponse.error(e));
			sink.complete();
		}
	}

	/**
	 * Context class to manage the state during graph execution
	 */
	private static class GeneratorContext {

		private final CompiledGraph compiledGraph;

		private final AtomicInteger iteration = new AtomicInteger(0);

		private OverAllState overallState;

		private RunnableConfig config;

		private String currentNodeId;

		private String nextNodeId;

		private Map<String, Object> currentState;

		private String resumeFrom;

		private ReturnFromEmbed returnFromEmbed;

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

			this.currentState = checkpoint.getState();
			this.currentNodeId = null;
			this.nextNodeId = checkpoint.getNextNodeId();
			this.overallState = initialState.input(this.currentState);
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

		Optional<String> getResumeFromAndReset() {
			final var result = ofNullable(resumeFrom);
			resumeFrom = null;
			return result;
		}

		Optional<ReturnFromEmbed> getReturnFromEmbedAndReset() {
			var result = ofNullable(returnFromEmbed);
			returnFromEmbed = null;
			return result;
		}

		void setReturnFromEmbedWithValue(Object value) {
			returnFromEmbed = new ReturnFromEmbed(value);
		}

		record ReturnFromEmbed(Object value) {
			<T> Optional<T> value(TypeRef<T> ref) {
				return ofNullable(value).flatMap(ref::cast);
			}
		}

	}

}
