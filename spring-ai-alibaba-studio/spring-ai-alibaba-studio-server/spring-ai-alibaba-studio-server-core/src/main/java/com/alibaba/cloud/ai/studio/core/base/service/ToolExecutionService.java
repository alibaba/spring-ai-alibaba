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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolExecutionResult;

/**
 * Service interface for tool execution operations. Handles the execution of various tools
 * and API calls.
 *
 * @since 1.0.0.3
 */
public interface ToolExecutionService {

	/**
	 * Executes a tool based on the provided request.
	 * @param request The tool execution request containing necessary parameters
	 * @return The result of the tool execution
	 */
	ToolExecutionResult executeTool(ToolExecutionRequest request);

	/**
	 * Makes an OpenAPI call based on the provided request.
	 * @param request The API call request containing necessary parameters
	 * @return The result of the API call
	 */
	ToolExecutionResult callOpenApi(ToolExecutionRequest request);

}
