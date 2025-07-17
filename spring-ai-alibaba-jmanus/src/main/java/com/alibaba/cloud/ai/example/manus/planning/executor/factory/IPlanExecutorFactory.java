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
package com.alibaba.cloud.ai.example.manus.planning.executor.factory;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;

/**
 * Interface for plan executor factory that creates executors for different plan types
 */
public interface IPlanExecutorFactory {

	/**
	 * Create executor for the given plan
	 */
	PlanExecutorInterface createExecutor(PlanInterface plan);

	/**
	 * Get all supported plan types
	 */
	String[] getSupportedPlanTypes();

	/**
	 * Check if a plan type is supported
	 */
	boolean isPlanTypeSupported(String planType);

	/**
	 * Create executor by plan type and ID
	 */
	PlanExecutorInterface createExecutorByType(String planType, String planId);

}
