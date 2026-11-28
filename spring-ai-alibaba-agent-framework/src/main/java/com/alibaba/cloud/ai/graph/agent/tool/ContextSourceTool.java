/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tool;

import java.util.Objects;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Marks a search or read callback that must stay usable when other tools are selected. */
public record ContextSourceTool(ToolCallback delegate, int maxCallsPerTurn) implements StateAwareToolCallback {

	public ContextSourceTool(ToolCallback delegate) {
		this(delegate, Integer.MAX_VALUE);
	}

	public ContextSourceTool {
		Objects.requireNonNull(delegate, "delegate must not be null");
		if (maxCallsPerTurn < 1) {
			throw new IllegalArgumentException("maxCallsPerTurn must be positive");
		}
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return this.delegate.getToolMetadata();
	}

	@Override
	public String call(String input) {
		return this.delegate.call(input);
	}

	@Override
	public String call(String input, ToolContext context) {
		return this.delegate.call(input, context);
	}

}
