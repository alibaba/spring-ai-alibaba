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

import com.alibaba.cloud.ai.graph.async.internal.UnmodifiableDeque;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * An asynchronous generator interface that allows generating asynchronous elements.
 *
 * @param <E> the type of elements. The generator will emit {@link CompletableFuture
 * CompletableFutures&lt;E&gt;} elements
 */
public interface AsyncGenerator<E> extends Iterable<E>, AsyncGeneratorOperators<E> {

	interface HasResultValue {

		Optional<Object> resultValue();

	}

	/**
	 * An asynchronous generator decorator that allows retrieving the result value of the
	 * asynchronous operation, if any.
	 *
	 * @param <E> the type of elements in the generator
	 */
	class WithResult<E> implements AsyncGenerator<E>, HasResultValue {

		protected final AsyncGenerator<E> delegate;

		private Object resultValue;

		public WithResult(AsyncGenerator<E> delegate) {
			this.delegate = delegate;
		}

		public AsyncGenerator<E> delegate() {
			return delegate;
		}

		/**
		 * Retrieves the result value of the generator, if any.
		 * @return an {@link Optional} containing the result value if present, or an empty
		 * Optional if not
		 */
		public Optional<Object> resultValue() {
			return ofNullable(resultValue);
		};

		@Override
		public final Data<E> next() {
			final Data<E> result = delegate.next();
			if (result.isDone()) {
				resultValue = result.resultValue;
			}
			return result;
		}

	}

	/**
	 * An asynchronous generator decorator that allows to generators composition embedding
	 * other generators.
	 *
	 * @param <E> the type of elements in the generator
	 */
	class WithEmbed<E> implements AsyncGenerator<E>, HasResultValue {

		protected final Deque<Embed<E>> generatorsStack = new ArrayDeque<>(2);

		private final Deque<Data<E>> returnValueStack = new ArrayDeque<>(2);

		public WithEmbed(AsyncGenerator<E> delegate, EmbedCompletionHandler onGeneratorDoneWithResult) {
			generatorsStack.push(new Embed<>(delegate, onGeneratorDoneWithResult));
		}

		public WithEmbed(AsyncGenerator<E> delegate) {
			this(delegate, null);
		}

		public Deque<Data<E>> resultValues() {
			return new UnmodifiableDeque<>(returnValueStack);
		}

		public Optional<Object> resultValue() {
			return ofNullable(returnValueStack.peek()).map(r -> r.resultValue);
		}

		private void clearPreviousReturnsValuesIfAny() {
			// Check if the return values are which ones from previous run
			if (returnValueStack.size() > 1 && returnValueStack.size() == generatorsStack.size()) {
				returnValueStack.clear();
			}
		}

		// private AsyncGenerator.WithResult<E> toGeneratorWithResult( AsyncGenerator<E>
		// generator ) {
		// return ( generator instanceof WithResult ) ?
		// (AsyncGenerator.WithResult<E>) generator :
		// new WithResult<>(generator);
		// }

		protected boolean isLastGenerator() {
			return generatorsStack.size() == 1;
		}

		@Override
		public Data<E> next() {
			if (generatorsStack.isEmpty()) { // GUARD
				throw new IllegalStateException("no generator found!");
			}

			final Embed<E> embed = generatorsStack.peek();
			final Data<E> result = embed.generator.next();

			if (result.isDone()) {
				clearPreviousReturnsValuesIfAny();
				returnValueStack.push(result);
				if (embed.onCompletion != null /* && result.resultValue != null */ ) {
					try {
						embed.onCompletion.accept(result.resultValue);
					}
					catch (Exception e) {
						return Data.error(e);
					}
				}
				if (isLastGenerator()) {
					return result;
				}
				generatorsStack.pop();
				return next();
			}
			if (result.embed != null) {
				if (generatorsStack.size() >= 2) {
					return Data.error(new UnsupportedOperationException(
							"Currently recursive nested generators are not supported!"));
				}
				generatorsStack.push(result.embed);
				return next();
			}

			return result;
		}

	}

	@FunctionalInterface
	interface EmbedCompletionHandler {

		void accept(Object t) throws Exception;

	}

	class Embed<E> {

		final AsyncGenerator<E> generator;

		final EmbedCompletionHandler onCompletion;

		public Embed(AsyncGenerator<E> generator, EmbedCompletionHandler onCompletion) {
			Objects.requireNonNull(generator, "generator cannot be null");
			this.generator = generator;
			this.onCompletion = onCompletion;
		}

	}

	/**
	 * Represents a data element in the AsyncGenerator.
	 *
	 * @param <E> the type of the data element
	 */
	class Data<E> {

		final CompletableFuture<E> data;

		final Embed<E> embed;

		final Object resultValue;

		public Data(CompletableFuture<E> data, Embed<E> embed, Object resultValue) {
			this.data = data;
			this.embed = embed;
			this.resultValue = resultValue;
		}

		public Optional<Object> resultValue() {
			return resultValue == null ? Optional.empty() : Optional.of(resultValue);
		}

		public boolean isDone() {
			return data == null && embed == null;
		}

		public boolean isError() {
			return data != null && data.isCompletedExceptionally();
		}

		public static <E> Data<E> of(CompletableFuture<E> data) {
			return new Data<>(data, null, null);
		}

		public static <E> Data<E> of(E data) {
			return new Data<>(completedFuture(data), null, null);
		}

		public static <E> Data<E> composeWith(AsyncGenerator<E> generator, EmbedCompletionHandler onCompletion) {
			return new Data<>(null, new Embed<>(generator, onCompletion), null);
		}

		public static <E> Data<E> done() {
			return new Data<>(null, null, null);
		}

		public static <E> Data<E> done(Object resultValue) {
			return new Data<>(null, null, resultValue);
		}

