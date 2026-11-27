/*
 * Copyright 2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A reactive tool that can emit output deltas before its final model-facing result.
 * Each call must produce exactly one completed part followed by successful completion.
 */
public interface StreamingToolCallback extends CancellableAsyncToolCallback {

	Flux<ToolStreamPart> stream(String arguments, ToolContext context, CancellationToken cancellation);

	@Override
	default String call(String arguments) {
		return call(arguments, new ToolContext(Map.of()));
	}

	@Override
	default CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken cancellation) {
		return callAsync(arguments, context, cancellation, ignored -> Mono.empty());
	}

	default CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken cancellation,
			Function<Object, Mono<Void>> delta) {
		var result = Flux.defer(() -> {
			cancellation.throwIfCancelled();
			return stream(arguments, context, cancellation);
		});
		var completed = new AtomicReference<ToolStreamPart.Completed>();
		var future = result.concatMap(part -> {
			if (completed.get() != null) {
				return Mono.error(new IllegalStateException("Streaming tool emitted data after its final result"));
			}
			if (part instanceof ToolStreamPart.Delta fragment) {
				return delta.apply(fragment.value());
			}
			completed.set((ToolStreamPart.Completed) part);
			return Mono.empty();
		}, 1).then(Mono.fromSupplier(() -> {
			if (completed.get() == null) {
				throw new IllegalStateException("Streaming tool completed without a final result");
			}
			return completed.get().result();
		})).toFuture();
		cancellation.onCancel(() -> future.cancel(true));
		return future;
	}
}
