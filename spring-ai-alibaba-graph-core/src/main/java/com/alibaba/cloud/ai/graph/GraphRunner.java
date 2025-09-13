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
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.Objects.requireNonNull;

/**
 * A reactive graph execution engine based on Project Reactor. This completely replaces
 * the traditional Iterable-based AsyncGenerator approach.
 */
public class GraphRunner {

	private static final Logger log = LoggerFactory.getLogger(GraphRunner.class);

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
		return Flux.defer(() -> {
			try {
				GraphRunnerContext context = new GraphRunnerContext(initialState, config, compiledGraph);
				return processGraphExecution(context);
			}
			catch (Exception e) {
				return Flux.error(e);
			}
		});
	}

	public Optional<Object> resultValue() {
		return Optional.ofNullable(resultValue.get());
	}

	private Flux<GraphResponse<NodeOutput>> processGraphExecution(GraphRunnerContext context) {
		try {
			if (context.shouldStop() || context.isMaxIterationsReached()) {
				return handleCompletion(context);
			}

			final var returnFromEmbed = context.getReturnFromEmbedAndReset();
			// Is it a resume from embed flux, can from a subgraph or normal node.
			if (returnFromEmbed.isPresent()) {
				var interruption = returnFromEmbed.get().value(new TypeRef<InterruptionMetadata>() {
				});
				if (interruption.isPresent()) {
					return Flux.just(GraphResponse.done(interruption.get()));
				}
				return Flux.just(GraphResponse.done(context.buildCurrentNodeOutput()));
			}

			// TODO, duplicate interruption mechanism with handleInterruption() below?
			// possibly needs to be unified.
			if (context.getCurrentNodeId() != null && config.isInterrupted(context.getCurrentNodeId())) {
				config.withNodeResumed(context.getCurrentNodeId());
				return Flux.just(GraphResponse.done(GraphResponse.done(context.getCurrentState())));
			}

			if (context.isStartNode()) {
				return handleStartNode(context);
			}

			if (context.isEndNode()) {
				return handleEndNode(context);
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
				return handleInterruption(context);
			}

			// Execute current node
			return executeCurrentNode(context);
		}
		catch (Exception e) {
			context.doListeners(ERROR, e);
			log.error("Error during graph execution", e);
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleStartNode(GraphRunnerContext context) {
		try {
			context.doListeners(START, null);
			Command nextCommand = context.getEntryPoint();
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			Optional<Checkpoint> cp = context.addCheckpoint(START, context.getNextNodeId());
			NodeOutput output = context.buildOutput(START, cp);

			context.setCurrentNodeId(context.getNextNodeId());
			// 合并输出和后续流程，保持顺序，processGraphExecution用Flux.defer包裹
			return Flux.just(GraphResponse.of(output)).concatWith(Flux.defer(() -> processGraphExecution(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleEndNode(GraphRunnerContext context) {
		try {
			context.doListeners(END, null);
			NodeOutput output = context.buildNodeOutput(END);
			return Flux.just(GraphResponse.of(output)).concatWith(Flux.defer(() -> handleCompletion(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleCompletion(GraphRunnerContext context) {
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
			return Flux.just(GraphResponse.done(resultValue.get()));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleInterruption(GraphRunnerContext context) {
		try {
			InterruptionMetadata metadata = InterruptionMetadata
				.builder(context.getCurrentNodeId(), context.cloneState(context.getCurrentState()))
				.build();
			resultValue.set(metadata);
			return Flux.just(GraphResponse.done(metadata));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> executeCurrentNode(GraphRunnerContext context) {
		try {
			context.setCurrentNodeId(context.getNextNodeId());
			String currentNodeId = context.getCurrentNodeId();
			AsyncNodeActionWithConfig action = context.getNodeAction(currentNodeId);

			if (action == null) {
				return Flux.just(GraphResponse.error(RunnableErrors.missingNode.exception(currentNodeId)));
			}

			// Check for interruptable action
			if (action instanceof InterruptableAction) {
				Optional<InterruptionMetadata> interruptMetadata = ((InterruptableAction) action)
					.interrupt(currentNodeId, context.cloneState(context.getCurrentState()));
				if (interruptMetadata.isPresent()) {
					resultValue.set(interruptMetadata.get());
					return Flux.just(GraphResponse.done(interruptMetadata.get()));
				}
			}

			// Execute action
			context.doListeners(NODE_BEFORE, null);

			// Convert CompletableFuture to Mono for reactive processing
			CompletableFuture<Map<String, Object>> future = action.apply(context.getOverallState(), context.config);

			return Mono.fromFuture(future)
				.flatMapMany(updateState -> handleActionResult(context, updateState))
				.onErrorResume(error -> {
					context.doListeners(NODE_AFTER, null);
					return Flux.just(GraphResponse.error(error));
				});

		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleActionResult(GraphRunnerContext context,
			Map<String, Object> updateState) {
		try {
			context.doListeners(NODE_AFTER, null);

			// Check for embedded flux stream
			Optional<Flux<GraphResponse<NodeOutput>>> embedFlux = getEmbedFlux(context, updateState);
			if (embedFlux.isPresent()) {
				return handleEmbeddedFlux(context, embedFlux.get(), updateState);
			}

			// FIXME, remove this this deprecated embedded generator support
			Optional<AsyncGenerator<NodeOutput>> embedGenerator = getEmbedGenerator(updateState);
			if (embedGenerator.isPresent()) {
				return handleEmbeddedGenerator(context, embedGenerator.get(), updateState);
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
			return Flux.just(GraphResponse.of(output)).concatWith(Flux.defer(() -> processGraphExecution(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleEmbeddedFlux(GraphRunnerContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState) {

		AtomicReference<GraphResponse<NodeOutput>> lastData = new AtomicReference<>();

		Flux<GraphResponse<NodeOutput>> processedFlux = embedFlux.map(data -> {
			if (data.getOutput() != null) {
				var output = data.getOutput().join();
				output.setSubGraph(true);
				GraphResponse<NodeOutput> newData = GraphResponse.of(output);
				lastData.set(newData);
				return newData;
			}
			lastData.set(data);
			return data;
		});

		Mono<Void> updateContextMono = Mono.fromRunnable(() -> {
			var data = lastData.get();
			if (data == null)
				return;
			var nodeResultValue = data.resultValue;

			if (nodeResultValue instanceof InterruptionMetadata) {
				context.setReturnFromEmbedWithValue(nodeResultValue);
				return;
			}

			if (nodeResultValue != null) {
				if (nodeResultValue instanceof Map<?, ?>) {
					Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
						.stream()
						.filter(e -> !(e.getValue() instanceof Flux))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return processedFlux.concatWith(updateContextMono.thenMany(Flux.defer(() -> processGraphExecution(context))));
	}

	private Optional<Flux<GraphResponse<NodeOutput>>> getEmbedFlux(GraphRunnerContext context,
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
						.of(new StreamingOutput(newResponse.getResult().getOutput().getText(),
								context.getCurrentNodeId(), context.getOverallState()));
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
							// FIXME
							if (!resultMap.containsKey(e.getKey()) && resultMap.containsKey("messages")) {
								List<Object> messages = (List<Object>) resultMap.get("messages");
								Object lastMessage = messages.get(messages.size() - 1);
								if (lastMessage instanceof AssistantMessage lastAssistantMessage) {
									resultMap.put(e.getKey(), lastAssistantMessage.getText());
								}
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
	private Flux<GraphResponse<NodeOutput>> handleEmbeddedGenerator(GraphRunnerContext context,
			AsyncGenerator<NodeOutput> generator, Map<String, Object> partialState) {

		return Flux.<GraphResponse<NodeOutput>>create(sink -> {
			try {
				generator.stream().peek(output -> {
					if (output != null) {
						output.setSubGraph(true);
						sink.next(GraphResponse.of(output));
					}
				});

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
				sink.complete();
			}
			catch (Exception e) {
				sink.error(e);
			}
		}).concatWith(Flux.defer(() -> {
			try {
				// After embedded flux completes, continue with main flow
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());

				return processGraphExecution(context);
			}
			catch (Exception e) {
				return Flux.just(GraphResponse.error(e));
			}
		}));
	}

}
