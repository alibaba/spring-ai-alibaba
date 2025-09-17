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
package com.alibaba.cloud.ai.manus.planning.service;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;

/**
 * Interface for plan creation services
 */
public interface IPlanCreator {

	/**
	 * Create an execution plan with memory support
	 * @param context execution context, containing the user request and the execution
	 * process information
	 */
	void createPlanWithMemory(ExecutionContext context);

	/**
	 * Create an execution plan without memory support
	 * @param context execution context, containing the user request and the execution
	 * process information
	 */
	void createPlanWithoutMemory(ExecutionContext context);

}
