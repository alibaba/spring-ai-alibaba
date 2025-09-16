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
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for graph execution handlers. This class demonstrates inheritance by
 * providing a common base for all execution handlers. It encapsulates common
 * functionality that can be shared across different executors.
 */
public abstract class BaseGraphExecutor {

	/**
	 * Abstract method to be implemented by subclasses. This demonstrates polymorphism as
	 * each subclass will provide its own implementation.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with execution result
	 */
	public abstract Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context,
			AtomicReference<Object> resultValue);

	/**
	 * Protected method that can be used by subclasses. This demonstrates encapsulation by
	 * providing controlled access to common functionality.
	 * @param context the graph runner context
	 * @param resultValue the atomic reference to store the result value
	 * @return Flux of GraphResponse with completion handling result
	 */
	protected Flux<GraphResponse<NodeOutput>> handleCompletion(GraphRunnerContext context,
			AtomicReference<Object> resultValue) {
		return Flux.defer(() -> {
			try {
				if (context.getCompiledGraph().compileConfig.releaseThread()
						&& context.getCompiledGraph().compileConfig.checkpointSaver().isPresent()) {
					com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.Tag tag = context
						.getCompiledGraph().compileConfig.checkpointSaver()
						.get()
						.release(context.getConfig());
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
		});
	}

}
