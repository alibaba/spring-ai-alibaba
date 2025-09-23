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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.GraphRunnerContext;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
import static java.util.Objects.requireNonNull;

/**
 * Node executor that processes node execution and result handling. This class
 * demonstrates inheritance by extending BaseGraphExecutor. It also demonstrates
 * polymorphism through its specific implementation of execute.
 */
public class NodeExecutor extends BaseGraphExecutor {

	private final MainGraphExecutor mainGraphExecutor;

	public NodeExecutor(MainGraphExecutor mainGraphExecutor) {
		this.mainGraphExecutor = mainGraphExecutor;
	}

	/**
	 * Implementation of the execute method. This demonstrates polymorphism as it provides
	 * a specific implementation for node execution.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with execution result
	 */
	@Override
	public Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context, AtomicReference<Object> resultValue) {
		return executeNode(context, resultValue);
	}

	/**
	 * Executes a node and handles its result.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with node execution result
	 */
	private Flux<GraphResponse<NodeOutput>> executeNode(GraphRunnerContext context,
			AtomicReference<Object> resultValue) {
		try {
			context.setCurrentNodeId(context.getNextNodeId());
			String currentNodeId = context.getCurrentNodeId();
			AsyncNodeActionWithConfig action = context.getNodeAction(currentNodeId);

			if (action == null) {
				return Flux.just(GraphResponse.error(RunnableErrors.missingNode.exception(currentNodeId)));
			}

			if (action instanceof InterruptableAction) {
				Optional<InterruptionMetadata> interruptMetadata = ((InterruptableAction) action)
					.interrupt(currentNodeId, context.cloneState(context.getCurrentState()));
				if (interruptMetadata.isPresent()) {
					resultValue.set(interruptMetadata.get());
					return Flux.just(GraphResponse.done(interruptMetadata.get()));
				}
			}

			context.doListeners(NODE_BEFORE, null);

			CompletableFuture<Map<String, Object>> future = action.apply(context.getOverallState(),
					context.getConfig());

			return Mono.fromFuture(future)
				.flatMapMany(updateState -> handleActionResult(context, updateState, resultValue))
				.onErrorResume(error -> {
					context.doListeners(NODE_AFTER, null);
					return Flux.just(GraphResponse.error(error));
				});

		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	/**
	 * Handles the action result and returns appropriate response.
	 * @param context the graph runner context
	 * @param updateState the updated state from the action
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with action result handling
	 */
	private Flux<GraphResponse<NodeOutput>> handleActionResult(GraphRunnerContext context,
			Map<String, Object> updateState, AtomicReference<Object> resultValue) {
		try {
			context.doListeners(NODE_AFTER, null);

			Optional<Flux<GraphResponse<NodeOutput>>> embedFlux = getEmbedFlux(context, updateState);
			if (embedFlux.isPresent()) {
				return handleEmbeddedFlux(context, embedFlux.get(), updateState, resultValue);
			}

			Optional<AsyncGenerator<NodeOutput>> embedGenerator = getEmbedGenerator(updateState);
			if (embedGenerator.isPresent()) {
				return handleEmbeddedGenerator(context, embedGenerator.get(), updateState, resultValue);
			}

			context.updateCurrentState(updateState);
			context.getOverallState().updateState(updateState);

			if (context.getCompiledGraph().compileConfig.interruptBeforeEdge()
					&& context.getCompiledGraph().compileConfig.interruptsAfter()
						.contains(context.getCurrentNodeId())) {
				context.setNextNodeId(INTERRUPT_AFTER);
			}
			else {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());
			}

			NodeOutput output = context.buildCurrentNodeOutput();
			// Recursively call the main execution handler
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	/**
	 * Gets embed flux from partial state.
	 * @param context the graph runner context
	 * @param partialState the partial state containing flux instances
	 * @return an Optional containing Data with the flux if found, empty otherwise
	 */
	private Optional<Flux<GraphResponse<NodeOutput>>> getEmbedFlux(GraphRunnerContext context,
			Map<String, Object> partialState) {
		return partialState.entrySet().stream().filter(e -> e.getValue() instanceof Flux<?>).findFirst().map(e -> {
			var chatFlux = (Flux<?>) e.getValue();
			var lastChatResponseRef = new AtomicReference<org.springframework.ai.chat.model.ChatResponse>(null);
			var lastGraphResponseRef = new AtomicReference<GraphResponse<NodeOutput>>(null);

			return chatFlux.map(element -> {
				if (element instanceof org.springframework.ai.chat.model.ChatResponse response) {
					org.springframework.ai.chat.model.ChatResponse lastResponse = lastChatResponseRef.get();
					if (lastResponse == null) {
						GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
							.of(new StreamingOutput(response.getResult().getOutput().getText(), context.getCurrentNodeId(), context.getOverallState()));
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

					var newMessage = new org.springframework.ai.chat.messages.AssistantMessage(
							currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText,
							currentMessage.getMetadata(), currentMessage.getToolCalls(), currentMessage.getMedia());

					var newGeneration = new org.springframework.ai.chat.model.Generation(newMessage,
							response.getResult().getMetadata());

					org.springframework.ai.chat.model.ChatResponse newResponse = new org.springframework.ai.chat.model.ChatResponse(
							List.of(newGeneration), response.getMetadata());
					lastChatResponseRef.set(newResponse);
					GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
						.of(new StreamingOutput(response.getResult().getOutput().getText(), context.getCurrentNodeId(),
								context.getOverallState()));
					// lastGraphResponseRef.set(lastGraphResponse);
					return lastGraphResponse;
				}
				else if (element instanceof GraphResponse) {
					GraphResponse<NodeOutput> graphResponse = (GraphResponse<NodeOutput>) element;
					lastGraphResponseRef.set(graphResponse);
					return graphResponse;
				}
				else {
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
								List<Object> messages = (List<Object>) resultMap.get("messages");
								Object lastMessage = messages.get(messages.size() - 1);
								if (lastMessage instanceof org.springframework.ai.chat.messages.AssistantMessage lastAssistantMessage) {
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
	 * Handles embedded flux processing.
	 * @param context the graph runner context
	 * @param embedFlux the embedded flux to handle
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with embedded flux handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleEmbeddedFlux(GraphRunnerContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState,
			AtomicReference<Object> resultValue) {

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
			var nodeResultValue = data.resultValue();

			if (nodeResultValue.isPresent() && nodeResultValue.get() instanceof InterruptionMetadata) {
				context.setReturnFromEmbedWithValue(nodeResultValue.get());
				return;
			}

			if (nodeResultValue.isPresent()) {
				Object value = nodeResultValue.get();
				if (value instanceof Map<?, ?>) {
					Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
						.stream()
						.filter(e -> !(e.getValue() instanceof Flux))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

					Map<String, Object> intermediateState = OverAllState.updateState(context.getCurrentState(),
							partialStateWithoutFlux, context.getKeyStrategyMap());
					var currentState = OverAllState.updateState(intermediateState, (Map<String, Object>) value,
							context.getKeyStrategyMap());
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

		return processedFlux
			.concatWith(updateContextMono.thenMany(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue))));
	}

	/**
	 * Gets embed generator from partial state.
	 * @param partialState the partial state containing generator instances
	 * @return an Optional containing Data with the generator if found, empty otherwise
	 */
	private Optional<AsyncGenerator<NodeOutput>> getEmbedGenerator(Map<String, Object> partialState) {
		return partialState.entrySet()
			.stream()
			.filter(e -> e.getValue() instanceof AsyncGenerator)
			.findFirst()
			.map(generatorEntry -> (AsyncGenerator<NodeOutput>) generatorEntry.getValue());
	}

	/**
	 * Handles embedded generator processing.
	 * @param context the graph runner context
	 * @param generator the embedded generator to handle
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with embedded generator handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleEmbeddedGenerator(GraphRunnerContext context,
			AsyncGenerator<NodeOutput> generator, Map<String, Object> partialState,
			AtomicReference<Object> resultValue) {

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
				}
				sink.complete();
			}
			catch (Exception e) {
				sink.error(e);
			}
		}).concatWith(Flux.defer(() -> {
			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
				context.setNextNodeId(nextCommand.gotoNode());
				context.updateCurrentState(nextCommand.update());

				return mainGraphExecutor.execute(context, resultValue);
			}
			catch (Exception e) {
				return Flux.just(GraphResponse.error(e));
			}
		}));
	}

}
