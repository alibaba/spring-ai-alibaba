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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

/**
 * 计划执行器接口 定义了执行计划的基本行为
 */
public interface PlanExecutorInterface {

	/**
	 * 执行整个计划的所有步骤
	 * @param context 执行上下文，包含用户请求和执行的过程信息
	 */
	void executeAllSteps(ExecutionContext context);

}
