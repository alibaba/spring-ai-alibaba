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

package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.GraphRunnerContext;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.NodeAggregationStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.NodePause;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.ChatResponseStreamAggregator;
import com.alibaba.cloud.ai.graph.utils.FinishReasonMetadata;
import com.alibaba.cloud.ai.graph.utils.LifeListenerUtil;
import com.alibaba.cloud.ai.graph.utils.SerializationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;
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

			var config = new ParallelStepJournal(context).restoreDispatch()
					.map(plan -> RunnableConfig.builder(context.getConfig())
							.addMetadata(ParallelNode.RESTORED_DISPATCH_KEY, plan).build())
					.orElseGet(context::getConfig);
			return NodeExecution.run(context, currentNodeId, context.branchTaskId(0, currentNodeId), action,
							context.getOverallState(), config)
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
			if (updateState.get(NodePause.RESULT_KEY) instanceof NodePause pause) {
				var writes = new LinkedHashMap<>(updateState);
				writes.remove(NodePause.RESULT_KEY);
				context.mergeIntoCurrentState(writes);
				context.checkpointPausedNode();
				context.doListeners(NODE_AFTER, null);
				resultValue.set(pause.interruption());
				return Flux.just(GraphResponse.done(pause.interruption()));
			}
			if (updateState.get(ParallelNode.EXECUTION_KEY) instanceof ParallelNode.Execution execution) {
				return executeParallel(context, execution, resultValue);
			}

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

			var interruption = interruptAfter(context, updateState);
			context.mergeIntoCurrentState(updateState);
			return checkpointAfterTransition(context, updateState, interruption.isEmpty())
					.flatMapMany(output -> {
						if (interruption.isPresent()) {
							resultValue.set(interruption.get());
							return Flux.just(GraphResponse.done(interruption.get()));
						}
						return Flux.just(GraphResponse.of(output),
								GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue)));
					});
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Mono<NodeOutput> checkpointAfterTransition(GraphRunnerContext context, Map<String, Object> update,
			boolean allowDeferredEdge) {
		String nodeId = context.getCurrentNodeId();
		var config = context.getCompiledGraph().compileConfig;
		Mono<String> next = allowDeferredEdge && config.interruptBeforeEdge() && config.interruptsAfter().contains(nodeId)
				? Mono.just(INTERRUPT_AFTER)
				: context.resolveNextNode(nodeId, context.getCurrentStateData()).map(Command::gotoNode);
		return next.flatMap(target -> Mono.fromCallable(() -> {
			context.setNextNodeId(target);
			NodeOutput output = context.buildNodeOutputAndAddCheckpoint(update);
			context.doListeners(NODE_AFTER, null);
			return output;
		}));
	}

	private Flux<GraphResponse<NodeOutput>> executeParallel(GraphRunnerContext context, ParallelNode.Execution execution,
			AtomicReference<Object> resultValue) {
		var journal = new ParallelStepJournal(context);
		journal.begin(execution);
		if (execution.strategy() == NodeAggregationStrategy.ANY_OF) {
			var restored = journal.restoreWinningWrites();
			if (restored.isPresent()) {
				return finishParallel(context, execution.routingUpdate(),
						List.of(new BranchResult(restored.get(), null, false)), resultValue);
			}
		}
		var interruptionsByTask = journal.restoreInterruptions();
		var feedback = context.getConfig().metadata(HUMAN_FEEDBACK_METADATA_KEY)
				.filter(InterruptionMetadata.class::isInstance).map(InterruptionMetadata.class::cast).orElse(null);
		String selectedTask = selectInterruptedTask(interruptionsByTask, feedback);
		var feedbackStateUpdate = context.getConfig().metadata(RunnableConfig.STATE_UPDATE_METADATA_KEY);
		if (selectedTask != null) {
			context.clearHumanFeedback();
		}
		var branchInput = context.getOverallState().snapShot().orElseThrow();
		branchInput.updateState(execution.routingUpdate());
		var branches = execution.branches();
		if (branches.isEmpty()) {
			return finishParallel(context, execution.routingUpdate(), List.of(), resultValue);
		}

		var scheduler = Schedulers.fromExecutor(getExecutor(context.getConfig(), context.getCurrentNodeId()));
		int concurrency = Math.min(execution.maxConcurrency(), branches.size());
		if (execution.strategy() == NodeAggregationStrategy.ANY_OF) {
			var failures = new ConcurrentLinkedQueue<Throwable>();
			var interruptions = new ConcurrentSkipListMap<Integer, BranchCandidate>();
			return Flux.range(0, branches.size())
				.flatMap(index -> {
					var branch = branches.get(index);
					String taskId = context.branchTaskId(index, branch.nodeId());
					var previousPause = interruptionsByTask.get(taskId);
					if (previousPause != null && !taskId.equals(selectedTask)) {
						interruptions.put(index,
								new BranchCandidate(new BranchResult(List.of(), previousPause, false), List.of()));
						return Mono.empty();
					}
					var branchFeedback = taskId.equals(selectedTask) ? feedback : null;
					var result = new AtomicReference<BranchResult>();
					return executeBranch(context, branchInput, branch, index, branchFeedback, feedbackStateUpdate,
									journal.restorePausedWrites(taskId), result::set)
						.subscribeOn(scheduler)
						.collectList()
						.flatMap(events -> {
							var candidate = new BranchCandidate(result.get(), events);
							if (candidate.result().interruption() != null) {
								journal.saveInterruption(taskId, candidate.result().interruption(), candidate.result().updates());
								interruptions.put(index, candidate);
								return Mono.empty();
							}
							return Mono.just(candidate);
						})
						.onErrorResume(error -> {
							failures.add(error);
							return Mono.empty();
						});
				}, concurrency)
				.next()
				.flatMapMany(winner -> {
					journal.saveWinningWrites(winner.result().updates());
					return Flux.fromIterable(winner.events())
					.concatWith(Flux.defer(() -> finishParallel(context, execution.routingUpdate(),
							List.of(winner.result()), resultValue)));
				})
				.switchIfEmpty(Flux.defer(() -> {
					if (interruptions.isEmpty()) {
						return Flux.error(Exceptions.multiple(failures));
					}
					return Flux.fromIterable(interruptions.values()).concatMap(candidate -> Flux.fromIterable(candidate.events()))
							.concatWith(Flux.defer(() -> finishParallel(context, execution.routingUpdate(),
									interruptions.values().stream().map(BranchCandidate::result).toList(), resultValue)));
				}));
		}

		var results = new BranchResult[branches.size()];
		return Flux.range(0, branches.size())
			.flatMapDelayError(index -> {
				var branch = branches.get(index);
				String taskId = context.branchTaskId(index, branch.nodeId());
				var completed = journal.restoreWrites(taskId);
				if (completed.isPresent()) {
					results[index] = new BranchResult(completed.get(), null, false);
					return Flux.empty();
				}
				if (interruptionsByTask.containsKey(taskId) && !taskId.equals(selectedTask)) {
					results[index] = new BranchResult(List.of(), interruptionsByTask.get(taskId), false);
					return Flux.empty();
				}
				var branchFeedback = taskId.equals(selectedTask) ? feedback : null;
				return executeBranch(context, branchInput, branch, index, branchFeedback, feedbackStateUpdate,
						journal.restorePausedWrites(taskId), result -> {
					if (result.interruption() == null) {
						journal.saveWrites(taskId, result.updates());
					}
					else {
						journal.saveInterruption(taskId, result.interruption(), result.updates());
					}
					results[index] = result;
				}).subscribeOn(scheduler);
			}, concurrency, Queues.XS_BUFFER_SIZE)
			.concatWith(Flux.defer(() -> finishParallel(context, execution.routingUpdate(), List.of(results), resultValue)));
	}

	private static String selectInterruptedTask(Map<String, InterruptionMetadata> paused,
			InterruptionMetadata feedback) {
		if (feedback == null || paused.isEmpty()) {
			return null;
		}
		if (feedback.interruptionId().isEmpty()) {
			if (paused.size() == 1 && paused.values().iterator().next().pendingInterruptions().size() <= 1) {
				return paused.keySet().iterator().next();
			}
			throw new IllegalArgumentException("Feedback for a parallel step with multiple pauses requires an interruption ID");
		}
		String id = feedback.interruptionId().orElseThrow();
		return paused.entrySet().stream().filter(entry -> hasInterruptionId(entry.getValue(), id))
				.map(Map.Entry::getKey).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No pending interruption with ID '" + id + "'"));
	}

	private static boolean hasInterruptionId(InterruptionMetadata interruption, String id) {
		return interruption.interruptionId().filter(id::equals).isPresent()
				|| interruption.pendingInterruptions().stream().anyMatch(child -> hasInterruptionId(child, id));
	}

	private Flux<GraphResponse<NodeOutput>> executeBranch(GraphRunnerContext context, OverAllState input,
			ParallelNode.Branch branch, int index, InterruptionMetadata feedback, Optional<Object> feedbackStateUpdate,
			List<Map<String, Object>> priorWrites, Consumer<BranchResult> completed) {
		return Flux.defer(() -> {
			var state = input.snapShot().orElseThrow();
			if (!branch.input().isEmpty()) {
				state.updateStateWithKeyStrategies(SerializationUtils.deepCopyMap(branch.input()), Map.of());
			}
			priorWrites.forEach(state::updateState);
			String taskId = context.branchTaskId(index, branch.nodeId());
			var builder = RunnableConfig.builder(context.getConfig())
					.addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, taskId)
					.removeMetadata(HUMAN_FEEDBACK_METADATA_KEY)
					.removeMetadata(RunnableConfig.STATE_UPDATE_METADATA_KEY);
			if (feedback != null) {
				builder.addHumanFeedback(feedback);
				feedbackStateUpdate.ifPresent(update -> builder.addMetadata(RunnableConfig.STATE_UPDATE_METADATA_KEY, update));
				if (branch.action() instanceof ResumableSubGraphAction action) {
					builder.addMetadata(action.getResumeSubGraphId(), true);
				}
			}
			var config = builder.build();
			Consumer<BranchResult> withInterruptionId = result -> {
				var pause = result.interruption();
				if (pause != null && pause.interruptionId().isEmpty()) {
					pause = InterruptionMetadata.builder(pause).toolFeedbacks(pause.toolFeedbacks())
							.interruptionId(UUID.randomUUID().toString()).build();
				}
				var writes = new ArrayList<>(priorWrites);
				writes.addAll(result.updates());
				completed.accept(new BranchResult(writes, pause, result.streamed()));
			};
			if (branch.action() instanceof InterruptableAction action) {
				var pause = action.interrupt(branch.nodeId(), state.snapShot().orElseThrow(), config);
				if (pause.isPresent()) {
					withInterruptionId.accept(new BranchResult(List.of(), pause.get(), false));
					return Flux.empty();
				}
			}
			notifyBranchListener(context, branch.nodeId(), state, config, NODE_BEFORE);
			return NodeExecution.run(context, branch.nodeId(), taskId, branch.action(), state, config)
				.flatMapMany(update -> streamBranchResult(context, branch.nodeId(), update, withInterruptionId))
				.doOnNext(event -> {
					if (!event.hasMetadata("taskId")) {
						event.addMetadata("taskId", taskId);
					}
				})
				.doOnTerminate(() -> notifyBranchListener(context, branch.nodeId(), state, config, NODE_AFTER))
				.doOnCancel(() -> notifyBranchListener(context, branch.nodeId(), state, config, NODE_AFTER));
		});
	}

	private static void notifyBranchListener(GraphRunnerContext context, String nodeId, OverAllState state,
			RunnableConfig config, String event) {
		LifeListenerUtil.processListenersLIFO(nodeId,
				new LinkedBlockingDeque<>(context.getCompiledGraph().compileConfig.lifecycleListeners()),
				state.data(), config, event, null);
	}

	private Flux<GraphResponse<NodeOutput>> streamBranchResult(GraphRunnerContext context, String nodeId,
			Map<String, Object> update, Consumer<BranchResult> completed) {
		if (update.get(NodePause.RESULT_KEY) instanceof NodePause pause) {
			var writes = new LinkedHashMap<>(update);
			writes.remove(NodePause.RESULT_KEY);
			completed.accept(new BranchResult(writes.isEmpty() ? List.of() : List.of(writes), pause.interruption(), false));
			return Flux.empty();
		}
		Map<String, Object> stateUpdate = new LinkedHashMap<>();
		List<GraphFlux<?>> streams = new ArrayList<>();
		update.forEach((key, value) -> {
			if (value instanceof GraphFlux<?> stream) {
				streams.add(stream.withIdentity(StringUtils.hasText(stream.getNodeId()) ? stream.getNodeId() : nodeId,
						StringUtils.hasText(stream.getKey()) ? stream.getKey() : key));
			}
			else if (value instanceof ParallelGraphFlux parallel) {
				streams.addAll(parallel.getGraphFluxes());
			}
			else if (value instanceof Flux<?> stream) {
				streams.add(GraphFlux.of(nodeId, key, stream));
			}
			else {
				stateUpdate.put(key, value);
			}
		});

		List<Map<String, Object>> updates = new ArrayList<>();
		updates.add(stateUpdate);
		var interruption = new AtomicReference<InterruptionMetadata>();
		return Flux.fromIterable(streams)
			.concatMap(stream -> {
				var last = new AtomicReference<Object>();
				return transformGraphFluxToFlux(context, stream, last)
					.concatMap(event -> {
						if (event.isError()) {
							return Mono.fromFuture(event.getOutput()).then(Mono.<GraphResponse<NodeOutput>>empty());
						}
						return event.isDone() ? Mono.empty() : Mono.just(event);
					})
					.doOnComplete(() -> {
						if (last.get() instanceof GraphResponse<?> response
								&& response.resultValue().orElse(null) instanceof InterruptionMetadata paused) {
							interruption.compareAndSet(null, paused);
						}
						else if (last.get() instanceof GraphResponse<?> response && response.isDone()
								&& response.hasMetadata(GraphRunnerContext.BRANCH_WRITES)) {
							updates.addAll(CheckpointWrite.decodeAll(response.getMetadata(GraphRunnerContext.BRANCH_WRITES)));
						}
						else {
							updates.add(graphFluxResultState(stream, last.get()));
						}
					});
			})
			.doOnComplete(() -> {
				var paused = interruption.get();
				completed.accept(new BranchResult(paused == null ? updates : List.of(), paused, !streams.isEmpty()));
			});
	}

	private Flux<GraphResponse<NodeOutput>> finishParallel(GraphRunnerContext context, Map<String, Object> routingUpdate,
			List<BranchResult> results, AtomicReference<Object> resultValue) {
		var interruptions = results.stream().map(BranchResult::interruption).filter(Objects::nonNull)
				.flatMap(pause -> pause.pendingInterruptions().isEmpty()
						? Stream.of(pause) : pause.pendingInterruptions().stream())
				.toList();
		if (!interruptions.isEmpty()) {
			var first = interruptions.get(0);
			var pause = interruptions.size() == 1 ? first : InterruptionMetadata.builder(first)
					.toolFeedbacks(first.toolFeedbacks()).pendingInterruptions(interruptions).build();
			resultValue.set(pause);
			return Flux.just(GraphResponse.done(pause));
		}

		var previousState = context.getOverallState();
		String previousNextNode = context.getNextNodeId();
		try {
			var outputUpdate = new LinkedHashMap<>(routingUpdate);
			context.setOverallState(previousState.snapShot().orElseThrow());
			context.mergeIntoCurrentState(routingUpdate);
			for (BranchResult result : results) {
				for (Map<String, Object> update : result.updates()) {
					context.mergeIntoCurrentState(update);
					outputUpdate.putAll(update);
				}
			}
			return checkpointAfterTransition(context, outputUpdate, true)
					.doOnError(error -> {
						context.setOverallState(previousState);
						context.setNextNodeId(previousNextNode);
					})
					.flatMapMany(output -> {
						GraphResponse<NodeOutput> continuation =
								GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue));
						return results.stream().anyMatch(BranchResult::streamed)
								? Flux.just(continuation) : Flux.just(GraphResponse.of(output), continuation);
					});
		}
		catch (Exception ex) {
			context.setOverallState(previousState);
			context.setNextNodeId(previousNextNode);
			return Flux.error(ex);
		}
	}

	private record BranchResult(List<Map<String, Object>> updates, InterruptionMetadata interruption, boolean streamed) {
	}

	private record BranchCandidate(BranchResult result, List<GraphResponse<NodeOutput>> events) {
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
		var upstreamAlreadyEmittedCompletion = new AtomicBoolean(false);

		return rawFlux.publish(shared -> {
			Flux<GraphResponse<NodeOutput>> events = streamEvents(context, shared, key, nodeId,
					lastGraphResponseRef, upstreamAlreadyEmittedCompletion);
			var sourceError = new AtomicReference<Throwable>();
			Flux<ChatResponse> responses = shared.ofType(ChatResponse.class).doOnError(sourceError::set);
			Mono<GraphResponse<NodeOutput>> aggregation = ChatResponseStreamAggregator
					.aggregate(responses, context.getConfig().mergeReasoningContent())
					.onErrorComplete(error -> error == sourceError.get())
					.doOnNext(lastChatResponseRef::set).then(Mono.empty());
			// Deliver queued chunks before the terminal error. The event stream reports
			// source errors so the aggregation subscriber does not report them twice.
			return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, events, aggregation);
		})
			.onErrorResume(error -> {
				// Handle actual error signals from the Flux
				log.error("Error signal occurred in embedded Flux stream for key '{}': {}",
					key, error.getMessage());
				context.doListeners(ERROR, nodeId, new Exception(error));
				GraphResponse<NodeOutput> errorResponse = GraphResponse.error(error);
				lastGraphResponseRef.set(errorResponse);
				return Flux.just(errorResponse);
			})
			.concatWith(Flux.defer(() -> {
				if (upstreamAlreadyEmittedCompletion.get()) {
					return Flux.empty();
				}
				if (lastChatResponseRef.get() == null) {
					GraphResponse<NodeOutput> lastGraphResponse = lastGraphResponseRef.get();
					if (lastGraphResponse != null && lastGraphResponse.resultValue().isPresent()) {
						Object result = lastGraphResponse.resultValue().get();

						// don't re-emit InterruptionMetadata, it will be handled by MainGraphExecutor
						if (result instanceof InterruptionMetadata) {
							return Flux.empty();
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
						return Flux.just(lastGraphResponse);
					}
					return Flux.empty();
				} else {
					ChatResponse lastChatResponse = lastChatResponseRef.get();
					// FINISHED StreamingOutput omits message for agent/graph LLM nodes (tool/hook unchanged); full text still in done(state).
					Generation generation = lastChatResponse.getResult();
					AssistantMessage answer = FinishReasonMetadata.apply(generation.getOutput(), generation.getMetadata());
					Message messageForCompletion = answer;
					if (shouldOmitMessageOnStreamCompletion(nodeId)) {
						messageForCompletion = null;
					}
					GraphResponse<NodeOutput> aggregatedResponse = GraphResponse
						.of(context.buildStreamingOutput(messageForCompletion, lastChatResponse, nodeId, false));
					// Then emit the completion response
					Map<String, Object> completionResult = new HashMap<>();
					completionResult.put(key, answer);
					if (!key.equals("messages")) {
						completionResult.put("messages", answer);
					}
					GraphResponse<NodeOutput> doneResponse = GraphResponse.done(completionResult);
					return Flux.just(aggregatedResponse, doneResponse);
				}
			}));
	}

	private Flux<GraphResponse<NodeOutput>> streamEvents(GraphRunnerContext context, Flux<?> source, String key,
			String nodeId, AtomicReference<GraphResponse<NodeOutput>> lastGraphResponse,
			AtomicBoolean upstreamAlreadyEmittedCompletion) {
		return source.switchIfEmpty(Flux.error(new IllegalStateException(
				"Empty flux detected for key '" + key + "'. This may indicate an LLM API error with null result.")))
				.map(element -> {
					GraphResponse<NodeOutput> event;
					if (element instanceof Throwable throwable) {
						log.error("Exception emitted as data element in embedded Flux for key '{}'", key, throwable);
						event = GraphResponse.error(throwable);
					}
					else if (element instanceof ChatResponse response) {
						Generation generation = response.getResult();
						event = GraphResponse.of(generation == null || generation.getOutput() == null
								? context.buildStreamingOutput(response, nodeId, true)
								: context.buildStreamingOutput(generation.getOutput(), response, nodeId, true));
					}
					else if (element instanceof GraphResponse<?> graphResponse) {
						event = (GraphResponse<NodeOutput>) graphResponse;
						if (event.isDone() || event.resultValue().isPresent()) {
							upstreamAlreadyEmittedCompletion.set(true);
						}
					}
					else if (element instanceof NodeOutput nodeOutput) {
						event = GraphResponse.of(nodeOutput);
					}
					else {
						event = GraphResponse.of(context.buildStreamingOutput(element, nodeId, true));
					}
					lastGraphResponse.set(event);
					return event;
				});
	}

	/**
	 * Whether the FINISHED {@link StreamingOutput} after a {@link ChatResponse} flux should omit
	 * {@link Message} text (agent model and ordinary graph LLM nodes). Tool/hook streams keep the message.
	 */
	private static boolean shouldOmitMessageOnStreamCompletion(String nodeId) {
		return nodeId.startsWith(RunnableConfig.AGENT_MODEL_NAME)
				|| (!nodeId.startsWith(RunnableConfig.AGENT_TOOL_NAME)
						&& !nodeId.startsWith(RunnableConfig.AGENT_HOOK_NAME_PREFIX));
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

		Flux<GraphResponse<NodeOutput>> processedFlux = embedFlux.concatMap(this::markSubGraphOutput)
			.doOnNext(lastData::set)
			// filter out InterruptionMetadata emitted directly by upstream to avoid duplicate sending
			// retain regular procedural events
			.filter(data -> {
				var value = data.resultValue();
				return value.isEmpty() || !(value.get() instanceof InterruptionMetadata);
			});

		Mono<Void> updateContextMono = Mono.defer(() -> {
			var data = lastData.get();
			if (data == null) {
				log.error("No data returned from last streaming node execution '{}', will goto END node directly.", context.getCurrentNodeId());
				context.setNextNodeId(END);
				context.doListeners(NODE_AFTER, null);
				return Mono.empty();
			}

			var nodeResultValue = data.resultValue();

			if (nodeResultValue.isPresent() && nodeResultValue.get() instanceof InterruptionMetadata) {
				context.setReturnFromEmbedWithValue(nodeResultValue.get());
				return Mono.empty();
			}

			Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof Flux) 
							&& !(e.getValue() instanceof GraphFlux)
							&& !(e.getValue() instanceof ParallelGraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			Map<String, Object> updateState = new HashMap<>();
			if (nodeResultValue.isPresent()) {
				Object value = nodeResultValue.get();
				if (value instanceof Map<?, ?>) {
					updateState = (Map<String, Object>) value;
				}
				else if (value instanceof BaseCheckpointSaver.Tag) {
					// When releaseThread=true, completion may return a Tag.
					// Tag is a checkpoint-release receipt, not a state update, so skip merge here.
				}
				else {
					throw new IllegalArgumentException("Node stream must return Map result using Data.done(),");
				}
			}

			List<Map<String, Object>> writes = data.hasMetadata(GraphRunnerContext.BRANCH_WRITES)
					? CheckpointWrite.decodeAll(data.getMetadata(GraphRunnerContext.BRANCH_WRITES))
					: List.of(updateState);
			Map<String, Object> combinedUpdateState = new HashMap<>(partialStateWithoutFlux);
			writes.forEach(combinedUpdateState::putAll);
			Optional<InterruptionMetadata> interruptAfterMetadata = interruptAfter(context, combinedUpdateState);

			context.mergeIntoCurrentState(partialStateWithoutFlux);
			writes.forEach(context::mergeIntoCurrentState);

			return finishStreamingStep(context, combinedUpdateState, interruptAfterMetadata);
		});

		return processedFlux
			.concatWith(updateContextMono.thenMany(Flux
				.just(GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue)))));
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
			GraphFlux<?> graphFlux, AtomicReference<Object> resultValue) {
		// Use nodeId from GraphFlux instead of context to preserve real node identity
		String effectiveNodeId = graphFlux.getNodeId();
		String key = graphFlux.getKey() != null ? graphFlux.getKey() : "result";

		// Step 1: Apply getEmbedFlux transformation logic to graphFlux.getFlux()
		Flux<GraphResponse<NodeOutput>> transformedFlux = transformFluxToGraphResponse(
				context, graphFlux.getFlux(), key, effectiveNodeId);

		// Step 2: Apply handleEmbeddedFlux processing logic (directly implemented)

		return transformedFlux.concatMap(this::markSubGraphOutput)
			.doOnNext(resultValue::set)
			// filter out InterruptionMetadata emitted directly by upstream to avoid duplicate sending
			// retain regular procedural events
			.filter(data -> {
				var value = data.resultValue();
				return value.isEmpty() || !(value.get() instanceof InterruptionMetadata);
			});
	}

	private Mono<GraphResponse<NodeOutput>> markSubGraphOutput(GraphResponse<NodeOutput> response) {
		if (response.getOutput() == null || response.isError()) {
			return Mono.just(response);
		}
		return Mono.fromFuture(response.getOutput()).map(output -> {
			output.setSubGraph(true);
			return GraphResponse.of(output, response.getAllMetadata());
		});
	}

	private Mono<Void> finishStreamingStep(GraphRunnerContext context, Map<String, Object> update,
			Optional<InterruptionMetadata> interruption) {
		return checkpointAfterTransition(context, update, interruption.isEmpty())
				.doOnNext(output -> interruption.ifPresent(context::setReturnFromEmbedWithValue))
				.then();
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

		AtomicReference<Object> lastDataRef = new AtomicReference<>();

		// Process the GraphFlux stream with preserved node ID
		Flux<GraphResponse<NodeOutput>> processedFlux = transformGraphFluxToFlux(context, graphFlux, lastDataRef);

		// Handle completion and result mapping
		Mono<Void> updateContextMono = Mono.defer(() -> {
			Object lastData = lastDataRef.get();

			if (lastData == null) {
				log.error("No data returned from last streaming node execution '{}', will goto END node directly.", context.getCurrentNodeId());
				context.setNextNodeId(END);
				return Mono.empty();
			}

			// Resolve the final GraphFlux result into a graph state update.
			Map<String, Object> resultMap = graphFluxResultState(graphFlux, lastData);

			// Merge non-GraphFlux state
			Map<String, Object> partialStateWithoutGraphFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof GraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			Map<String, Object> combinedUpdateState = new HashMap<>(partialStateWithoutGraphFlux);
			combinedUpdateState.putAll(resultMap);
			Optional<InterruptionMetadata> interruptAfterMetadata = interruptAfter(context, combinedUpdateState);

			context.mergeIntoCurrentState(partialStateWithoutGraphFlux);

			// Merge the result from GraphFlux processing
			if (!resultMap.isEmpty()) {
				context.mergeIntoCurrentState(resultMap);
			}

			return finishStreamingStep(context, partialStateWithoutGraphFlux, interruptAfterMetadata);
		});

		return processedFlux
				.concatWith(updateContextMono.thenMany(Flux
					.just(GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue)))));
	}

	private Map<String, Object> graphFluxResultState(GraphFlux<?> graphFlux, Object lastData) {
		if (lastData instanceof GraphResponse<?> graphResponse) {
			Optional<Object> resultValue = graphResponse.resultValue();
			if (resultValue.isPresent()) {
				Object value = resultValue.get();
				if (value instanceof Map<?, ?> resultMap) {
					return copyStateMap(resultMap);
				}
			}
		}

		Map<String, Object> state = new HashMap<>();
		state.put(graphFluxStateKey(graphFlux), lastData);
		return state;
	}

	private static String graphFluxStateKey(GraphFlux<?> graphFlux) {
		return StringUtils.hasText(graphFlux.getKey()) ? graphFlux.getKey() : "result";
	}

	private static Map<String, Object> copyStateMap(Map<?, ?> resultMap) {
		Map<String, Object> state = new HashMap<>();
		for (Map.Entry<?, ?> entry : resultMap.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException("GraphFlux done result map keys must be String");
			}
			state.put(key, entry.getValue());
		}
		return state;
	}

	/**
	 * Checks the after-node interrupt hook using the pre-merge state.
	 * <p>
	 * This method must be called <strong>before</strong> the streaming state updates are
	 * merged into the {@link OverAllState} to keep semantics consistent with the
	 * non-streaming interruptAfter hook.
	 * @param context the graph runner context
	 * @param actionResult the streaming node action result (state delta) passed to interruptAfter
	 * @return interruption metadata if the hook triggers
	 */
	private Optional<InterruptionMetadata> interruptAfter(GraphRunnerContext context,
			Map<String, Object> actionResult) {
		String currentNodeId = context.getCurrentNodeId();
		AsyncNodeActionWithConfig action = context.getNodeAction(currentNodeId);

		if (!(action instanceof InterruptableAction interruptableAction)) {
			return Optional.empty();
		}

		try {
			OverAllState stateBeforeMerge = context.cloneState(context.getCurrentStateData());
			return interruptableAction.interruptAfter(currentNodeId, stateBeforeMerge, actionResult,
					context.getConfig());
		}
		catch (Exception e) {
			context.doListeners(ERROR, e);
			throw new RuntimeException("Failed to check interruptAfter hook for streaming node", e);
		}
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

		var results = new ArrayList<AtomicReference<Object>>();

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
					AtomicReference<Object> nodeDataRef = new AtomicReference<>();
					results.add(nodeDataRef);

					return transformGraphFluxToFlux(context, graphFlux, nodeDataRef)
							.subscribeOn(scheduler);
				}).collect(Collectors.toList());
		
		// Merge all parallel streams while preserving node identities
		// Each Flux is already subscribed on the scheduler, so they will execute in parallel
		Flux<GraphResponse<NodeOutput>> mergedFlux = Flux.merge(fluxList);

		// Handle completion and result mapping for all nodes
		Mono<Void> updateContextMono = Mono.defer(() -> {
			var updates = new ArrayList<Map<String, Object>>();
			for (int index = 0; index < results.size(); index++) {
				updates.add(graphFluxResultState(parallelGraphFlux.getGraphFluxes().get(index), results.get(index).get()));
			}

			// Merge non-ParallelGraphFlux state
			Map<String, Object> partialStateWithoutParallelGraphFlux = partialState.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof ParallelGraphFlux))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			// Check interruptAfter hook for streaming nodes using the pre-merge state.
			Map<String, Object> combinedUpdateState = new HashMap<>(partialStateWithoutParallelGraphFlux);
			updates.forEach(combinedUpdateState::putAll);
			Optional<InterruptionMetadata> interruptAfterMetadata = interruptAfter(context, combinedUpdateState);

			context.mergeIntoCurrentState(partialStateWithoutParallelGraphFlux);
			updates.forEach(context::mergeIntoCurrentState);
			return finishStreamingStep(context, partialStateWithoutParallelGraphFlux, interruptAfterMetadata);
		});

		return mergedFlux
				.concatWith(updateContextMono.thenMany(Flux
					.just(GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue)))));
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
		return checkpointAfterTransition(context, partialState, true)
				.flatMapMany(output -> Flux.just(GraphResponse.of(output),
						GraphResponse.continueWith(() -> mainGraphExecutor.execute(context, resultValue))));
	}
}
