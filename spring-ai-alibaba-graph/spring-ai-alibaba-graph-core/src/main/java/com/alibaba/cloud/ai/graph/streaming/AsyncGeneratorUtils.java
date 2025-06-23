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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Utility class for handling asynchronous generator merging and output processing
 */
public class AsyncGeneratorUtils {

	private static final Logger log = LoggerFactory.getLogger(AsyncGeneratorUtils.class);

	/**
	 * Creates an appropriate generator based on the number of generator entries
	 * @param generatorEntries list of generator entries
	 * @param <T> output type
	 * @return single generator or merged generator
	 */
	@SuppressWarnings("unchecked")
	public static <T> AsyncGenerator<T> createAppropriateGenerator(List<Map.Entry<String, Object>> generatorEntries,
			List<AsyncGenerator<T>> asyncNodeGenerators, Map<String, KeyStrategy> keyStrategyMap) {
		if (generatorEntries.size() == 1) {
			// Only one generator, return it directly
			return (AsyncGenerator<T>) generatorEntries.get(0).getValue();
		}

		// Multiple generators, create a merged generator
		List<AsyncGenerator<T>> generators = generatorEntries.stream()
			.map(entry -> (AsyncGenerator<T>) entry.getValue())
			.collect(Collectors.toList());
		generators.addAll(asyncNodeGenerators);
		return createMergedGenerator(generators, keyStrategyMap);
	}

	/**
	 * Creates a merged generator that combines outputs from multiple generators
	 * @param generators list of generators to merge
	 * @param <T> output type
	 * @return merged generator
	 */
	public static <T> AsyncGenerator<T> createMergedGenerator(List<AsyncGenerator<T>> generators,
			Map<String, KeyStrategy> keyStrategyMap) {
		return new AsyncGenerator<>() {
			// Switch to StampedLock to simplify lock management
			private final StampedLock lock = new StampedLock();

			private AtomicInteger pollCounter = new AtomicInteger(0);

			private Map<String, Object> mergedResult = new HashMap<>();

			private final List<AsyncGenerator<T>> activeGenerators = new CopyOnWriteArrayList<>(generators);

			private final Map<AsyncGenerator<T>, Map<String, Object>> generatorResults = new HashMap<>();

			@Override
			public AsyncGenerator.Data<T> next() {
				while (true) {
					// Read optimistically and check quickly
					long stamp = lock.tryOptimisticRead();
					boolean empty = activeGenerators.isEmpty();
					if (!lock.validate(stamp)) {
						stamp = lock.readLock();
						try {
							empty = activeGenerators.isEmpty();
						}
						finally {
							lock.unlockRead(stamp);
						}
					}
					if (empty) {
						return AsyncGenerator.Data.done(mergedResult);
					}

					// Fine-grained lock control
					final int currentIdx;
					AsyncGenerator<T> current;
					long writeStamp = lock.writeLock();
					try {
						final int size = activeGenerators.size();
						if (size == 0)
							return AsyncGenerator.Data.done(mergedResult);

						currentIdx = pollCounter.updateAndGet(i -> (i + 1) % size);
						current = activeGenerators.get(currentIdx);
					}
					finally {
						lock.unlockWrite(writeStamp);
					}

					// Execute the generator 'next()' in the unlocked state
					AsyncGenerator.Data<T> data = current.next();

					writeStamp = lock.writeLock();
					try {
						// Double checks prevent status changes
						if (!activeGenerators.contains(current)) {
							continue;
						}

						if (data.isDone() || data.isError()) {
							handleCompletedGenerator(current, data);
							if (activeGenerators.isEmpty()) {
								return AsyncGenerator.Data.done(mergedResult);
							}
							continue;
						}

						handleCompletedGenerator(current, data);
						return data;
					}
					finally {
						lock.unlockWrite(writeStamp);
					}
				}

			}

			/**
			 * Helper method to handle completed or errored generators
			 */
			private void handleCompletedGenerator(AsyncGenerator<T> generator, AsyncGenerator.Data<T> data) {
				// Remove generator if done or error
				if (data.isDone() || data.isError()) {
					activeGenerators.remove(generator);
				}

				// Process result if exists
				data.resultValue().ifPresent(result -> {
					if (result instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> mapResult = (Map<String, Object>) result;
						mergedResult = OverAllState.updateState(mergedResult, mapResult, keyStrategyMap);
					}
				});

				// Remove from generator results if present
				generatorResults.remove(generator);
			}
		};
	}

}
