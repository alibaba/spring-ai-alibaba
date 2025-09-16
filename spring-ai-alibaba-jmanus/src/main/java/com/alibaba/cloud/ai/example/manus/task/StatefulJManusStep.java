/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.task;

import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;


public interface StatefulJManusStep extends StatefulTask {

	/**
	 * Gets the execution history snapshot from the integrated JManus plan system. This
	 * provides a formatted string representation of all completed and current steps,
	 * which is particularly useful for:
	 * <ul>
	 * <li>Providing context to LLM-based steps</li>
	 * <li>Debugging and logging execution flow</li>
	 * <li>Creating execution summaries</li>
	 * </ul>
	 * @param context The execution context
	 * @param onlyCompletedAndFirstInProgress If true, only shows completed steps and
	 * first in-progress step
	 * @return Formatted execution history string, or empty string if no history is
	 * available
	 */
	default String getExecutionHistory(JManusExecutionContext context, boolean onlyCompletedAndFirstInProgress) {
		return context.getExecutionHistorySnapshot(onlyCompletedAndFirstInProgress);
	}

	/**
	 * Gets a combined view of both the structured context data and the execution history.
	 * This method provides comprehensive context information by combining:
	 * <ul>
	 * <li>Structured data summary from the context</li>
	 * <li>Formatted execution history from the plan system</li>
	 * </ul>
	 *
	 * Use this when you need full context awareness, such as for:
	 * <ul>
	 * <li>Final summary or analysis steps</li>
	 * <li>Error recovery or fallback logic</li>
	 * <li>Complex decision-making that requires full context</li>
	 * </ul>
	 * @param context The execution context
	 * @return A formatted string containing both context data summary and execution
	 * history
	 */
	default String getCombinedContext(JManusExecutionContext context) {
		return context.getCombinedContextView();
	}

	/**
	 * Convenience method to get a specific piece of data from previous steps. This is a
	 * shorthand for {@code context.get(key).orElse(defaultValue)}.
	 * @param <T> The type of the data
	 * @param context The execution context
	 * @param key The context key for the data
	 * @param defaultValue The default value if the key is not found
	 * @return The data associated with the key, or the default value if not found
	 */
	default <T> T getPreviousStepResult(JManusExecutionContext context, ContextKey<T> key, T defaultValue) {
		return context.get(key).orElse(defaultValue);
	}

	/**
	 * Convenience method to store data for subsequent steps. This is a shorthand for
	 * {@code context.put(key, value)}.
	 * @param <T> The type of the data
	 * @param context The execution context
	 * @param key The context key for the data
	 * @param value The data to store
	 * @return The previous value associated with the key, or null if none
	 */
	default <T> T setStepResult(JManusExecutionContext context, ContextKey<T> key, T value) {
		return context.put(key, value);
	}

	/**
	 * Indicates whether this step requires access to execution history. Steps that return
	 * true will ensure that the JManus execution context is properly initialized with
	 * plan execution information before execution.
	 * @return true if this step needs execution history access, false otherwise
	 */
	default boolean requiresExecutionHistory() {
		return false;
	}

}
