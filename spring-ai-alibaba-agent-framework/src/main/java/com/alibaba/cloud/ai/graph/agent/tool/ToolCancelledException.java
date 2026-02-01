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
package com.alibaba.cloud.ai.graph.agent.tool;

/**
 * Exception thrown when a tool execution is cancelled.
 *
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>A {@link CancellationToken} is triggered during tool execution</li>
 *   <li>A tool's {@link java.util.concurrent.CompletableFuture} is cancelled</li>
 *   <li>A tool explicitly checks for cancellation and aborts</li>
 * </ul>
 *
 * @author disaster
 * @since 1.0.0
 * @see CancellationToken
 * @see AsyncToolCallback
 */
public class ToolCancelledException extends RuntimeException {

	public ToolCancelledException(String message) {
		super(message);
	}

	public ToolCancelledException(String message, Throwable cause) {
		super(message, cause);
	}

}