		public static <E> Data<E> error(Throwable exception) {
			CompletableFuture<E> future = new CompletableFuture<>();
			future.completeExceptionally(exception);
			return Data.of(future);
		}

	}

	default AsyncGeneratorOperators<E> async(Executor executor) {
		return new AsyncGeneratorOperators<E>() {
			@Override
			public Data<E> next() {
				return AsyncGenerator.this.next();
			}

			@Override
			public Executor executor() {
				return executor;
			}
		};
	}

	/**
	 * Retrieves the next asynchronous element.
	 * @return the next element from the generator
	 */
	Data<E> next();

	/**
	 * Converts the AsyncGenerator to a CompletableFuture.
	 * @return a CompletableFuture representing the completion of the AsyncGenerator
	 */
	default CompletableFuture<Object> toCompletableFuture() {
		final Data<E> next = next();
		if (next.isDone()) {
			return completedFuture(next.resultValue);
		}
		return next.data.thenCompose(v -> toCompletableFuture());
	}

	/**
	 * Returns a sequential Stream with the elements of this AsyncGenerator. Each
	 * CompletableFuture is resolved and then make available to the stream.
	 * @return a Stream of elements from the AsyncGenerator
	 */
	default Stream<E> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
	}

	/**
	 * Returns an iterator over the elements of this AsyncGenerator. Each call to `next`
	 * retrieves the next "resolved" asynchronous element from the generator.
	 * @return an iterator over the elements of this AsyncGenerator
	 */
	default Iterator<E> iterator() {
		return new InternalIterator<E>(this);
	}

	/**
	 * Returns an empty AsyncGenerator.
	 * @param <E> the type of elements
	 * @return an empty AsyncGenerator
	 */
	static <E> AsyncGenerator<E> empty() {
		return Data::done;
	}

	/**
	 * create a generator, mapping each element to an asynchronous counterpart.
	 * @param <E> the type of elements in the collection
	 * @param <U> the type of elements in the CompletableFuture
	 * @param iterator the elements iterator
	 * @param mapFunction the function to map elements to {@link CompletableFuture}
	 * @return an AsyncGenerator instance with mapped elements
	 */
	static <E, U> AsyncGenerator<U> map(Iterator<E> iterator, Function<E, CompletableFuture<U>> mapFunction) {
		return () -> {
			if (!iterator.hasNext()) {
				return Data.done();
			}
			return Data.of(mapFunction.apply(iterator.next()));
		};
	}

	/**
	 * Collects asynchronous elements from an iterator.
	 * @param <E> the type of elements in the iterator
	 * @param <U> the type of elements in the CompletableFuture
	 * @param iterator the iterator containing elements to collect
	 * @param consumer the function to consume elements and add them to the accumulator
	 * @return an AsyncGenerator instance with collected elements
	 */
	static <E, U> AsyncGenerator<U> collect(Iterator<E> iterator,
			BiConsumer<E, Consumer<CompletableFuture<U>>> consumer) {
		final List<CompletableFuture<U>> accumulator = new ArrayList<>();

		final Consumer<CompletableFuture<U>> addElement = accumulator::add;
		while (iterator.hasNext()) {
			consumer.accept(iterator.next(), addElement);
		}

		final Iterator<CompletableFuture<U>> it = accumulator.iterator();
		return () -> {
			if (!it.hasNext()) {
				return Data.done();
			}
			return Data.of(it.next());
		};
	}

	/**
	 * create a generator, mapping each element to an asynchronous counterpart.
	 * @param <E> the type of elements in the collection
	 * @param <U> the type of elements in the CompletableFuture
	 * @param collection the collection of elements to map
	 * @param mapFunction the function to map elements to CompletableFuture
	 * @return an AsyncGenerator instance with mapped elements
	 */
	static <E, U> AsyncGenerator<U> map(Collection<E> collection, Function<E, CompletableFuture<U>> mapFunction) {
		if (collection == null || collection.isEmpty()) {
			return empty();
		}
		return map(collection.iterator(), mapFunction);
	}

	/**
	 * Collects asynchronous elements from a collection.
	 * @param <E> the type of elements in the iterator
	 * @param <U> the type of elements in the CompletableFuture
	 * @param collection the iterator containing elements to collect
	 * @param consumer the function to consume elements and add them to the accumulator
	 * @return an AsyncGenerator instance with collected elements
	 */
	static <E, U> AsyncGenerator<U> collect(Collection<E> collection,
			BiConsumer<E, Consumer<CompletableFuture<U>>> consumer) {
		if (collection == null || collection.isEmpty()) {
			return empty();
		}
		return collect(collection.iterator(), consumer);
	}

}

class InternalIterator<E> implements Iterator<E> {

	private final AsyncGenerator<E> delegate;

	private AsyncGenerator.Data<E> currentFetchedData;

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	private final ReentrantReadWriteLock.ReadLock r = rwl.readLock();

	private final ReentrantReadWriteLock.WriteLock w = rwl.writeLock();

	public InternalIterator(AsyncGenerator<E> delegate) {
		this.delegate = delegate;
		currentFetchedData = delegate.next();
	}

	@Override
	public boolean hasNext() {
		try {
			r.lock();
			final AsyncGenerator.Data<E> value = currentFetchedData;
			return value != null && !value.isDone();
		}
		finally {
			r.unlock();
		}
	}

	@Override
	public E next() {
		try {
			w.lock();

			AsyncGenerator.Data<E> next = currentFetchedData;

			if (next == null || next.isDone()) {
				throw new IllegalStateException("no more elements into iterator");
			}

			if (!next.isError()) {
				currentFetchedData = delegate.next();
			}

			return next.data.join();
		}
		finally {
			w.unlock();
		}
	}

};
