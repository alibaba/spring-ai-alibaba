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
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.GraphRunnerContext.INTERRUPT_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.ERROR;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * Main graph executor that handles the primary execution flow. This class demonstrates
 * inheritance by extending BaseGraphExecutor. It also demonstrates polymorphism through
 * its specific implementation of execute.
 */
public class MainGraphExecutor extends BaseGraphExecutor {

	private final NodeExecutor nodeExecutor;

	public MainGraphExecutor() {
		this.nodeExecutor = new NodeExecutor(this);
	}

	/**
	 * Implementation of the execute method. This demonstrates polymorphism as it provides
	 * a specific implementation for main execution flow.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with execution result
	 */
	@Override
	public Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context, AtomicReference<Object> resultValue) {
		try {
			if (context.shouldStop()) {
				return handleCompletion(context, resultValue);
			}
			if (context.isMaxIterationsReached()) {
				return context.isParallelBranch()
						? graphError(context, new IllegalStateException("Parallel branch reached the graph recursion limit"))
						: handleCompletion(context, resultValue);
			}

			final var returnFromEmbed = context.getReturnFromEmbedAndReset();
			if (returnFromEmbed.isPresent()) {
				var interruption = returnFromEmbed.get().value(new TypeRef<InterruptionMetadata>() {
				});
				if (interruption.isPresent()) {
					return Flux.just(GraphResponse.done(interruption.get()));
				}
				return Flux.just(GraphResponse.done(context.buildNodeOutputAndAddCheckpoint(Map.of())));
			}

			if (context.getCurrentNodeId() != null && context.getConfig().isInterrupted(context.getCurrentNodeId())) {
				context.getConfig().withNodeResumed(context.getCurrentNodeId());
				return Flux.just(GraphResponse.done(context.getCurrentStateData()));
			}

			if (context.isStartNode()) {
				return handleStartNode(context, resultValue);
			}

			if (context.isEndNode()) {
				return handleEndNode(context, resultValue);
			}

			final var resumeFrom = context.getResumeFromAndReset();
			if (resumeFrom.isPresent() && context.getCompiledGraph().compileConfig.interruptBeforeEdge()
					&& Objects.equals(context.getNextNodeId(), INTERRUPT_AFTER)) {
				return context.resolveNextNode(resumeFrom.get(), context.getCurrentStateData())
						.flatMapMany(command -> {
							context.setNextNodeId(command.gotoNode());
							context.setCurrentNodeId(null);
							return context.isEndNode() ? handleEndNode(context, resultValue)
									: executeNext(context, resultValue);
						})
						.onErrorResume(error -> graphError(context, error));
			}

			return executeNext(context, resultValue);
		}
		catch (Exception e) {
			return graphError(context, e);
		}
	}

	private Flux<GraphResponse<NodeOutput>> executeNext(GraphRunnerContext context, AtomicReference<Object> resultValue) {
		if (context.shouldInterrupt()) {
			return Mono.fromCallable(() -> InterruptionMetadata
					.builder(context.getCurrentNodeId(), context.cloneState(context.getCurrentStateData())).build())
					.map(GraphResponse::<NodeOutput>done).flux()
					.onErrorResume(error -> graphError(context, error));
		}
		return nodeExecutor.execute(context, resultValue);
	}

	private Flux<GraphResponse<NodeOutput>> graphError(GraphRunnerContext context, Throwable error) {
		context.doListeners(ERROR, error instanceof Exception exception ? exception : new Exception(error));
		return Flux.just(GraphResponse.error(error));
	}

	/**
	 * Handles the start node execution.
	 * @param context the graph runner context
	 * @return Flux of GraphResponse with start node handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleStartNode(GraphRunnerContext context,
			AtomicReference<Object> resultValue) {
		context.doListeners(START, null);
		return context.resolveEntryPoint().flatMap(command -> Mono.fromCallable(() -> {
			context.setNextNodeId(command.gotoNode());
			Optional<Checkpoint> checkpoint = context.addCheckpoint(START, command.gotoNode());
			NodeOutput output = context.buildOutput(START, checkpoint);
			context.setCurrentNodeId(command.gotoNode());
			return output;
		})).flatMapMany(output -> Flux.just(GraphResponse.of(output),
				GraphResponse.continueWith(() -> execute(context, resultValue))))
				.onErrorResume(error -> graphError(context, error));
	}

	/**
	 * Handles the end node execution.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with end node handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleEndNode(GraphRunnerContext context,
			AtomicReference<Object> resultValue) {
		try {
			context.doListeners(END, null);
			NodeOutput output = context.buildNodeOutput(END);
			return Flux.just(GraphResponse.of(output),
					GraphResponse.continueWith(() -> handleCompletion(context, resultValue)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

}
