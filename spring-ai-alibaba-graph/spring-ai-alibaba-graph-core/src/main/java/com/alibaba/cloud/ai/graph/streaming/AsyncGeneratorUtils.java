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

import org.bsc.async.AsyncGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for handling asynchronous generator merging and output processing
 */
public class AsyncGeneratorUtils {

	/**
	 * Creates an appropriate generator based on the number of generator entries
	 * @param generatorEntries list of generator entries
	 * @param <T> output type
	 * @return single generator or merged generator
	 */
	@SuppressWarnings("unchecked")
	public static <T> AsyncGenerator<T> createAppropriateGenerator(List<Map.Entry<String, Object>> generatorEntries) {
		if (generatorEntries.size() == 1) {
			// Only one generator, return it directly
			return (AsyncGenerator<T>) generatorEntries.get(0).getValue();
		}

		// Multiple generators, create a merged generator
		List<AsyncGenerator<T>> generators = generatorEntries.stream()
			.map(entry -> (AsyncGenerator<T>) entry.getValue())
			.collect(Collectors.toList());

		return createMergedGenerator(generators);
	}

	/**
	 * Creates a merged generator that combines outputs from multiple generators
	 * @param generators list of generators to merge
	 * @param <T> output type
	 * @return merged generator
	 */
	public static <T> AsyncGenerator<T> createMergedGenerator(List<AsyncGenerator<T>> generators) {
		return new AsyncGenerator<T>() {
			private int currentGeneratorIndex = 0;

			private boolean isDone = false;

			@Override
			public AsyncGenerator.Data<T> next() {
				if (isDone) {
					return AsyncGenerator.Data.done();
				}

				// Poll all generators in round-robin fashion
				int startIndex = currentGeneratorIndex;
				do {
					AsyncGenerator<T> currentGenerator = generators.get(currentGeneratorIndex);
					AsyncGenerator.Data<T> data = currentGenerator.next();

					// If current generator has data, return it
					if (!data.isDone()) {
						// Update index for next generator to process
						currentGeneratorIndex = (currentGeneratorIndex + 1) % generators.size();
						return data;
					}

					// Move to next generator
					currentGeneratorIndex = (currentGeneratorIndex + 1) % generators.size();

					// If all generators have been checked and none has data, complete
					if (currentGeneratorIndex == startIndex) {
						isDone = true;
						return AsyncGenerator.Data.done();
					}
				}
				while (true);
			}
		};
	}

}
