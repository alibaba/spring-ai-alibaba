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

import com.alibaba.cloud.ai.graph.GraphRunnerContext;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import reactor.core.publisher.Flux;

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
			if (context.shouldStop() || context.isMaxIterationsReached()) {
				return handleCompletion(context, resultValue);
			}

			final var returnFromEmbed = context.getReturnFromEmbedAndReset();
			if (returnFromEmbed.isPresent()) {
				var interruption = returnFromEmbed.get().value(new TypeRef<InterruptionMetadata>() {
				});
				if (interruption.isPresent()) {
					return Flux.just(GraphResponse.done(interruption.get()));
				}
				return Flux.just(GraphResponse.done(context.buildCurrentNodeOutput()));
			}

			if (context.getCurrentNodeId() != null && context.getConfig().isInterrupted(context.getCurrentNodeId())) {
				context.getConfig().withNodeResumed(context.getCurrentNodeId());
				return Flux.just(GraphResponse.done(GraphResponse.done(context.getCurrentState())));
			}

			if (context.isStartNode()) {
				return handleStartNode(context);
			}

			if (context.isEndNode()) {
				return handleEndNode(context, resultValue);
			}

			final var resumeFrom = context.getResumeFromAndReset();
			if (resumeFrom.isPresent()) {
				if (context.getCompiledGraph().compileConfig.interruptBeforeEdge()
						&& java.util.Objects.equals(context.getNextNodeId(), INTERRUPT_AFTER)) {
					var nextNodeCommand = context.nextNodeId(resumeFrom.get(), context.getCurrentState());
					context.setNextNodeId(nextNodeCommand.gotoNode());
					context.updateCurrentState(nextNodeCommand.update());
					context.setCurrentNodeId(null);
				}
			}

			if (context.shouldInterrupt()) {
				try {
					InterruptionMetadata metadata = InterruptionMetadata
						.builder(context.getCurrentNodeId(), context.cloneState(context.getCurrentState()))
						.build();
					return Flux.just(GraphResponse.done(metadata));
				}
				catch (Exception e) {
					return Flux.just(GraphResponse.error(e));
				}
			}

			return nodeExecutor.execute(context, resultValue);
		}
		catch (Exception e) {
			context.doListeners(ERROR, e);
			org.slf4j.LoggerFactory.getLogger(com.alibaba.cloud.ai.graph.GraphRunner.class)
				.error("Error during graph execution", e);
			return Flux.just(GraphResponse.error(e));
		}
	}

	/**
	 * Handles the start node execution.
	 * @param context the graph runner context
	 * @return Flux of GraphResponse with start node handling result
	 */
	private Flux<GraphResponse<NodeOutput>> handleStartNode(GraphRunnerContext context) {
		try {
			context.doListeners(START, null);
			Command nextCommand = context.getEntryPoint();
			context.setNextNodeId(nextCommand.gotoNode());
			context.updateCurrentState(nextCommand.update());

			Optional<Checkpoint> cp = context.addCheckpoint(START, context.getNextNodeId());
			NodeOutput output = context.buildOutput(START, cp);

			context.setCurrentNodeId(context.getNextNodeId());
			// Recursively call the main execution handler
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> execute(context, new AtomicReference<>())));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
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
			return Flux.just(GraphResponse.of(output))
				.concatWith(Flux.defer(() -> handleCompletion(context, resultValue)));
		}
		catch (Exception e) {
			return Flux.just(GraphResponse.error(e));
		}
	}

}
