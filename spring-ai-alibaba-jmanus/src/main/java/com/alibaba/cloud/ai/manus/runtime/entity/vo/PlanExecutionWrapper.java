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
