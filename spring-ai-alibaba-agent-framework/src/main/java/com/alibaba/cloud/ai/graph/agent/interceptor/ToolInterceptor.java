/*
 * Copyright 2024-2026 the original author or authors.
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

/**
 * Tool interceptor that can wrap tool calls.
 * Implementations can modify requests, responses, or add behavior like retry, caching, etc.
 */
public abstract class ToolInterceptor implements Interceptor {

	/**
	 * Wrap a tool call with custom logic.
	 *
	 * Implementations can:
	 * - Modify the request before calling the handler
	 * - Call the handler multiple times (retry logic)
	 * - Modify the response after handler returns
	 * - Add caching, logging, monitoring, etc.
	 *
	 * @param request The tool call request
	 * @param handler The next handler in the chain (or base handler)
	 * @return The tool call response
	 */
	public abstract ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler);
}
