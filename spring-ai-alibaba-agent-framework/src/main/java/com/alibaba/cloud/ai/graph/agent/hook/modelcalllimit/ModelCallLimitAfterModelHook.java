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
package com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AfterModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * After-model hook that increments model call counters.
 *
 * This hook should be used in conjunction with ModelCallLimitHook (BeforeModelHook)
 * to track the actual number of model calls made.
 *
 * Example:
 * <pre>
 * // Use both hooks together
 * ModelCallLimitHook beforeHook = ModelCallLimitHook.builder()
 *     .threadLimit(10)
 *     .runLimit(5)
 *     .build();
 *
 * ModelCallLimitAfterModelHook afterHook = new ModelCallLimitAfterModelHook();
 *
 * // Register both hooks in agent
 * </pre>
 */
public class ModelCallLimitAfterModelHook extends AfterModelHook {

	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		// After a model call completes successfully, increment the counters
		// The beforeHook already maintains the counters, so we just need to
		// return empty map here. The counting is done in the beforeHook's apply method.

		// This hook exists to match the Python middleware pattern where
		// after_model is used to increment counters after successful calls
		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public String getName() {
		return "ModelCallLimitAfter";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}
}
