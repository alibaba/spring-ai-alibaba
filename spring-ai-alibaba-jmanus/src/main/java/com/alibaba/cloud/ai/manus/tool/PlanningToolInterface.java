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
package com.alibaba.cloud.ai.manus.tool;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * Common interface for planning tools, defining basic behaviors of all planning tools
 */
public interface PlanningToolInterface {

	/**
	 * Get current plan ID
	 * @return Current plan ID, returns null if no plan exists
	 */
	String getCurrentPlanId();

	/**
	 * Get current execution plan
	 * @return Current execution plan, returns null if no plan exists
	 */
	PlanInterface getCurrentPlan();

	/**
	 * Get function tool callback for LLM integration
	 * @return FunctionToolCallback instance
	 */
	FunctionToolCallback<?, ToolExecuteResult> getFunctionToolCallback();

	/**
	 * Get function tool callback for LLM integration with specific planning tool instance
	 * @param planningToolInterface The planning tool instance to use
	 * @return FunctionToolCallback instance
	 */
	FunctionToolCallback<?, ToolExecuteResult> getFunctionToolCallback(PlanningToolInterface planningToolInterface);

}
