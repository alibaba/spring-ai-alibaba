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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Execution context for a tool call.
 * <p>
 * This context carries runtime information about where the tool call happens, such as
 * {@link RunnableConfig} and {@link OverAllState}, making it possible for
 * {@link ToolInterceptor} implementations to perform logging/auditing/observability etc.
 * without relying on loosely-typed maps.
 */
public final class ToolCallExecutionContext {

	private final RunnableConfig config;

	private final OverAllState state;

	public ToolCallExecutionContext(RunnableConfig config, OverAllState state) {
		this.config = requireNonNull(config, "config must not be null");
		this.state = requireNonNull(state, "state must not be null");
	}

	public RunnableConfig config() {
		return this.config;
	}

	public OverAllState state() {
		return this.state;
	}

	public Optional<String> threadId() {
		return this.config.threadId();
	}

	public Optional<String> checkpointId() {
		return this.config.checkPointId();
	}

}

