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
package com.alibaba.cloud.ai.graph.async;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * Represents a queue-based asynchronous generator.
 */
public class AsyncGeneratorQueue {

	/**
	 * Inner class to generate asynchronous elements from the queue.
	 *
	 * @param <E> the type of elements in the queue
	 */
	public static class Generator<E> implements AsyncGenerator<E> {

		AsyncGenerator.Data<E> isEnd = null;

		final BlockingQueue<AsyncGenerator.Data<E>> queue;

		/**
		 * Constructs a Generator with the specified queue.
		 * @param queue the blocking queue to generate elements from
		 */
		public Generator(BlockingQueue<Data<E>> queue) {
			this.queue = queue;
		}

		public BlockingQueue<Data<E>> queue() {
			return queue;
		}

		/**
		 * Retrieves the next element from the queue asynchronously.
		 * @return the next element from the queue
		 */
		@Override
		public Data<E> next() {
			while (isEnd == null) {
				Data<E> value = queue.poll();
				if (value != null) {
					if (value.isDone()) {
						isEnd = value;
					}
					return value;
				}
			}
			return isEnd;
		}

	}

	/**
	 * Creates an AsyncGenerator from the provided blocking queue and consumer.
	 * @param <E> the type of elements in the queue
	 * @param <Q> the type of blocking queue
	 * @param queue the blocking queue to generate elements from
	 * @param consumer the consumer for processing elements from the queue
	 * @return an AsyncGenerator instance
	 */
	public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue,
			Consumer<Q> consumer) {
		return of(queue, consumer, commonPool());
	}

	/**
	 * Creates an AsyncGenerator from the provided queue, executor, and consumer.
	 * @param <E> the type of elements in the queue
	 * @param <Q> the type of blocking queue
	 * @param queue the blocking queue to generate elements from
	 * @param consumer the consumer for processing elements from the queue
	 * @param executor the executor for asynchronous processing
	 * @return an AsyncGenerator instance
	 */
	public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue,
			Consumer<Q> consumer, Executor executor) {
		Objects.requireNonNull(queue);
		Objects.requireNonNull(executor);
		Objects.requireNonNull(consumer);

		executor.execute(() -> {
			try {
				consumer.accept(queue);
			}
			catch (Throwable ex) {
				CompletableFuture<E> error = new CompletableFuture<>();
				error.completeExceptionally(ex);
				queue.add(AsyncGenerator.Data.of(error));
			}
			finally {
				queue.add(AsyncGenerator.Data.done());
			}

		});

		return new Generator<>(queue);
	}

	/**
	 * Creates an AsyncGenerator from the provided queue, executor, and consumer.
	 * @param <E> the type of elements in the queue
	 * @param <Q> the type of blocking queue
	 * @param queue the blocking queue to generate elements from
	 * @param executor the executor for asynchronous processing
	 * @param consumer the consumer for processing elements from the queue
	 * @return an AsyncGenerator instance
	 * @deprecated use of(Q, Consumer, Executor)
	 */
	@Deprecated
	public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Executor executor,
			Consumer<Q> consumer) {
		return of(queue, consumer, executor);
	}

}
