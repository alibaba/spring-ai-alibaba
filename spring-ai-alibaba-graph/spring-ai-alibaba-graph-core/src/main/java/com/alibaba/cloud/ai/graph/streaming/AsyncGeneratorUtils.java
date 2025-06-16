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

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

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
			private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

			private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

			private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

			private int currentIndex = 0;

			private volatile Map<String, Object> mergedResult = new HashMap<>();

			private volatile List<AsyncGenerator<T>> activeGenerators = new ArrayList<>(generators);

			private final Map<AsyncGenerator<T>, Map<String, Object>> generatorResults = new HashMap<>();

			@Override
			public AsyncGenerator.Data<T> next() {
				readLock.lock();
				try {
					if (activeGenerators.isEmpty()) {
						return AsyncGenerator.Data.done(mergedResult);
					}
				}
				finally {
					readLock.unlock();
				}

				writeLock.lock();
				try {
					// Track whether we've found an active generator with data
					boolean foundData = false;

					// Use a fixed number of attempts to avoid infinite loops
					int attempts = 0;
					final int MAX_ATTEMPTS = activeGenerators.size() * 2; // Allow two
																			// full cycles

					while (attempts < MAX_ATTEMPTS && !activeGenerators.isEmpty()) {
						AsyncGenerator<T> current = activeGenerators.get(currentIndex);
						AsyncGenerator.Data<T> data = current.next();

						// Handle error state - remove generator and update merged result
						if (data.isError()) {
							log.debug("Removing errored generator: {}", current);
							handleCompletedGenerator(current, data);
							currentIndex = (currentIndex + 1) % Math.max(1, activeGenerators.size());
							attempts++;
							continue;
						}

						// Handle completion - remove generator and update merged result
						if (data.isDone()) {
							log.debug("Generator completed: {}", current);
							handleCompletedGenerator(current, data);
							currentIndex = (currentIndex + 1) % Math.max(1, activeGenerators.size());
							attempts++;
							continue;
						}

						// If we get here, the generator has valid data
						foundData = true;

						// Process and store the result
						Object result = data.resultValue();
						if (result instanceof Map) {
							@SuppressWarnings("unchecked")
							Map<String, Object> mapResult = (Map<String, Object>) result;

							// Store per-generator results
							generatorResults.computeIfAbsent(current, k -> new HashMap<>()).putAll(mapResult);

							// Update merged result using key strategy
							mergedResult = OverAllState.updateState(mergedResult, mapResult, keyStrategyMap);
						}

						// Move index for round-robin processing
						currentIndex = (currentIndex + 1) % activeGenerators.size();
						return data;
					}

					// If we looped through all generators without finding data
					if (!foundData && activeGenerators.isEmpty()) {
						return AsyncGenerator.Data.done(mergedResult);
					}

					// If we've attempted too many times but still have active generators
					if (attempts >= MAX_ATTEMPTS && !activeGenerators.isEmpty()) {
						log.warn("Reached max attempts while {} generators remain active", activeGenerators.size());
						return AsyncGenerator.Data.done(mergedResult);
					}

					// This should not happen, but as a fallback
					return AsyncGenerator.Data.done(mergedResult);
				}
				finally {
					writeLock.unlock();
				}
			}

			/**
			 * Helper method to handle completed or errored generators
			 */
			private void handleCompletedGenerator(AsyncGenerator<T> generator, AsyncGenerator.Data<T> data) {
				activeGenerators.remove(generator);

				// Process result if exists
				Object result = data.resultValue();
				if (result instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mapResult = (Map<String, Object>) result;
					mergedResult = OverAllState.updateState(mergedResult, mapResult, keyStrategyMap);
				}

				// Remove from generator results if present
				generatorResults.remove(generator);
			}
		};
	}

}
