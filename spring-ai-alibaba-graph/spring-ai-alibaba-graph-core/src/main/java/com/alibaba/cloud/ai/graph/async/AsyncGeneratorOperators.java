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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;

public interface AsyncGeneratorOperators<E> {

	AsyncGenerator.Data<E> next();

	default Executor executor() {
		return Runnable::run;
	}

	/**
	 * Maps the elements of this generator to a new asynchronous generator.
	 * @param mapFunction the function to map elements to a new asynchronous counterpart
	 * @param <U> the type of elements in the new generator
	 * @return a generator with mapped elements
	 */
	default <U> AsyncGenerator<U> map(Function<E, U> mapFunction) {
		return () -> {
			final AsyncGenerator.Data<E> next = next();
			if (next.isDone()) {
				return AsyncGenerator.Data.done(next.resultValue);
			}
			return AsyncGenerator.Data.of(next.data.thenApplyAsync(mapFunction, executor()));
		};
	}

	/**
	 * Maps the elements of this generator to a new asynchronous generator, and flattens
	 * the resulting nested generators.
	 * @param mapFunction the function to map elements to a new asynchronous counterpart
	 * @param <U> the type of elements in the new generator
	 * @return a generator with mapped and flattened elements
	 */
	default <U> AsyncGenerator<U> flatMap(Function<E, CompletableFuture<U>> mapFunction) {
		return () -> {
			final AsyncGenerator.Data<E> next = next();
			if (next.isDone()) {
				return AsyncGenerator.Data.done(next.resultValue);
			}
			return AsyncGenerator.Data.of(next.data.thenComposeAsync(mapFunction, executor()));
		};
	}

	/**
	 * Filters the elements of this generator based on the given predicate. Only elements
	 * that satisfy the predicate will be included in the resulting generator.
	 * @param predicate the predicate to test elements against
	 * @return a generator with elements that satisfy the predicate
	 */
	default AsyncGenerator<E> filter(Predicate<E> predicate) {
		return () -> {
			AsyncGenerator.Data<E> next = next();
			while (!next.isDone()) {

				final E value = next.data.join();

				if (predicate.test(value)) {
					return next;
				}
				next = next();
			}
			return AsyncGenerator.Data.done(next.resultValue);
		};
	}

	/**
	 * Asynchronously iterates over the elements of the AsyncGenerator and applies the
	 * given consumer to each element.
	 * @param consumer the consumer function to be applied to each element
	 * @return a CompletableFuture representing the completion of the iteration process.
	 */
	default CompletableFuture<Object> forEachAsync(Consumer<E> consumer) {

		final var next = next();
		if (next.isDone()) {
			return completedFuture(next.resultValue);
		}
		if (next.embed != null) {
			return next.embed.generator.async(executor())
				.forEachAsync(consumer)
				.thenCompose(v -> forEachAsync(consumer));
		}
		else {
			return next.data.thenApplyAsync(v -> {
				consumer.accept(v);
				return null;
			}, executor()).thenCompose(v -> forEachAsync(consumer));
		}

	}

	/**
	 * Collects elements from the AsyncGenerator asynchronously into a list.
	 * @param <R> the type of the result list
	 * @param result the result list to collect elements into
	 * @param consumer the consumer function for processing elements
	 * @return a CompletableFuture representing the completion of the collection process
	 */
	default <R extends List<E>> CompletableFuture<R> collectAsync(R result, BiConsumer<R, E> consumer) {
		final var next = next();
		if (next.isDone()) {
			return completedFuture(result);
		}
		return next.data.thenApplyAsync(v -> {
			consumer.accept(result, v);
			return null;
		}, executor()).thenCompose(v -> collectAsync(result, consumer));

	}

}
