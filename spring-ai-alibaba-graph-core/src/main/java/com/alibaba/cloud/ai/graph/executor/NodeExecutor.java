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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.*;
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
				context.getConfig().metadata(RunnableConfig.STATE_UPDATE_METADATA_KEY).ifPresent(updateFromFeedback -> {
					if (updateFromFeedback instanceof Map<?, ?>) {
						context.mergeIntoCurrentState((Map<String, Object>) updateFromFeedback);
					} else {
						throw new RuntimeException();
					}
				});
				Optional<InterruptionMetadata> interruptMetadata = ((InterruptableAction) action)
					.interrupt(currentNodeId, context.cloneState(context.getCurrentStateData()), context.getConfig());
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
						context.doListeners(ERROR, new Exception(error));
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
           // Priority 1: Check for GraphFlux (highest priority)
			Optional<GraphFlux<?>> embedGraphFlux = getEmbedGraphFlux(updateState,context);
			if (embedGraphFlux.isPresent()) {
				return handleGraphFlux(context, embedGraphFlux.get(), updateState, resultValue);
			}

			// Priority 2: Check for ParallelGraphFlux
			Optional<ParallelGraphFlux> embedParallelGraphFlux = getEmbedParallelGraphFlux(updateState);
			if (embedParallelGraphFlux.isPresent()) {
				return handleParallelGraphFlux(context, embedParallelGraphFlux.get(), updateState, resultValue);
			}

			// Priority 3: Check for traditional Flux (backward compatibility)
			Optional<Flux<GraphResponse<NodeOutput>>> embedFlux = getEmbedFlux(context, updateState);
			if (embedFlux.isPresent()) {
				return handleEmbeddedFlux(context, embedFlux.get(), updateState, resultValue);
			}

			context.mergeIntoCurrentState(updateState);

			if (context.getCompiledGraph().compileConfig.interruptBeforeEdge()
					&& context.getCompiledGraph().compileConfig.interruptsAfter()
						.contains(context.getCurrentNodeId())) {
				context.setNextNodeId(INTERRUPT_AFTER);
			}
			else {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
				context.setNextNodeId(nextCommand.gotoNode());
			}
			NodeOutput output = context.buildCurrentNodeOutput();

			context.doListeners(NODE_AFTER, null);
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
			var lastChatResponseRef = new AtomicReference<ChatResponse>(null);
			var lastGraphResponseRef = new AtomicReference<GraphResponse<NodeOutput>>(null);

            return chatFlux.filter(element -> {
                // skip ChatResponse.getResult() == null
                if (element instanceof ChatResponse response) {
                    return response.getResult() != null;
                }
                return true;
            }).map(element -> {
				if (element instanceof ChatResponse response) {
					ChatResponse lastResponse = lastChatResponseRef.get();
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
						Map<String, Object> completionResult = new HashMap<>();
						completionResult.put(e.getKey(), lastChatResponseRef.get().getResult().getOutput());
						if (!e.getKey().equals("messages")) {
							completionResult.put("messages", lastChatResponseRef.get().getResult().getOutput());
						}
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

			Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof Flux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			context.mergeIntoCurrentState(partialStateWithoutFlux);

			if (nodeResultValue.isPresent()) {
				Object value = nodeResultValue.get();
				if (value instanceof Map<?, ?>) {
					context.mergeIntoCurrentState((Map<String, Object>) value);
				}
				else {
					throw new IllegalArgumentException("Node stream must return Map result using Data.done(),");
				}
			}

			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
				context.setNextNodeId(nextCommand.gotoNode());

				// save checkpoint after embedded flux completes
				context.buildCurrentNodeOutput();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return processedFlux
			.concatWith(updateContextMono.thenMany(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue))));
	}

	/**
	 * Gets GraphFlux from partial state.
	 * @param partialState the partial state containing GraphFlux instances
	 * @return an Optional containing GraphFlux if found, empty otherwise
	 */
	private Optional<GraphFlux<?>> getEmbedGraphFlux(Map<String, Object> partialState, GraphRunnerContext context) {
		return partialState.entrySet()
				.stream()
				.filter(e -> e.getValue() instanceof GraphFlux)
				.findFirst()
				.map(e -> {
					GraphFlux<Object> graphFlux = (GraphFlux<Object>) e.getValue();
					return GraphFlux.of(StringUtils.hasText(graphFlux.getNodeId()) ? graphFlux.getNodeId() : context.getCurrentNodeId(),
							StringUtils.hasText(graphFlux.getKey()) ? graphFlux.getKey() : e.getKey(),
							graphFlux.getFlux(),
							graphFlux.getMapResult(),
							graphFlux.getChunkResult());
				});
	}

	/**
	 * Gets ParallelGraphFlux from partial state.
	 * @param partialState the partial state containing ParallelGraphFlux instances
	 * @return an Optional containing ParallelGraphFlux if found, empty otherwise
	 */
	private Optional<ParallelGraphFlux> getEmbedParallelGraphFlux(Map<String, Object> partialState) {
		return partialState.entrySet()
				.stream()
				.filter(e -> e.getValue() instanceof ParallelGraphFlux)
				.findFirst()
				.map(e -> (ParallelGraphFlux) e.getValue());
	}

	/**
	 * Handles GraphFlux processing with node ID preservation.
	 * @param context the graph runner context
	 * @param graphFlux the GraphFlux to handle
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with GraphFlux handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleGraphFlux(GraphRunnerContext context,
															GraphFlux<?> graphFlux, Map<String, Object> partialState,
															AtomicReference<Object> resultValue) {

		// Use nodeId from GraphFlux instead of context to preserve real node identity
		String effectiveNodeId = graphFlux.getNodeId();
		AtomicReference<Object> lastDataRef = new AtomicReference<>();

		// Process the GraphFlux stream with preserved node ID
		Flux<GraphResponse<NodeOutput>> processedFlux = graphFlux.getFlux()
				.map(element -> {
					if (graphFlux.hasMapResult()){
						lastDataRef.set(graphFlux.getMapResult().apply(element));
					}else {
						lastDataRef.set(element);
					}

					// Create StreamingOutput with GraphFlux's nodeId (preserves real node identity)
					StreamingOutput output = graphFlux.hasChunkResult() ?
							new StreamingOutput(graphFlux.getChunkResult().apply(element), element, effectiveNodeId, context.getOverallState()):
							new StreamingOutput(element, effectiveNodeId, context.getOverallState());
					output.setSubGraph(true);
					return GraphResponse.<NodeOutput>of(output);
				})
				.onErrorMap(error -> new RuntimeException("GraphFlux processing error in node: " + effectiveNodeId, error));

		// Handle completion and result mapping
		Mono<Void> updateContextMono = Mono.fromRunnable(() -> {
			Object lastData = lastDataRef.get();

			// Apply mapResult function if available
			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put(graphFlux.getKey(), lastData);

			// Merge non-GraphFlux state
			Map<String, Object> partialStateWithoutGraphFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof GraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			context.mergeIntoCurrentState(partialStateWithoutGraphFlux);

			// Merge the result from GraphFlux processing
			if (!resultMap.isEmpty()) {
				context.mergeIntoCurrentState(resultMap);
			}

			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
				context.setNextNodeId(nextCommand.gotoNode());

				// Save checkpoint after GraphFlux completes
				context.buildCurrentNodeOutput();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return processedFlux
				.concatWith(updateContextMono.thenMany(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue))));
	}

	/**
	 * Handles ParallelGraphFlux processing with node ID preservation for all parallel streams.
	 * @param context the graph runner context
	 * @param parallelGraphFlux the ParallelGraphFlux to handle
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with ParallelGraphFlux handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleParallelGraphFlux(GraphRunnerContext context,
																	ParallelGraphFlux parallelGraphFlux, Map<String, Object> partialState,
																	AtomicReference<Object> resultValue) throws Exception {

		if (parallelGraphFlux.isEmpty()) {
			// Handle empty ParallelGraphFlux
			return handleNonStreamingResult(context, partialState, resultValue);
		}

		Map<String, AtomicReference<Object>> nodeDataRefs = new HashMap<>();

		// Create merged flux from all GraphFlux instances with preserved node IDs
		List<Flux<GraphResponse<NodeOutput>>> fluxList = parallelGraphFlux.getGraphFluxes()
				.stream()
				.map(graphFlux -> {
					String nodeId = graphFlux.getNodeId();
					AtomicReference<Object> nodeDataRef = new AtomicReference<>();
					nodeDataRefs.put(nodeId, nodeDataRef);

					return graphFlux.getFlux()
							.map(element -> {
								nodeDataRef.set(graphFlux.getMapResult().apply(element));
								// Create StreamingOutput with specific nodeId (preserves parallel node identity)
								StreamingOutput output = graphFlux.hasChunkResult() ?
										new StreamingOutput(graphFlux.getChunkResult().apply(element), element, nodeId, context.getOverallState()):
										new StreamingOutput(element, nodeId, context.getOverallState());
								output.setSubGraph(true);
								return GraphResponse.<NodeOutput>of(output);
							})
							.onErrorMap(error -> new RuntimeException("ParallelGraphFlux processing error in node: " + nodeId, error));
				})
				.collect(Collectors.toList());

		// Merge all parallel streams while preserving node identities
		Flux<GraphResponse<NodeOutput>> mergedFlux = Flux.merge(fluxList);

		// Handle completion and result mapping for all nodes
		Mono<Void> updateContextMono = Mono.fromRunnable(() -> {
			Map<String, Object> combinedResultMap = new HashMap<>();

			// Process results from each GraphFlux with node-specific prefixes
			for (GraphFlux<?> graphFlux : parallelGraphFlux.getGraphFluxes()) {
				String nodeId = graphFlux.getNodeId();
				Object nodeData = nodeDataRefs.get(nodeId).get();

				combinedResultMap.put(graphFlux.getKey(),nodeData);
			}

			// Merge non-ParallelGraphFlux state
			Map<String, Object> partialStateWithoutParallelGraphFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof ParallelGraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			context.mergeIntoCurrentState(partialStateWithoutParallelGraphFlux);

			// Merge the combined results from ParallelGraphFlux processing
			if (!combinedResultMap.isEmpty()) {
				context.mergeIntoCurrentState(combinedResultMap);
			}

			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
				context.setNextNodeId(nextCommand.gotoNode());

				// Save checkpoint after ParallelGraphFlux completes
				context.buildCurrentNodeOutput();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return mergedFlux
				.concatWith(updateContextMono.thenMany(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue))));
	}

	/**
	 * Handles non-streaming result processing.
	 * @param context the graph runner context
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with non-streaming result
	 */
	private Flux<GraphResponse<NodeOutput>> handleNonStreamingResult(GraphRunnerContext context,
																	 Map<String, Object> partialState, AtomicReference<Object> resultValue) throws Exception {
		if (context.getCompiledGraph().compileConfig.interruptBeforeEdge()
				&& context.getCompiledGraph().compileConfig.interruptsAfter()
				.contains(context.getCurrentNodeId())) {
			context.setNextNodeId(INTERRUPT_AFTER);
		}
		else {
			Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
			context.setNextNodeId(nextCommand.gotoNode());
		}

		NodeOutput output = context.buildCurrentNodeOutput();
		// Recursively call the main execution handler
		return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue)));
	}
}
