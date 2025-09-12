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
import java.util.concurrent.CompletableFuture;
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
		return Flux.defer(() -> {
			try {
				GeneratorContext context = new GeneratorContext(initialState, config, compiledGraph);
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

	private Flux<GraphResponse<NodeOutput>> processGraphExecution(GeneratorContext context) {
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

	private Flux<GraphResponse<NodeOutput>> handleStartNode(GeneratorContext context) {
		try {
			context.doListeners(START, null);
			Command nextCommand = context.getEntryPoint();
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			Optional<Checkpoint> cp = context.addCheckpoint(START, context.getNextNodeId());
			NodeOutput output = context.buildOutput(START, cp);

			context.setCurrentNodeId(context.getNextNodeId());
			// 合并输出和后续流程，保持顺序，processGraphExecution用Flux.defer包裹
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> processGraphExecution(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleEndNode(GeneratorContext context) {
		try {
			context.doListeners(END, null);
			NodeOutput output = context.buildNodeOutput(END);
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> handleCompletion(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleCompletion(GeneratorContext context) {
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

	private Flux<GraphResponse<NodeOutput>> handleInterruption(GeneratorContext context) {
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

	private Flux<GraphResponse<NodeOutput>> executeCurrentNode(GeneratorContext context) {
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

	private Flux<GraphResponse<NodeOutput>> handleActionResult(GeneratorContext context,
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
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> processGraphExecution(context)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

	private Flux<GraphResponse<NodeOutput>> handleEmbeddedFlux(GeneratorContext context,
			Flux<GraphResponse<NodeOutput>> embedFlux, Map<String, Object> partialState) {

		// 使用更可靠的完成检测机制
		return embedFlux
			.map(data -> {
				if (data.getOutput() != null) {
					var output = data.getOutput().join();
					output.setSubGraph(true);
					return GraphResponse.of(output);
				}
				return data;
			})
			// 收集所有数据并在流完成时处理状态更新
			.collectList()
			.flatMapMany(dataList -> {
				// 获取最后一个有效的结果数据
				GraphResponse<NodeOutput> lastData = dataList.isEmpty() ? null :
						(GraphResponse<NodeOutput>)dataList.get(dataList.size() - 1);

				// 执行状态更新
				if (lastData != null) {
					var nodeResultValue = lastData.resultValue;

					if (nodeResultValue instanceof InterruptionMetadata) {
						context.setReturnFromEmbedWithValue(nodeResultValue);
						// 如果是中断，直接返回数据流，不继续执行
						return Flux.fromIterable(dataList);
					}

					if (nodeResultValue != null && nodeResultValue instanceof Map<?, ?>) {
						try {
							// Remove Flux from partial state
							Map<String, Object> partialStateWithoutFlux = partialState.entrySet()
								.stream()
								.filter(e -> !(e.getValue() instanceof Flux))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

							// Apply partial state update first
							Map<String, Object> intermediateState = OverAllState.updateState(
								context.getCurrentState(),
								partialStateWithoutFlux,
								context.getKeyStrategyMap()
							);
							var currentState = OverAllState.updateState(
								intermediateState,
								(Map<String, Object>) nodeResultValue,
								context.getKeyStrategyMap()
							);
							context.updateCurrentState(currentState);
							context.getOverallState().updateState(currentState);

							// 更新下一个节点信息
							Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getCurrentState());
							context.setNextNodeId(nextCommand.gotoNode());
							context.updateCurrentState(nextCommand.update());
						}
						catch (Exception e) {
							return Flux.concat(
								Flux.fromIterable(dataList),
								Flux.just(GraphResponse.error(e))
							);
						}
					}
				}

				// 返回数据流 + 后续处理
				return Flux.concat(
					Flux.fromIterable(dataList),
					Flux.defer(() -> processGraphExecution(context))
				);
			});
	}

	private Optional<Flux<GraphResponse<NodeOutput>>> getEmbedFlux(GeneratorContext context,
			Map<String, Object> partialState) {
		return partialState.entrySet().stream().filter(e -> e.getValue() instanceof Flux<?>).findFirst().map(e -> {
			var chatFlux = (Flux<?>) e.getValue();
			var lastChatResponseRef = new AtomicReference<ChatResponse>(null);
			var lastGraphResponseRef = new AtomicReference<GraphResponse<NodeOutput>>(null);
			var completionSignal = new AtomicReference<GraphResponse<NodeOutput>>(null);

			// Handle different element types in the flux
			return chatFlux
				.map(element -> {
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
								.of(new StreamingOutput(newResponse.getResult().getOutput().getText(), context.getCurrentNodeId(), context.getOverallState()));
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
				})
				// 在主流完成后，确保发送完成信号
				.doOnComplete(() -> {
					// 生成最终的完成信号
					GraphResponse<NodeOutput> finalSignal;
					if (lastChatResponseRef.get() != null) {
						Map<String, Object> completionResult = Map.of(e.getKey(),
								lastChatResponseRef.get().getResult().getOutput());
						finalSignal = GraphResponse.done(completionResult);
					} else {
						GraphResponse<NodeOutput> lastGraphResponse = lastGraphResponseRef.get();
						if (lastGraphResponse != null && lastGraphResponse.resultValue().isPresent()) {
							Object result = lastGraphResponse.resultValue().get();
							if (result instanceof Map resultMap) {
								if (!resultMap.containsKey(e.getKey()) && resultMap.containsKey("messages")) {
									List<Object> messages = (List<Object>)resultMap.get("messages");
									resultMap.put(e.getKey(), ((AssistantMessage)messages.get(messages.size() - 1)).getText());
								}
							}
							finalSignal = lastGraphResponse;
						} else {
							// 即使没有数据，也要发送一个标记完成的信号
							finalSignal = GraphResponse.done(Map.of(e.getKey(), ""));
						}
					}
					completionSignal.set(finalSignal);
				})
				// 添加完成信号到流的末尾
				.concatWith(Mono.fromSupplier(() -> {
					GraphResponse<NodeOutput> signal = completionSignal.get();
					return signal != null ? signal : GraphResponse.done(Map.of(e.getKey(), ""));
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
	private Flux<GraphResponse<NodeOutput>> handleEmbeddedGenerator(GeneratorContext context,
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
		})
		.concatWith(Flux.defer(() -> {
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
