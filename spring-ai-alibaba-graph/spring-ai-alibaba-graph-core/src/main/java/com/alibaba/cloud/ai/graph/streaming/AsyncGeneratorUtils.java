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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Utility class for handling asynchronous generator merging and output processing
 */
public class AsyncGeneratorUtils {

    /**
     * Creates an appropriate generator based on the number of generator entries
     *
     * @param generatorEntries list of generator entries
     * @param <T>              output type
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
     *
     * @param generators list of generators to merge
     * @param <T>        output type
     * @return merged generator
     */
    public static <T> AsyncGenerator<T> createMergedGenerator(List<AsyncGenerator<T>> generators, Map<String, KeyStrategy> keyStrategyMap) {
        return new AsyncGenerator<T>() {
            private int currentIndex = 0;
            private Map<String, Object> mergedResult = new HashMap<>();
            private List<AsyncGenerator<T>> activeGenerators = new ArrayList<>(generators);

            @Override
            public AsyncGenerator.Data<T> next() {
                // If there is no active generator, return the merge result
                if (activeGenerators.isEmpty()) {
                    return AsyncGenerator.Data.done(mergedResult);
                }

                // Polling to process each generator
                while (!activeGenerators.isEmpty()) {
                    AsyncGenerator<T> current = activeGenerators.get(currentIndex);
                    AsyncGenerator.Data<T> data = current.next();

                    // If generator completes
                    if (data.isDone()) {
                        // Removed completed generator from active list
                        activeGenerators.remove(current);
                        
                        // Results when processing is completed
                        Object result = data.resultValue();
                        if (result != null) {
                            if (result instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> mapResult = (Map<String, Object>) result;
                                // Update status using keyStrategyMap
                                mergedResult = OverAllState.updateState(mergedResult, mapResult, keyStrategyMap);
                            } else {
                                throw new IllegalArgumentException("Generator must return a Map type result");
                            }
                        }
                        
                        // If all generators are completed, return the final merge result
                        if (activeGenerators.isEmpty()) {
                            return AsyncGenerator.Data.done(mergedResult);
                        }
                        
                        //Continue to process the next generator
                        continue;
                    }

                    // 如果generator还有数据，返回当前数据
                    currentIndex = (currentIndex + 1) % activeGenerators.size();
                    return data;
                }

                // 所有generator都处理完成，返回最终结果
                return AsyncGenerator.Data.done(mergedResult);
            }
        };
    }

}
