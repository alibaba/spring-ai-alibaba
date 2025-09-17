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
package com.alibaba.cloud.ai.manus.runtime.executor;

import java.util.concurrent.CompletableFuture;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;

/**
 * Plan executor interface defining basic behaviors for plan execution
 */
public interface PlanExecutorInterface {

	/**
	 * Execute all steps of the entire plan
	 * @param context Execution context containing user request and execution process
	 * information
	 */
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context);

}
