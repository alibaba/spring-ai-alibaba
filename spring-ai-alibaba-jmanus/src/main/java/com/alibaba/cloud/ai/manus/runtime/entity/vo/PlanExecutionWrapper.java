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

package com.alibaba.cloud.ai.manus.runtime.entity.vo;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper class that contains both PlanExecutionResult and rootPlanId Used for
 * asynchronous task tracking where frontend needs rootPlanId to monitor progress
 */
public class PlanExecutionWrapper {

	private final CompletableFuture<PlanExecutionResult> result;

	private final String rootPlanId;

	public PlanExecutionWrapper(CompletableFuture<PlanExecutionResult> result, String rootPlanId) {
		this.result = result;
		this.rootPlanId = rootPlanId;
	}

	public CompletableFuture<PlanExecutionResult> getResult() {
		return result;
	}

	public String getRootPlanId() {
		return rootPlanId;
	}

}
