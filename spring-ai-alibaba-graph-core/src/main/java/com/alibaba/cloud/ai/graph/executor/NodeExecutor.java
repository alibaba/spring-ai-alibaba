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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static com.alibaba.cloud.ai.graph.internal.node.ParallelNode.getExecutor;

/**
 * Node executor that processes node execution and result handling. This class
 * demonstrates inheritance by extending BaseGraphExecutor. It also demonstrates
 * polymorphism through its specific implementation of execute.
 */
public class NodeExecutor extends BaseGraphExecutor {

	private static final Logger log = LoggerFactory.getLogger(NodeExecutor.class);

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

			// Check for Flux
			Optional<Flux<GraphResponse<NodeOutput>>> embedFlux = getEmbedFlux(context, updateState);
			if (embedFlux.isPresent()) {
				return handleEmbeddedFlux(mainGraphExecutor, context, embedFlux.get(), updateState, resultValue);
			}

			// Check for ParallelGraphFlux (returned from ParallelNode)
			Optional<ParallelGraphFlux> embedParallelGraphFlux = getEmbedParallelGraphFlux(updateState);
			if (embedParallelGraphFlux.isPresent()) {
				return handleParallelGraphFlux(context, embedParallelGraphFlux.get(), updateState, resultValue);
			}

			// Check for GraphFlux (backward compatibility)
			Optional<GraphFlux<?>> embedGraphFlux = getEmbedGraphFlux(updateState,context);
			if (embedGraphFlux.isPresent()) {
				return handleGraphFlux(context, embedGraphFlux.get(), updateState, resultValue);
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
			NodeOutput output = context.buildNodeOutputAndAddCheckpoint(updateState);

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
	 * Transforms a raw Flux to Flux<GraphResponse<NodeOutput>> with embedded flux processing logic.
	 * This is the core transformation logic extracted from getEmbedFlux for reuse.
	 * @param context the graph runner context
	 * @param rawFlux the raw flux to transform
	 * @param key the key associated with the flux (for logging and completion result)
	 * @param nodeId the node ID to use for building streaming output
	 * @return Flux of GraphResponse with transformed elements
	 */
	private Flux<GraphResponse<NodeOutput>> transformFluxToGraphResponse(
			GraphRunnerContext context, Flux<?> rawFlux, String key, String nodeId) {
		var lastChatResponseRef = new AtomicReference<ChatResponse>(null);
		var lastGraphResponseRef = new AtomicReference<GraphResponse<NodeOutput>>(null);

		return rawFlux.filter(element -> {
				// skip ChatResponse.getResult() == null
				if (element instanceof ChatResponse response) {
					return response.getResult() != null &&  response.getResult().getOutput() != null;
				}
				// Don't filter out Exception/Throwable - we need to handle them
				return true;
			})
			.switchIfEmpty(Flux.error(new IllegalStateException(
				"Empty flux detected for key '" + key + "'. This may indicate an LLM API error with null result.")))
			.map(element -> {
				// Handle Exception/Throwable as data elements (not error signals)
				if (element instanceof Throwable throwable) {
					log.error("Exception emitted as data element in embedded Flux stream for key '{}': {}",
						key, throwable.getMessage(), throwable);
					GraphResponse<NodeOutput> errorResponse = GraphResponse.error(throwable);
					lastGraphResponseRef.set(errorResponse);
					return errorResponse;
				}
				if (element instanceof ChatResponse response) {
					ChatResponse lastResponse = lastChatResponseRef.get();
					final var currentMessage = response.getResult().getOutput();

					if (lastResponse == null) {
						lastChatResponseRef.set(response);
					} else {
						var lastMessageText = "";
						if (lastResponse.getResult().getOutput().getText() != null) {
							lastMessageText = lastResponse.getResult().getOutput().getText();
						}

						final var currentMessageText = currentMessage.getText();

						var newMessage = AssistantMessage.builder()
								.content(currentMessageText != null ? lastMessageText.concat(currentMessageText) : lastMessageText)
								.properties(currentMessage.getMetadata()) // TODO, reasoningContent in metadata is not aggregated
								.toolCalls(mergeToolCalls(lastResponse.getResult().getOutput().getToolCalls(),
										currentMessage.getToolCalls()))
								.media(currentMessage.getMedia())
								.build();

						var newGeneration = new Generation(newMessage,
								response.getResult().getMetadata());

						ChatResponse newResponse = new ChatResponse(
								List.of(newGeneration), response.getMetadata());
						lastChatResponseRef.set(newResponse);
					}
					GraphResponse<NodeOutput> lastGraphResponse = GraphResponse
						.of(context.buildStreamingOutput(response.getResult().getOutput(), response, nodeId, true));
					 lastGraphResponseRef.set(lastGraphResponse);
					return lastGraphResponse;
				}
				else if (element instanceof GraphResponse) {
					GraphResponse<NodeOutput> graphResponse = (GraphResponse<NodeOutput>) element;
					lastGraphResponseRef.set(graphResponse);
					return graphResponse;
				} else if (element instanceof NodeOutput nodeOutput) {
					GraphResponse<NodeOutput> graphResponse = GraphResponse.of(nodeOutput);
					lastGraphResponseRef.set(graphResponse);
					return graphResponse;
				}
				else {
					try {
						log.info("Received element of type '{}' in embedded Flux for key '{}', wrapping in StreamingOutput.",
							element.getClass().getName(), key);
						StreamingOutput<?> streamingOutput = context.buildStreamingOutput(element, nodeId, true);
						GraphResponse<NodeOutput> graphResponse = GraphResponse.of(streamingOutput);
						lastGraphResponseRef.set(graphResponse);
						return graphResponse;
					}
					catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			})
			.onErrorResume(error -> {
				// Handle actual error signals from the Flux
				log.error("Error signal occurred in embedded Flux stream for key '{}': {}",
					key, error.getMessage());
				GraphResponse<NodeOutput> errorResponse = GraphResponse.error(error);
				lastGraphResponseRef.set(errorResponse);
				return Flux.just(errorResponse);
			})
			.concatWith(Mono.defer(() -> {
				if (lastChatResponseRef.get() == null) {
					GraphResponse<?> lastGraphResponse = lastGraphResponseRef.get();
					if (lastGraphResponse != null && lastGraphResponse.resultValue().isPresent()) {
						Object result = lastGraphResponse.resultValue().get();

						// don't re-emit InterruptionMetadata, it will be handled by MainGraphExecutor
						if (result instanceof InterruptionMetadata) {
							return Mono.empty();
						}

						if (result instanceof Map resultMap) {
							if (!resultMap.containsKey(key) && resultMap.containsKey("messages")) {
								List<Object> messages = (List<Object>) resultMap.get("messages");
								Object lastMessage = messages.get(messages.size() - 1);
								if (lastMessage instanceof AssistantMessage lastAssistantMessage) {
									resultMap.put(key, lastAssistantMessage.getText());
								}
							}
						}
						return Mono.just(lastGraphResponseRef.get());
					}
					return Mono.empty();
				} else {
					return Mono.fromCallable(() -> {
						Map<String, Object> completionResult = new HashMap<>();
						completionResult.put(key, lastChatResponseRef.get().getResult().getOutput());
						if (!key.equals("messages")) {
							completionResult.put("messages", lastChatResponseRef.get().getResult().getOutput());
						}
						return GraphResponse.done(completionResult);
					});
				}
			}));
	}

  /**
   * Merges tool calls from two messages.
   * Tool calls with the same id will be merged.
   *
   * @return the merged list of tool calls
   */
  private List<ToolCall> mergeToolCalls(List<ToolCall> lastToolCalls, List<ToolCall> currentToolCalls) {

	  if (lastToolCalls == null || lastToolCalls.isEmpty()) {
		  return currentToolCalls != null ? currentToolCalls : List.of();
	  }
	  if (currentToolCalls == null || currentToolCalls.isEmpty()) {
		  return lastToolCalls;
	  }


	  Map<String, ToolCall> toolCallMap = new LinkedHashMap<>();

	  List<AssistantMessage.ToolCall> resultCalls = new ArrayList<>();
	  currentToolCalls.forEach(tc -> toolCallMap.put(tc.id(), tc));

	  // remove duplicate while keep order
	  lastToolCalls.forEach(tc->{
		  if( !toolCallMap.containsKey(tc.id()) ) {
			  resultCalls.add(tc);
		  }
	  });

	  resultCalls.addAll(currentToolCalls);

	  return resultCalls;
  }

	/**
	 * Processes a Flux<GraphResponse<NodeOutput>> with embedded flux handling logic.
	 * This is the core processing logic extracted from handleEmbeddedFlux for reuse.
	 * @param mainGraphExecutor the main graph executor
	 * @param context the graph runner context
	 * @param embedFlux the embedded flux to process
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with processed result
	 */
	private Flux<GraphResponse<NodeOutput>> processGraphResponseFlux(
			MainGraphExecutor mainGraphExecutor, GraphRunnerContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState,
			AtomicReference<Object> resultValue) {
		AtomicReference<GraphResponse<NodeOutput>> lastData = new AtomicReference<>();

		Flux<GraphResponse<NodeOutput>> processedFlux = embedFlux.map(data -> {
				if (data.getOutput() != null && !data.getOutput().isCompletedExceptionally()) {
					var output = data.getOutput().join();
					output.setSubGraph(true);
					GraphResponse<NodeOutput> newData = GraphResponse.of(output);
					lastData.set(newData);
					return newData;
				}
				lastData.set(data);
				return data;
			})
			// filter out InterruptionMetadata emitted directly by upstream to avoid duplicate sending
			// retain regular procedural events
			.filter(data -> {
				var value = data.resultValue();
				return value.isEmpty() || !(value.get() instanceof InterruptionMetadata);
			});

		Mono<Void> updateContextMono = Mono.fromRunnable(() -> {
			var data = lastData.get();
			if (data == null) {
				log.error("No data returned from last streaming node execution '{}', will goto END node directly.", context.getCurrentNodeId());
				context.setNextNodeId(END);
				context.doListeners(NODE_AFTER, null);
				return;
			}

			var nodeResultValue = data.resultValue();

			if (nodeResultValue.isPresent() && nodeResultValue.get() instanceof InterruptionMetadata) {
				context.setReturnFromEmbedWithValue(nodeResultValue.get());
				return;
			}

			Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof Flux) 
							&& !(e.getValue() instanceof GraphFlux)
							&& !(e.getValue() instanceof ParallelGraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			context.mergeIntoCurrentState(partialStateWithoutFlux);

			Map<String, Object> updateState = new HashMap<>();
			if (nodeResultValue.isPresent()) {
				Object value = nodeResultValue.get();
				if (value instanceof Map<?, ?>) {
					updateState = (Map<String, Object>) value;
					context.mergeIntoCurrentState(updateState);
				}
				else {
					throw new IllegalArgumentException("Node stream must return Map result using Data.done(),");
				}
			}

			try {
				Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentStateData());
				context.setNextNodeId(nextCommand.gotoNode());

				context.buildNodeOutputAndAddCheckpoint(updateState);

				context.doListeners(NODE_AFTER, null);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return processedFlux
			.concatWith(updateContextMono.thenMany(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue))));
	}

	/**
	 * Gets embed flux from partial state.
	 * @param context the graph runner context
	 * @param partialState the partial state containing flux instances
	 * @return an Optional containing Data with the flux if found, empty otherwise
	 */
	public Optional<Flux<GraphResponse<NodeOutput>>> getEmbedFlux(GraphRunnerContext context,
			Map<String, Object> partialState) {
		return partialState.entrySet().stream().filter(e -> e.getValue() instanceof Flux<?>).findFirst().map(e -> {
			var chatFlux = (Flux<?>) e.getValue();
			return transformFluxToGraphResponse(context, chatFlux, e.getKey(), context.getCurrentNodeId());
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
	public Flux<GraphResponse<NodeOutput>> handleEmbeddedFlux(MainGraphExecutor mainGraphExecutor, GraphRunnerContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState,
			AtomicReference<Object> resultValue) {
		return processGraphResponseFlux(mainGraphExecutor, context, embedFlux, partialState, resultValue);
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
	 * Handles GraphFlux processing with combined embedded flux transformation and processing.
	 * This method applies both getEmbedFlux transformation logic and handleEmbeddedFlux processing logic.
	 * @param context the graph runner context
	 * @param graphFlux the GraphFlux to handle
	 * @param partialState the partial state
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with GraphFlux handling result
	 */
	private Flux<GraphResponse<NodeOutput>> transformGraphFluxToFlux(GraphRunnerContext context,
			GraphFlux<?> graphFlux, Map<String, Object> partialState,
			AtomicReference<Object> resultValue) {
		// Use nodeId from GraphFlux instead of context to preserve real node identity
		String effectiveNodeId = graphFlux.getNodeId();
		String key = graphFlux.getKey() != null ? graphFlux.getKey() : "result";

		// Step 1: Apply getEmbedFlux transformation logic to graphFlux.getFlux()
		Flux<GraphResponse<NodeOutput>> transformedFlux = transformFluxToGraphResponse(
				context, graphFlux.getFlux(), key, effectiveNodeId);

		// Step 2: Apply handleEmbeddedFlux processing logic (directly implemented)

		return transformedFlux.map(data -> {
				if (data.getOutput() != null && !data.getOutput().isCompletedExceptionally()) {
					var output = data.getOutput().join();
					output.setSubGraph(true);
					GraphResponse<NodeOutput> newData = GraphResponse.of(output);
					resultValue.set(newData);
					return newData;
				}
				resultValue.set(data);
				return data;
			})
			// filter out InterruptionMetadata emitted directly by upstream to avoid duplicate sending
			// retain regular procedural events
			.filter(data -> {
				var value = data.resultValue();
				return value.isEmpty() || !(value.get() instanceof InterruptionMetadata);
			});
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
		Flux<GraphResponse<NodeOutput>> processedFlux = transformGraphFluxToFlux(context, graphFlux, partialState, lastDataRef);

		// Handle completion and result mapping
		Mono<Void> updateContextMono = Mono.fromRunnable(() -> {
			Object lastData = lastDataRef.get();

			if (lastData == null) {
				log.error("No data returned from last streaming node execution '{}', will goto END node directly.", context.getCurrentNodeId());
				context.setNextNodeId(END);
				return;
			}

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

				context.buildNodeOutputAndAddCheckpoint(partialStateWithoutGraphFlux);

				context.doListeners(NODE_AFTER, null);
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

		// Get executor from context, fallback to Schedulers.parallel() if not available
		// Note: DEFAULT_EXECUTOR from ParallelNode is private, so we use Schedulers.parallel() as fallback
		Executor executor = getExecutor(context.getConfig(), context.getCurrentNodeId());
		
		// Convert Executor to Scheduler for Reactor, use Schedulers.parallel() as fallback
		Scheduler scheduler = executor != null ? Schedulers.fromExecutor(executor) : Schedulers.parallel();

		// Create merged flux from all GraphFlux instances with preserved node IDs
		// Use subscribeOn(scheduler) to ensure each Flux executes in parallel on the scheduler
		List<Flux<GraphResponse<NodeOutput>>> fluxList = parallelGraphFlux.getGraphFluxes()
				.stream()
				.map(graphFlux -> {
					String nodeId = graphFlux.getNodeId();
					AtomicReference<Object> nodeDataRef = new AtomicReference<>();
					nodeDataRefs.put(nodeId, nodeDataRef);

					return transformGraphFluxToFlux(context, graphFlux, partialState, nodeDataRef)
							.subscribeOn(scheduler);
				}).collect(Collectors.toList());
		
		// Merge all parallel streams while preserving node identities
		// Each Flux is already subscribed on the scheduler, so they will execute in parallel
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

				context.buildNodeOutputAndAddCheckpoint(partialStateWithoutParallelGraphFlux);

				context.doListeners(NODE_AFTER, null);
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

		NodeOutput output = context.buildNodeOutputAndAddCheckpoint(partialState);
		// Recursively call the main execution handler
		return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue)));
	}
}
