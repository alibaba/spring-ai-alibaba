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
package com.alibaba.cloud.ai.graph.agent.interceptor;

import org.springframework.ai.tool.ToolCallback;

import java.util.Collections;
import java.util.List;

/**
 * Model interceptor that can wrap model calls.
 * Implementations can modify requests, responses, or add behavior like retry, fallback, etc.
 */
public abstract class ModelInterceptor implements Interceptor {

	/**
	 * Wrap a model call with custom logic.
	 *
	 * Implementations can:
	 * - Modify the request before calling the handler
	 * - Call the handler multiple times (retry logic)
	 * - Modify the response after handler returns
	 * - Handle exceptions and provide fallbacks
	 *
	 * @param request The model request
	 * @param handler The next handler in the chain (or base handler)
	 * @return The model response
	 */
	public abstract ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler);

	/**
	 * Get tools provided by this interceptor.
	 * Interceptors can provide built-in tools that will be automatically added to the agent.
	 *
	 * @return List of tools provided by this interceptor, empty list by default
	 */
	public List<ToolCallback> getTools() {
		return Collections.emptyList();
	}
}
